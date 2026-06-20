package com.serranoie.app.minus.domain.time

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.presentation.BUDGET_END_DATE_KEY
import com.serranoie.app.minus.presentation.settingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import logcat.logcat
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
    @param:ApplicationContext private val context: Context,
    private val budgetRepository: BudgetRepository
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

    private val _needsBudgetSetup = MutableStateFlow(false)
    val needsBudgetSetup: StateFlow<Boolean> = _needsBudgetSetup.asStateFlow()

    suspend fun handleEndingPeriod() {
        val endingPeriodState = resolveEndingPeriodState()
        if (!endingPeriodState.shouldHandleEndingPeriod) {
            logcat { "No ending period to handle" }
            // Check if no budget is set - if so, signal UI to show budget setup
            val budgetSettings = budgetRepository.getBudgetSettingsSync()
            if (budgetSettings == null) {
                logcat { "No budget settings found, triggering budget setup prompt" }
                _needsBudgetSetup.value = true
            }
            return
        }

        context.settingsDataStore.edit { it[MIDNIGHT_TRANSITION_OCCURRED_KEY] = false }

        val lastPeriodEndDate = endingPeriodState.periodEndDate ?: run {
            logcat { "No period end date found" }
            return
        }
        persistLastPeriodSnapshot(
            periodEndDate = lastPeriodEndDate,
            remainingAmount = endingPeriodState.remainingAmount,
        )

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

            RemainingBudgetStrategy.SPLIT_EQUALLY,
            RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> {
                enqueuePendingRollover(
                    strategy = settings.remainingBudgetStrategy,
                    remainingAmount = endingPeriodState.remainingAmount,
                )
                _midnightTransitionData.value = MidnightTransitionData(
                    periodStartDate = periodStartDate,
                    periodEndDate = lastPeriodEndDate,
                    totalBudget = settings.totalBudget,
                    remainingAmount = endingPeriodState.remainingAmount,
                    totalSpent = totalSpent,
                    currencyCode = settings.currencyCode,
                    shouldNavigateToAnalyticsOnly = true,
                )
                _shouldShowTransitionDialog.value = true
                logcat { "Ending period detected, queued pending rollover and routing to analytics" }
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
        val dataStoreIsInFuture = dataStoreEndDate?.isAfter(today) ?: false
        val effectiveEndDate =
            if (dataStoreIsInFuture) {
                settingsEndDate ?: dataStoreEndDate
            } else {
                dataStoreEndDate
                    ?: settingsEndDate
            }
        val periodEndedBasedOnDate = effectiveEndDate?.let { today.isAfter(it) } ?: false

        val periodEndDate = if (transitionOccurred) {
            val lastPeriodEndMillis = prefs[LAST_PERIOD_END_KEY] ?: endDateMillis
            lastPeriodEndMillis?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            } ?: effectiveEndDate
        } else {
            effectiveEndDate
        }

        val remaining = if (transitionOccurred) {
            BigDecimal(prefs[REMAINING_FROM_LAST_PERIOD_KEY] ?: "0")
        } else {
            computeRemainingFromCurrentPeriod()
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

    fun onBudgetSetupHandled() {
        _needsBudgetSetup.value = false
    }

    suspend fun rollRemainingSplitEqually() {
        val data = _midnightTransitionData.value ?: return
        enqueuePendingRollover(
            strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
            remainingAmount = data.remainingAmount,
        )
        onTransitionDialogConfirmed()
    }

    suspend fun rollRemainingToFirstDay() {
        val data = _midnightTransitionData.value ?: return
        enqueuePendingRollover(
            strategy = RemainingBudgetStrategy.ADD_TO_FIRST_DAY,
            remainingAmount = data.remainingAmount,
        )
        onTransitionDialogConfirmed()
    }

    private suspend fun persistLastPeriodSnapshot(
        periodEndDate: LocalDate,
        remainingAmount: BigDecimal,
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[LAST_PERIOD_END_KEY] = periodEndDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            prefs[REMAINING_FROM_LAST_PERIOD_KEY] = remainingAmount.toPlainString()
        }
    }

    private suspend fun enqueuePendingRollover(
        strategy: RemainingBudgetStrategy,
        remainingAmount: BigDecimal,
    ) {
        context.settingsDataStore.edit { prefs ->
            prefs[PENDING_ROLLOVER_STRATEGY_KEY] = strategy.name
            prefs[PENDING_ROLLOVER_AMOUNT_KEY] = remainingAmount.toPlainString()
        }
    }
}
