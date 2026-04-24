package com.serranoie.app.minus.domain.time

import android.content.Context
import logcat.logcat
import androidx.datastore.preferences.core.edit
import com.serranoie.app.minus.BUDGET_END_DATE_KEY
import com.serranoie.app.minus.CURRENT_PERIOD_ID_KEY
import com.serranoie.app.minus.CURRENT_PERIOD_STARTED_AT_KEY
import com.serranoie.app.minus.DEFAULT_NOTIFICATION_HOUR
import com.serranoie.app.minus.DEFAULT_NOTIFICATION_MINUTE
import com.serranoie.app.minus.LAST_PERIOD_END_KEY
import com.serranoie.app.minus.MIDNIGHT_TRANSITION_OCCURRED_KEY
import com.serranoie.app.minus.NOTIFICATION_HOUR_KEY
import com.serranoie.app.minus.NOTIFICATION_MINUTE_KEY
import com.serranoie.app.minus.REMAINING_FROM_LAST_PERIOD_KEY
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.settingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

data class MidnightTransitionData(
	val periodStartDate: LocalDate,
	val periodEndDate: LocalDate,
	val totalBudget: BigDecimal,
	val remainingAmount: BigDecimal,
	val totalSpent: BigDecimal,
	val currencyCode: String
)

@Singleton
class MidnightPeriodChecker @Inject constructor(
	@ApplicationContext private val context: Context,
	private val budgetRepository: BudgetRepository,
	private val notificationScheduler: NotificationScheduler,
	private val timeProvider: TimeProvider
) {
	private val _midnightTransitionData = MutableStateFlow<MidnightTransitionData?>(null)
	val midnightTransitionData: StateFlow<MidnightTransitionData?> =
		_midnightTransitionData.asStateFlow()

	private val _shouldShowTransitionDialog = MutableStateFlow(false)
	val shouldShowTransitionDialog: StateFlow<Boolean> = _shouldShowTransitionDialog.asStateFlow()

	companion object {
	}

	suspend fun checkMidnightTransition() {
		val prefs = context.settingsDataStore.data.first()
		val transitionOccurred = prefs[MIDNIGHT_TRANSITION_OCCURRED_KEY] ?: false

		val endDateMillis = prefs[BUDGET_END_DATE_KEY]
		val periodEndedBasedOnDate = endDateMillis?.let { millis ->
			val endDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
			LocalDate.now().isAfter(endDate) || LocalDate.now().isEqual(endDate)
		} ?: false

		if (!transitionOccurred && !periodEndedBasedOnDate) {
			logcat { "No midnight transition detected" }
			return
		}

		context.settingsDataStore.edit { it[MIDNIGHT_TRANSITION_OCCURRED_KEY] = false }

		val lastPeriodEndMillis = prefs[LAST_PERIOD_END_KEY] ?: endDateMillis
		if (lastPeriodEndMillis == null) {
			logcat { "No period end date found" }
			return
		}

		val remainingStr = if (transitionOccurred) {
			prefs[REMAINING_FROM_LAST_PERIOD_KEY] ?: "0"
		} else {
			"0"
		}
		val remaining = BigDecimal(remainingStr)

		val settings = budgetRepository.getBudgetSettingsSync() ?: run {
			logcat { "No budget settings found" }
			return
		}

		val periodEndDate =
			Instant.ofEpochMilli(lastPeriodEndMillis).atZone(ZoneId.systemDefault()).toLocalDate()
		val daysInPeriod =
			ChronoUnit.DAYS.between(settings.startDate, settings.getPeriodEndDate()) + 1
		val periodStartDate = periodEndDate.minusDays(daysInPeriod - 1)

		val totalSpent = if (transitionOccurred) {
			settings.totalBudget.subtract(remaining)
		} else {
			settings.totalBudget
		}

		_midnightTransitionData.value = MidnightTransitionData(
			periodStartDate = periodStartDate,
			periodEndDate = periodEndDate,
			totalBudget = settings.totalBudget,
			remainingAmount = remaining,
			totalSpent = totalSpent,
			currencyCode = settings.currencyCode
		)

		_shouldShowTransitionDialog.value = true
		logcat { "Midnight transition detected, showing dialog" }
	}

	fun onTransitionDialogConfirmed() {
		_shouldShowTransitionDialog.value = false
		_midnightTransitionData.value = null
	}

	fun onTransitionDialogDismissed() {
		_shouldShowTransitionDialog.value = false
		_midnightTransitionData.value = null
	}

	suspend fun rollRemainingSplitEqually() {
		val data = _midnightTransitionData.value ?: return
		val settings = budgetRepository.getBudgetSettingsSync() ?: return
		val updatedSettings = settings.copy(
			totalBudget = settings.totalBudget.add(data.remainingAmount),
			startDate = LocalDate.now(),
			rollOverCarryForward = false,
			rollOverLimit = null,
			remainingBudgetStrategy = RemainingBudgetStrategy.SPLIT_EQUALLY
		)
		persistBudgetSettings(updatedSettings, forceNewPeriodBoundary = true)
		onTransitionDialogConfirmed()
	}

	suspend fun rollRemainingToFirstDay() {
		val data = _midnightTransitionData.value ?: return
		val settings = budgetRepository.getBudgetSettingsSync() ?: return
		val updatedSettings = settings.copy(
			startDate = LocalDate.now(),
			rollOverCarryForward = true,
			rollOverLimit = data.remainingAmount,
			remainingBudgetStrategy = RemainingBudgetStrategy.ADD_TO_FIRST_DAY
		)
		persistBudgetSettings(updatedSettings, forceNewPeriodBoundary = true)
		onTransitionDialogConfirmed()
	}

	private suspend fun persistBudgetSettings(
		settings: com.serranoie.app.minus.domain.model.BudgetSettings,
		forceNewPeriodBoundary: Boolean = false,
	) {
		val previousSettings = budgetRepository.getBudgetSettingsSync()
		val previousPrefs = context.settingsDataStore.data.first()
		budgetRepository.saveBudgetSettings(settings)
		val shouldCreateNewPeriodBoundary = forceNewPeriodBoundary ||
			previousSettings == null ||
			previousSettings.startDate != settings.startDate
		val periodStartMillis = if (shouldCreateNewPeriodBoundary) {
			timeProvider.nowEpochMillis()
		} else {
			previousPrefs[CURRENT_PERIOD_STARTED_AT_KEY]
				?: settings.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
		}
		val periodId = if (shouldCreateNewPeriodBoundary) {
			periodStartMillis
		} else {
			previousPrefs[CURRENT_PERIOD_ID_KEY] ?: periodStartMillis
		}
		val periodEndDate = settings.getPeriodEndDate()
		val millis = periodEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
		context.settingsDataStore.edit { prefs ->
			prefs[BUDGET_END_DATE_KEY] = millis
			prefs[CURRENT_PERIOD_STARTED_AT_KEY] = periodStartMillis
			prefs[CURRENT_PERIOD_ID_KEY] = periodId
			if (!prefs.contains(NOTIFICATION_HOUR_KEY)) {
				prefs[NOTIFICATION_HOUR_KEY] = DEFAULT_NOTIFICATION_HOUR
			}
			if (!prefs.contains(NOTIFICATION_MINUTE_KEY)) {
				prefs[NOTIFICATION_MINUTE_KEY] = DEFAULT_NOTIFICATION_MINUTE
			}
		}
		notificationScheduler.schedulePeriodEndNotification(periodEndDate)
	}

	fun reset() {
		_midnightTransitionData.value = null
		_shouldShowTransitionDialog.value = false
	}
}
