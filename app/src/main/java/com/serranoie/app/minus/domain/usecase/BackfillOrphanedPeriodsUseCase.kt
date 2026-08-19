package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.splitRecurringAndOneTime
import kotlinx.coroutines.flow.first
import logcat.logcat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

internal data class BackfillWindow(
    val startDate: LocalDate,
    val endDate: LocalDate,
)

internal fun computeBackfillWindows(
    currentPeriodStart: LocalDate,
    windowDays: Long,
    earliestOrphanedDate: LocalDate,
): List<BackfillWindow> {
    if (windowDays <= 0) return emptyList()

    val windows = mutableListOf<BackfillWindow>()
    var windowEnd = currentPeriodStart.minusDays(1)
    while (!windowEnd.isBefore(earliestOrphanedDate)) {
        val windowStart = windowEnd.minusDays(windowDays - 1)
        windows.add(BackfillWindow(windowStart, windowEnd))
        windowEnd = windowStart.minusDays(1)
    }
    return windows
}

/**
 * One-time migration for transactions that predate real period tracking (periodId <= 0, e.g.
 * bulk CSV imports from before this app assigned periods). Slices them into real,
 * cadence-aligned period windows, persists each as a genuine ArchivedBudget row using the same
 * recurring-charge accounting as everywhere else, and reassigns those transactions' periodId —
 * so from then on there's nothing left to reconstruct on the fly, and export/import round-trips
 * cleanly.
 */
class BackfillOrphanedPeriodsUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke() {
        if (settingsRepository.getString(BACKFILL_DONE_KEY) != null) return

        val settings = budgetRepository.getBudgetSettingsSync()
        if (settings == null) {
            logcat(TAG) { "no budget settings yet, skipping (will retry next launch)" }
            return
        }

        val allTransactions = budgetRepository.getTransactions().first()
        val orphaned = allTransactions.filter { it.periodId <= 0L && !it.isDeleted }
        if (orphaned.isEmpty()) {
            logcat(TAG) { "no orphaned transactions, nothing to backfill" }
            settingsRepository.setString(BACKFILL_DONE_KEY, "true")
            return
        }

        val earliestDate = orphaned.mapNotNull { it.date?.toLocalDate() }.minOrNull()
        if (earliestDate == null) {
            settingsRepository.setString(BACKFILL_DONE_KEY, "true")
            return
        }

        val existingArchives = budgetRepository.getArchivedBudgets().first()
        val paidOccurrences = budgetRepository.getPaidRecurrentOccurrences().first()
        val windowDays = settings.getDaysForPeriod().toLong()
        val dailyBudget = settings.calculateDailyBudget()

        val windows = computeBackfillWindows(
            currentPeriodStart = settings.startDate,
            windowDays = windowDays,
            earliestOrphanedDate = earliestDate,
        )

        val newArchives = mutableListOf<ArchivedBudget>()
        val transactionUpdates = mutableListOf<Transaction>()

        for (window in windows) {
            // Never clobber a window that already overlaps a real archived period.
            val overlapsExisting = existingArchives.any { existing ->
                window.startDate <= existing.endDate && window.endDate >= existing.startDate
            }
            if (overlapsExisting) continue

            val windowOrphaned = orphaned.filter { tx ->
                val date = tx.date?.toLocalDate() ?: return@filter false
                !date.isBefore(window.startDate) && !date.isAfter(window.endDate)
            }
            if (windowOrphaned.isEmpty()) continue

            val periodId = window.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val (paidRecurring, _, oneTimeSpends) = splitRecurringAndOneTime(
                allTransactions = allTransactions,
                filteredTransactions = windowOrphaned,
                periodStart = window.startDate,
                periodEnd = window.endDate,
                today = window.endDate,
                paidOccurrences = paidOccurrences,
            )
            val spentAmount = (oneTimeSpends + paidRecurring).distinctBy { it.id }.sumOf { it.amount }

            newArchives.add(
                ArchivedBudget(
                    periodId = periodId,
                    totalBudget = dailyBudget.multiply(BigDecimal(windowDays)),
                    spentAmount = spentAmount,
                    startDate = window.startDate,
                    endDate = window.endDate,
                    currencyCode = settings.currencyCode,
                    periodType = settings.period,
                )
            )
            windowOrphaned.forEach { tx -> transactionUpdates.add(tx.copy(periodId = periodId)) }
        }

        if (newArchives.isNotEmpty()) {
            budgetRepository.upsertArchivedBudgets(newArchives)
        }
        if (transactionUpdates.isNotEmpty()) {
            budgetRepository.upsertTransactions(transactionUpdates)
        }

        logcat(TAG) { "backfilled ${newArchives.size} periods from ${transactionUpdates.size} orphaned transactions" }
        settingsRepository.setString(BACKFILL_DONE_KEY, "true")
    }

    companion object {
        private const val TAG = "BackfillOrphanedPeriods"
        const val BACKFILL_DONE_KEY = "orphaned_periods_backfilled"
    }
}
