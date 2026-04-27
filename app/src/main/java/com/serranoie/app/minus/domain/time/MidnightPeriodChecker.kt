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
	val currencyCode: String,
	val shouldNavigateToAnalyticsOnly: Boolean = false,
)

@Singleton
class MidnightPeriodChecker @Inject constructor(
	@ApplicationContext private val context: Context,
	private val budgetRepository: BudgetRepository,
	private val notificationScheduler: NotificationScheduler,
	private val timeProvider: TimeProvider
) {
	data class EndingPeriodState(
		val shouldHandleEndingPeriod: Boolean,
		val transitionOccurred: Boolean,
		val periodEndDate: LocalDate?,
		val remainingAmount: BigDecimal,
	)

	private val _midnightTransitionData = MutableStateFlow<MidnightTransitionData?>(null)
	val midnightTransitionData: StateFlow<MidnightTransitionData?> =
		_midnightTransitionData.asStateFlow()

	private val _shouldShowTransitionDialog = MutableStateFlow(false)
	val shouldShowTransitionDialog: StateFlow<Boolean> = _shouldShowTransitionDialog.asStateFlow()

	companion object {
	}

	suspend fun handleEndingPeriod() {
		val endingPeriodState = resolveEndingPeriodState()
		if (!endingPeriodState.shouldHandleEndingPeriod) {
			logcat { "No ending period to handle" }
			return
		}

		context.settingsDataStore.edit { it[MIDNIGHT_TRANSITION_OCCURRED_KEY] = false }

		val lastPeriodEndDate = endingPeriodState.periodEndDate ?: run {
			logcat { "No period end date found" }
			return
		}

		val settings = budgetRepository.getBudgetSettingsSync() ?: run {
			logcat { "No budget settings found" }
			return
		}

		if (endingPeriodState.remainingAmount <= BigDecimal.ZERO) {
			val daysInPeriod =
				ChronoUnit.DAYS.between(settings.startDate, settings.getPeriodEndDate()) + 1
			val periodStartDate = lastPeriodEndDate.minusDays(daysInPeriod - 1)
			_midnightTransitionData.value = MidnightTransitionData(
				periodStartDate = periodStartDate,
				periodEndDate = lastPeriodEndDate,
				totalBudget = settings.totalBudget,
				remainingAmount = endingPeriodState.remainingAmount,
				totalSpent = settings.totalBudget,
				currencyCode = settings.currencyCode,
				shouldNavigateToAnalyticsOnly = true,
			)
			_shouldShowTransitionDialog.value = true
			logcat { "Ending period detected without remaining budget, routing user to analytics" }
			return
		}

		val daysInPeriod =
			ChronoUnit.DAYS.between(settings.startDate, settings.getPeriodEndDate()) + 1
		val periodStartDate = lastPeriodEndDate.minusDays(daysInPeriod - 1)

		val totalSpent = if (endingPeriodState.transitionOccurred) {
			settings.totalBudget.subtract(endingPeriodState.remainingAmount)
		} else {
			settings.totalBudget
		}

		when (settings.remainingBudgetStrategy) {
			RemainingBudgetStrategy.ASK_ALWAYS -> {
				_midnightTransitionData.value = MidnightTransitionData(
					periodStartDate = periodStartDate,
					periodEndDate = lastPeriodEndDate,
					totalBudget = settings.totalBudget,
					remainingAmount = endingPeriodState.remainingAmount,
					totalSpent = totalSpent,
					currencyCode = settings.currencyCode
				)
				_shouldShowTransitionDialog.value = true
				logcat { "Ending period detected, asking user for rollover strategy" }
			}
			RemainingBudgetStrategy.SPLIT_EQUALLY -> {
				rollRemainingSplitEquallyAutomatically(
					remainingAmount = endingPeriodState.remainingAmount,
					periodEndDate = lastPeriodEndDate,
					totalSpent = totalSpent,
					routeToAnalytics = true,
				)
			}
			RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> {
				rollRemainingToFirstDayAutomatically(
					remainingAmount = endingPeriodState.remainingAmount,
					periodEndDate = lastPeriodEndDate,
					totalSpent = totalSpent,
					routeToAnalytics = true,
				)
			}
		}
	}

	suspend fun resolveEndingPeriodState(): EndingPeriodState {
		val prefs = context.settingsDataStore.data.first()
		val transitionOccurred = prefs[MIDNIGHT_TRANSITION_OCCURRED_KEY] ?: false
		val today = LocalDate.now()

		val settings = budgetRepository.getBudgetSettingsSync()
		val settingsEndDate = settings?.getPeriodEndDate()

		val endDateMillis = prefs[BUDGET_END_DATE_KEY]
		val dataStoreEndDate = endDateMillis?.let { millis ->
			Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
		}
		val effectiveEndDate = dataStoreEndDate ?: settingsEndDate
		val periodEndedBasedOnDate = effectiveEndDate?.let { today.isAfter(it) } ?: false

		val lastPeriodEndMillis = prefs[LAST_PERIOD_END_KEY] ?: endDateMillis
		val periodEndDate = lastPeriodEndMillis?.let {
			Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
		} ?: effectiveEndDate

		val remaining = if (transitionOccurred) {
			BigDecimal(prefs[REMAINING_FROM_LAST_PERIOD_KEY] ?: "0")
		} else {
			computeRemainingFromCurrentPeriod()
		}

		logcat {
			"resolveEndingPeriodState transitionOccurred=$transitionOccurred dataStoreEndDate=$dataStoreEndDate settingsEndDate=$settingsEndDate effectiveEndDate=$effectiveEndDate shouldHandle=${transitionOccurred || periodEndedBasedOnDate}"
		}

		return EndingPeriodState(
			shouldHandleEndingPeriod = transitionOccurred || periodEndedBasedOnDate,
			transitionOccurred = transitionOccurred,
			periodEndDate = periodEndDate,
			remainingAmount = remaining
		)
	}

	private suspend fun computeRemainingFromCurrentPeriod(): BigDecimal {
		val settings = budgetRepository.getBudgetSettingsSync() ?: return BigDecimal.ZERO
		val periodEnd = settings.getPeriodEndDate()
		val transactions = budgetRepository.getTransactions().first()
		val periodTransactions = transactions.filter { transaction ->
			val txDate = transaction.date?.toLocalDate()
			txDate != null && !txDate.isBefore(settings.startDate) && !txDate.isAfter(periodEnd)
		}
		val totalSpent = periodTransactions
			.filter { !it.isDeleted }
			.sumOf { it.amount }
		return settings.totalBudget.subtract(totalSpent)
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
		rollRemainingSplitEquallyAutomatically(
			remainingAmount = data.remainingAmount,
			periodEndDate = data.periodEndDate,
			totalSpent = data.totalSpent,
			routeToAnalytics = false,
		)
		onTransitionDialogConfirmed()
	}

	suspend fun rollRemainingToFirstDay() {
		val data = _midnightTransitionData.value ?: return
		rollRemainingToFirstDayAutomatically(
			remainingAmount = data.remainingAmount,
			periodEndDate = data.periodEndDate,
			totalSpent = data.totalSpent,
			routeToAnalytics = false,
		)
		onTransitionDialogConfirmed()
	}

	private suspend fun rollRemainingSplitEquallyAutomatically(
		remainingAmount: BigDecimal,
		periodEndDate: LocalDate,
		totalSpent: BigDecimal,
		routeToAnalytics: Boolean,
	) {
		val settings = budgetRepository.getBudgetSettingsSync() ?: return
		val (newStartDate, newEndDate, newDaysInPeriod) = calculateNextPeriodWindow(settings, periodEndDate)
		val updatedSettings = settings.copy(
			totalBudget = settings.totalBudget.add(remainingAmount),
			startDate = newStartDate,
			endDate = newEndDate,
			daysInPeriod = newDaysInPeriod,
			rollOverCarryForward = false,
			rollOverLimit = null,
			remainingBudgetStrategy = RemainingBudgetStrategy.SPLIT_EQUALLY
		)
		persistBudgetSettings(updatedSettings, forceNewPeriodBoundary = true)
		if (routeToAnalytics) {
			_midnightTransitionData.value = MidnightTransitionData(
				periodStartDate = settings.startDate,
				periodEndDate = periodEndDate,
				totalBudget = settings.totalBudget,
				remainingAmount = remainingAmount,
				totalSpent = totalSpent,
				currencyCode = settings.currencyCode,
				shouldNavigateToAnalyticsOnly = true,
			)
			_shouldShowTransitionDialog.value = true
			logcat { "Ending period detected, applied SPLIT_EQUALLY automatically and routing to analytics" }
		}
	}

	private suspend fun rollRemainingToFirstDayAutomatically(
		remainingAmount: BigDecimal,
		periodEndDate: LocalDate,
		totalSpent: BigDecimal,
		routeToAnalytics: Boolean,
	) {
		val settings = budgetRepository.getBudgetSettingsSync() ?: return
		val (newStartDate, newEndDate, newDaysInPeriod) = calculateNextPeriodWindow(settings, periodEndDate)
		val updatedSettings = settings.copy(
			startDate = newStartDate,
			endDate = newEndDate,
			daysInPeriod = newDaysInPeriod,
			rollOverCarryForward = true,
			rollOverLimit = remainingAmount,
			remainingBudgetStrategy = RemainingBudgetStrategy.ADD_TO_FIRST_DAY
		)
		persistBudgetSettings(updatedSettings, forceNewPeriodBoundary = true)
		if (routeToAnalytics) {
			_midnightTransitionData.value = MidnightTransitionData(
				periodStartDate = settings.startDate,
				periodEndDate = periodEndDate,
				totalBudget = settings.totalBudget,
				remainingAmount = remainingAmount,
				totalSpent = totalSpent,
				currencyCode = settings.currencyCode,
				shouldNavigateToAnalyticsOnly = true,
			)
			_shouldShowTransitionDialog.value = true
			logcat { "Ending period detected, applied ADD_TO_FIRST_DAY automatically and routing to analytics" }
		}
	}

	private fun calculateNextPeriodWindow(
		settings: com.serranoie.app.minus.domain.model.BudgetSettings,
		periodEndDate: LocalDate,
	): Triple<LocalDate, LocalDate?, Int> {
		val previousDays = ChronoUnit.DAYS.between(settings.startDate, settings.getPeriodEndDate())
			.toInt()
			.plus(1)
			.coerceAtLeast(1)
		val newStartDate = periodEndDate.plusDays(1)
		val newEndDate = settings.endDate?.let { newStartDate.plusDays(previousDays.toLong() - 1) }
		return Triple(newStartDate, newEndDate, previousDays)
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
