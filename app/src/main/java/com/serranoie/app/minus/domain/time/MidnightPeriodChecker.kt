package com.serranoie.app.minus.domain.time

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.serranoie.app.minus.BUDGET_END_DATE_KEY
import com.serranoie.app.minus.LAST_PERIOD_END_KEY
import com.serranoie.app.minus.MIDNIGHT_TRANSITION_OCCURRED_KEY
import com.serranoie.app.minus.REMAINING_FROM_LAST_PERIOD_KEY
import com.serranoie.app.minus.data.repository.BudgetRepository
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
	private val budgetRepository: BudgetRepository
) {
	private val _midnightTransitionData = MutableStateFlow<MidnightTransitionData?>(null)
	val midnightTransitionData: StateFlow<MidnightTransitionData?> =
		_midnightTransitionData.asStateFlow()

	private val _shouldShowTransitionDialog = MutableStateFlow(false)
	val shouldShowTransitionDialog: StateFlow<Boolean> = _shouldShowTransitionDialog.asStateFlow()

	companion object {
		private const val TAG = "MidnightPeriodChecker"
	}

	suspend fun checkMidnightTransition() {
		val prefs = context.settingsDataStore.data.first()
		val transitionOccurred = prefs[MIDNIGHT_TRANSITION_OCCURRED_KEY] ?: false

		val endDateMillis = prefs[BUDGET_END_DATE_KEY]
		val periodEndedBasedOnDate = endDateMillis?.let { millis ->
			val endDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
			LocalDate.now().isAfter(endDate)
		} ?: false

		if (!transitionOccurred && !periodEndedBasedOnDate) {
			Log.d(TAG, "No midnight transition detected")
			return
		}

		context.settingsDataStore.edit { it[MIDNIGHT_TRANSITION_OCCURRED_KEY] = false }

		val lastPeriodEndMillis = prefs[LAST_PERIOD_END_KEY] ?: endDateMillis
		if (lastPeriodEndMillis == null) {
			Log.d(TAG, "No period end date found")
			return
		}

		val remainingStr = if (transitionOccurred) {
			prefs[REMAINING_FROM_LAST_PERIOD_KEY] ?: "0"
		} else {
			"0"
		}
		val remaining = BigDecimal(remainingStr)

		val settings = budgetRepository.getBudgetSettingsSync() ?: run {
			Log.d(TAG, "No budget settings found")
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
		Log.d(TAG, "Midnight transition detected, showing dialog")
	}

	fun onTransitionDialogConfirmed() {
		_shouldShowTransitionDialog.value = false
		_midnightTransitionData.value = null
	}

	fun onTransitionDialogDismissed() {
		_shouldShowTransitionDialog.value = false
		_midnightTransitionData.value = null
	}

	fun reset() {
		_midnightTransitionData.value = null
		_shouldShowTransitionDialog.value = false
	}
}