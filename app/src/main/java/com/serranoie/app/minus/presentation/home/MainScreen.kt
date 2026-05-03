package com.serranoie.app.minus.presentation.home

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.rememberSwipeableState
import com.serranoie.app.minus.LocalWindowInsets
import com.serranoie.app.minus.LocalWindowSize
import com.serranoie.app.minus.ONBOARDING_COMPLETED_KEY
import com.serranoie.app.minus.presentation.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.budget.NumpadWithViewModel
import com.serranoie.app.minus.presentation.budget.mvi.intent.BudgetSystemIntent
import com.serranoie.app.minus.presentation.budget.mvi.intent.BudgetTransactionIntent
import com.serranoie.app.minus.presentation.budget.mvi.intent.BudgetEditorIntent
import com.serranoie.app.minus.presentation.editor.AnimState
import com.serranoie.app.minus.presentation.editor.EditorWithViewModel
import com.serranoie.app.minus.presentation.history.History
import com.serranoie.app.minus.presentation.tutorial.FIRST_LAUNCH_TUTORIAL_STAGE_KEY
import com.serranoie.app.minus.presentation.tutorial.FirstLaunchTutorialStage
import com.serranoie.app.minus.presentation.tutorial.firstLaunchTutorialStageFlow
import com.serranoie.app.minus.presentation.ui.theme.colorEditor
import com.serranoie.app.minus.presentation.ui.theme.colorOnEditor
import com.serranoie.app.minus.presentation.ui.theme.component.TopSheetLayout
import com.serranoie.app.minus.presentation.ui.theme.component.TopSheetValue
import com.serranoie.app.minus.presentation.ui.theme.isNightMode
import com.serranoie.app.minus.settingsDataStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalWearMaterialApi::class)
@Composable
fun MainScreen(
	onNavigateToAnalytics: () -> Unit = {},
	onNavigateToSettings: () -> Unit = {},
	onNavigateToWallet: () -> Unit = {},
	openWalletOnStart: Boolean = false,
	forceWalletSetup: Boolean = false,
	budgetViewModel: BudgetViewModel = hiltViewModel()
) {
	val topSheetState = rememberSwipeableState(TopSheetValue.HalfExpanded)
	var nightMode by remember { mutableStateOf(false) }
	val context = LocalContext.current
	val coroutineScope = rememberCoroutineScope()
	val budgetUiState by budgetViewModel.uiState.collectAsStateWithLifecycle()

	val localDensity = LocalDensity.current
	val windowSizeClass = LocalWindowSize.current
	val windowInsets = LocalWindowInsets.current

	val onboardingCompletedFlow = remember(context) {
		context.settingsDataStore.data.map { it[ONBOARDING_COMPLETED_KEY] ?: false }
	}
	val onboardingCompleted by onboardingCompletedFlow.collectAsStateWithLifecycle(initialValue = false)
	val tutorialStage by context.firstLaunchTutorialStageFlow()
		.collectAsStateWithLifecycle(initialValue = FirstLaunchTutorialStage.COMPLETED)

	var shownStage by remember { mutableStateOf<FirstLaunchTutorialStage?>(null) }

	fun advanceTutorialIfCurrent(expected: FirstLaunchTutorialStage) {
		if (tutorialStage != expected) return
		coroutineScope.launch {
			context.settingsDataStore.edit { prefs ->
				prefs[FIRST_LAUNCH_TUTORIAL_STAGE_KEY] = expected.next().name
			}
		}
	}

	val isHistoryVisible =
		windowSizeClass != WindowWidthSizeClass.Compact || topSheetState.currentValue == TopSheetValue.Expanded

	var pendingDeleteTransaction by remember {
		mutableStateOf<com.serranoie.app.minus.domain.model.Transaction?>(
			null
		)
	}
	var snackbarAutoDismissJob by remember { mutableStateOf<Job?>(null) }
	var pendingDeleteJob by remember { mutableStateOf<Job?>(null) }
	val snackbarHostState = remember { SnackbarHostState() }

	fun executeDelete(transaction: com.serranoie.app.minus.domain.model.Transaction) {
		budgetViewModel.processIntent(BudgetTransactionIntent.DeleteTransactionTapped(transaction))
	}

	fun cancelPendingDelete() {
		pendingDeleteJob?.cancel()
		pendingDeleteJob = null
		snackbarAutoDismissJob?.cancel()
		snackbarAutoDismissJob = null
		pendingDeleteTransaction?.let { tx ->
			budgetViewModel.processIntent(BudgetTransactionIntent.RestoreTransactionTapped(tx))
		}
		pendingDeleteTransaction = null
		snackbarHostState.currentSnackbarData?.dismiss()
	}

	fun queueDeleteWithUndo(
		transaction: com.serranoie.app.minus.domain.model.Transaction, message: String
	) {
		pendingDeleteJob?.cancel()
		snackbarAutoDismissJob?.cancel()

		pendingDeleteTransaction = transaction
		executeDelete(transaction)

		coroutineScope.launch {
			val result = snackbarHostState.showSnackbar(
				message = message,
				actionLabel = "",
				duration = SnackbarDuration.Short,
			)
			if (result == SnackbarResult.ActionPerformed) {
				cancelPendingDelete()
			}
		}

		pendingDeleteJob = coroutineScope.launch {
			delay(3500L)
			pendingDeleteTransaction = null
			pendingDeleteJob = null
		}
	}

	fun hideGlobalSnackbar() {
		snackbarAutoDismissJob?.cancel()
		snackbarAutoDismissJob = null
	}

	fun showInfoSnackbar(message: String) {
		snackbarAutoDismissJob?.cancel()
		coroutineScope.launch {
			snackbarHostState.showSnackbar(
				message = message,
				duration = androidx.compose.material3.SnackbarDuration.Short,
			)
		}
	}

	val quickLogSwipeModifier = Modifier.pointerInput(isHistoryVisible) {
		if (!isHistoryVisible) return@pointerInput
		var totalDrag = 0f
		detectHorizontalDragGestures(onHorizontalDrag = { _, dragAmount ->
			totalDrag += dragAmount
		}, onDragEnd = {
			if (kotlin.math.abs(totalDrag) > 120f) {
				budgetViewModel.processIntent(BudgetEditorIntent.SetAnimState(AnimState.EDITING))
				coroutineScope.launch {
					runCatching { topSheetState.animateTo(TopSheetValue.HalfExpanded) }
				}
			}
			totalDrag = 0f
		})
	}

	LaunchedEffect(
		tutorialStage, onboardingCompleted, budgetUiState.numpadInput, isHistoryVisible
	) {
		if (!onboardingCompleted || tutorialStage == FirstLaunchTutorialStage.COMPLETED) return@LaunchedEffect
		if (shownStage == tutorialStage) return@LaunchedEffect
	}

	nightMode = isNightMode()

	BoxWithConstraints(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surface),
	) {
		val contentHeight = constraints.maxHeight.toFloat()
		val contentWidth = constraints.maxWidth.toFloat()

		val keyboardAdditionalOffset =
			windowInsets.calculateBottomPadding().minus(16.dp).coerceAtLeast(0.dp)

		val navigationBarOffset = windowInsets.calculateBottomPadding().coerceAtLeast(16.dp)

		val defaultInternalKeyboardHeight = if (windowSizeClass == WindowWidthSizeClass.Compact) {
			contentWidth
		} else {
			contentWidth / 2f
		}.coerceAtMost(with(localDensity) { 500.dp.toPx() }).coerceAtMost(contentHeight / 2)

		val systemKeyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
		val systemKeyboardHeightPx = with(localDensity) { systemKeyboardHeight.toPx() }
		val isShowSystemKeyboard = systemKeyboardHeightPx > 0f

		var keepImeLayout by remember { mutableStateOf(false) }
		var lastImeHeightPx by remember { mutableStateOf(0f) }
		LaunchedEffect(systemKeyboardHeightPx) {
			if (systemKeyboardHeightPx > 0f) {
				lastImeHeightPx = systemKeyboardHeightPx
				keepImeLayout = true
			} else {
				delay(140)
				keepImeLayout = false
			}
		}

		val calcModeKeyboardHeight =
			(contentHeight * 0.50f).coerceAtMost(contentHeight - with(localDensity) { navigationBarOffset.toPx()/* + 96.dp.toPx()*/ })

		val dragProgress = budgetUiState.dragProgress
		val effectiveProgress = if (budgetUiState.isCalculation) {
			1f - dragProgress
		} else {
			dragProgress
		}

		val internalKeyboardTarget =
			defaultInternalKeyboardHeight + (calcModeKeyboardHeight - defaultInternalKeyboardHeight) * effectiveProgress
		val targetKeyboardHeight = if (keepImeLayout && lastImeHeightPx > 0f) {
			lastImeHeightPx
		} else {
			internalKeyboardTarget
		}

		val editorHeight by remember(
			contentHeight,
			targetKeyboardHeight,
			keyboardAdditionalOffset,
			navigationBarOffset,
			budgetUiState.isCalculation,
			dragProgress
		) {
			derivedStateOf {
				contentHeight.minus(
					targetKeyboardHeight.plus(with(localDensity) {
						keyboardAdditionalOffset.toPx()
					}).coerceAtLeast(0f)
				)
					.coerceAtMost(contentHeight - with(localDensity) { navigationBarOffset.toPx() + 96.dp.toPx() })
			}
		}

		val editorHeightAnimated by animateFloatAsState(
			label = "editorHeightAnimatedValue",
			targetValue = editorHeight,
			animationSpec = tween(durationMillis = 120),
		)

		val keyboardHeightAnimated by animateFloatAsState(
			label = "keyboardHeightAnimatedValue",
			targetValue = targetKeyboardHeight,
			animationSpec = tween(durationMillis = 50),
		)

		Row {
			if (windowSizeClass != WindowWidthSizeClass.Compact) {
				Surface(
					color = colorEditor,
					modifier = Modifier
						.fillMaxSize()
						.weight(1f)
						.navigationBarsPadding(),
				) {
					Box {
						History(
							modifier = (if (tutorialStage == FirstLaunchTutorialStage.HISTORY_GESTURES) {
								Modifier
							} else {
								Modifier
							}).then(quickLogSwipeModifier),
							onQueueDeleteWithUndo = { transaction, message, _ ->
								queueDeleteWithUndo(transaction, message)
							},
							onCancelPendingDelete = { cancelPendingDelete() },
							onShowInfoSnackbar = { message -> showInfoSnackbar(message) })
						StatusBarStub()
					}
				}
				Spacer(
					Modifier
						.fillMaxHeight()
						.width(16.dp)
				)
			}

			Box(
				Modifier
					.fillMaxSize()
					.weight(1f)
			) {
				// Phone layout
				if (windowSizeClass == WindowWidthSizeClass.Compact) {
					val currentEditorHeight = with(localDensity) {
						val halfExpanedOffset =
							(-contentHeight + navigationBarOffset.toPx() + 16.dp.toPx() + editorHeightAnimated).coerceAtMost(
								0f
							)

						(topSheetState.offset.value.coerceIn(
							halfExpanedOffset, 0f
						) + contentHeight - navigationBarOffset.toPx() - 16.dp.toPx()).toDp()
					}

					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(bottom = keyboardAdditionalOffset),
						contentAlignment = Alignment.BottomCenter,
					) {
						Card(
							shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
							colors = CardDefaults.cardColors(
								containerColor = MaterialTheme.colorScheme.surface,
								contentColor = MaterialTheme.colorScheme.onSurface,
							),
							modifier = Modifier
								.fillMaxWidth()
								.height(with(localDensity) {
									keyboardHeightAnimated.toDp().coerceAtLeast(220.dp)
								})
						) {
							Box(
								modifier = Modifier
									.fillMaxSize()
									.navigationBarsPadding()
							) {
								NumpadWithViewModel(
									numberHintAnchorModifier = Modifier,
									applyHintAnchorModifier = Modifier,
									onAnyNumberTapped = {
										advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_ANY_NUMBER)
									},
									onApplyTapped = {
										Log.d("MainScreen", "Numpad check/save button pressed")
										advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_DONE_SAVE)
									},
									onShowSnackbar = { message ->
										coroutineScope.launch {
											snackbarHostState.showSnackbar(
												message = message, duration = SnackbarDuration.Short
											)
										}
									})
							}
						}
					}

					TopSheetLayout(
						swipeableState = topSheetState,
						customHalfHeight = editorHeightAnimated,
						isLockSwipeable = { budgetUiState.lockSwipeable },
						isLockDraggable = { budgetUiState.lockDraggable },
						onDismiss = {},
						sheetContentHalfExpand = {
							EditorWithViewModel(
								modifier = Modifier.requiredHeight(currentEditorHeight),
								onOpenHistory = {},
								onOpenSettings = onNavigateToSettings,
								onOpenAnalytics = onNavigateToAnalytics,
								onOpenWallet = onNavigateToWallet,
								openWalletOnStart = openWalletOnStart,
								forceWalletSetup = forceWalletSetup,
								onBudgetPillClickForTutorial = {
									advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_BUDGET_PILL)
								},
								onAnalyticsClickForTutorial = {
									advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_ANALYTICS)
								},
								budgetPillHintAnchorModifier = Modifier,
								analyticsHintAnchorModifier = Modifier
							)
						},
						sheetContentExpand = {
							History(
								modifier = Modifier.then(quickLogSwipeModifier),
								onQueueDeleteWithUndo = { transaction, message, _ ->
									queueDeleteWithUndo(transaction, message)
								},
								onCancelPendingDelete = { cancelPendingDelete() },
								onShowInfoSnackbar = { message -> showInfoSnackbar(message) })
						})

					StatusBarStub()
				} else {
					// Tablet layout - Editor on top, Numpad below
					Column(
						modifier = Modifier.fillMaxSize()
					) {
						Card(
							shape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp),
							colors = CardDefaults.cardColors(
								containerColor = colorEditor,
								contentColor = colorOnEditor,
							),
							modifier = Modifier.weight(1f)
						) {
							EditorWithViewModel(
								modifier = Modifier.fillMaxSize(),
								onOpenHistory = {},
								onOpenSettings = onNavigateToSettings,
								onOpenAnalytics = onNavigateToAnalytics,
								onOpenWallet = onNavigateToWallet,
								openWalletOnStart = openWalletOnStart,
								forceWalletSetup = forceWalletSetup,
								onBudgetPillClickForTutorial = {
									advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_BUDGET_PILL)
								},
								onAnalyticsClickForTutorial = {
									advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_ANALYTICS)
								},
								budgetPillHintAnchorModifier = Modifier,
								analyticsHintAnchorModifier = Modifier
							)
						}

						Card(
							shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
							colors = CardDefaults.cardColors(
								containerColor = MaterialTheme.colorScheme.surface,
								contentColor = MaterialTheme.colorScheme.onSurface,
							),
							modifier = Modifier
								.fillMaxWidth()
								.height(with(localDensity) {
									keyboardHeightAnimated.toDp().coerceAtLeast(220.dp)
								})
						) {
							Box(
								modifier = Modifier
									.fillMaxSize()
									.navigationBarsPadding()
							) {
								NumpadWithViewModel(
									numberHintAnchorModifier = Modifier,
									applyHintAnchorModifier = Modifier,
									onAnyNumberTapped = {
										advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_ANY_NUMBER)
									},
									onApplyTapped = {
										Log.d("MainScreen", "Numpad check/save button pressed")
										advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_DONE_SAVE)
									},
									onShowSnackbar = { message ->
										coroutineScope.launch {
											snackbarHostState.showSnackbar(
												message = message, duration = SnackbarDuration.Short
											)
										}
									})
							}
						}
					}
				}
			}
		}

		SnackbarHost(
			hostState = snackbarHostState,
			modifier = Modifier
				.align(Alignment.BottomCenter)
				.padding(horizontal = 16.dp, vertical = 20.dp)
				.navigationBarsPadding()
		)
	}
}

@Composable
fun StatusBarStub() {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.requiredHeight(
				LocalWindowInsets.current.calculateTopPadding()
			)
			.background(colorEditor)
	)
}
