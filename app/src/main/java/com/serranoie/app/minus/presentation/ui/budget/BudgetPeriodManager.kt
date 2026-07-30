package com.serranoie.app.minus.presentation.ui.budget

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.time.TimeProvider
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigDecimal
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
	}

	suspend fun clearEarlyFinishState() {
		settingsRepository.clearEarlyFinish()
	}

	suspend fun persistBudgetSettings(
		settings: BudgetSettings,
		forceNewPeriodBoundary: Boolean,
	): PeriodBoundaryResult {
		val userSettings = settingsRepository.getSettings()
		val (pendingRolloverAmount, pendingRolloverStrategy) = settingsRepository.getPendingRollover()

		val shouldApplyPendingRollover =
			forceNewPeriodBoundary && pendingRolloverAmount > BigDecimal.ZERO
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
		val previousSettings = budgetRepository.getBudgetSettingsSync()
		val previousPeriodId = userSettings.currentPeriodId

		if (forceNewPeriodBoundary && previousSettings != null && previousPeriodId != 0L) {
			archivePeriod(previousPeriodId, previousSettings)
		}

		budgetRepository.saveBudgetSettings(effectiveSettings)

		val shouldCreateNewPeriodBoundary =
			forceNewPeriodBoundary || previousSettings == null || previousSettings.startDate != effectiveSettings.startDate

		val periodStartMillis = if (shouldCreateNewPeriodBoundary) {
			timeProvider.nowEpochMillis()
		} else {
			val existingStart = userSettings.currentPeriodStartedAt
			if (existingStart != 0L) existingStart
			else effectiveSettings.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
				.toEpochMilli()
		}
		val periodId = if (shouldCreateNewPeriodBoundary) {
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
		if (shouldCreateNewPeriodBoundary) {
			settingsRepository.clearLastPeriodSnapshot()
		}

		if (shouldCreateNewPeriodBoundary) {
			budgetRepository.assignQueuedTransactionsToPeriod(periodId)
		}

		notificationScheduler.schedulePeriodEndNotification(periodEndDate)
		return PeriodBoundaryResult(periodStartMillis = periodStartMillis, periodId = periodId)
	}

	private suspend fun archivePeriod(periodId: Long, settings: BudgetSettings) {
		val transactions = budgetRepository.getTransactions().firstOrNull() ?: emptyList()
		val periodTransactions = transactions.filter {
			it.periodId == periodId && !it.isDeleted
		}
		val totalSpent = periodTransactions.sumOf { it.amount }
		budgetRepository.archiveCurrentPeriod(periodId, settings, totalSpent)
	}
}
