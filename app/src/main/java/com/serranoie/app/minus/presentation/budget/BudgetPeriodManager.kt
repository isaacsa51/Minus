package com.serranoie.app.minus.presentation.budget

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.serranoie.app.minus.BUDGET_END_DATE_KEY
import com.serranoie.app.minus.CURRENT_PERIOD_ID_KEY
import com.serranoie.app.minus.CURRENT_PERIOD_STARTED_AT_KEY
import com.serranoie.app.minus.DEFAULT_NOTIFICATION_HOUR
import com.serranoie.app.minus.DEFAULT_NOTIFICATION_MINUTE
import com.serranoie.app.minus.EARLY_FINISH_ACTIVE_KEY
import com.serranoie.app.minus.EARLY_FINISH_ACTUAL_DATE_KEY
import com.serranoie.app.minus.EARLY_FINISH_ORIGINAL_END_DATE_KEY
import com.serranoie.app.minus.NOTIFICATION_HOUR_KEY
import com.serranoie.app.minus.NOTIFICATION_MINUTE_KEY
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.time.CURRENT_PERIOD_ROLLOVER_AMOUNT_KEY
import com.serranoie.app.minus.domain.time.CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD_KEY
import com.serranoie.app.minus.domain.time.PENDING_ROLLOVER_AMOUNT_KEY
import com.serranoie.app.minus.domain.time.PENDING_ROLLOVER_STRATEGY_KEY
import com.serranoie.app.minus.domain.time.TimeProvider
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.settingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class PeriodBoundaryResult(
	val periodStartMillis: Long,
	val periodId: Long,
)

class BudgetPeriodManager @Inject constructor(
	@ApplicationContext private val context: Context,
	private val budgetRepository: BudgetRepository,
	private val timeProvider: TimeProvider,
	private val notificationScheduler: NotificationScheduler,
) {

	suspend fun updatePeriodEndNotificationTime(hour: Int, minute: Int) {
		context.settingsDataStore.edit { prefs ->
			prefs[NOTIFICATION_HOUR_KEY] = hour
			prefs[NOTIFICATION_MINUTE_KEY] = minute
		}
		budgetRepository.getBudgetSettingsSync()?.let { settings ->
			notificationScheduler.schedulePeriodEndNotification(settings.getPeriodEndDate())
		}
	}

	suspend fun finishBudgetEarly() {
		val settings = budgetRepository.getBudgetSettingsSync() ?: return
		val originalEndDate = settings.getPeriodEndDate()
		val now = LocalDate.now()

		context.settingsDataStore.edit { prefs ->
			prefs[EARLY_FINISH_ACTIVE_KEY] = true
			prefs[EARLY_FINISH_ACTUAL_DATE_KEY] =
				now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
			prefs[EARLY_FINISH_ORIGINAL_END_DATE_KEY] =
				originalEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
		}
	}

	suspend fun clearEarlyFinishState() {
		context.settingsDataStore.edit { prefs ->
			prefs[EARLY_FINISH_ACTIVE_KEY] = false
			prefs.remove(EARLY_FINISH_ACTUAL_DATE_KEY)
			prefs.remove(EARLY_FINISH_ORIGINAL_END_DATE_KEY)
		}
	}

	suspend fun persistBudgetSettings(
		settings: BudgetSettings,
		forceNewPeriodBoundary: Boolean,
	): PeriodBoundaryResult {
		val previousPrefs = context.settingsDataStore.data.first()
		val pendingRolloverAmount = previousPrefs[PENDING_ROLLOVER_AMOUNT_KEY]?.toBigDecimalOrNull()
		val pendingRolloverStrategy = previousPrefs[PENDING_ROLLOVER_STRATEGY_KEY]?.let {
			runCatching { RemainingBudgetStrategy.valueOf(it) }.getOrNull()
		}
		val shouldApplyPendingRollover =
			forceNewPeriodBoundary && pendingRolloverAmount != null && pendingRolloverAmount > BigDecimal.ZERO
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
		budgetRepository.saveBudgetSettings(effectiveSettings)

		val shouldCreateNewPeriodBoundary =
			forceNewPeriodBoundary || previousSettings == null || previousSettings.startDate != effectiveSettings.startDate

		val periodStartMillis = if (shouldCreateNewPeriodBoundary) {
			timeProvider.nowEpochMillis()
		} else {
			previousPrefs[CURRENT_PERIOD_STARTED_AT_KEY]
				?: effectiveSettings.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
					.toEpochMilli()
		}
		val periodId = if (shouldCreateNewPeriodBoundary) {
			periodStartMillis
		} else {
			previousPrefs[CURRENT_PERIOD_ID_KEY] ?: periodStartMillis
		}

		val periodEndDate = effectiveSettings.getPeriodEndDate()
		val millis = periodEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

		context.settingsDataStore.edit { prefs ->
			prefs[BUDGET_END_DATE_KEY] = millis
			prefs[CURRENT_PERIOD_STARTED_AT_KEY] = periodStartMillis
			prefs[CURRENT_PERIOD_ID_KEY] = periodId
			prefs[CURRENT_PERIOD_ROLLOVER_AMOUNT_KEY] = appliedRolloverAmount.toPlainString()
			prefs[CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD_KEY] = appliedCarryForward
			if (shouldApplyPendingRollover) {
				prefs.remove(PENDING_ROLLOVER_AMOUNT_KEY)
				prefs.remove(PENDING_ROLLOVER_STRATEGY_KEY)
			}
			if (!prefs.contains(NOTIFICATION_HOUR_KEY)) {
				prefs[NOTIFICATION_HOUR_KEY] = DEFAULT_NOTIFICATION_HOUR
			}
			if (!prefs.contains(NOTIFICATION_MINUTE_KEY)) {
				prefs[NOTIFICATION_MINUTE_KEY] = DEFAULT_NOTIFICATION_MINUTE
			}
		}

		if (shouldCreateNewPeriodBoundary) {
			budgetRepository.assignQueuedTransactionsToPeriod(periodId)
		}

		notificationScheduler.schedulePeriodEndNotification(periodEndDate)
		return PeriodBoundaryResult(periodStartMillis = periodStartMillis, periodId = periodId)
	}
}
