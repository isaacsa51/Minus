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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.SwipeableState
import androidx.wear.compose.material.rememberSwipeableState
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.LocalWindowSize
import com.serranoie.app.minus.presentation.ui.budget.BudgetUiState
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetEditorIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetTransactionIntent
import com.serranoie.app.minus.presentation.ui.editor.AnimState
import com.serranoie.app.minus.presentation.ui.editor.Editor
import com.serranoie.app.minus.presentation.ui.history.HistoryScreen
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorButton
import com.serranoie.app.minus.presentation.ui.theme.colorEditor
import com.serranoie.app.minus.presentation.ui.theme.colorOnEditor
import com.serranoie.app.minus.presentation.ui.theme.component.TopSheetLayout
import com.serranoie.app.minus.presentation.ui.theme.component.TopSheetValue
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditorState
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Numpad
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.SavedCategoriesGrid
import com.serranoie.app.minus.presentation.ui.theme.isNightMode
import com.serranoie.app.minus.presentation.ui.tutorial.FIRST_LAUNCH_TUTORIAL_STAGE_KEY
import com.serranoie.app.minus.presentation.ui.tutorial.FirstLaunchTutorialStage
import com.serranoie.app.minus.presentation.util.LocalCensorMode
import com.serranoie.app.minus.presentation.util.StatusBarPadding
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
    onboardingCompleted: Boolean,
    tutorialStage: FirstLaunchTutorialStage,
    showCreditQuickToggleFeature: Boolean,
    directCategoryPopupEnabled: Boolean,
    categoryLayoutModeEnabled: Boolean,
    categoryGridModeEnabled: Boolean,
    onProcessIntent: (MainScreenUiIntent) -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToWallet: () -> Unit,
    openWalletOnStart: Boolean,
    showBudgetPeriodSheet: Boolean,
    forceBudgetPeriodSheetSetup: Boolean,
    selectedViewPeriod: BudgetPeriod?,
    onPeriodSelected: (BudgetPeriod) -> Unit,
    settingsDataStore: DataStore<Preferences>?,
    undoSnackbarActionLabel: String,
) {
    val topSheetState = rememberSwipeableState(TopSheetValue.HalfExpanded)
    var nightMode by remember { mutableStateOf(false) }
    var showCategoryGrid by remember { mutableStateOf(false) }
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
                SnackbarResult.ActionPerformed -> onProcessIntent(MainScreenUiIntent.CancelPendingDelete)
                SnackbarResult.Dismissed -> onProcessIntent(MainScreenUiIntent.DismissSnackbar)
            }
        }
    }

    fun executeDelete(transaction: Transaction) {
        onProcessIntent(
            MainScreenUiIntent.ProcessBudgetTransactionIntent(
                BudgetTransactionIntent.DeleteTransactionTapped(transaction),
            ),
        )
    }

    fun queueDeleteWithUndo(
        transaction: Transaction,
        message: String,
    ) {
        onProcessIntent(MainScreenUiIntent.QueueDeleteWithUndo(transaction, message))
    }

    fun cancelPendingDelete() {
        onProcessIntent(MainScreenUiIntent.CancelPendingDelete)
    }

    fun showInfoSnackbar(message: String) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }

    fun advanceTutorial(expected: FirstLaunchTutorialStage) {
        if (tutorialStage != expected) return
        coroutineScope.launch {
            settingsDataStore?.edit { prefs ->
                prefs[FIRST_LAUNCH_TUTORIAL_STAGE_KEY] = expected.next().name
            }
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
                        onProcessIntent(
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
        onProcessIntent(MainScreenUiIntent.SetShownStage(tutorialStage))
    }

    nightMode = isNightMode()

    LaunchedEffect(windowSizeClass) {
        if (windowSizeClass != WindowWidthSizeClass.Compact && !budgetUiState.isCalculation) {
            onProcessIntent(
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
                onNavigateToAnalytics = onNavigateToAnalytics,
                onNavigateToSettings = onNavigateToSettings,
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
                PhoneLayout(
                    budgetUiState = budgetUiState,
                    topSheetState = topSheetState,
                    contentHeight = contentHeight,
                    contentWidth = contentWidth,
                    localDensity = localDensity,
                    windowInsets = windowInsets,
                    showCreditQuickToggleFeature = showCreditQuickToggleFeature,
                    directCategoryPopupEnabled = directCategoryPopupEnabled,
                    categoryLayoutModeEnabled = categoryLayoutModeEnabled,
                    categoryGridModeEnabled = categoryGridModeEnabled,
                    showCategoryGrid = showCategoryGrid,
                    onShowCategoryGrid = { showCategoryGrid = true },
                    onHideCategoryGrid = { showCategoryGrid = false },
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToAnalytics = onNavigateToAnalytics,
                    onNavigateToWallet = onNavigateToWallet,
                    openWalletOnStart = openWalletOnStart,
                    onProcessIntent = onProcessIntent,
                    onAdvanceTutorial = ::advanceTutorial,
                    quickLogSwipeModifier = quickLogSwipeModifier,
                    queueDeleteWithUndo = ::queueDeleteWithUndo,
                    cancelPendingDelete = ::cancelPendingDelete,
                    showInfoSnackbar = ::showInfoSnackbar,
                    snackbarHostState = snackbarHostState,
                    showBudgetPeriodSheet = showBudgetPeriodSheet,
                    forceBudgetPeriodSheetSetup = forceBudgetPeriodSheetSetup,
                    selectedViewPeriod = selectedViewPeriod,
                    onPeriodSelected = onPeriodSelected,
                )
            } else {
                // Expanded (>= 840dp): two-pane tablet layout
                TabletLayout(
                    budgetUiState = budgetUiState,
                    contentHeight = contentHeight,
                    contentWidth = contentWidth,
                    localDensity = localDensity,
                    windowInsets = windowInsets,
                    showCreditQuickToggleFeature = showCreditQuickToggleFeature,
                    directCategoryPopupEnabled = directCategoryPopupEnabled,
                    categoryLayoutModeEnabled = categoryLayoutModeEnabled,
                    categoryGridModeEnabled = categoryGridModeEnabled,
                    showCategoryGrid = showCategoryGrid,
                    onShowCategoryGrid = { showCategoryGrid = true },
                    onHideCategoryGrid = { showCategoryGrid = false },
                    onNavigateToWallet = onNavigateToWallet,
                    openWalletOnStart = openWalletOnStart,
                    onProcessIntent = onProcessIntent,
                    onAdvanceTutorial = ::advanceTutorial,
                    quickLogSwipeModifier = quickLogSwipeModifier,
                    queueDeleteWithUndo = ::queueDeleteWithUndo,
                    cancelPendingDelete = ::cancelPendingDelete,
                    showInfoSnackbar = ::showInfoSnackbar,
                    snackbarHostState = snackbarHostState,
                    showBudgetPeriodSheet = showBudgetPeriodSheet,
                    forceBudgetPeriodSheetSetup = forceBudgetPeriodSheetSetup,
                    selectedViewPeriod = selectedViewPeriod,
                    onPeriodSelected = onPeriodSelected,
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
    topSheetState: SwipeableState<TopSheetValue>,
    contentHeight: Float,
    contentWidth: Float,
    localDensity: Density,
    windowInsets: PaddingValues,
    showCreditQuickToggleFeature: Boolean,
    directCategoryPopupEnabled: Boolean,
    categoryLayoutModeEnabled: Boolean,
    categoryGridModeEnabled: Boolean,
    showCategoryGrid: Boolean,
    onShowCategoryGrid: () -> Unit,
    onHideCategoryGrid: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToWallet: () -> Unit,
    openWalletOnStart: Boolean,
    onProcessIntent: (MainScreenUiIntent) -> Unit,
    onAdvanceTutorial: (FirstLaunchTutorialStage) -> Unit,
    quickLogSwipeModifier: Modifier,
    queueDeleteWithUndo: (Transaction, String) -> Unit,
    cancelPendingDelete: () -> Unit,
    showInfoSnackbar: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    showBudgetPeriodSheet: Boolean,
    forceBudgetPeriodSheetSetup: Boolean,
    selectedViewPeriod: BudgetPeriod?,
    onPeriodSelected: (BudgetPeriod) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val navigationBarOffset = windowInsets.calculateBottomPadding()
    val navBarHeightPx = with(localDensity) { navigationBarOffset.toPx() }

    val defaultInternalKeyboardHeightBase =
        contentWidth
            .coerceAtMost(with(localDensity) { 500.dp.toPx() })
            .coerceAtMost(contentHeight / 2)
    val rowHeightPx = defaultInternalKeyboardHeightBase / 4
    val defaultInternalKeyboardHeight = rowHeightPx * 4
    val calcModeKeyboardHeight = rowHeightPx * 5

    var localDragProgress by remember { mutableFloatStateOf(0f) }

    val systemKeyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val systemKeyboardHeightPx = with(localDensity) { systemKeyboardHeight.toPx() }

    var keepImeLayout by remember { mutableStateOf(false) }
    var lastImeHeightPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(systemKeyboardHeightPx) {
        if (systemKeyboardHeightPx > 0f) {
            lastImeHeightPx = systemKeyboardHeightPx
            keepImeLayout = true
        } else {
            delay(140.milliseconds)
            keepImeLayout = false
        }
    }

    val effectiveProgress =
        if (budgetUiState.isCalculation) 1f - localDragProgress else localDragProgress

    val internalKeyboardTarget =
        defaultInternalKeyboardHeight + (calcModeKeyboardHeight - defaultInternalKeyboardHeight) * effectiveProgress
    val targetKeyboardHeight =
        if (keepImeLayout && lastImeHeightPx > 0f) {
            lastImeHeightPx
        } else {
            internalKeyboardTarget
        }

    val editorHeight by remember(
        contentHeight,
        targetKeyboardHeight,
        navBarHeightPx,
        budgetUiState.isCalculation,
        localDragProgress,
    ) {
        derivedStateOf {
            contentHeight
                .minus(
                    targetKeyboardHeight.plus(navBarHeightPx).coerceAtLeast(0f),
                ).coerceAtMost(contentHeight - (navBarHeightPx + with(localDensity) { 96.dp.toPx() }))
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

    val currentEditorHeight =
        with(localDensity) {
            val halfExpanedOffset =
                (-contentHeight + navBarHeightPx + with(localDensity) { 16.dp.toPx() } + editorHeightAnimated).coerceAtMost(
                    0f,
                )

            (
                topSheetState.offset.value.coerceIn(
                    halfExpanedOffset,
                    0f,
                ) + contentHeight - navBarHeightPx - with(localDensity) { 16.dp.toPx() }
            ).toDp()
        }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val halfExpandedOffsetPx =
            with(localDensity) {
                (-contentHeight + navBarHeightPx + 16.dp.toPx() + editorHeightAnimated).coerceAtMost(0f)
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
                    .height(with(localDensity) { (keyboardHeightAnimated + navBarHeightPx).toDp() })
                    .zIndex(if (isSheetExpanding) 0f else 1f),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
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
                    if (showCategoryGrid && categoryGridModeEnabled) {
                        SavedCategoriesGrid(
                            tags = budgetUiState.tags,
                            selectedCategory = budgetUiState.currentComment,
                            onCategorySelected = { category ->
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetEditorIntent(
                                        BudgetEditorIntent.CommentUpdated(category),
                                    ),
                                )
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.ApplyTapped,
                                    ),
                                )
                                onHideCategoryGrid()
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Numpad(
                            editorState = editorState,
                            numberHintAnchorModifier = Modifier,
                            applyHintAnchorModifier = Modifier,
                            onNumberInput = { digit ->
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.NumberTapped(digit.toString()),
                                    ),
                                )
                                onAdvanceTutorial(FirstLaunchTutorialStage.TAP_ANY_NUMBER)
                            },
                            onDotInput = {
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.DotTapped,
                                    ),
                                )
                            },
                            onBackspace = {
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.BackspaceTapped,
                                    ),
                                )
                            },
                            onBackspaceLongPress = {
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.ResetInputTapped,
                                    ),
                                )
                            },
                            onOperatorInput = { op ->
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.OperatorTapped(op),
                                    ),
                                )
                            },
                            onEqualsInput = {
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.EqualsTapped,
                                    ),
                                )
                            },
                            onApply = {
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.ApplyTapped,
                                    ),
                                )
                                onAdvanceTutorial(FirstLaunchTutorialStage.TAP_DONE_SAVE)
                            },
                            onDragProgressChanged = { progress -> localDragProgress = progress },
                            dragProgress = effectiveProgress,
                            isCalculation = budgetUiState.isCalculation,
                            onCalculationModeChanged = { enabled ->
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.SetCalculationMode(enabled),
                                    ),
                                )
                            },
                            onShowSnackbar = { message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short,
                                    )
                                }
                            },
                            onTestNotifications = {
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.TriggerTestNotifications,
                                    ),
                                )
                            },
                            rowHeight = with(localDensity) { rowHeightPx.toDp() },
                            enableCalculationMode = !categoryGridModeEnabled,
                        )
                    }
                }
        }

        TopSheetLayout(
            swipeableState = topSheetState,
            customHalfHeight = editorHeightAnimated,
            isLockSwipeable = {
                budgetUiState.lockSwipeable || localDragProgress > 0f || (budgetUiState.isCalculation && effectiveProgress < 1f) ||
                    (!budgetUiState.isCalculation && effectiveProgress > 0f)
            },
            isLockDraggable = {
                budgetUiState.lockDraggable || localDragProgress > 0f || (budgetUiState.isCalculation && effectiveProgress < 1f) ||
                    (!budgetUiState.isCalculation && effectiveProgress > 0f)
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
                    onOpenWallet = { onProcessIntent(MainScreenUiIntent.ShowBudgetPeriodSheet()) },
                    openWalletOnStart = openWalletOnStart,
                    showBudgetPeriodSheet = showBudgetPeriodSheet,
                    forceBudgetPeriodSheetSetup = forceBudgetPeriodSheetSetup,
                    selectedViewPeriod = selectedViewPeriod,
                    onPeriodSelected = onPeriodSelected,
                    onShowBudgetPeriodSheet = {
                        onProcessIntent(MainScreenUiIntent.ShowBudgetPeriodSheet())
                    },
                    onHideBudgetPeriodSheet = {
                        onProcessIntent(MainScreenUiIntent.HideBudgetPeriodSheet)
                    },
                    onAnalyticsClickForTutorial = {
                        onAdvanceTutorial(FirstLaunchTutorialStage.TAP_ANALYTICS)
                    },
                    onFocus = {
                        if (budgetUiState.numpadInput.isNotEmpty() && budgetUiState.animState != AnimState.EDITING) {
                            onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetEditorIntent(
                                    BudgetEditorIntent.SetAnimState(AnimState.EDITING),
                                ),
                            )
                        }
                    },
                    onCommentClick = {},
                    onCommentUpdate = { comment ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.CommentUpdated(comment),
                            ),
                        )
                    },
                    onDeleteTag = { tag ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.DeleteTag(tag),
                            ),
                        )
                    },
                    onRecurrentToggle = { enabled ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.SetRecurrentEnabled(enabled),
                            ),
                        )
                    },
                    onCreditToggle = { enabled ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.SetCreditEnabled(enabled),
                            ),
                        )
                    },
                    showCreditQuickToggleFeature = showCreditQuickToggleFeature,
                    directCategoryPopupEnabled = directCategoryPopupEnabled,
                    categoryLayoutModeEnabled = categoryLayoutModeEnabled,
                    categoryGridModeEnabled = categoryGridModeEnabled,
                    onShowCategoryGrid = onShowCategoryGrid,
                    onDismissRecurrentDialog = {
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.DismissRecurrentDialog,
                            ),
                        )
                    },
                    onDismissCreditCutoffDialog = {
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.DismissCreditCutoffDialog,
                            ),
                        )
                    },
                    onRecurrentExpenseConfirm = { freq, date, day, fallbackComment ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.RecurrentExpenseApplied(freq, date, day, fallbackComment),
                            ),
                        )
                    },
                    onCreditCutoffConfirm = { day ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.CreditCutoffDayConfirmed(day),
                            ),
                        )
                    },
                    onSaveBudget = { settings ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.UpdateSettings(settings),
                            ),
                        )
                    },
                    budgetPillHintAnchorModifier = Modifier,
                    analyticsHintAnchorModifier = Modifier,
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
                        onQueueDeleteWithUndo = { tx, msg, _ -> queueDeleteWithUndo(tx, msg) },
                        onCancelPendingDelete = { cancelPendingDelete() },
                        onShowInfoSnackbar = { msg -> showInfoSnackbar(msg) },
                    )
                }
            },
        )

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
    showCreditQuickToggleFeature: Boolean,
    directCategoryPopupEnabled: Boolean,
    categoryLayoutModeEnabled: Boolean,
    categoryGridModeEnabled: Boolean,
    showCategoryGrid: Boolean,
    onShowCategoryGrid: () -> Unit,
    onHideCategoryGrid: () -> Unit,
    onNavigateToWallet: () -> Unit,
    openWalletOnStart: Boolean,
    onProcessIntent: (MainScreenUiIntent) -> Unit,
    onAdvanceTutorial: (FirstLaunchTutorialStage) -> Unit,
    quickLogSwipeModifier: Modifier,
    queueDeleteWithUndo: (Transaction, String) -> Unit,
    cancelPendingDelete: () -> Unit,
    showInfoSnackbar: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    showBudgetPeriodSheet: Boolean,
    forceBudgetPeriodSheetSetup: Boolean,
    selectedViewPeriod: BudgetPeriod?,
    onPeriodSelected: (BudgetPeriod) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val navigationBarOffset = windowInsets.calculateBottomPadding()
    val navBarHeightPx = with(localDensity) { navigationBarOffset.toPx() }

    val defaultInternalKeyboardHeightBase =
        (contentWidth / 2f)
            .coerceAtMost(with(localDensity) { 500.dp.toPx() })
            .coerceAtMost(contentHeight / 2)
    val rowHeightPx = defaultInternalKeyboardHeightBase / 4
    val defaultInternalKeyboardHeight = rowHeightPx * 4
    val calcModeKeyboardHeight = rowHeightPx * 5

    var localDragProgress by remember { mutableFloatStateOf(0f) }

    val effectiveProgress =
        if (budgetUiState.isCalculation) 1f - localDragProgress else localDragProgress

    val internalKeyboardTarget =
        defaultInternalKeyboardHeight + (calcModeKeyboardHeight - defaultInternalKeyboardHeight) * effectiveProgress

    val keyboardAnimationSpec =
        remember<AnimationSpec<Float>>(localDragProgress) {
            if (localDragProgress > 0f && localDragProgress < 1f) {
                tween(durationMillis = 0)
            } else {
                tween(durationMillis = 200, easing = FastOutSlowInEasing)
            }
        }

    val keyboardHeightAnimated by animateFloatAsState(
        label = "keyboardHeightAnimatedValue",
        targetValue = internalKeyboardTarget,
        animationSpec = keyboardAnimationSpec,
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
            StatusBarPadding()
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
                Editor(
                    uiState = budgetUiState,
                    animState = budgetUiState.animState,
                    modifier = Modifier.fillMaxSize(),
                    onOpenHistory = {},
                    onOpenSettings = {},
                    onOpenAnalytics = {},
                    onOpenWallet = { onProcessIntent(MainScreenUiIntent.ShowBudgetPeriodSheet()) },
                    openWalletOnStart = openWalletOnStart,
                    showBudgetPeriodSheet = showBudgetPeriodSheet,
                    forceBudgetPeriodSheetSetup = forceBudgetPeriodSheetSetup,
                    selectedViewPeriod = selectedViewPeriod,
                    onPeriodSelected = onPeriodSelected,
                    onShowBudgetPeriodSheet = {
                        onProcessIntent(MainScreenUiIntent.ShowBudgetPeriodSheet())
                    },
                    onHideBudgetPeriodSheet = {
                        onProcessIntent(MainScreenUiIntent.HideBudgetPeriodSheet)
                    },
                    onAnalyticsClickForTutorial = {
                        onAdvanceTutorial(FirstLaunchTutorialStage.TAP_ANALYTICS)
                    },
                    onFocus = {
                        if (budgetUiState.numpadInput.isNotEmpty() && budgetUiState.animState != AnimState.EDITING) {
                            onProcessIntent(
                                MainScreenUiIntent.ProcessBudgetEditorIntent(
                                    BudgetEditorIntent.SetAnimState(AnimState.EDITING),
                                ),
                            )
                        }
                    },
                    onCommentClick = {},
                    onCommentUpdate = { comment ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.CommentUpdated(comment),
                            ),
                        )
                    },
                    onDeleteTag = { tag ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.DeleteTag(tag),
                            ),
                        )
                    },
                    onRecurrentToggle = { enabled ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.SetRecurrentEnabled(enabled),
                            ),
                        )
                    },
                    onCreditToggle = { enabled ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.SetCreditEnabled(enabled),
                            ),
                        )
                    },
                    showCreditQuickToggleFeature = showCreditQuickToggleFeature,
                    directCategoryPopupEnabled = directCategoryPopupEnabled,
                    categoryLayoutModeEnabled = categoryLayoutModeEnabled,
                    categoryGridModeEnabled = categoryGridModeEnabled,
                    onShowCategoryGrid = onShowCategoryGrid,
                    onDismissRecurrentDialog = {
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.DismissRecurrentDialog,
                            ),
                        )
                    },
                    onDismissCreditCutoffDialog = {
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.DismissCreditCutoffDialog,
                            ),
                        )
                    },
                    onRecurrentExpenseConfirm = { freq, date, day, fallbackComment ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.RecurrentExpenseApplied(freq, date, day, fallbackComment),
                            ),
                        )
                    },
                    onCreditCutoffConfirm = { day ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.CreditCutoffDayConfirmed(day),
                            ),
                        )
                    },
                    onSaveBudget = { settings ->
                        onProcessIntent(
                            MainScreenUiIntent.ProcessBudgetEditorIntent(
                                BudgetEditorIntent.UpdateSettings(settings),
                            ),
                        )
                    },
                    showAnalyticsButton = false,
                    showSettingsButton = false,
                    budgetPillHintAnchorModifier = Modifier,
                    analyticsHintAnchorModifier = Modifier,
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
                        .height(with(localDensity) { (keyboardHeightAnimated + navBarHeightPx).toDp() }),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter,
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
                    if (showCategoryGrid && categoryGridModeEnabled) {
                        SavedCategoriesGrid(
                            tags = budgetUiState.tags,
                            selectedCategory = budgetUiState.currentComment,
                            onCategorySelected = { category ->
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetEditorIntent(
                                        BudgetEditorIntent.CommentUpdated(category),
                                    ),
                                )
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.ApplyTapped,
                                    ),
                                )
                                onHideCategoryGrid()
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Numpad(
                            editorState = editorState,
                            numberHintAnchorModifier = Modifier,
                            applyHintAnchorModifier = Modifier,
                            onNumberInput = { digit ->
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.NumberTapped(digit.toString()),
                                    ),
                                )
                                onAdvanceTutorial(FirstLaunchTutorialStage.TAP_ANY_NUMBER)
                            },
                            onDotInput = {
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.DotTapped,
                                    ),
                                )
                            },
                            onBackspace = {
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.BackspaceTapped,
                                    ),
                                )
                            },
                            onBackspaceLongPress = {
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.ResetInputTapped,
                                    ),
                                )
                            },
                            onOperatorInput = { op ->
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.OperatorTapped(op),
                                    ),
                                )
                            },
                            onEqualsInput = {
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.EqualsTapped,
                                    ),
                                )
                            },
                            onApply = {
                                Log.d("MainScreen", "Numpad check/save button pressed")
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.ApplyTapped,
                                    ),
                                )
                                onAdvanceTutorial(FirstLaunchTutorialStage.TAP_DONE_SAVE)
                            },
                            onDragProgressChanged = { progress -> localDragProgress = progress },
                            dragProgress = localDragProgress,
                            isCalculation = budgetUiState.isCalculation,
                            onCalculationModeChanged = { enabled ->
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.SetCalculationMode(enabled),
                                    ),
                                )
                                localDragProgress = 0f
                            },
                            onShowSnackbar = { message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short,
                                    )
                                }
                            },
                            onTestNotifications = {
                                onProcessIntent(
                                    MainScreenUiIntent.ProcessBudgetNumpadIntent(
                                        BudgetNumpadIntent.TriggerTestNotifications,
                                    ),
                                )
                            },
                            rowHeight = with(localDensity) { rowHeightPx.toDp() },
                            enableCalculationMode = !categoryGridModeEnabled,
                        )
                    }
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
                onboardingCompleted = true,
                tutorialStage = FirstLaunchTutorialStage.COMPLETED,
                showCreditQuickToggleFeature = true,
                directCategoryPopupEnabled = false,
                categoryLayoutModeEnabled = false,
                categoryGridModeEnabled = false,
                onProcessIntent = {},
                onNavigateToAnalytics = {},
                onNavigateToSettings = {},
                onNavigateToWallet = {},
                openWalletOnStart = false,
                showBudgetPeriodSheet = false,
                forceBudgetPeriodSheetSetup = false,
                selectedViewPeriod = BudgetPeriod.DAILY,
                onPeriodSelected = {},
                settingsDataStore = null,
                undoSnackbarActionLabel = "Undo",
            )
        }
    }
}
