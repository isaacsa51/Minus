package com.serranoie.app.minus.presentation.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.presentation.CATEGORY_GRID_MODE_KEY
import com.serranoie.app.minus.presentation.CATEGORY_PICKER_DIRECT_POPUP_KEY
import com.serranoie.app.minus.presentation.CREDIT_QUICK_TOGGLE_FEATURE_KEY
import com.serranoie.app.minus.presentation.ONBOARDING_COMPLETED_KEY
import com.serranoie.app.minus.presentation.TUTORIAL_BOX_COMPLETED_KEY
import com.serranoie.app.minus.presentation.settingsDataStore
import com.serranoie.app.minus.presentation.ui.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetTransactionIntent
import com.serranoie.app.minus.presentation.ui.changelog.ChangelogGate
import com.serranoie.app.minus.presentation.ui.tutorial.FirstLaunchTutorialStage
import com.serranoie.app.minus.presentation.ui.tutorial.TutorialBox
import com.serranoie.app.minus.presentation.ui.tutorial.TutorialTooltip
import com.serranoie.app.minus.presentation.ui.tutorial.firstLaunchTutorialStageFlow
import com.serranoie.app.minus.presentation.ui.tutorial.rememberTutorialBoxState
import com.serranoie.app.minus.presentation.ui.tutorial.tutorialBoxCompletedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import logcat.logcat

