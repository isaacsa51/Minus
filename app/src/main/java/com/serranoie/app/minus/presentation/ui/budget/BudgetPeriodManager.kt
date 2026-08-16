package com.serranoie.app.minus.presentation.ui.budget

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.time.MidnightPeriodChecker
import com.serranoie.app.minus.domain.time.TimeProvider
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class PeriodBoundaryResult(
    val periodStartMillis: Long,
    val periodId: Long,
)

class BudgetPeriodManager @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
    private val notificationScheduler: NotificationScheduler,
    private val midnightPeriodChecker: MidnightPeriodChecker,
) {

    suspend fun updatePeriodEndNotificationTime(hour: Int, minute: Int) {
        settingsRepository.setNotificationTime(hour, minute)
        budgetRepository.getBudgetSettingsSync()?.let { settings ->
            notificationScheduler.schedulePeriodEndNotification(settings.getPeriodEndDate())
        }
    }

    suspend fun updateRecurrentNotificationTime(hour: Int, minute: Int) {
        settingsRepository.setRecurrentNotificationTime(hour, minute)
        notificationScheduler.rescheduleRecurrentExpenseNotifications()
    }

    suspend fun finishBudgetEarly() {
        val settings = budgetRepository.getBudgetSettingsSync() ?: return
        val originalEndDate = settings.getPeriodEndDate()
        val now = LocalDate.now()

        settingsRepository.setEarlyFinishActive(
            active = true,
            actualDate = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            originalEndDate = originalEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                .toEpochMilli()
        )

        val remainingAmount = computeRemainingAmount(settings, now)
        midnightPeriodChecker.handleEarlyFinish(settings, remainingAmount)
    }

    private suspend fun computeRemainingAmount(settings: BudgetSettings, today: LocalDate): BigDecimal {
        val transactions = budgetRepository.getTransactions().firstOrNull() ?: emptyList()
        val totalSpent = transactions
            .filter { tx ->
                val txDate = tx.date?.toLocalDate()
                !tx.isDeleted && txDate != null && !txDate.isBefore(settings.startDate) && !txDate.isAfter(today)
            }
            .sumOf { it.amount }
        return settings.totalBudget.subtract(totalSpent)
    }

    suspend fun clearEarlyFinishState() {
        settingsRepository.clearEarlyFinish()
    }

    suspend fun persistBudgetSettings(
        settings: BudgetSettings,
        forceNewPeriodBoundary: Boolean,
    ): PeriodBoundaryResult {
        val userSettings = settingsRepository.getSettings()
        val previousSettings = budgetRepository.getBudgetSettingsSync()

        // No UI call site actually passes forceNewPeriodBoundary=true today (the
        // "New Budget Period" screen after a finish-early/natural period end reuses
        // the same generic "save settings" callback as a mid-period edit). The real,
        // reliable signal that this save is starting a new period rather than
        // tweaking the active one is the start date changing — archiving and
        // rollover application must key off the SAME condition that already
        // resets the period boundary below, or they silently never fire.
        val isNewPeriodBoundary =
            forceNewPeriodBoundary || previousSettings == null || previousSettings.startDate != settings.startDate

        val (pendingRolloverAmount, pendingRolloverStrategy) = settingsRepository.getPendingRollover()

        val shouldApplyPendingRollover =
            isNewPeriodBoundary && pendingRolloverAmount > BigDecimal.ZERO
        val appliedRolloverAmount =
            if (shouldApplyPendingRollover) pendingRolloverAmount else BigDecimal.ZERO
        val appliedCarryForward =
            shouldApplyPendingRollover && pendingRolloverStrategy == RemainingBudgetStrategy.ADD_TO_FIRST_DAY

        val effectiveSettings = if (shouldApplyPendingRollover && pendingRolloverStrategy != null) {
            when (pendingRolloverStrategy) {
                RemainingBudgetStrategy.SPLIT_EQUALLY -> settings.copy(
                    totalBudget = settings.totalBudget.add(pendingRolloverAmount),
                    rollOverCarryForward = false,
                    rollOverLimit = pendingRolloverAmount,
                )

                RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> settings.copy(
                    rollOverCarryForward = true,
                    rollOverLimit = pendingRolloverAmount,
                )

                RemainingBudgetStrategy.ASK_ALWAYS -> settings
            }
        } else {
            settings
        }

        clearEarlyFinishState()
        val previousPeriodId = userSettings.currentPeriodId

        if (isNewPeriodBoundary && previousSettings != null && previousPeriodId != 0L) {
            val actualEndDate = if (userSettings.earlyFinishActive && userSettings.earlyFinishActualDate > 0L) {
                Instant.ofEpochMilli(userSettings.earlyFinishActualDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            } else {
                null
            }
            archivePeriod(previousPeriodId, previousSettings, actualEndDate)
        }

        budgetRepository.saveBudgetSettings(effectiveSettings)

        val periodStartMillis = if (isNewPeriodBoundary) {
            timeProvider.nowEpochMillis()
        } else {
            val existingStart = userSettings.currentPeriodStartedAt
            if (existingStart != 0L) existingStart
            else effectiveSettings.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                .toEpochMilli()
        }
        val periodId = if (isNewPeriodBoundary) {
            periodStartMillis
        } else {
            val existingId = userSettings.currentPeriodId
            if (existingId != 0L) existingId else periodStartMillis
        }

        val periodEndDate = effectiveSettings.getPeriodEndDate()
        val millis = periodEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        settingsRepository.setBudgetEndDate(millis)
        settingsRepository.setCurrentPeriod(periodId, periodStartMillis)
        settingsRepository.setCurrentPeriodRollover(appliedRolloverAmount, appliedCarryForward)

        if (shouldApplyPendingRollover) {
            settingsRepository.clearPendingRollover()
        }
        if (isNewPeriodBoundary) {
            settingsRepository.clearLastPeriodSnapshot()
        }

        if (isNewPeriodBoundary) {
            budgetRepository.assignQueuedTransactionsToPeriod(periodId)
        }

        notificationScheduler.schedulePeriodEndNotification(periodEndDate)
        return PeriodBoundaryResult(periodStartMillis = periodStartMillis, periodId = periodId)
    }

    private suspend fun archivePeriod(periodId: Long, settings: BudgetSettings, actualEndDate: LocalDate?) {
        val transactions = budgetRepository.getTransactions().firstOrNull() ?: emptyList()
        val periodTransactions = transactions.filter {
            it.periodId == periodId && !it.isDeleted
        }
        val totalSpent = periodTransactions.sumOf { it.amount }
        val archivedSettings = if (actualEndDate != null) settings.copy(endDate = actualEndDate) else settings
        budgetRepository.archiveCurrentPeriod(periodId, archivedSettings, totalSpent)
    }
}
