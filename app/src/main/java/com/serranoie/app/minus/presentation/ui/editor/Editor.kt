@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.editor

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.ui.budget.BudgetUiState
import com.serranoie.app.minus.presentation.ui.editor.calculation.evaluateCalculation
import com.serranoie.app.minus.presentation.ui.editor.category.CategoryToolbar
import com.serranoie.app.minus.presentation.ui.editor.category.FocusController
import com.serranoie.app.minus.presentation.ui.editor.dialogs.CreditCutoffDayDialog
import com.serranoie.app.minus.presentation.ui.editor.dialogs.RecurrentExpenseDialog
import com.serranoie.app.minus.presentation.ui.editor.sheets.BudgetPeriodSheet
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorButton
import com.serranoie.app.minus.presentation.ui.theme.component.AutoResizeBasicTextField
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetPill
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.ui.theme.displayLargeCondensed
import com.serranoie.app.minus.presentation.util.LocalCensorMode
import com.serranoie.app.minus.presentation.util.Utils.weakHapticFeedback
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.logcat
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Editor(
    uiState: BudgetUiState,
    animState: AnimState,
    onInputChange: (String) -> Unit = {},
    onFocus: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAnalytics: () -> Unit = {},
    onOpenWallet: () -> Unit = {},
    openWalletOnStart: Boolean = false,
    showBudgetPeriodSheet: Boolean = false,
    forceBudgetPeriodSheetSetup: Boolean = false,
    selectedViewPeriod: BudgetPeriod? = null,
    onShowBudgetPeriodSheet: () -> Unit = {},
    onHideBudgetPeriodSheet: () -> Unit = {},
    onPeriodSelected: (BudgetPeriod) -> Unit = {},
    onCommentClick: () -> Unit,
    onBudgetPillClickForTutorial: () -> Unit = {},
    onAnalyticsClickForTutorial: () -> Unit = {},
    onChangePeriod: (BudgetPeriod) -> Unit = {},
    onFinishBudgetEarly: () -> Unit = {},
    onSaveBudget: (BudgetSettings) -> Unit = {},
    onCommentUpdate: (String) -> Unit = {},
    onDeleteTag: (String) -> Unit = {},
    onRecurrentToggle: (Boolean) -> Unit = {},
    onCreditToggle: (Boolean) -> Unit = {},
    showCreditQuickToggleFeature: Boolean = false,
    onDismissRecurrentDialog: () -> Unit = {},
    onDismissCreditCutoffDialog: () -> Unit = {},
    onRecurrentExpenseConfirm: (RecurrentFrequency, LocalDate, Int?, String) -> Unit = { _, _, _, _ -> },
    onCreditCutoffConfirm: (Int) -> Unit = {},
    showAnalyticsButton: Boolean = true,
    showSettingsButton: Boolean = true,
    budgetPillHintAnchorModifier: Modifier = Modifier,
    analyticsHintAnchorModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val editorFocusController = remember { FocusController() }

    if (uiState.showRecurrentDialog) {
        RecurrentExpenseDialog(
            budgetSettings = uiState.budgetSettings,
            onDismiss = onDismissRecurrentDialog,
            onConfirm = onRecurrentExpenseConfirm,
            fallbackComments = mapOf(
                RecurrentFrequency.WEEKLY to stringResource(R.string.recurrent_ticket_weekly_unnamed),
                RecurrentFrequency.BIWEEKLY to stringResource(R.string.recurrent_ticket_biweekly_unnamed),
                RecurrentFrequency.MONTHLY to stringResource(R.string.recurrent_ticket_monthly_unnamed),
            ),
        )
    }

    if (uiState.showCreditCutoffDialog) {
        CreditCutoffDayDialog(
            onDismiss = onDismissCreditCutoffDialog,
            onConfirm = onCreditCutoffConfirm
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorButton)
            .statusBarsPadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onFocus() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BudgetPill(
                budgetState = uiState.budgetState,
                budgetSettings = uiState.budgetSettings,
                viewPeriod = selectedViewPeriod ?: BudgetPeriod.DAILY,
                currencyCode = uiState.budgetSettings?.currencyCode ?: "USD",
                centerRemainingAmount = animState == AnimState.EDITING,
                onOpenBudgetSheet = {
                    view.weakHapticFeedback()
                    onShowBudgetPeriodSheet()
                },
                modifier = Modifier
                    .weight(1f)
                    .animateContentSize(animationSpec = tween(200))
                    .padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
                    .then(budgetPillHintAnchorModifier)
            )

            AnimatedContent(
                targetState = animState == AnimState.EDITING,
                transitionSpec = {
                    slideInHorizontally(animationSpec = tween(200)) { it } + fadeIn(tween(200)) togetherWith slideOutHorizontally(
                        animationSpec = tween(200)
                    ) { -it } + fadeOut(
                        tween(
                            200
                        )
                    )
                },
                label = "topBarTrailingSwitch"
            ) { isEditing ->
                if (isEditing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showCreditQuickToggleFeature) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(
                                    ButtonGroupDefaults.ConnectedSpaceBetween
                                ),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                val creditDescription = "Credit card payment"
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                        TooltipAnchorPosition.Above
                                    ),
                                    tooltip = {
                                        PlainTooltip(
                                            modifier = Modifier.semantics {
                                                liveRegion = LiveRegionMode.Assertive
                                                paneTitle = creditDescription
                                            }
                                        ) { Text(creditDescription) }
                                    },
                                    state = rememberTooltipState(),
                                ) {
                                    ToggleButton(
                                        checked = uiState.isCreditEnabled,
                                        onCheckedChange = onCreditToggle,
                                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                        modifier = Modifier.semantics { role = Role.RadioButton },
                                        colors = ToggleButtonDefaults.toggleButtonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(
                                                alpha = 0.5f
                                            ),
                                            checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                            checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CreditCard,
                                            contentDescription = creditDescription
                                        )
                                    }
                                }

                                val recurrentDescription = "Recurrent payment"
                                TooltipBox(
                                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                        TooltipAnchorPosition.Above
                                    ),
                                    tooltip = {
                                        PlainTooltip(
                                            modifier = Modifier.semantics {
                                                liveRegion = LiveRegionMode.Assertive
                                                paneTitle = recurrentDescription
                                            }
                                        ) { Text(recurrentDescription) }
                                    },
                                    state = rememberTooltipState(),
                                ) {
                                    ToggleButton(
                                        checked = uiState.isRecurrentEnabled,
                                        onCheckedChange = onRecurrentToggle,
                                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                        modifier = Modifier.semantics { role = Role.RadioButton },
                                        colors = ToggleButtonDefaults.toggleButtonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(
                                                alpha = 0.5f
                                            ),
                                            checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                            checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.EventRepeat,
                                            contentDescription = recurrentDescription
                                        )
                                    }
                                }
                            }
                        } else {
                            val recurrentDescription = "Recurrent payment"
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Below
                                ),
                                tooltip = {
                                    PlainTooltip(
                                        modifier = Modifier.semantics {
                                            liveRegion = LiveRegionMode.Assertive
                                            paneTitle = recurrentDescription
                                        }
                                    ) { Text(recurrentDescription) }
                                },
                                state = rememberTooltipState(),
                            ) {
                                ToggleButton(
                                    checked = uiState.isRecurrentEnabled,
                                    onCheckedChange = onRecurrentToggle,
                                    shapes = ToggleButtonDefaults.shapes(),
                                    modifier = Modifier.semantics { role = Role.RadioButton },
                                    colors = ToggleButtonDefaults.toggleButtonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(
                                            alpha = 0.5f
                                        ),
                                        checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                        checkedContentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EventRepeat,
                                        contentDescription = recurrentDescription
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (showAnalyticsButton) {
                            IconButton(
                                onClick = {
                                    onAnalyticsClickForTutorial()
                                    view.weakHapticFeedback()
                                    onOpenAnalytics()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .then(analyticsHintAnchorModifier)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BarChart,
                                    contentDescription = "Analytics",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }

                        if (showSettingsButton) {
                            val isCensored = LocalCensorMode.current
                            IconButton(
                                onClick = {
                                    onOpenSettings()
                                    view.weakHapticFeedback()
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                BadgedBox(
                                    badge = { if (isCensored) Badge() },
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedContent(
            targetState = animState,
            transitionSpec = {
                when (targetState) {
                    AnimState.EDITING -> fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    AnimState.IDLE -> fadeIn(tween(300)) togetherWith fadeOut(tween(200))
                    else -> fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                }
            },
            label = "editorContent"
        ) { state ->
            when (state) {
                AnimState.EDITING -> {
                    EditingContent(
                        input = uiState.numpadInput,
                        currencyCode = uiState.budgetSettings?.currencyCode ?: "USD",
                        tags = uiState.tags,
                        currentComment = uiState.currentComment,
                        onCommentUpdate = onCommentUpdate,
                        onDeleteTag = onDeleteTag,
                        editorFocusController = editorFocusController,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                AnimState.IDLE, AnimState.RESET -> {
                    IdleContent(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Saving...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showBudgetPeriodSheet) {
        ModalBottomSheet(
            onDismissRequest = onHideBudgetPeriodSheet,
            sheetState = sheetState,
        ) {
            logcat {
                "Opening BudgetPeriodSheet: forceBudgetPeriodSheetSetup=$forceBudgetPeriodSheetSetup, hasBudgetSettings=${uiState.budgetSettings != null}, currentPeriodId=${uiState.currentPeriodId}, startInEditMode=$forceBudgetPeriodSheetSetup"
            }
            BudgetPeriodSheet(
                budgetSettings = uiState.budgetSettings,
                budgetState = uiState.budgetState,
                selectedPeriod = selectedViewPeriod,
                pendingExpensesCount = uiState.pendingExpensesForNextPeriod.size,
                currencyCode = uiState.budgetSettings?.currencyCode ?: "USD",
                startInEditMode = forceBudgetPeriodSheetSetup,
                onPeriodSelected = { newPeriod ->
                    logcat { "BudgetPeriodSheet onPeriodSelected -> newPeriod=$newPeriod" }
                    onPeriodSelected(newPeriod)
                },
                onSaveBudget = { newSettings ->
                    logcat { "BudgetPeriodSheet onSaveBudget -> $newSettings" }
                    onSaveBudget(newSettings)
                    scope.launch { sheetState.hide() }
                    onHideBudgetPeriodSheet()
                },
                onEditBudget = {
                    onShowBudgetPeriodSheet()
                    scope.launch { sheetState.hide() }
                },
                onFinishEarly = {
                    onFinishBudgetEarly()
                    onOpenAnalytics()
                    scope.launch { sheetState.hide() }
                    onHideBudgetPeriodSheet()
                },
            )
        }
    }
}

@Composable
private fun EditingContent(
    input: String,
    currencyCode: String,
    tags: List<String>,
    currentComment: String,
    onCommentUpdate: (String) -> Unit,
    onDeleteTag: (String) -> Unit,
    editorFocusController: FocusController,
    modifier: Modifier = Modifier
) {
    val currencyFormat = symbolOnlyCurrencyFormat(currencyCode)
    val currencySymbol = SupportedCurrency.findByCode(currencyCode)?.symbol ?: "$"

    val hasExpressionOperators = remember(input) { input.any { it in "+-×÷" } }

    val calculationResult = remember(input, hasExpressionOperators) {
        if (!hasExpressionOperators || input.isEmpty()) return@remember null

        val last = input.lastOrNull()
        if (last != null && (last in "+-×÷" || last == '.')) {
            null
        } else {
            evaluateCalculation(input)
        }
    }

    val displayContent = if (hasExpressionOperators) {
        "$currencySymbol $input"
    } else {
        try {
            val value = input.toBigDecimalOrNull() ?: BigDecimal.ZERO
            currencyFormat.format(value)
        } catch (_: Exception) {
            input.ifEmpty { currencyFormat.format(BigDecimal.ZERO) }
        }
    }

    val baseTextStyle = MaterialTheme.typography.displayLargeCondensed.copy(
        fontWeight = FontWeight.W500,
        fontSize = 86.sp,
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val availableWidth = maxWidth - 32.dp
        val amountSlotHeight = 124.dp
        val containerSizePx = remember(availableWidth, amountSlotHeight, density) {
            with(density) {
                IntSize(
                    width = availableWidth.toPx().toInt(),
                    height = amountSlotHeight.toPx().toInt()
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(amountSlotHeight)
                    .padding(start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                AnimatedContent(
                    targetState = if (hasExpressionOperators && calculationResult != null) "result" else "input",
                    transitionSpec = {
                        (
                                fadeIn(animationSpec = tween(200)) + slideInHorizontally(
                                    animationSpec = tween(200)
                                ) { it / 4 }
                                ) togetherWith (
                                fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                                    animationSpec = tween(200)
                                ) { -it / 4 }
                                )
                    },
                    label = "EditorNumberTransition",
                    modifier = Modifier.fillMaxWidth()
                ) { state ->
                    if (state == "result" && hasExpressionOperators && calculationResult != null) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AutoResizeBasicTextField(
                                value = displayContent,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.wrapContentWidth(Alignment.End),
                                textStyle = baseTextStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End
                                ),
                                singleLine = true,
                                minFontSize = 20.sp,
                                maxFontSize = 57.sp,
                                containerSize = containerSizePx
                            )
                            AutoResizeBasicTextField(
                                value = "= ${currencySymbol}$calculationResult",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .wrapContentWidth(Alignment.End)
                                    .padding(top = 4.dp),
                                textStyle = baseTextStyle.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End
                                ),
                                singleLine = true,
                                minFontSize = 16.sp,
                                maxFontSize = 36.sp,
                                containerSize = containerSizePx
                            )
                        }
                    } else {
                        AutoResizeBasicTextField(
                            value = displayContent,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.wrapContentWidth(Alignment.End),
                            textStyle = baseTextStyle.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            ),
                            singleLine = true,
                            containerSize = containerSizePx,
                            decorationBox = { innerTextField ->
                                Box { innerTextField() }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            CategoryToolbar(
                tags = tags,
                currentComment = currentComment,
                stage = EditStage.EDIT_SPENT,
                onCommentUpdate = onCommentUpdate,
                onDeleteTag = onDeleteTag,
                editorFocusController = editorFocusController,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp)
            )
        }
    }
}

@Composable
private fun IdleContent(
    modifier: Modifier = Modifier
) {
    val cursorVisible = remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(530.milliseconds)
            cursorVisible.value = !cursorVisible.value
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = if (cursorVisible.value) "|" else "",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Preview
@Composable
fun EditorPreview_Idle() {
    MinusTheme {
        Editor(
            uiState = BudgetUiState(
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("500.00"),
                    period = BudgetPeriod.DAILY,
                    startDate = LocalDate.now(),
                    currencyCode = "USD"
                ),
                budgetState = BudgetState(
                    remainingToday = BigDecimal("110.00"),
                    totalSpentToday = BigDecimal("12.50"),
                    dailyBudget = BigDecimal("122.50"),
                    daysRemaining = 15,
                    progress = 0.1f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("500.00"),
                    totalSpentInPeriod = BigDecimal("12.50")
                ),
                transactions = emptyList(),
                numpadInput = "",
                isNumpadValid = false
            ),
            animState = AnimState.IDLE,
            onFocus = {},
            onOpenHistory = {},
            onOpenSettings = {},
            onCommentClick = {},
            onCommentUpdate = {},
            onDeleteTag = {},
            onRecurrentToggle = {},
            onCreditToggle = {},
            onDismissRecurrentDialog = {},
            onDismissCreditCutoffDialog = {},
            onRecurrentExpenseConfirm = { _, _, _, _ -> },
            onCreditCutoffConfirm = {}
        )
    }
}

@PreviewLightDark
@Composable
fun EditorPreview_Editing() {
    MinusTheme {
        Editor(
            uiState = BudgetUiState(
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("500.00"),
                    period = BudgetPeriod.DAILY,
                    startDate = LocalDate.now(),
                    currencyCode = "USD"
                ),
                budgetState = BudgetState(
                    remainingToday = BigDecimal("110.00"),
                    totalSpentToday = BigDecimal("12.50"),
                    dailyBudget = BigDecimal("122.50"),
                    daysRemaining = 15,
                    progress = 0.1f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("500.00"),
                    totalSpentInPeriod = BigDecimal("12.50")
                ),
                transactions = emptyList(),
                numpadInput = "250",
                isNumpadValid = true
            ),
            animState = AnimState.EDITING,
            onFocus = {},
            onOpenHistory = {},
            onOpenSettings = {},
            onCommentClick = {},
            onCommentUpdate = {},
            onDeleteTag = {},
            onRecurrentToggle = {},
            onCreditToggle = {},
            onDismissRecurrentDialog = {},
            onDismissCreditCutoffDialog = {},
            onRecurrentExpenseConfirm = { _, _, _, _ -> },
            onCreditCutoffConfirm = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun EditorPreview_Editing_WithCredit() {
    MinusTheme {
        Editor(
            uiState = BudgetUiState(
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("500.00"),
                    period = BudgetPeriod.DAILY,
                    startDate = LocalDate.now(),
                    currencyCode = "USD"
                ),
                budgetState = BudgetState(
                    remainingToday = BigDecimal("110.00"),
                    totalSpentToday = BigDecimal("12.50"),
                    dailyBudget = BigDecimal("122.50"),
                    daysRemaining = 15,
                    progress = 0.1f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("500.00"),
                    totalSpentInPeriod = BigDecimal("12.50")
                ),
                transactions = emptyList(),
                numpadInput = "250",
                isNumpadValid = true,
                isCreditEnabled = true,
                isRecurrentEnabled = true
            ),
            animState = AnimState.EDITING,
            showCreditQuickToggleFeature = true,
            onFocus = {},
            onOpenHistory = {},
            onOpenSettings = {},
            onCommentClick = {},
            onCommentUpdate = {},
            onDeleteTag = {},
            onRecurrentToggle = {},
            onCreditToggle = {},
            onDismissRecurrentDialog = {},
            onDismissCreditCutoffDialog = {},
            onRecurrentExpenseConfirm = { _, _, _, _ -> },
            onCreditCutoffConfirm = {}
        )
    }
}
