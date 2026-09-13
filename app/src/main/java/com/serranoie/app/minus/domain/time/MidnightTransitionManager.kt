package com.serranoie.app.minus.domain.time

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

const val MIDNIGHT_TRANSITION_OCCURRED_KEY_NAME = "midnight_transition_occurred"
const val LAST_PERIOD_END_KEY_NAME = "last_period_end_millis"
const val REMAINING_FROM_LAST_PERIOD_KEY_NAME = "remaining_from_last_period"
const val PENDING_ROLLOVER_AMOUNT_KEY_NAME = "pending_rollover_amount"
const val PENDING_ROLLOVER_STRATEGY_KEY_NAME = "pending_rollover_strategy"
const val CURRENT_PERIOD_ROLLOVER_AMOUNT_KEY_NAME = "current_period_rollover_amount"
const val CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD_KEY_NAME = "current_period_rollover_carry_forward"

val MIDNIGHT_TRANSITION_OCCURRED_KEY = booleanPreferencesKey(MIDNIGHT_TRANSITION_OCCURRED_KEY_NAME)
val LAST_PERIOD_END_KEY = longPreferencesKey(LAST_PERIOD_END_KEY_NAME)
val REMAINING_FROM_LAST_PERIOD_KEY = stringPreferencesKey(REMAINING_FROM_LAST_PERIOD_KEY_NAME)
val PENDING_ROLLOVER_AMOUNT_KEY = stringPreferencesKey(PENDING_ROLLOVER_AMOUNT_KEY_NAME)
val PENDING_ROLLOVER_STRATEGY_KEY = stringPreferencesKey(PENDING_ROLLOVER_STRATEGY_KEY_NAME)
val CURRENT_PERIOD_ROLLOVER_AMOUNT_KEY =
    stringPreferencesKey(CURRENT_PERIOD_ROLLOVER_AMOUNT_KEY_NAME)
val CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD_KEY =
    booleanPreferencesKey(CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD_KEY_NAME)

@Singleton
class MidnightTransitionManager @Inject constructor(
    private val midnightPeriodChecker: MidnightPeriodChecker,
) {
    val shouldShowTransitionDialog: StateFlow<Boolean> =
        midnightPeriodChecker.shouldShowTransitionDialog
    val midnightTransitionData: StateFlow<MidnightTransitionData?> =
        midnightPeriodChecker.midnightTransitionData
    val needsBudgetSetup: StateFlow<Boolean> = midnightPeriodChecker.needsBudgetSetup
    val pendingRollover: Flow<Pair<BigDecimal, RemainingBudgetStrategy?>> =
        midnightPeriodChecker.pendingRollover

    suspend fun handleAppStart() {
        midnightPeriodChecker.handleEndingPeriod()
    }

    fun onTransitionDialogConfirmed() {
        midnightPeriodChecker.onTransitionDialogConfirmed()
    }

    fun onTransitionDialogDismissed() {
        midnightPeriodChecker.onTransitionDialogDismissed()
    }

    fun onBudgetSetupHandled() {
        midnightPeriodChecker.onBudgetSetupHandled()
    }

    suspend fun resolveUnresolvedSurplus(strategy: RemainingBudgetStrategy?) {
        midnightPeriodChecker.resolveUnresolvedSurplus(strategy)
    }

    suspend fun reopenUnresolvedSurplusDialog() {
        midnightPeriodChecker.reopenUnresolvedSurplusDialog()
    }
}
