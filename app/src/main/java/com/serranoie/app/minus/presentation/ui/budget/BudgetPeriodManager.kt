package com.serranoie.app.minus.presentation.ui.budget

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.time.MidnightPeriodChecker
import com.serranoie.app.minus.domain.time.TimeProvider
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
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
        notificationScheduler.scheduleCreditCutoffReminder()
    }

    suspend fun finishBudgetEarly() = withContext(NonCancellable) {
        val settings = budgetRepository.getBudgetSettingsSync() ?: return@withContext
        val originalEndDate = settings.getPeriodEndDate()
        val now = LocalDate.now()

        settingsRepository.setEarlyFinishActive(
            active = true,
            actualDate = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            originalEndDate = originalEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                .toEpochMilli()
        )

        settingsRepository.setPeriodEndAlreadyHandled(true)

        val periodId = settingsRepository.getSettings().currentPeriodId
        val remainingAmount = settings.totalBudget.subtract(
            midnightPeriodChecker.periodSpent(periodId, settings, now)
        )
        midnightPeriodChecker.handleEarlyFinish(settings, remainingAmount)
    }

    suspend fun clearEarlyFinishState() {
        val s = settingsRepository.getSettings()
        settingsRepository.setEarlyFinishActive(
            false,
            s.earlyFinishActualDate,
            s.earlyFinishOriginalEndDate
        )
    }

    suspend fun persistBudgetSettings(
        settings: BudgetSettings,
        forceNewPeriodBoundary: Boolean,
    ): PeriodBoundaryResult = withContext(NonCancellable) {
        val userSettings = settingsRepository.getSettings()
        val previousSettings = budgetRepository.getBudgetSettingsSync()

        val previousPeriodOver = previousSettings != null && (
                userSettings.earlyFinishActive ||
                        userSettings.periodEndAlreadyHandled ||
                        LocalDate.now().isAfter(previousSettings.getPeriodEndDate())
                )
        val isNewPeriodBoundary = forceNewPeriodBoundary || previousSettings == null ||
                (previousPeriodOver && previousSettings.startDate != settings.startDate)

        val (pendingRolloverAmount, pendingRolloverStrategy) = settingsRepository.getPendingRollover()

        val previousPeriodId = userSettings.currentPeriodId
        val archivedSpent =
            if (isNewPeriodBoundary && previousSettings != null && previousPeriodId != 0L) {
                val actualEndDate = if (userSettings.earlyFinishActualDate > 0L) {
                    Instant.ofEpochMilli(userSettings.earlyFinishActualDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                } else {
                    null
                }
                archivePeriod(previousPeriodId, previousSettings, actualEndDate, settings.startDate)
            } else {
                null
            }

        val rolloverAmount = when {
            !isNewPeriodBoundary || pendingRolloverStrategy == null -> BigDecimal.ZERO
            archivedSpent != null && previousSettings != null ->
                previousSettings.totalBudget.subtract(archivedSpent)

            else -> pendingRolloverAmount
        }
        val shouldApplyPendingRollover = rolloverAmount > BigDecimal.ZERO
        val appliedRolloverAmount = if (shouldApplyPendingRollover) rolloverAmount else BigDecimal.ZERO
        val appliedCarryForward =
            shouldApplyPendingRollover && pendingRolloverStrategy == RemainingBudgetStrategy.ADD_TO_FIRST_DAY

        val effectiveSettings = if (shouldApplyPendingRollover) {
            when (pendingRolloverStrategy) {
                RemainingBudgetStrategy.SPLIT_EQUALLY -> settings.copy(
                    totalBudget = settings.totalBudget.add(rolloverAmount),
                    rollOverCarryForward = false,
                    rollOverLimit = rolloverAmount,
                )

                RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> settings.copy(
                    totalBudget = settings.totalBudget.add(rolloverAmount),
                    rollOverCarryForward = true,
                    rollOverLimit = rolloverAmount,
                    rollOverAppliedDate = maxOf(settings.startDate, LocalDate.now()),
                )

                null, RemainingBudgetStrategy.ASK_ALWAYS -> settings
            }
        } else {
            settings
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

        if (isNewPeriodBoundary && pendingRolloverStrategy != null) {
            settingsRepository.clearPendingRollover()
        }
        if (isNewPeriodBoundary) {
            settingsRepository.setCurrentPeriodRollover(appliedRolloverAmount, appliedCarryForward)
            settingsRepository.clearLastPeriodSnapshot()
            settingsRepository.setPeriodEndAlreadyHandled(false)
            settingsRepository.clearEarlyFinish()
        }

        if (isNewPeriodBoundary) {
            budgetRepository.assignQueuedTransactionsToPeriod(periodId)
        }

        notificationScheduler.schedulePeriodEndNotification(periodEndDate)
        notificationScheduler.scheduleCreditCutoffReminder()
        PeriodBoundaryResult(periodStartMillis = periodStartMillis, periodId = periodId)
    }

    private suspend fun archivePeriod(
        periodId: Long,
        settings: BudgetSettings,
        actualEndDate: LocalDate?,
        nextStartDate: LocalDate
    ): BigDecimal {
        val endDate = minOf(actualEndDate ?: settings.getPeriodEndDate(), nextStartDate.minusDays(1))
            .coerceAtLeast(settings.startDate)
        val archivedSettings = settings.copy(endDate = endDate)
        val totalSpent = midnightPeriodChecker.periodSpent(periodId, archivedSettings, endDate)
        budgetRepository.archiveCurrentPeriod(periodId, archivedSettings, totalSpent)
        return totalSpent
    }
}
