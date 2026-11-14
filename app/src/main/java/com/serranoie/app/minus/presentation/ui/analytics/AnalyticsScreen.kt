package com.serranoie.app.minus.presentation.ui.analytics

import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import com.serranoie.app.minus.R

@Composable
fun AnalyticsScreen(
    activityResultRegistryOwner: ActivityResultRegistryOwner?,
    onNavigateToMainWithWallet: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToSubscriptions: () -> Unit = {},
    isRootDestination: Boolean = false,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val goalPocketLabel = stringResource(R.string.pockets_goal_name)

    BackHandler(enabled = isRootDestination || uiState.selectedPeriodId != null) {
        viewModel.onClose()
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AnalyticsUiEffect.NavigateToMainWithWallet -> {
                    viewModel.consumeEffect()
                    onNavigateToMainWithWallet()
                }
                is AnalyticsUiEffect.NavigateToMain -> {
                    viewModel.consumeEffect()
                    onNavigateToMain()
                }
                AnalyticsUiEffect.PocketActionFailed -> {
                    viewModel.consumeEffect()
                    Toast.makeText(context, R.string.pockets_action_failed, Toast.LENGTH_SHORT).show()
                }
                null -> { /* no-op */ }
            }
        }
    }

    Analytics(
        state = uiState.displayState,
        archivedBudgets = uiState.archivedBudgets,
        categories = uiState.categories,
        pockets = uiState.pockets,
        actions = AnalyticsActions(
            onCreateNewPeriod = {
                viewModel.onCreateNewPeriod()
            },
            onClose = {
                viewModel.onClose()
            },
            onNavigateToSubscriptions = onNavigateToSubscriptions,
            onMarkCreditPaid = {
                viewModel.onMarkCreditPaid()
            },
            onPayTransactionClick = { txId ->
                viewModel.onPayTransactionClick(txId)
            },
            onCutoffDayChanged = { day ->
                viewModel.onCutoffDayChanged(day)
            },
            onHistoricalPeriodSelected = { periodId ->
                viewModel.onPeriodSelected(periodId)
            },
            onTutorialCompleted = { hasSpends ->
                viewModel.onTutorialCompleted(hasSpends)
            },
            onGranularityChanged = { granularity ->
                viewModel.onGranularityChanged(granularity)
            },
            onUpdateTransaction = { tx ->
                viewModel.updateTransaction(tx)
            },
            onDeleteTransaction = { tx ->
                viewModel.deleteTransaction(tx)
            },
            onDeleteArchivedPeriod = { periodId ->
                viewModel.deleteArchivedPeriod(periodId)
            },
            onCreatePocket = viewModel::onCreatePocket,
            onPocketDeposit = { id, amount -> viewModel.onPocketDeposit(id, amount, goalPocketLabel) },
            onPocketWithdraw = { id, amount -> viewModel.onPocketWithdraw(id, amount, goalPocketLabel) },
            onDeletePocket = viewModel::onDeletePocket,
        ),
        activityResultRegistryOwner = activityResultRegistryOwner,
    )
}
