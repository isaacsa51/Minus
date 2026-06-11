package com.serranoie.app.minus.presentation.ui.home

import android.util.Log
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.SwipeableState
import androidx.wear.compose.material.rememberSwipeableState
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.LocalWindowSize
import com.serranoie.app.minus.presentation.ONBOARDING_COMPLETED_KEY
import com.serranoie.app.minus.presentation.settingsDataStore
import com.serranoie.app.minus.presentation.ui.budget.BudgetUiState
import com.serranoie.app.minus.presentation.ui.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetEditorIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetTransactionIntent
import com.serranoie.app.minus.presentation.ui.editor.AnimState
import com.serranoie.app.minus.presentation.ui.editor.Editor
import com.serranoie.app.minus.presentation.ui.history.History
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorButton
import com.serranoie.app.minus.presentation.ui.theme.colorEditor
import com.serranoie.app.minus.presentation.ui.theme.colorOnEditor
import com.serranoie.app.minus.presentation.ui.theme.component.TopSheetLayout
import com.serranoie.app.minus.presentation.ui.theme.component.TopSheetValue
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditorState
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Numpad
import com.serranoie.app.minus.presentation.ui.theme.isNightMode
import com.serranoie.app.minus.presentation.ui.tutorial.FIRST_LAUNCH_TUTORIAL_STAGE_KEY
import com.serranoie.app.minus.presentation.ui.tutorial.FirstLaunchTutorialStage
import com.serranoie.app.minus.presentation.ui.tutorial.firstLaunchTutorialStageFlow
import com.serranoie.app.minus.presentation.util.StatusBarPadding
import com.serranoie.app.minus.presentation.util.Utils.toggleFeedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds
import com.serranoie.app.minus.presentation.ui.editor.EditMode as EditorEditMode
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditMode as NumpadEditMode

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
	val budgetUiState by budgetViewModel.uiState.collectAsStateWithLifecycle()
	val context = LocalContext.current
	val coroutineScope = rememberCoroutineScope()

	val onboardingCompletedFlow = remember(context) {
		context.settingsDataStore.data.map { it[ONBOARDING_COMPLETED_KEY] ?: false }
	}
	val onboardingCompleted by onboardingCompletedFlow.collectAsStateWithLifecycle(initialValue = false)
	val tutorialStage by context.firstLaunchTutorialStageFlow()
		.collectAsStateWithLifecycle(initialValue = FirstLaunchTutorialStage.COMPLETED)

	MainScreenContent(
		budgetUiState = budgetUiState,
		onboardingCompleted = onboardingCompleted,
		tutorialStage = tutorialStage,
		onNavigateToAnalytics = onNavigateToAnalytics,
		onNavigateToSettings = onNavigateToSettings,
		onNavigateToWallet = onNavigateToWallet,
		openWalletOnStart = openWalletOnStart,
		forceWalletSetup = forceWalletSetup,
		onProcessIntent = { intent ->
			when (intent) {
				is BudgetEditorIntent -> budgetViewModel.processIntent(intent)
				is BudgetTransactionIntent -> budgetViewModel.processIntent(intent)
				is BudgetNumpadIntent -> budgetViewModel.processIntent(intent)
			}
		},
		onAdvanceTutorial = { expected ->
			if (tutorialStage != expected) return@MainScreenContent
			coroutineScope.launch {
				context.settingsDataStore.edit { prefs ->
					prefs[FIRST_LAUNCH_TUTORIAL_STAGE_KEY] = expected.next().name
				}
			}
		},
		history = { modifier, onQueueDeleteWithUndo, onCancelPendingDelete, onShowInfoSnackbar ->
			History(
				modifier = modifier.background(colorButton),
				onQueueDeleteWithUndo = onQueueDeleteWithUndo,
				onCancelPendingDelete = onCancelPendingDelete,
				onShowInfoSnackbar = onShowInfoSnackbar
			)
		})
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalWearMaterialApi::class)
@Composable
private fun MainScreenContent(
	budgetUiState: BudgetUiState,
	onboardingCompleted: Boolean,
	tutorialStage: FirstLaunchTutorialStage,
	onNavigateToAnalytics: () -> Unit,
	onNavigateToSettings: () -> Unit,
	onNavigateToWallet: () -> Unit,
	openWalletOnStart: Boolean,
	forceWalletSetup: Boolean,
	onProcessIntent: (Any) -> Unit,
	onAdvanceTutorial: (FirstLaunchTutorialStage) -> Unit,
	history: @Composable (
		Modifier,
		onQueueDeleteWithUndo: (transaction: Transaction, message: String, onUndo: () -> Unit) -> Unit,
		onCancelPendingDelete: () -> Unit,
		onShowInfoSnackbar: (message: String) -> Unit
	) -> Unit
) {
	val topSheetState = rememberSwipeableState(TopSheetValue.HalfExpanded)
	var nightMode by remember { mutableStateOf(false) }
	val coroutineScope = rememberCoroutineScope()

	val localDensity = LocalDensity.current
	val windowSizeClass = LocalWindowSize.current
	val windowInsets = LocalWindowInsets.current
	val configuration = LocalConfiguration.current
	val shouldExpandRail = windowSizeClass == WindowWidthSizeClass.Expanded &&
		configuration.screenWidthDp > configuration.screenHeightDp

	var shownStage by remember { mutableStateOf<FirstLaunchTutorialStage?>(null) }

	val isHistoryVisible =
		windowSizeClass != WindowWidthSizeClass.Compact || topSheetState.currentValue == TopSheetValue.Expanded

	var pendingDeleteTransaction by remember {
		mutableStateOf<Transaction?>(
			null
		)
	}
	var snackbarAutoDismissJob by remember { mutableStateOf<Job?>(null) }
	var pendingDeleteJob by remember { mutableStateOf<Job?>(null) }
	val snackbarHostState = remember { SnackbarHostState() }

	fun executeDelete(transaction: Transaction) {
		onProcessIntent(BudgetTransactionIntent.DeleteTransactionTapped(transaction))
	}

	fun cancelPendingDelete() {
		pendingDeleteJob?.cancel()
		pendingDeleteJob = null
		snackbarAutoDismissJob?.cancel()
		snackbarAutoDismissJob = null
		pendingDeleteTransaction?.let { tx ->
			onProcessIntent(BudgetTransactionIntent.RestoreTransactionTapped(tx))
		}
		pendingDeleteTransaction = null
		snackbarHostState.currentSnackbarData?.dismiss()
	}

	fun queueDeleteWithUndo(
		transaction: Transaction, message: String
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
			delay(3500L.milliseconds)
			pendingDeleteTransaction = null
			pendingDeleteJob = null
		}
	}

	fun showInfoSnackbar(message: String) {
		snackbarAutoDismissJob?.cancel()
		coroutineScope.launch {
			snackbarHostState.showSnackbar(
				message = message,
				duration = SnackbarDuration.Short,
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
				onProcessIntent(BudgetEditorIntent.SetAnimState(AnimState.EDITING))
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

	LaunchedEffect(windowSizeClass) {
		if (windowSizeClass != WindowWidthSizeClass.Compact && !budgetUiState.isCalculation) {
			onProcessIntent(BudgetNumpadIntent.SetCalculationMode(true))
		}
	}

	Row(modifier = Modifier.fillMaxSize()) {
		if (windowSizeClass != WindowWidthSizeClass.Compact) {
			MainNavigationRail(
				expanded = shouldExpandRail,
				onNavigateToAnalytics = onNavigateToAnalytics,
				onNavigateToSettings = onNavigateToSettings,
			)
		}

		BoxWithConstraints(
			modifier = Modifier
				.weight(1f)
				.fillMaxHeight()
				.background(MaterialTheme.colorScheme.surface),
		) {
			val contentHeight = constraints.maxHeight.toFloat()
			val contentWidth = constraints.maxWidth.toFloat()

			if (windowSizeClass == WindowWidthSizeClass.Compact) {
				PhoneLayout(
					budgetUiState = budgetUiState,
					topSheetState = topSheetState,
					contentHeight = contentHeight,
					contentWidth = contentWidth,
					localDensity = localDensity,
					windowInsets = windowInsets,
					onNavigateToSettings = onNavigateToSettings,
					onNavigateToAnalytics = onNavigateToAnalytics,
					onNavigateToWallet = onNavigateToWallet,
					openWalletOnStart = openWalletOnStart,
					forceWalletSetup = forceWalletSetup,
					onProcessIntent = onProcessIntent,
					onAdvanceTutorial = onAdvanceTutorial,
					history = history,
					quickLogSwipeModifier = quickLogSwipeModifier,
					queueDeleteWithUndo = ::queueDeleteWithUndo,
					cancelPendingDelete = ::cancelPendingDelete,
					showInfoSnackbar = ::showInfoSnackbar,
					snackbarHostState = snackbarHostState,
				)
			} else {
				TabletLayout(
					budgetUiState = budgetUiState,
					contentHeight = contentHeight,
					contentWidth = contentWidth,
					localDensity = localDensity,
					windowInsets = windowInsets,
					onNavigateToWallet = onNavigateToWallet,
					openWalletOnStart = openWalletOnStart,
					forceWalletSetup = forceWalletSetup,
					onProcessIntent = onProcessIntent,
					onAdvanceTutorial = onAdvanceTutorial,
					history = history,
					quickLogSwipeModifier = quickLogSwipeModifier,
					queueDeleteWithUndo = ::queueDeleteWithUndo,
					cancelPendingDelete = ::cancelPendingDelete,
					showInfoSnackbar = ::showInfoSnackbar,
					snackbarHostState = snackbarHostState,
				)
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
}

@Composable
private fun MainNavigationRail(
	expanded: Boolean,
	onNavigateToAnalytics: () -> Unit,
	onNavigateToSettings: () -> Unit,
) {
	val itemModifier = if (expanded) Modifier.fillMaxWidth() else Modifier

	NavigationRail(
		modifier = if (expanded) Modifier.width(104.dp) else Modifier,
		containerColor = MaterialTheme.colorScheme.surface,
		contentColor = MaterialTheme.colorScheme.onSurface,
	) {
		Spacer(Modifier.weight(1f))
		NavigationRailItem(
			modifier = itemModifier,
			selected = false,
			onClick = onNavigateToAnalytics,
			icon = { Icon(Icons.Rounded.BarChart, contentDescription = "Analytics") },
			label = { Text("Analytics") }
		)
		NavigationRailItem(
			modifier = itemModifier,
			selected = false,
			onClick = onNavigateToSettings,
			icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
			label = { Text("Settings") }
		)
		Spacer(Modifier.weight(1f))
	}
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalWearMaterialApi::class)
@Composable
private fun PhoneLayout(
	budgetUiState: BudgetUiState,
	topSheetState: SwipeableState<TopSheetValue>,
	contentHeight: Float,
	contentWidth: Float,
	localDensity: Density,
	windowInsets: PaddingValues,
	onNavigateToSettings: () -> Unit,
	onNavigateToAnalytics: () -> Unit,
	onNavigateToWallet: () -> Unit,
	openWalletOnStart: Boolean,
	forceWalletSetup: Boolean,
	onProcessIntent: (Any) -> Unit,
	onAdvanceTutorial: (FirstLaunchTutorialStage) -> Unit,
	history: @Composable (
		Modifier,
		onQueueDeleteWithUndo: (transaction: Transaction, message: String, onUndo: () -> Unit) -> Unit,
		onCancelPendingDelete: () -> Unit,
		onShowInfoSnackbar: (message: String) -> Unit
	) -> Unit,
	quickLogSwipeModifier: Modifier,
	queueDeleteWithUndo: (Transaction, String) -> Unit,
	cancelPendingDelete: () -> Unit,
	showInfoSnackbar: (String) -> Unit,
	snackbarHostState: SnackbarHostState,
) {
	val coroutineScope = rememberCoroutineScope()
	val navigationBarOffset = windowInsets.calculateBottomPadding()
	val navBarHeightPx = with(localDensity) { navigationBarOffset.toPx() }

	val defaultInternalKeyboardHeightBase =
		contentWidth.coerceAtMost(with(localDensity) { 500.dp.toPx() }).coerceAtMost(contentHeight / 2)
	val rowHeightPx = defaultInternalKeyboardHeightBase / 4
	val defaultInternalKeyboardHeight = rowHeightPx * 4
	val calcModeKeyboardHeight = rowHeightPx * 5

	var localDragProgress by remember { mutableFloatStateOf(0f) }

	val systemKeyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
	val systemKeyboardHeightPx = with(localDensity) { systemKeyboardHeight.toPx() }

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

	val effectiveProgress = if (budgetUiState.isCalculation) {
		1f - localDragProgress
	} else {
		localDragProgress
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
		navBarHeightPx,
		budgetUiState.isCalculation,
		localDragProgress
	) {
		derivedStateOf {
			contentHeight.minus(
				targetKeyboardHeight.plus(navBarHeightPx).coerceAtLeast(0f)
			)
				.coerceAtMost(contentHeight - (navBarHeightPx + with(localDensity) { 96.dp.toPx() }))
		}
	}

	val keyboardAnimationSpec = remember<AnimationSpec<Float>>(localDragProgress) {
		if (localDragProgress > 0f && localDragProgress < 1f) {
			tween(durationMillis = 0)
		} else {
			tween(durationMillis = 200, easing = FastOutSlowInEasing)
		}
	}

	val editorHeightAnimated by animateFloatAsState(
		label = "editorHeightAnimatedValue",
		targetValue = editorHeight,
		animationSpec = keyboardAnimationSpec,
	)

	val keyboardHeightAnimated by animateFloatAsState(
		label = "keyboardHeightAnimatedValue",
		targetValue = targetKeyboardHeight,
		animationSpec = keyboardAnimationSpec,
	)

	val currentEditorHeight = with(localDensity) {
		val halfExpanedOffset =
			(-contentHeight + navBarHeightPx + with(localDensity) { 16.dp.toPx() } + editorHeightAnimated).coerceAtMost(
				0f
			)

		(topSheetState.offset.value.coerceIn(
			halfExpanedOffset, 0f
		) + contentHeight - navBarHeightPx - with(localDensity) { 16.dp.toPx() }).toDp()
	}

	Box(
		modifier = Modifier.fillMaxSize(),
		contentAlignment = Alignment.BottomCenter,
	) {
		val halfExpandedOffsetPx = with(localDensity) {
			(-contentHeight + navBarHeightPx + 16.dp.toPx() + editorHeightAnimated).coerceAtMost(0f)
		}
		val isSheetExpanding by remember(halfExpandedOffsetPx) {
			derivedStateOf {
				topSheetState.offset.value > halfExpandedOffsetPx + 10f
			}
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
					(keyboardHeightAnimated + navBarHeightPx).toDp()
				})
				.zIndex(if (isSheetExpanding) 0f else 1f)
		) {
			Box(
				modifier = Modifier.fillMaxSize(),
				contentAlignment = Alignment.BottomCenter
			) {
				val editorState = remember(budgetUiState) {
					EditorState(
						mode = when (budgetUiState.editMode) {
							EditorEditMode.ADD -> NumpadEditMode.ADD
							EditorEditMode.EDIT -> NumpadEditMode.EDIT
						},
						rawSpentValue = budgetUiState.numpadInput,
						stage = if (budgetUiState.numpadInput.isNotEmpty()) EditStage.EDIT_SPENT else EditStage.IDLE,
						currentSpent = budgetUiState.numpadInput,
						currentComment = budgetUiState.currentComment,
						editedTransaction = null
					)
				}
				Numpad(
					editorState = editorState,
					numberHintAnchorModifier = Modifier,
					applyHintAnchorModifier = Modifier,
					onNumberInput = { digit ->
						onProcessIntent(BudgetNumpadIntent.NumberTapped(digit.toString()))
						onAdvanceTutorial(FirstLaunchTutorialStage.TAP_ANY_NUMBER)
					},
					onDotInput = { onProcessIntent(BudgetNumpadIntent.DotTapped) },
					onBackspace = { onProcessIntent(BudgetNumpadIntent.BackspaceTapped) },
					onBackspaceLongPress = { onProcessIntent(BudgetNumpadIntent.ResetInputTapped) },
					onOperatorInput = { op ->
						onProcessIntent(
							BudgetNumpadIntent.OperatorTapped(
								op
							)
						)
					},
					onEqualsInput = { onProcessIntent(BudgetNumpadIntent.EqualsTapped) },
					onApply = {
						Log.d("MainScreen", "Numpad check/save button pressed")
						onProcessIntent(BudgetNumpadIntent.ApplyTapped)
						onAdvanceTutorial(FirstLaunchTutorialStage.TAP_DONE_SAVE)
					},
					onDragProgressChanged = { progress ->
						localDragProgress = progress
					},
					dragProgress = effectiveProgress,
					isCalculation = budgetUiState.isCalculation,
					onCalculationModeChanged = { enabled ->
						onProcessIntent(
							BudgetNumpadIntent.SetCalculationMode(
								enabled
							)
						)
					},
					onShowSnackbar = { message ->
						coroutineScope.launch {
							snackbarHostState.showSnackbar(
								message = message, duration = SnackbarDuration.Short
							)
						}
					},
					rowHeight = with(localDensity) { rowHeightPx.toDp() }
				)
			}
		}

		TopSheetLayout(
			swipeableState = topSheetState,
			customHalfHeight = editorHeightAnimated,
			isLockSwipeable = {
				budgetUiState.lockSwipeable || localDragProgress > 0f || (budgetUiState.isCalculation && effectiveProgress < 1f) || (!budgetUiState.isCalculation && effectiveProgress > 0f)
			},
			isLockDraggable = {
				budgetUiState.lockDraggable || localDragProgress > 0f || (budgetUiState.isCalculation && effectiveProgress < 1f) || (!budgetUiState.isCalculation && effectiveProgress > 0f)
			},
			onDismiss = {},
			sheetContentHalfExpand = {
				Editor(
					uiState = budgetUiState,
					animState = budgetUiState.animState,
					modifier = Modifier.requiredHeight(currentEditorHeight),
					onOpenHistory = {},
					onOpenSettings = onNavigateToSettings,
					onOpenAnalytics = onNavigateToAnalytics,
					onOpenWallet = onNavigateToWallet,
					openWalletOnStart = openWalletOnStart,
					forceWalletSetup = forceWalletSetup,
					onBudgetPillClickForTutorial = {
						onAdvanceTutorial(FirstLaunchTutorialStage.TAP_BUDGET_PILL)
					},
					onAnalyticsClickForTutorial = {
						onAdvanceTutorial(FirstLaunchTutorialStage.TAP_ANALYTICS)
					},
					onFocus = {
						if (budgetUiState.numpadInput.isNotEmpty() && budgetUiState.animState != AnimState.EDITING) {
							onProcessIntent(BudgetEditorIntent.SetAnimState(AnimState.EDITING))
						}
					},
					onCommentClick = {},
					onCommentUpdate = { comment ->
						onProcessIntent(
							BudgetEditorIntent.CommentUpdated(
								comment
							)
						)
					},
					onDeleteTag = { tag ->
						onProcessIntent(
							BudgetEditorIntent.DeleteTag(
								tag
							)
						)
					},
					onRecurrentToggle = { enabled ->
						onProcessIntent(
							BudgetEditorIntent.SetRecurrentEnabled(
								enabled
							)
						)
					},
					onCreditToggle = { enabled ->
						onProcessIntent(
							BudgetEditorIntent.SetCreditEnabled(
								enabled
							)
						)
					},
					onDismissRecurrentDialog = { onProcessIntent(BudgetEditorIntent.DismissRecurrentDialog) },
					onDismissCreditCutoffDialog = { onProcessIntent(BudgetEditorIntent.DismissCreditCutoffDialog) },
					onRecurrentExpenseConfirm = { freq, date, day ->
						onProcessIntent(
							BudgetEditorIntent.RecurrentExpenseApplied(
								freq, date, day
							)
						)
					},
					onCreditCutoffConfirm = { day ->
						onProcessIntent(
							BudgetEditorIntent.CreditCutoffDayConfirmed(
								day
							)
						)
					},
					onFinishBudgetEarly = { onProcessIntent(BudgetEditorIntent.FinishBudgetEarly) },
					onSaveBudget = { settings ->
						onProcessIntent(
							BudgetEditorIntent.UpdateSettings(
								settings
							)
						)
					},
					budgetPillHintAnchorModifier = Modifier,
					analyticsHintAnchorModifier = Modifier
				)
			},
			sheetContentExpand = {
				if (topSheetState.targetValue == TopSheetValue.Expanded || topSheetState.currentValue == TopSheetValue.Expanded) {
					history(
						Modifier.then(quickLogSwipeModifier),
						{ transaction, message, _ ->
							queueDeleteWithUndo(transaction, message)
						},
						{ cancelPendingDelete() },
						{ message -> showInfoSnackbar(message) })
				}
			})

		LaunchedEffect(budgetUiState.isCalculation) {
			localDragProgress = 0f
		}
	}

	StatusBarPadding()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabletLayout(
	budgetUiState: BudgetUiState,
	contentHeight: Float,
	contentWidth: Float,
	localDensity: Density,
	windowInsets: PaddingValues,
	onNavigateToWallet: () -> Unit,
	openWalletOnStart: Boolean,
	forceWalletSetup: Boolean,
	onProcessIntent: (Any) -> Unit,
	onAdvanceTutorial: (FirstLaunchTutorialStage) -> Unit,
	history: @Composable (
		Modifier,
		onQueueDeleteWithUndo: (transaction: Transaction, message: String, onUndo: () -> Unit) -> Unit,
		onCancelPendingDelete: () -> Unit,
		onShowInfoSnackbar: (message: String) -> Unit
	) -> Unit,
	quickLogSwipeModifier: Modifier,
	queueDeleteWithUndo: (Transaction, String) -> Unit,
	cancelPendingDelete: () -> Unit,
	showInfoSnackbar: (String) -> Unit,
	snackbarHostState: SnackbarHostState,
) {
	val coroutineScope = rememberCoroutineScope()
	val navigationBarOffset = windowInsets.calculateBottomPadding()
	val navBarHeightPx = with(localDensity) { navigationBarOffset.toPx() }

	val defaultInternalKeyboardHeightBase =
		(contentWidth / 2f).coerceAtMost(with(localDensity) { 500.dp.toPx() }).coerceAtMost(contentHeight / 2)
	val rowHeightPx = defaultInternalKeyboardHeightBase / 4
	val defaultInternalKeyboardHeight = rowHeightPx * 4
	val calcModeKeyboardHeight = rowHeightPx * 5

	var localDragProgress by remember { mutableFloatStateOf(0f) }

	val effectiveProgress = if (budgetUiState.isCalculation) {
		1f - localDragProgress
	} else {
		localDragProgress
	}

	val internalKeyboardTarget =
		defaultInternalKeyboardHeight + (calcModeKeyboardHeight - defaultInternalKeyboardHeight) * effectiveProgress
	val targetKeyboardHeight = internalKeyboardTarget

	val keyboardAnimationSpec = remember<AnimationSpec<Float>>(localDragProgress) {
		if (localDragProgress > 0f && localDragProgress < 1f) {
			tween(durationMillis = 0)
		} else {
			tween(durationMillis = 200, easing = FastOutSlowInEasing)
		}
	}

	val keyboardHeightAnimated by animateFloatAsState(
		label = "keyboardHeightAnimatedValue",
		targetValue = targetKeyboardHeight,
		animationSpec = keyboardAnimationSpec,
	)

	Row(modifier = Modifier.fillMaxSize()) {
		// History pane (50%)
		Box(
			modifier = Modifier
				.weight(1f)
				.fillMaxHeight()
				.background(colorEditor)
				.navigationBarsPadding(),
		) {
			history(
				quickLogSwipeModifier,
				{ transaction, message, _ -> queueDeleteWithUndo(transaction, message) },
				{ cancelPendingDelete() },
				{ message -> showInfoSnackbar(message) }
			)
			StatusBarPadding()
		}

		VerticalDivider(
			modifier = Modifier.padding(horizontal = 8.dp),
			thickness = 1.dp,
			color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
		)

		// Editor and Numpad pane (50%)
		Column(
			modifier = Modifier
				.weight(1f)
				.fillMaxHeight()
		) {
			Card(
				shape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp),
				colors = CardDefaults.cardColors(
					containerColor = colorEditor,
					contentColor = colorOnEditor,
				),
				modifier = Modifier.weight(1f)
			) {
				Editor(
					uiState = budgetUiState,
					animState = budgetUiState.animState,
					modifier = Modifier.fillMaxSize(),
					onOpenHistory = {},
					onOpenSettings = {},
					onOpenAnalytics = {},
					onOpenWallet = onNavigateToWallet,
					openWalletOnStart = openWalletOnStart,
					forceWalletSetup = forceWalletSetup,
					onBudgetPillClickForTutorial = {
						onAdvanceTutorial(FirstLaunchTutorialStage.TAP_BUDGET_PILL)
					},
					onAnalyticsClickForTutorial = {
						onAdvanceTutorial(FirstLaunchTutorialStage.TAP_ANALYTICS)
					},
					onFocus = {
						if (budgetUiState.numpadInput.isNotEmpty() && budgetUiState.animState != AnimState.EDITING) {
							onProcessIntent(BudgetEditorIntent.SetAnimState(AnimState.EDITING))
						}
					},
					onCommentClick = {},
					onCommentUpdate = { comment -> onProcessIntent(BudgetEditorIntent.CommentUpdated(comment)) },
					onDeleteTag = { tag -> onProcessIntent(BudgetEditorIntent.DeleteTag(tag)) },
					onRecurrentToggle = { enabled -> onProcessIntent(BudgetEditorIntent.SetRecurrentEnabled(enabled)) },
					onCreditToggle = { enabled -> onProcessIntent(BudgetEditorIntent.SetCreditEnabled(enabled)) },
					onDismissRecurrentDialog = { onProcessIntent(BudgetEditorIntent.DismissRecurrentDialog) },
					onDismissCreditCutoffDialog = { onProcessIntent(BudgetEditorIntent.DismissCreditCutoffDialog) },
					onRecurrentExpenseConfirm = { freq, date, day ->
						onProcessIntent(BudgetEditorIntent.RecurrentExpenseApplied(freq, date, day))
					},
					onCreditCutoffConfirm = { day -> onProcessIntent(BudgetEditorIntent.CreditCutoffDayConfirmed(day)) },
					onFinishBudgetEarly = { onProcessIntent(BudgetEditorIntent.FinishBudgetEarly) },
					onSaveBudget = { settings -> onProcessIntent(BudgetEditorIntent.UpdateSettings(settings)) },
					showAnalyticsButton = false,
					showSettingsButton = false,
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
						(keyboardHeightAnimated + navBarHeightPx).toDp()
					})
			) {
				Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.BottomCenter
				) {
					val editorState = remember(budgetUiState) {
						EditorState(
							mode = when (budgetUiState.editMode) {
								EditorEditMode.ADD -> NumpadEditMode.ADD
								EditorEditMode.EDIT -> NumpadEditMode.EDIT
							},
							rawSpentValue = budgetUiState.numpadInput,
							stage = if (budgetUiState.numpadInput.isNotEmpty()) EditStage.EDIT_SPENT else EditStage.IDLE,
							currentSpent = budgetUiState.numpadInput,
							currentComment = budgetUiState.currentComment,
							editedTransaction = null
						)
					}
					Numpad(
						editorState = editorState,
						numberHintAnchorModifier = Modifier,
						applyHintAnchorModifier = Modifier,
						onNumberInput = { digit ->
							onProcessIntent(BudgetNumpadIntent.NumberTapped(digit.toString()))
							onAdvanceTutorial(FirstLaunchTutorialStage.TAP_ANY_NUMBER)
						},
						onDotInput = { onProcessIntent(BudgetNumpadIntent.DotTapped) },
						onBackspace = { onProcessIntent(BudgetNumpadIntent.BackspaceTapped) },
						onBackspaceLongPress = { onProcessIntent(BudgetNumpadIntent.ResetInputTapped) },
						onOperatorInput = { op -> onProcessIntent(BudgetNumpadIntent.OperatorTapped(op)) },
						onEqualsInput = { onProcessIntent(BudgetNumpadIntent.EqualsTapped) },
						onApply = {
							Log.d("MainScreen", "Numpad check/save button pressed")
							onProcessIntent(BudgetNumpadIntent.ApplyTapped)
							onAdvanceTutorial(FirstLaunchTutorialStage.TAP_DONE_SAVE)
						},
						onDragProgressChanged = { progress ->
							localDragProgress = progress
						},
						dragProgress = localDragProgress,
						isCalculation = budgetUiState.isCalculation,
						onCalculationModeChanged = { enabled ->
							onProcessIntent(BudgetNumpadIntent.SetCalculationMode(enabled))
							localDragProgress = 0f
						},
						onShowSnackbar = { message ->
							coroutineScope.launch {
								snackbarHostState.showSnackbar(
									message = message, duration = SnackbarDuration.Short
								)
							}
						},
						rowHeight = with(localDensity) { rowHeightPx.toDp() }
					)
				}
			}
		}
	}
}

@Preview
@PreviewScreenSizes
@Composable
private fun MainScreenPreview() {
	val configuration = LocalConfiguration.current
	val windowSizeClass = when {
		configuration.screenWidthDp < 600 -> WindowWidthSizeClass.Compact
		configuration.screenWidthDp < 840 -> WindowWidthSizeClass.Medium
		else -> WindowWidthSizeClass.Expanded
	}

	CompositionLocalProvider(LocalWindowSize provides windowSizeClass) {
		MinusTheme {
			MainScreenContent(
				budgetUiState = BudgetUiState(
					budgetSettings = BudgetSettings(
						totalBudget = BigDecimal("500.00"),
						period = BudgetPeriod.DAILY,
						startDate = LocalDate.now(),
						currencyCode = "USD"
					), budgetState = BudgetState(
						remainingToday = BigDecimal("110.00"),
						totalSpentToday = BigDecimal("12.50"),
						dailyBudget = BigDecimal("122.50"),
						daysRemaining = 15,
						progress = 0.1f,
						isOverBudget = false,
						totalBudget = BigDecimal("500.00"),
						totalSpentInPeriod = BigDecimal("12.50")
					), transactions = emptyList(), numpadInput = "", isNumpadValid = false
				),
				onboardingCompleted = true,
				tutorialStage = FirstLaunchTutorialStage.COMPLETED,
				onNavigateToAnalytics = {},
				onNavigateToSettings = {},
				onNavigateToWallet = {},
				openWalletOnStart = false,
				forceWalletSetup = false,
				onProcessIntent = {},
				onAdvanceTutorial = {},
				history = { modifier, _, _, _ ->
					Box(
						modifier = modifier
							.fillMaxSize()
							.background(colorButton),
						contentAlignment = Alignment.Center
					) {
						Text("History Placeholder")
					}
				})
		}
	}
}
