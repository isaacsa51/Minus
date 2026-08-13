package com.serranoie.app.minus.presentation.ui.analytics

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
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
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
            }
        ),
        activityResultRegistryOwner = activityResultRegistryOwner,
    )
}
