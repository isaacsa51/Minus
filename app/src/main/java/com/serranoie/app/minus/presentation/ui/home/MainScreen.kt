package com.serranoie.app.minus.presentation.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.CREDIT_QUICK_TOGGLE_FEATURE_KEY
import com.serranoie.app.minus.presentation.ONBOARDING_COMPLETED_KEY
import com.serranoie.app.minus.presentation.settingsDataStore
import com.serranoie.app.minus.presentation.ui.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetTransactionIntent
import com.serranoie.app.minus.presentation.ui.tutorial.FirstLaunchTutorialStage
import com.serranoie.app.minus.presentation.ui.tutorial.firstLaunchTutorialStageFlow
import kotlinx.coroutines.flow.map

@Composable
fun MainScreen(
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    openWalletOnStart: Boolean = false,
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    mainScreenViewModel: MainScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val mainScreenState by mainScreenViewModel.uiState.collectAsStateWithLifecycle()
    val budgetUiState by budgetViewModel.uiState.collectAsStateWithLifecycle()

    val onboardingCompleted by context.settingsDataStore.data
        .map { it[ONBOARDING_COMPLETED_KEY] ?: false }
        .collectAsStateWithLifecycle(initialValue = false)

    val tutorialStage by context.firstLaunchTutorialStageFlow()
        .collectAsStateWithLifecycle(initialValue = FirstLaunchTutorialStage.COMPLETED)

    val showCreditQuickToggleFeature by context.settingsDataStore.data
        .map { it[CREDIT_QUICK_TOGGLE_FEATURE_KEY] ?: false }
        .collectAsStateWithLifecycle(initialValue = false)

    val undoSnackbarActionLabel = stringResource(R.string.undo)

    LaunchedEffect(openWalletOnStart, mainScreenState.walletSheetOpened) {
        if (openWalletOnStart && !mainScreenState.walletSheetOpened) {
            mainScreenViewModel.processIntent(
                MainScreenUiIntent.ShowBudgetPeriodSheet(forceSetup = true),
                tutorialStage,
            )
            mainScreenViewModel.processIntent(
                MainScreenUiIntent.MarkWalletSheetOpened,
                tutorialStage,
            )
        }
    }

    LaunchedEffect(Unit) {
        mainScreenViewModel.effects.collect { effect ->
            when (effect) {
                is MainScreenUiEffect.RequestUndo -> {
                    mainScreenState.pendingDeleteTransaction?.let { tx ->
                        budgetViewModel.processIntent(
                            BudgetTransactionIntent.RestoreTransactionTapped(tx)
                        )
                    }
                }
                is MainScreenUiEffect.UpdateDragProgress -> {
                    budgetViewModel.processIntent(
                        BudgetNumpadIntent.SetDragProgress(effect.progress)
                    )
                }
                is MainScreenUiEffect.OpenWallet -> onNavigateToWallet()
                is MainScreenUiEffect.OpenAnalytics -> onNavigateToAnalytics()
                is MainScreenUiEffect.ShowUndoSnackbar -> {}
            }
        }
    }

    MainScreenContent(
        mainScreenState = mainScreenState,
        budgetUiState = budgetUiState,
        onboardingCompleted = onboardingCompleted,
        tutorialStage = tutorialStage,
        showCreditQuickToggleFeature = showCreditQuickToggleFeature,
        onProcessIntent = { intent ->
            when (intent) {
                is MainScreenUiIntent.ProcessBudgetTransactionIntent ->
                    budgetViewModel.processIntent(intent.intent)
                is MainScreenUiIntent.ProcessBudgetEditorIntent ->
                    budgetViewModel.processIntent(intent.intent)
                is MainScreenUiIntent.ProcessBudgetNumpadIntent ->
                    budgetViewModel.processIntent(intent.intent)
                else -> mainScreenViewModel.processIntent(intent, tutorialStage)
            }
            if (intent is MainScreenUiIntent.QueueDeleteWithUndo) {
                mainScreenViewModel.onTransactionDeleteQueued(intent.transaction, intent.message)
            }
            if (intent is MainScreenUiIntent.CancelPendingDelete) {
                mainScreenViewModel.onPendingDeleteCanceled()
            }
        },
        onNavigateToAnalytics = onNavigateToAnalytics,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToWallet = onNavigateToWallet,
        openWalletOnStart = openWalletOnStart,
        showBudgetPeriodSheet = mainScreenState.showBudgetPeriodSheet,
        forceBudgetPeriodSheetSetup = mainScreenState.forceBudgetPeriodSheetSetup,
        selectedViewPeriod = mainScreenState.selectedViewPeriod,
        settingsDataStore = context.settingsDataStore,
        undoSnackbarActionLabel = undoSnackbarActionLabel,
    )
}
