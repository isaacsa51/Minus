package com.serranoie.app.minus.presentation.ui.home

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Snackbar
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.SwipeableState
import androidx.wear.compose.material.rememberSwipeableState
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.FirstLaunchTutorialStage
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.LocalWindowSize
import com.serranoie.app.minus.presentation.ui.budget.BudgetUiState
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetEditorIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.ui.editor.AnimState
import com.serranoie.app.minus.presentation.ui.editor.Editor
import com.serranoie.app.minus.presentation.ui.history.HistoryScreen
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorButton
import com.serranoie.app.minus.presentation.ui.theme.colorEditor
import com.serranoie.app.minus.presentation.ui.theme.colorOnEditor
import com.serranoie.app.minus.presentation.ui.theme.component.TopSheetLayout
import com.serranoie.app.minus.presentation.ui.theme.component.TopSheetValue
import com.serranoie.app.minus.presentation.ui.theme.component.animatedHeightPx
import com.serranoie.app.minus.presentation.ui.theme.component.animatedRequiredHeightPx
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditorState
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Numpad
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.SavedCategoriesGrid
import com.serranoie.app.minus.presentation.ui.theme.isNightMode
import com.serranoie.app.minus.presentation.ui.tutorial.TutorialBoxState
import com.serranoie.app.minus.presentation.ui.tutorial.markForTutorial
import com.serranoie.app.minus.presentation.util.LocalCensorMode
import com.serranoie.app.minus.presentation.util.StatusBarPadding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import com.serranoie.app.minus.presentation.ui.editor.EditMode as EditorEditMode
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditMode as NumpadEditMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalWearMaterialApi::class)
@Composable
fun MainScreenContent(
    mainScreenState: MainScreenUiState,
    budgetUiState: BudgetUiState,
    actions: MainScreenActions,
    openWalletOnStart: Boolean,
    tutorialBoxState: TutorialBoxState? = null,
) {
    val onboardingCompleted = mainScreenState.onboardingCompleted
    val tutorialStage = mainScreenState.tutorialStage
    val showCreditQuickToggleFeature = mainScreenState.showCreditQuickToggleFeature
    val directCategoryPopupEnabled = mainScreenState.directCategoryPopupEnabled
    val categoryGridModeEnabled = mainScreenState.categoryGridModeEnabled
    val showBudgetPeriodSheet = mainScreenState.showBudgetPeriodSheet
    val forceBudgetPeriodSheetSetup = mainScreenState.forceBudgetPeriodSheetSetup
    val selectedViewPeriod = mainScreenState.selectedViewPeriod

    val topSheetState = rememberSwipeableState(TopSheetValue.HalfExpanded)
    var nightMode by remember { mutableStateOf(false) }
    var showCategoryGrid by remember { mutableStateOf(false) }

    LaunchedEffect(budgetUiState.numpadInput) {
        if (budgetUiState.numpadInput.isEmpty() && showCategoryGrid) {
            showCategoryGrid = false
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val localDensity = LocalDensity.current
    val windowSizeClass = LocalWindowSize.current
    val windowInsets = LocalWindowInsets.current
    val configuration = LocalConfiguration.current
    val shouldExpandRail =
        windowSizeClass == WindowWidthSizeClass.Expanded && configuration.screenWidthDp > configuration.screenHeightDp

    val isHistoryVisible =
        windowSizeClass != WindowWidthSizeClass.Compact || topSheetState.currentValue == TopSheetValue.Expanded

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(
        mainScreenState.isSnackbarVisible,
        mainScreenState.snackbarMessage,
        mainScreenState.snackbarActionLabel,
    ) {
        if (mainScreenState.isSnackbarVisible) {
            val result =
                snackbarHostState.showSnackbar(
                    message = mainScreenState.snackbarMessage,
                    actionLabel = mainScreenState.snackbarActionLabel,
                    duration = SnackbarDuration.Short,
                )
            when (result) {
                SnackbarResult.ActionPerformed -> actions.onProcessIntent(MainScreenUiIntent.CancelPendingDelete)
                SnackbarResult.Dismissed -> actions.onProcessIntent(MainScreenUiIntent.DismissSnackbar)
            }
        }
    }

    fun queueDeleteWithUndo(
        transaction: Transaction,
        message: String,
    ) {
        actions.onProcessIntent(MainScreenUiIntent.QueueDeleteWithUndo(transaction, message))
    }

    fun cancelPendingDelete() {
        actions.onProcessIntent(MainScreenUiIntent.CancelPendingDelete)
    }

    fun showInfoSnackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    val quickLogSwipeModifier =
        Modifier.pointerInput(isHistoryVisible) {
            if (!isHistoryVisible) return@pointerInput
            var totalDrag = 0f
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                onDragEnd = {
                    if (abs(totalDrag) > 120f) {
                        actions.onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.SetAnimState(AnimState.EDITING),
                            ),
                        )
                        coroutineScope.launch {
                            runCatching { topSheetState.animateTo(TopSheetValue.HalfExpanded) }
                        }
                    }
                    totalDrag = 0f
                },
            )
        }

    LaunchedEffect(
        tutorialStage,
        onboardingCompleted,
        budgetUiState.numpadInput,
        isHistoryVisible,
    ) {
        if (!onboardingCompleted || tutorialStage == FirstLaunchTutorialStage.COMPLETED) return@LaunchedEffect
        if (mainScreenState.shownStage == tutorialStage) return@LaunchedEffect
        actions.onProcessIntent(MainScreenUiIntent.SetShownStage(tutorialStage))
    }

    nightMode = isNightMode()

    LaunchedEffect(windowSizeClass) {
        if (windowSizeClass != WindowWidthSizeClass.Compact && !budgetUiState.isCalculation) {
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                    BudgetNumpadIntent.SetCalculationMode(true),
                ),
            )
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        if (windowSizeClass == WindowWidthSizeClass.Expanded) {
            MainNavigationRail(
                expanded = shouldExpandRail,
                onNavigateToAnalytics = actions.onNavigateToAnalytics,
                onNavigateToSettings = actions.onNavigateToSettings,
                tutorialBoxState = tutorialBoxState,
            )
        }

        BoxWithConstraints(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            val contentHeight = constraints.maxHeight.toFloat()
            val contentWidth = constraints.maxWidth.toFloat()

            if (windowSizeClass != WindowWidthSizeClass.Expanded) {
                val actionsForLayout =
                    remember(actions, snackbarHostState) {
                        actions.copy(
                            onShowSnackbar = { message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short,
                                    )
                                }
                            },
                        )
                    }

                val featureFlagsForLayout =
                    remember(
                        showCreditQuickToggleFeature,
                        directCategoryPopupEnabled,
                        categoryGridModeEnabled,
                    ) {
                        MainScreenFeatureFlags(
                            showCreditQuickToggleFeature = showCreditQuickToggleFeature,
                            directCategoryPopupEnabled = directCategoryPopupEnabled,
                            categoryGridModeEnabled = categoryGridModeEnabled,
                        )
                    }

                val budgetPeriodStateForLayout =
                    remember(
                        showBudgetPeriodSheet,
                        forceBudgetPeriodSheetSetup,
                        selectedViewPeriod,
                        actions,
                    ) {
                        MainScreenBudgetPeriodState(
                            showBudgetPeriodSheet = showBudgetPeriodSheet,
                            forceBudgetPeriodSheetSetup = forceBudgetPeriodSheetSetup,
                            selectedViewPeriod = selectedViewPeriod,
                            onPeriodSelected = actions.onPeriodSelected,
                        )
                    }

                PhoneLayout(
                    budgetUiState = budgetUiState,
                    actions = actionsForLayout,
                    featureFlags = featureFlagsForLayout,
                    budgetPeriodState = budgetPeriodStateForLayout,
                    topSheetState = topSheetState,
                    contentHeight = contentHeight,
                    contentWidth = contentWidth,
                    localDensity = localDensity,
                    windowInsets = windowInsets,
                    showCategoryGrid = showCategoryGrid,
                    onShowCategoryGrid = { showCategoryGrid = true },
                    onHideCategoryGrid = { showCategoryGrid = false },
                    openWalletOnStart = openWalletOnStart,
                    quickLogSwipeModifier = quickLogSwipeModifier,
                    queueDeleteWithUndo = ::queueDeleteWithUndo,
                    cancelPendingDelete = ::cancelPendingDelete,
                    showInfoSnackbar = ::showInfoSnackbar,
                    snackbarHostState = snackbarHostState,
                    tutorialBoxState = tutorialBoxState,
                )
            } else {
                val actionsForLayout =
                    remember(actions, snackbarHostState) {
                        actions.copy(
                            onShowSnackbar = { message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short,
                                    )
                                }
                            },
                        )
                    }

                val featureFlagsForLayout =
                    remember(
                        showCreditQuickToggleFeature,
                        directCategoryPopupEnabled,
                        categoryGridModeEnabled,
                    ) {
                        MainScreenFeatureFlags(
                            showCreditQuickToggleFeature = showCreditQuickToggleFeature,
                            directCategoryPopupEnabled = directCategoryPopupEnabled,
                            categoryGridModeEnabled = categoryGridModeEnabled,
                        )
                    }

                val budgetPeriodStateForLayout =
                    remember(
                        showBudgetPeriodSheet,
                        forceBudgetPeriodSheetSetup,
                        selectedViewPeriod,
                        actions,
                    ) {
                        MainScreenBudgetPeriodState(
                            showBudgetPeriodSheet = showBudgetPeriodSheet,
                            forceBudgetPeriodSheetSetup = forceBudgetPeriodSheetSetup,
                            selectedViewPeriod = selectedViewPeriod,
                            onPeriodSelected = actions.onPeriodSelected,
                        )
                    }

                // Expanded (>= 840dp): two-pane tablet layout
                TabletLayout(
                    budgetUiState = budgetUiState,
                    actions = actionsForLayout,
                    featureFlags = featureFlagsForLayout,
                    budgetPeriodState = budgetPeriodStateForLayout,
                    contentHeight = contentHeight,
                    contentWidth = contentWidth,
                    localDensity = localDensity,
                    windowInsets = windowInsets,
                    showCategoryGrid = showCategoryGrid,
                    onShowCategoryGrid = { showCategoryGrid = true },
                    onHideCategoryGrid = { showCategoryGrid = false },
                    openWalletOnStart = openWalletOnStart,
                    quickLogSwipeModifier = quickLogSwipeModifier,
                    queueDeleteWithUndo = ::queueDeleteWithUndo,
                    cancelPendingDelete = ::cancelPendingDelete,
                    showInfoSnackbar = ::showInfoSnackbar,
                    snackbarHostState = snackbarHostState,
                    tutorialBoxState = tutorialBoxState,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .navigationBarsPadding(),
            ) { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    actionColor = MaterialTheme.colorScheme.tertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun MainNavigationRail(
    expanded: Boolean,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    tutorialBoxState: TutorialBoxState? = null,
) {
    val itemModifier = if (expanded) Modifier.fillMaxWidth() else Modifier
    val analyticsItemModifier = itemModifier.let { base ->
        if (tutorialBoxState != null) base.markForTutorial(tutorialBoxState, index = 5)
        else base
    }

    NavigationRail(
        modifier = if (expanded) Modifier.width(104.dp) else Modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Spacer(Modifier.weight(1f))
        NavigationRailItem(
            modifier = analyticsItemModifier,
            selected = false,
            onClick = onNavigateToAnalytics,
            icon = { Icon(Icons.Rounded.BarChart, contentDescription = "Analytics") },
            label = { Text("Analytics") },
        )
        NavigationRailItem(
            modifier = itemModifier,
            selected = false,
            onClick = onNavigateToSettings,
            icon = {
                val isCensored = LocalCensorMode.current
                BadgedBox(
                    badge = { if (isCensored) Badge() },
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            },
            label = { Text("Settings") },
        )
        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalWearMaterialApi::class)
@Composable
private fun PhoneLayout(
    budgetUiState: BudgetUiState,
    actions: MainScreenActions,
    featureFlags: MainScreenFeatureFlags,
    budgetPeriodState: MainScreenBudgetPeriodState,
    topSheetState: SwipeableState<TopSheetValue>,
    contentHeight: Float,
    contentWidth: Float,
    localDensity: Density,
    windowInsets: PaddingValues,
    showCategoryGrid: Boolean,
    onShowCategoryGrid: () -> Unit,
    onHideCategoryGrid: () -> Unit,
    openWalletOnStart: Boolean,
    quickLogSwipeModifier: Modifier,
    queueDeleteWithUndo: (Transaction, String) -> Unit,
    cancelPendingDelete: () -> Unit,
    showInfoSnackbar: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    tutorialBoxState: TutorialBoxState? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val navigationBarOffset = windowInsets.calculateBottomPadding()
    val navBarHeightPx = with(localDensity) { navigationBarOffset.toPx() }

    val hasHardKeyboard =
        configuration.keyboard == Configuration.KEYBOARD_QWERTY
    val isSquareScreen =
        configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat() > 0.8f

    var isNumpadExpandedManually by remember { mutableStateOf<Boolean?>(null) }
    val isNumpadCollapsed =
        isNumpadExpandedManually?.let { !it } ?: (hasHardKeyboard && isSquareScreen)

    val heightFactor = if (isSquareScreen) 0.35f else 0.45f
    val defaultInternalKeyboardHeightBase =
        contentWidth
            .coerceAtMost(with(localDensity) { 500.dp.toPx() })
            .coerceAtMost(contentHeight * heightFactor)
    val rowHeightPx = defaultInternalKeyboardHeightBase / 4
    val defaultInternalKeyboardHeight =
        if (isNumpadCollapsed) with(localDensity) { 64.dp.toPx() } else rowHeightPx * 4
    val calcModeKeyboardHeight =
        if (isNumpadCollapsed || hasHardKeyboard) defaultInternalKeyboardHeight else rowHeightPx * 5

    var localDragProgress by remember { mutableFloatStateOf(0f) }

    var externalSheetDragOffset by remember { mutableFloatStateOf(0f) }
    val collapseDragEndJob = remember { mutableStateOf<Job?>(null) }
    val topSheetStateRef = rememberUpdatedState(topSheetState)

    val systemKeyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val systemKeyboardHeightPx = with(localDensity) { systemKeyboardHeight.toPx() }

    val isShowSystemKeyboard = systemKeyboardHeightPx > 0f
    var keepImeLayout by remember { mutableStateOf(false) }
    var lastImeHeightPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(systemKeyboardHeightPx) {
        if (systemKeyboardHeightPx > 0f) {
            lastImeHeightPx = maxOf(lastImeHeightPx, systemKeyboardHeightPx)
            keepImeLayout = true
        } else {
            delay(140.milliseconds)
            keepImeLayout = false
            lastImeHeightPx = 0f
        }
    }

    val effectiveProgress =
        if (budgetUiState.isCalculation) 1f - localDragProgress else localDragProgress

    val internalKeyboardTarget =
        defaultInternalKeyboardHeight + (calcModeKeyboardHeight - defaultInternalKeyboardHeight) * effectiveProgress

    val currentKeyboardHeight = if (isShowSystemKeyboard || keepImeLayout) {
        lastImeHeightPx
    } else {
        internalKeyboardTarget
    }

    val editorHeight by remember(
        contentHeight,
        currentKeyboardHeight,
        isShowSystemKeyboard,
        keepImeLayout,
        navBarHeightPx,
        budgetUiState.isCalculation,
        localDragProgress,
    ) {
        derivedStateOf {
            val additionalOffset =
                with(localDensity) { if (isShowSystemKeyboard || keepImeLayout) 18.dp.toPx() else 18.dp.toPx() }
            contentHeight
                .minus(
                    currentKeyboardHeight.plus(navBarHeightPx).plus(additionalOffset)
                        .coerceAtLeast(0f),
                )
                .coerceAtMost(contentHeight - (navBarHeightPx + with(localDensity) { 98.dp.toPx() }))
        }
    }

    val keyboardAnimationSpec =
        remember<AnimationSpec<Float>>(localDragProgress) {
            if (localDragProgress > 0f && localDragProgress < 1f) {
                tween(durationMillis = 0)
            } else {
                tween(durationMillis = 200, easing = FastOutSlowInEasing)
            }
        }

    val editorHeightAnimatedState = animateFloatAsState(
        label = "editorHeightAnimatedValue",
        targetValue = editorHeight,
        animationSpec = keyboardAnimationSpec,
    )

    val keyboardHeightAnimatedState = animateFloatAsState(
        label = "keyboardHeightAnimatedValue",
        targetValue = currentKeyboardHeight,
        animationSpec = keyboardAnimationSpec,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.Zero, Key.NumPad0 -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.NumberTapped("0")
                                )
                            ); true
                        }

                        Key.One, Key.NumPad1 -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.NumberTapped("1")
                                )
                            ); true
                        }

                        Key.Two, Key.NumPad2 -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.NumberTapped("2")
                                )
                            ); true
                        }

                        Key.Three, Key.NumPad3 -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.NumberTapped("3")
                                )
                            ); true
                        }

                        Key.Four, Key.NumPad4 -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.NumberTapped("4")
                                )
                            ); true
                        }

                        Key.Five, Key.NumPad5 -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.NumberTapped("5")
                                )
                            ); true
                        }

                        Key.Six, Key.NumPad6 -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.NumberTapped("6")
                                )
                            ); true
                        }

                        Key.Seven, Key.NumPad7 -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.NumberTapped("7")
                                )
                            ); true
                        }

                        Key.Eight -> {
                            if (keyEvent.isShiftPressed) {
                                actions.onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.OperatorTapped('×')
                                    )
                                ); true
                            } else {
                                actions.onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.NumberTapped("8")
                                    )
                                ); true
                            }
                        }

                        Key.Nine, Key.NumPad9 -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.NumberTapped("9")
                                )
                            ); true
                        }

                        Key.Period, Key.NumPadDot -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.DotTapped
                                )
                            ); true
                        }

                        Key.Backspace -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.BackspaceTapped
                                )
                            ); true
                        }

                        Key.Enter, Key.NumPadEnter -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.ApplyTapped
                                )
                            ); true
                        }

                        Key.Plus, Key.NumPadAdd -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.OperatorTapped('+')
                                )
                            ); true
                        }

                        Key.Minus, Key.NumPadSubtract -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.OperatorTapped('-')
                                )
                            ); true
                        }

                        Key.Multiply, Key.NumPadMultiply -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.OperatorTapped('×')
                                )
                            ); true
                        }

                        Key.Slash, Key.NumPadDivide -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.OperatorTapped('÷')
                                )
                            ); true
                        }

                        Key.Equals, Key.NumPadEquals -> {
                            if (keyEvent.isShiftPressed) {
                                actions.onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.OperatorTapped('+')
                                    )
                                ); true
                            } else {
                                actions.onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.EqualsTapped
                                    )
                                ); true
                            }
                        }

                        Key.Escape -> {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                    BudgetNumpadIntent.ResetInputTapped
                                )
                            ); true
                        }

                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        LaunchedEffect(budgetUiState.animState) {
            if (budgetUiState.animState == AnimState.EDITING) {
                focusRequester.requestFocus()
            }
        }
        val halfExpandedOffsetPx =
            with(localDensity) {
                (-contentHeight + navBarHeightPx + 18.dp.toPx() + editorHeightAnimatedState.value).coerceAtMost(
                    0f
                )
            }
        val isSheetExpanding by remember(halfExpandedOffsetPx) {
            derivedStateOf { topSheetState.offset.value > halfExpandedOffsetPx + 10f }
        }

        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animatedHeightPx {
                        keyboardHeightAnimatedState.value + navBarHeightPx + with(localDensity) { 16.dp.toPx() }
                    }
                    .zIndex(if (isSheetExpanding) 0f else 1f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
                    .pointerInput(isNumpadCollapsed) {
                        detectVerticalDragGestures { change, dragAmount ->
                            if (dragAmount < -20f && isNumpadCollapsed) {
                                isNumpadExpandedManually = true
                                focusManager.clearFocus()
                            } else if (dragAmount > 20f && !isNumpadCollapsed && (hasHardKeyboard || isSquareScreen)) {
                                isNumpadExpandedManually = false
                            }
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (hasHardKeyboard || isSquareScreen) {
                            isNumpadExpandedManually = !isNumpadCollapsed
                            focusManager.clearFocus()
                            focusRequester.requestFocus()
                        }
                    },
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (isNumpadCollapsed) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ExpandLess,
                            contentDescription = "Expand Numpad",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    MainScreenNumpadSection(
                        budgetUiState = budgetUiState,
                        showCategoryGrid = showCategoryGrid,
                        actions = actions,
                        featureFlags = featureFlags,
                        effectiveProgress = effectiveProgress,
                        onDragProgressChanged = { progress -> localDragProgress = progress },
                        hasHardKeyboard = hasHardKeyboard,
                        tutorialBoxState = tutorialBoxState,
                    )
                }
            }
        }

        val expandHeightPx = contentHeight - navBarHeightPx - with(localDensity) { 18.dp.toPx() }
        TopSheetLayout(
            swipeableState = topSheetState,
            customHalfHeight = editorHeightAnimatedState.value,
            customCardHeight = {
                val halfHeightPx = editorHeightAnimatedState.value
                val maxOffset = (-(expandHeightPx - halfHeightPx)).coerceAtMost(0f)
                val offset = runCatching { topSheetState.offset.value }.getOrDefault(maxOffset)
                val progress =
                    if (maxOffset == 0f) 1f else (1f - (offset / maxOffset)).coerceIn(0f, 1f)
                halfHeightPx + (expandHeightPx - halfHeightPx) * progress
            },
            cardOffsetAdjustment = {
                runCatching { -topSheetState.offset.value }.getOrDefault(0f)
            },
            isLockSwipeable = {
                budgetUiState.lockSwipeable || localDragProgress > 0f || showCategoryGrid ||
                        (budgetUiState.isCalculation && effectiveProgress < 1f) ||
                        (!budgetUiState.isCalculation && effectiveProgress > 0f)
            },
            isLockDraggable = {
                budgetUiState.lockDraggable || localDragProgress > 0f || showCategoryGrid ||
                        (budgetUiState.isCalculation && effectiveProgress < 1f) ||
                        (!budgetUiState.isCalculation && effectiveProgress > 0f)
            },
            canDismissBySwipeUp = { true },
            externalDragOffset = { externalSheetDragOffset },
            onDismiss = {},
            sheetContentHalfExpand = {
                MainScreenEditorSection(
                    budgetUiState = budgetUiState,
                    actions = actions,
                    featureFlags = featureFlags,
                    budgetPeriodState = budgetPeriodState,
                    showCategoryGrid = showCategoryGrid,
                    onShowCategoryGrid = onShowCategoryGrid,
                    onHideCategoryGrid = onHideCategoryGrid,
                    modifier = Modifier.animatedRequiredHeightPx {
                        with(localDensity) {
                            val statusOffsetPx = 18.dp.toPx()
                            val halfExpanedOffsetPxLocal =
                                (-contentHeight + navBarHeightPx + statusOffsetPx + editorHeightAnimatedState.value)
                                    .coerceAtMost(0f)
                            val computed =
                                topSheetState.offset.value.coerceIn(halfExpanedOffsetPxLocal, 0f) +
                                    contentHeight - navBarHeightPx - statusOffsetPx
                            minOf(computed, editorHeightAnimatedState.value)
                        }
                    },
                    onNavigateToSettings = actions.onNavigateToSettings,
                    onNavigateToAnalytics = actions.onNavigateToAnalytics,
                    openWalletOnStart = openWalletOnStart,
                    tutorialBoxState = tutorialBoxState,
                    onFocus = {
                        focusRequester.requestFocus()
                        if (budgetUiState.numpadInput.isNotEmpty() && budgetUiState.animState != AnimState.EDITING) {
                            actions.onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetEditorIntent(
                                    BudgetEditorIntent.SetAnimState(AnimState.EDITING),
                                ),
                            )
                        }
                    },
                    onApply = {
                        actions.onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                BudgetNumpadIntent.ApplyTapped,
                            ),
                        )
                    }
                )
            },
            sheetContentExpand = {
                if (topSheetState.targetValue == TopSheetValue.Expanded || topSheetState.currentValue == TopSheetValue.Expanded) {
                    HistoryScreen(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(colorButton)
                                .then(quickLogSwipeModifier),
                        onCollapseDragDelta = { delta ->
                            externalSheetDragOffset += delta
                            collapseDragEndJob.value?.cancel()
                            collapseDragEndJob.value = coroutineScope.launch {
                                delay(120.milliseconds)
                                val committed = externalSheetDragOffset
                                if (committed != 0f) {
                                    val settleSpec = tween<Float>(300)
                                    val targetState =
                                        if (committed < -120f) TopSheetValue.Dismissed
                                        else TopSheetValue.HalfExpanded
                                    val dragAnim = launch {
                                        val durationMs = 300L
                                        val frameMs = 16L
                                        val startNanos = System.nanoTime()
                                        val startValue = committed
                                        while (true) {
                                            val elapsedMs =
                                                (System.nanoTime() - startNanos) / 1_000_000L
                                            if (elapsedMs >= durationMs) {
                                                externalSheetDragOffset = 0f
                                                break
                                            }
                                            val fraction =
                                                elapsedMs.toFloat() / durationMs
                                            val eased = FastOutSlowInEasing
                                                .transform(fraction)
                                            externalSheetDragOffset =
                                                startValue * (1f - eased)
                                            delay(frameMs)
                                        }
                                    }
                                    val swipeAnim = launch {
                                        runCatching {
                                            topSheetStateRef.value
                                                .animateTo(targetState, settleSpec)
                                        }
                                    }
                                    dragAnim.join()
                                    swipeAnim.join()
                                }
                            }
                        },
                        onQueueDeleteWithUndo = { tx, msg, _ -> queueDeleteWithUndo(tx, msg) },
                        onCancelPendingDelete = { cancelPendingDelete() },
                        onShowInfoSnackbar = { msg -> showInfoSnackbar(msg) },
                    )
                }
            },
        )

        LaunchedEffect(budgetUiState.isCalculation) {
            localDragProgress = 0f
            externalSheetDragOffset = 0f
            collapseDragEndJob.value?.cancel()
        }
    }
    StatusBarPadding()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabletLayout(
    budgetUiState: BudgetUiState,
    actions: MainScreenActions,
    featureFlags: MainScreenFeatureFlags,
    budgetPeriodState: MainScreenBudgetPeriodState,
    contentHeight: Float,
    contentWidth: Float,
    localDensity: Density,
    windowInsets: PaddingValues,
    showCategoryGrid: Boolean,
    onShowCategoryGrid: () -> Unit,
    onHideCategoryGrid: () -> Unit,
    openWalletOnStart: Boolean,
    quickLogSwipeModifier: Modifier,
    queueDeleteWithUndo: (Transaction, String) -> Unit,
    cancelPendingDelete: () -> Unit,
    showInfoSnackbar: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    tutorialBoxState: TutorialBoxState? = null,
) {
    val configuration = LocalConfiguration.current
    val hasHardKeyboard =
        configuration.keyboard == Configuration.KEYBOARD_QWERTY
    val navigationBarOffset = windowInsets.calculateBottomPadding()
    val navBarHeightPx = with(localDensity) { navigationBarOffset.toPx() }

    val defaultInternalKeyboardHeightBase =
        (contentWidth / 2f)
            .coerceAtMost(with(localDensity) { 500.dp.toPx() })
            .coerceAtMost(contentHeight / 2.5f)
    val rowHeightPx = defaultInternalKeyboardHeightBase / 4
    val defaultInternalKeyboardHeight = rowHeightPx * 4
    val calcModeKeyboardHeight = if (hasHardKeyboard) defaultInternalKeyboardHeight else rowHeightPx * 5

    var localDragProgress by remember { mutableFloatStateOf(0f) }

    val effectiveProgressState = remember(budgetUiState.isCalculation) {
        derivedStateOf {
            if (budgetUiState.isCalculation) 1f - localDragProgress else localDragProgress
        }
    }
    val effectiveProgress by effectiveProgressState

    val internalKeyboardTargetState = remember(
        defaultInternalKeyboardHeight,
        calcModeKeyboardHeight,
        budgetUiState.isCalculation
    ) {
        derivedStateOf {
            defaultInternalKeyboardHeight + (calcModeKeyboardHeight - defaultInternalKeyboardHeight) * effectiveProgressState.value
        }
    }

    val keyboardAnimationSpec =
        remember<AnimationSpec<Float>>(localDragProgress) {
            if (localDragProgress > 0f && localDragProgress < 1f) {
                tween(durationMillis = 0)
            } else {
                tween(durationMillis = 200, easing = FastOutSlowInEasing)
            }
        }

    val keyboardHeightAnimatedState = animateFloatAsState(
        targetValue = internalKeyboardTargetState.value,
        animationSpec = keyboardAnimationSpec,
        label = "keyboardHeightAnimatedValue",
    )

    Row(modifier = Modifier.fillMaxSize()) {
        // History pane (50%)
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(colorEditor)
                    .navigationBarsPadding(),
        ) {
            HistoryScreen(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(colorButton)
                        .then(quickLogSwipeModifier),
                onQueueDeleteWithUndo = { tx, msg, _ -> queueDeleteWithUndo(tx, msg) },
                onCancelPendingDelete = { cancelPendingDelete() },
                onShowInfoSnackbar = { msg -> showInfoSnackbar(msg) },
            )
        }

        VerticalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )

        // Editor and Numpad pane (50%)
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
        ) {
            Card(
                shape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = colorEditor,
                        contentColor = colorOnEditor,
                    ),
                modifier = Modifier.weight(1f),
            ) {
                MainScreenEditorSection(
                    budgetUiState = budgetUiState,
                    actions = actions,
                    featureFlags = featureFlags,
                    budgetPeriodState = budgetPeriodState,
                    showCategoryGrid = showCategoryGrid,
                    onShowCategoryGrid = onShowCategoryGrid,
                    onHideCategoryGrid = onHideCategoryGrid,
                    modifier = Modifier.fillMaxSize(),
                    openWalletOnStart = openWalletOnStart,
                    tutorialBoxState = tutorialBoxState,
                    showAnalyticsButton = false,
                    showSettingsButton = false,
                    onApply = {
                        actions.onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                BudgetNumpadIntent.ApplyTapped,
                            ),
                        )
                    }
                )
            }

            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(with(localDensity) { (keyboardHeightAnimatedState.value + navBarHeightPx).toDp() }),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    MainScreenNumpadSection(
                        budgetUiState = budgetUiState,
                        showCategoryGrid = showCategoryGrid,
                        actions = actions,
                        featureFlags = featureFlags,
                        effectiveProgress = effectiveProgress,
                        onDragProgressChanged = { progress -> localDragProgress = progress },
                        hasHardKeyboard = hasHardKeyboard,
                        tutorialBoxState = tutorialBoxState,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScreenNumpadSection(
    budgetUiState: BudgetUiState,
    showCategoryGrid: Boolean,
    actions: MainScreenActions,
    featureFlags: MainScreenFeatureFlags,
    effectiveProgress: Float,
    onDragProgressChanged: (Float) -> Unit,
    hasHardKeyboard: Boolean = false,
    modifier: Modifier = Modifier,
    tutorialBoxState: TutorialBoxState? = null,
) {
    val editorState =
        remember(budgetUiState) {
            EditorState(
                mode =
                    when (budgetUiState.editMode) {
                        EditorEditMode.ADD -> NumpadEditMode.ADD
                        EditorEditMode.EDIT -> NumpadEditMode.EDIT
                    },
                rawSpentValue = budgetUiState.numpadInput,
                stage = if (budgetUiState.numpadInput.isNotEmpty()) EditStage.EDIT_SPENT else EditStage.IDLE,
                currentSpent = budgetUiState.numpadInput,
                currentComment = budgetUiState.currentComment,
                editedTransaction = null,
            )
        }
    val categoryGridContent: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? =
        if (showCategoryGrid && featureFlags.categoryGridModeEnabled) {
            {
                SavedCategoriesGrid(
                    tags = budgetUiState.tags,
                    selectedCategory = budgetUiState.currentComment,
                    onCategorySelected = { category ->
                        actions.onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.CommentUpdated(category),
                            ),
                        )
                    },
                    applyWindowInsets = false,
                )
            }
        } else {
            null
        }

    Numpad(
        modifier =
            modifier.then(
                tutorialBoxState?.let { state ->
                    Modifier.markForTutorial(state, index = 0)
                } ?: Modifier,
            ),
        editorState = editorState,
        hasHardKeyboard = hasHardKeyboard,
        numberHintAnchorModifier = Modifier,
        applyHintAnchorModifier = Modifier,
        onNumberInput = { digit ->
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                    BudgetNumpadIntent.NumberTapped(digit.toString()),
                ),
            )
            actions.onAdvanceTutorial(FirstLaunchTutorialStage.TAP_ANY_NUMBER)
        },
        onDotInput = {
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                    BudgetNumpadIntent.DotTapped,
                ),
            )
        },
        onBackspace = {
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                    BudgetNumpadIntent.BackspaceTapped,
                ),
            )
        },
        onBackspaceLongPress = {
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                    BudgetNumpadIntent.ResetInputTapped,
                ),
            )
        },
        onOperatorInput = { op ->
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                    BudgetNumpadIntent.OperatorTapped(op),
                ),
            )
        },
        onEqualsInput = {
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                    BudgetNumpadIntent.EqualsTapped,
                ),
            )
        },
        onApply = {
            Log.d("MainScreen", "Numpad check/save button pressed")
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                    BudgetNumpadIntent.ApplyTapped,
                ),
            )
            actions.onAdvanceTutorial(FirstLaunchTutorialStage.TAP_DONE_SAVE)
        },
        onDragProgressChanged = onDragProgressChanged,
        dragProgress = effectiveProgress,
        isCalculation = budgetUiState.isCalculation,
        onCalculationModeChanged = { enabled ->
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                    BudgetNumpadIntent.SetCalculationMode(enabled),
                ),
            )
        },
        onShowSnackbar = actions.onShowSnackbar,
        onTestNotifications = {
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                    BudgetNumpadIntent.TriggerTestNotifications,
                ),
            )
        },
        enableCalculationMode = true,
        enableCalcModeSwipe = !showCategoryGrid,
        leftContent = categoryGridContent,
        tutorialBoxState = tutorialBoxState,
    )
}