private const val TAG = "ISAAC:MainScreen"

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun MainScreen(
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {},
    openWalletOnStart: Boolean = false,
    onRequestNotificationPermission: () -> Unit = {},
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    mainScreenViewModel: MainScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val mainScreenState by mainScreenViewModel.uiState.collectAsStateWithLifecycle()
    val budgetUiState by budgetViewModel.uiState.collectAsStateWithLifecycle()

    val onboardingCompleted by context.settingsDataStore.data.map {
        it[ONBOARDING_COMPLETED_KEY] ?: false
    }.collectAsStateWithLifecycle(initialValue = false)

    val tutorialStage by context.firstLaunchTutorialStageFlow()
        .collectAsStateWithLifecycle(initialValue = FirstLaunchTutorialStage.COMPLETED)

    val showCreditQuickToggleFeature by context.settingsDataStore.data.map {
        it[CREDIT_QUICK_TOGGLE_FEATURE_KEY] ?: false
    }.collectAsStateWithLifecycle(initialValue = false)

    val effectiveSelectedPeriod =
        mainScreenState.selectedViewPeriod ?: budgetUiState.budgetSettings?.period
        ?: BudgetPeriod.DAILY

    val directCategoryPopupEnabled by context.settingsDataStore.data
        .map { it[CATEGORY_PICKER_DIRECT_POPUP_KEY] ?: false }
        .collectAsStateWithLifecycle(initialValue = false)

    val categoryGridModeEnabled by context.settingsDataStore.data
        .map { it[CATEGORY_GRID_MODE_KEY] ?: false }
        .collectAsStateWithLifecycle(initialValue = false)

    val undoSnackbarActionLabel = stringResource(R.string.undo)

    logcat(TAG) { "MainScreen composed (openWalletOnStart=$openWalletOnStart, effectivePeriod=$effectiveSelectedPeriod)" }

    LaunchedEffect(Unit) {
        logcat(TAG) { "Triggering onRequestNotificationPermission from MainScreen LaunchedEffect" }
        onRequestNotificationPermission()
    }

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
                    budgetViewModel.processIntent(
                        BudgetTransactionIntent.RestoreTransactionTapped(effect.transaction),
                    )
                }

                is MainScreenUiEffect.UpdateDragProgress -> {
                    budgetViewModel.processIntent(
                        BudgetNumpadIntent.SetDragProgress(effect.progress),
                    )
                }

                is MainScreenUiEffect.OpenWallet -> {
                    onNavigateToWallet()
                }

                is MainScreenUiEffect.OpenAnalytics -> {
                    onNavigateToAnalytics()
                }

                is MainScreenUiEffect.ShowUndoSnackbar -> {}
            }
        }
    }

    val tutorialBoxCompleted by context.tutorialBoxCompletedFlow()
        .collectAsStateWithLifecycle(initialValue = false)
    val showNumpadTutorial = !tutorialBoxCompleted
    val tutorialBoxState = rememberTutorialBoxState()
    val tutorialScope = rememberCoroutineScope()

    LaunchedEffect(tutorialBoxCompleted) {
        if (!tutorialBoxCompleted && tutorialBoxState.isCompleted) {
            tutorialBoxState.resetForReplay()
        }
    }

    ChangelogGate(
        currentVersionCode = run {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                0,
            )
            @Suppress("DEPRECATION") info.longVersionCode.toInt()
        },
    ) {
        TutorialBox(
            showTutorial = showNumpadTutorial,
            onTutorialCompleted = {
                logcat(TAG) { "TutorialBox completed → persisting tutorialBoxCompleted=true" }
                tutorialScope.launch {
                    context.settingsDataStore.edit { prefs ->
                        prefs[TUTORIAL_BOX_COMPLETED_KEY] = true
                    }
                }
            },
            onTutorialReopened = {
                logcat(TAG) { "TutorialBox reopened (gated target became measurable) → persisting tutorialBoxCompleted=false" }
                tutorialScope.launch {
                    context.settingsDataStore.edit { prefs ->
                        prefs[TUTORIAL_BOX_COMPLETED_KEY] = false
                    }
                }
            },
            state = tutorialBoxState,
            tutorialTarget = { index ->
                when (index) {
                    0 -> TutorialTooltip(
                        title = null,
                        description = stringResource(R.string.tutorial_numpad_description),
                    )
                    2 -> TutorialTooltip(
                        title = stringResource(R.string.tutorial_settings_title),
                        description = stringResource(R.string.tutorial_settings_description),
                    )
                    1 -> TutorialTooltip(
                        title = stringResource(R.string.tutorial_budget_pill_title),
                        description = stringResource(R.string.tutorial_budget_pill_description),
                    )
                    3 -> TutorialTooltip(
                        title = stringResource(R.string.tutorial_comment_title),
                        description = stringResource(R.string.tutorial_comment_description),
                    )
                    4 -> TutorialTooltip(
                        title = stringResource(R.string.tutorial_recurrent_title),
                        description = stringResource(R.string.tutorial_recurrent_description),
                    )
                    5 -> TutorialTooltip(
                        title = stringResource(R.string.tutorial_analytics_title),
                        description = stringResource(R.string.tutorial_analytics_description),
                    )
                    6 -> TutorialTooltip(
                        title = stringResource(R.string.tutorial_privacy_title),
                        description = stringResource(R.string.tutorial_privacy_description),
                    )
                    else -> Text(text = "")
                }
            },
        ) {
            MainScreenContent(
                mainScreenState = mainScreenState,
                budgetUiState = budgetUiState,
                onboardingCompleted = onboardingCompleted,
                tutorialStage = tutorialStage,
                showCreditQuickToggleFeature = showCreditQuickToggleFeature,
                directCategoryPopupEnabled = directCategoryPopupEnabled,
                categoryGridModeEnabled = categoryGridModeEnabled,
                onProcessIntent = { intent ->
                    when (intent) {
                        is MainScreenUiIntent.ProcessBudgetTransactionIntent -> {
                            budgetViewModel.processIntent(intent.intent)
                        }

                        is MainScreenUiIntent.ProcessBudgetEditorIntent -> {
                            budgetViewModel.processIntent(intent.intent)
                        }

                        is MainScreenUiIntent.ProcessBudgetNumpadIntent -> {
                            budgetViewModel.processIntent(intent.intent)
                        }

                        else -> {
                            mainScreenViewModel.processIntent(intent, tutorialStage)
                        }
                    }
                },
                onNavigateToAnalytics = onNavigateToAnalytics,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToWallet = onNavigateToWallet,
                openWalletOnStart = openWalletOnStart,
                showBudgetPeriodSheet = mainScreenState.showBudgetPeriodSheet,
                forceBudgetPeriodSheetSetup = mainScreenState.forceBudgetPeriodSheetSetup,
                selectedViewPeriod = effectiveSelectedPeriod,
                onPeriodSelected = { period ->
                    mainScreenViewModel.processIntent(
                        MainScreenUiIntent.SetSelectedPeriod(period), tutorialStage
                    )
                },
                settingsDataStore = context.settingsDataStore,
                undoSnackbarActionLabel = undoSnackbarActionLabel,
                tutorialBoxState = tutorialBoxState,
            )
        }
    }
}
