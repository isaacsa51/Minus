package com.serranoie.app.minus.presentation.ui.e2e.pockets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.RolloverDialog
import kotlinx.coroutines.launch

@Composable
fun PocketRolloverHost(harness: PocketE2EHarness, onSweepFailed: (Throwable) -> Unit = {}) {
    MinusTheme {
        val manager = harness.transitionManager
        val show by manager.shouldShowTransitionDialog.collectAsState()
        val data by manager.midnightTransitionData.collectAsState()
        val pockets by harness.budgetRepository.getSavingsPockets().collectAsState(initial = emptyList())
        val scope = rememberCoroutineScope()
        val goalLabel = stringResource(R.string.pockets_goal_name)
        val transition = data
        if (show && transition != null && !transition.shouldNavigateToAnalyticsOnly) {
            RolloverDialog(
                remainingAmount = transition.remainingAmount,
                currencyCode = transition.currencyCode,
                periodLabel = "${transition.periodStartDate} - ${transition.periodEndDate}",
                spentAmount = if (transition.isPersistedReopen) null else transition.totalSpent,
                onSplitEqually = {
                    scope.launch { manager.resolveUnresolvedSurplus(RemainingBudgetStrategy.SPLIT_EQUALLY) }
                },
                onCarryToNextDay = {
                    scope.launch { manager.resolveUnresolvedSurplus(RemainingBudgetStrategy.ADD_TO_FIRST_DAY) }
                },
                onDiscardRemaining = { scope.launch { manager.resolveUnresolvedSurplus(null) } },
                pockets = pockets,
                onSaveToPocket = { pocket ->
                    scope.launch {
                        runCatching { manager.sweepSurplusToPocket(pocket, pocket.name.ifBlank { goalLabel }) }
                            .onFailure(onSweepFailed)
                    }
                },
                onDismiss = { manager.onTransitionDialogDismissed() },
            )
        }
    }
}