@Composable
private fun MainScreenEditorSection(
    budgetUiState: BudgetUiState,
    actions: MainScreenActions,
    featureFlags: MainScreenFeatureFlags,
    budgetPeriodState: MainScreenBudgetPeriodState,
    showCategoryGrid: Boolean,
    onShowCategoryGrid: () -> Unit,
    onHideCategoryGrid: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    openWalletOnStart: Boolean = false,
    tutorialBoxState: TutorialBoxState? = null,
    showAnalyticsButton: Boolean = true,
    showSettingsButton: Boolean = true,
    onFocus: () -> Unit = {},
    onApply: () -> Unit = {},
) {
    Editor(
        uiState = budgetUiState,
        animState = budgetUiState.animState,
        modifier = modifier,
        onApply = onApply,
        onOpenHistory = {},
        onOpenSettings = onNavigateToSettings,
        onOpenAnalytics = onNavigateToAnalytics,
        onOpenWallet = {
            val noBudget =
                budgetUiState.budgetSettings == null || budgetUiState.budgetSettings.endDate == null
            actions.onProcessIntent(
                MainScreenUiIntent.ShowBudgetPeriodSheet(forceSetup = noBudget)
            )
        },
        openWalletOnStart = openWalletOnStart,
        showBudgetPeriodSheet = budgetPeriodState.showBudgetPeriodSheet,
        forceBudgetPeriodSheetSetup = budgetPeriodState.forceBudgetPeriodSheetSetup,
        selectedViewPeriod = budgetPeriodState.selectedViewPeriod,
        onPeriodSelected = budgetPeriodState.onPeriodSelected,
        onShowBudgetPeriodSheet = {
            val noBudget =
                budgetUiState.budgetSettings == null || budgetUiState.budgetSettings.endDate == null
            actions.onProcessIntent(
                MainScreenUiIntent.ShowBudgetPeriodSheet(forceSetup = noBudget)
            )
        },
        onHideBudgetPeriodSheet = {
            actions.onProcessIntent(MainScreenUiIntent.HideBudgetPeriodSheet)
        },
        onAnalyticsClickForTutorial = {
            actions.onAdvanceTutorial(FirstLaunchTutorialStage.TAP_ANALYTICS)
        },
        onFocus = onFocus,
        onCommentClick = {},
        onCommentUpdate = { comment ->
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetEditorIntent(
                    BudgetEditorIntent.CommentUpdated(comment),
                ),
            )
        },
        onDeleteTag = { tag ->
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetEditorIntent(
                    BudgetEditorIntent.DeleteTag(tag),
                ),
            )
        },
        onRecurrentToggle = { enabled ->
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetEditorIntent(
                    BudgetEditorIntent.SetRecurrentEnabled(enabled),
                ),
            )
        },
        onCreditToggle = { enabled ->
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetEditorIntent(
                    BudgetEditorIntent.SetCreditEnabled(enabled),
                ),
            )
        },
        showCreditQuickToggleFeature = featureFlags.showCreditQuickToggleFeature,
        directCategoryPopupEnabled = featureFlags.directCategoryPopupEnabled,
        categoryGridModeEnabled = featureFlags.categoryGridModeEnabled,
        isCategoryGridVisible = showCategoryGrid,
        isCalculation = budgetUiState.isCalculation,
        onShowCategoryGrid = onShowCategoryGrid,
        onHideCategoryGrid = onHideCategoryGrid,
        onDisableCalculationMode = {
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetNumpadIntent(
                    BudgetNumpadIntent.SetCalculationMode(false),
                ),
            )
        },
        onDismissRecurrentDialog = {
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetEditorIntent(
                    BudgetEditorIntent.DismissRecurrentDialog,
                ),
            )
        },
        onDismissCreditCutoffDialog = {
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetEditorIntent(
                    BudgetEditorIntent.DismissCreditCutoffDialog,
                ),
            )
        },
        onRecurrentExpenseConfirm = { freq, date, day, fallbackComment ->
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetEditorIntent(
                    BudgetEditorIntent.RecurrentExpenseApplied(
                        freq,
                        date,
                        day,
                        fallbackComment
                    ),
                ),
            )
        },
        onCreditCutoffConfirm = { day ->
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetEditorIntent(
                    BudgetEditorIntent.CreditCutoffDayConfirmed(day),
                ),
            )
        },
        onSaveBudget = { settings ->
            actions.onProcessIntent(
                MainScreenUiIntent.ProcessBudgetEditorIntent(
                    BudgetEditorIntent.UpdateSettings(settings),
                ),
            )
        },
        showAnalyticsButton = showAnalyticsButton,
        showSettingsButton = showSettingsButton,
        budgetPillHintAnchorModifier = tutorialBoxState
            ?.let { state -> Modifier.markForTutorial(state, index = 1) }
            ?: Modifier,
        analyticsHintAnchorModifier = tutorialBoxState
            ?.let { state -> Modifier.markForTutorial(state, index = 5) }
            ?: Modifier,
        tutorialBoxState = tutorialBoxState,
    )
}

