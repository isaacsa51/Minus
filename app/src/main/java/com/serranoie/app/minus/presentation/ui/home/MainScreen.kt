package com.serranoie.app.minus.presentation.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.presentation.ui.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetTransactionIntent
import com.serranoie.app.minus.presentation.ui.changelog.ChangelogGate
import com.serranoie.app.minus.presentation.ui.tutorial.TutorialBox
import com.serranoie.app.minus.presentation.ui.tutorial.TutorialTooltip
import com.serranoie.app.minus.presentation.ui.tutorial.rememberTutorialBoxState
import logcat.logcat

private const val TAG = "ISAAC:MainScreen"

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun MainScreen(
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    openWalletOnStart: Boolean = false,
    onRequestNotificationPermission: () -> Unit = {},
    budgetViewModel: BudgetViewModel = hiltViewModel(),
    mainScreenViewModel: MainScreenViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val mainScreenState by mainScreenViewModel.uiState.collectAsStateWithLifecycle()
    val budgetUiState by budgetViewModel.uiState.collectAsStateWithLifecycle()

    val tutorialStage = mainScreenState.tutorialStage
    val tutorialBoxCompleted = mainScreenState.tutorialBoxCompleted

    val effectiveSelectedPeriod =
        mainScreenState.selectedViewPeriod ?: budgetUiState.budgetSettings?.period
        ?: BudgetPeriod.DAILY

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

                is MainScreenUiEffect.OpenAnalytics -> {
                    onNavigateToAnalytics()
                }

                is MainScreenUiEffect.ShowUndoSnackbar -> {}
            }
        }
    }

    val showNumpadTutorial = !tutorialBoxCompleted
    val tutorialBoxState = rememberTutorialBoxState()

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
                mainScreenViewModel.processIntent(
                    MainScreenUiIntent.SetTutorialBoxCompleted(true),
                    tutorialStage,
                )
            },
            onTutorialReopened = {
                logcat(TAG) { "TutorialBox reopened (gated target became measurable) → persisting tutorialBoxCompleted=false" }
                mainScreenViewModel.processIntent(
                    MainScreenUiIntent.SetTutorialBoxCompleted(false),
                    tutorialStage,
                )
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
                    7 -> TutorialTooltip(
                        title = stringResource(R.string.tutorial_calc_title),
                        description = stringResource(R.string.tutorial_calc_description),
                    )
                    8 -> TutorialTooltip(
                        title = stringResource(R.string.tutorial_credit_toggle_title),
                        description = stringResource(R.string.tutorial_credit_toggle_description),
                    )
                    else -> Text(text = "")
                }
            },
        ) {
            MainScreenContent(
                mainScreenState = mainScreenState,
                budgetUiState = budgetUiState,
                actions =
                    MainScreenActions(
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
                        onAdvanceTutorial = { expected ->
                            mainScreenViewModel.processIntent(
                                MainScreenUiIntent.AdvanceTutorial(expected),
                                tutorialStage
                            )
                        },
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onNavigateToSettings = onNavigateToSettings,
                        onPeriodSelected = { period ->
                            mainScreenViewModel.processIntent(
                                MainScreenUiIntent.SetSelectedPeriod(period), tutorialStage
                            )
                        },
                    ),
                openWalletOnStart = openWalletOnStart,
                tutorialBoxState = tutorialBoxState,
            )
        }
    }
}
