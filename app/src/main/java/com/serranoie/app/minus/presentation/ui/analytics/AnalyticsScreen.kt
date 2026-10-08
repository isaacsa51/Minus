package com.serranoie.app.minus.presentation.ui.analytics

import com.serranoie.app.minus.presentation.ui.currency.CurrencyConversionHost
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
                null -> { /* no-op */ }
            }
        }
    }

    CurrencyConversionHost {
        Analytics(
            state = uiState.displayState,
            archivedBudgets = uiState.archivedBudgets,
            categories = uiState.categories,
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
            ),
            activityResultRegistryOwner = activityResultRegistryOwner,
        )
    }
}