@Preview
@PreviewScreenSizes
@Composable
private fun MainScreenPreview() {
    val configuration = LocalConfiguration.current
    val windowSizeClass =
        when {
            configuration.screenWidthDp < 600 -> WindowWidthSizeClass.Compact
            configuration.screenWidthDp < 840 -> WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Expanded
        }
    CompositionLocalProvider(LocalWindowSize provides windowSizeClass) {
        MinusTheme {
            MainScreenContent(
                mainScreenState = MainScreenUiState(),
                budgetUiState =
                    BudgetUiState(
                        budgetSettings =
                            BudgetSettings(
                                totalBudget = BigDecimal("500.00"),
                                period = BudgetPeriod.DAILY,
                                startDate = LocalDate.now(),
                                currencyCode = "USD",
                            ),
                        budgetState =
                            BudgetState(
                                remainingToday = BigDecimal("110.00"),
                                totalSpentToday = BigDecimal("12.50"),
                                dailyBudget = BigDecimal("122.50"),
                                daysRemaining = 15,
                                progress = 0.1f,
                                isOverBudget = false,
                                totalBudget = BigDecimal("500.00"),
                                totalSpentInPeriod = BigDecimal("12.50"),
                            ),
                        transactions = emptyList(),
                        numpadInput = "12",
                        isNumpadValid = false,
                    ),
                actions =
                    MainScreenActions(
                        onProcessIntent = {},
                        onAdvanceTutorial = {},
                        onNavigateToAnalytics = {},
                        onNavigateToSettings = {},
                        onNavigateToWallet = {},
                        onPeriodSelected = {},
                    ),
                openWalletOnStart = false,
            )
        }
    }
}
