@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.editor.sheets

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.domain.model.SupportedCurrencyData
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.CalculatedSplitCard
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.allocationFor
import com.serranoie.app.minus.presentation.ui.editor.sheets.split.availablePeriodsFor
import com.serranoie.app.minus.presentation.ui.onboarding.FinishDateSelector
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.colorButton
import com.serranoie.app.minus.presentation.ui.theme.displaySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.component.ButtonRow
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.budget.SpendBudgetCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.TotalBudgetCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.formula.BudgetFormulaRequest
import com.serranoie.app.minus.presentation.ui.theme.component.date.DaysLeftCard
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.Utils.confirmFeedback
import com.serranoie.app.minus.presentation.util.Utils.weakHapticFeedback
import com.serranoie.app.minus.presentation.util.font.format.CurrencyAmountInputVisualTransformation
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import logcat.logcat
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

const val BUDGET_PERIOD_SHEET_TAG = "BudgetPeriodSheet"
const val BUDGET_PERIOD_EDIT_BUTTON_TAG = "BudgetPeriodSheet.EditButton"
const val BUDGET_PERIOD_FINISH_EARLY_BUTTON_TAG = "BudgetPeriodSheet.FinishEarlyButton"
const val BUDGET_PERIOD_APPLY_BUTTON_TAG = "BudgetPeriodSheet.ApplyButton"
const val BUDGET_PERIOD_BUDGET_INPUT_TAG = "BudgetPeriodSheet.BudgetInput"
const val BUDGET_PERIOD_PREVIOUS_VALUES_TAG = "BudgetPeriodSheet.PreviousValues"
const val BUDGET_PERIOD_DATE_ROW_TAG = "BudgetPeriodSheet.DateRow"
const val BUDGET_PERIOD_CURRENCY_ROW_TAG = "BudgetPeriodSheet.CurrencyRow"
const val BUDGET_PERIOD_NEXT_BUTTON_TAG = "BudgetPeriodSheet.NextButton"
const val BUDGET_PERIOD_SPLIT_TOGGLE_ROW_TAG = "BudgetPeriodSheet.SplitToggleRow"
const val BUDGET_PERIOD_ROLLOVER_PREVIEW_BANNER_TAG = "BudgetPeriodSheet.RolloverPreviewBanner"

fun budgetPeriodCardTag(period: BudgetPeriod) = "BudgetPeriodSheet.Period.${period.name}"
fun budgetPeriodToggleTag(period: BudgetPeriod) = "BudgetPeriodSheet.SplitToggle.${period.name}"
fun budgetStrategyOptionTag(strategy: RemainingBudgetStrategy) = "BudgetPeriodSheet.Strategy.${strategy.name}"
fun budgetSplitModeOptionTag(mode: BudgetSplitMode) = "BudgetPeriodSheet.SplitMode.${mode.name}"

@Composable
fun BudgetPeriodSheet(
    budgetSettings: BudgetSettings?,
    budgetState: BudgetState?,
    selectedPeriod: BudgetPeriod? = budgetSettings?.period,
    currencyCode: String,
    onPeriodSelected: (BudgetPeriod) -> Unit,
    onSaveBudget: ((BudgetSettings) -> Unit)? = null,
    onEditBudget: (() -> Unit)? = null,
    onFinishEarly: (() -> Unit)? = null,
    startInEditMode: Boolean = false,
    pendingExpensesCount: Int = 0,
    onShowFormula: ((BudgetFormulaRequest) -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val currencyFormat = remember(currencyCode) {
        symbolOnlyCurrencyFormat(currencyCode)
    }

    val startDate = budgetSettings?.startDate ?: LocalDate.now()
    val endDate = budgetSettings?.endDate
    val totalBudget = budgetSettings?.totalBudget ?: BigDecimal.ZERO
    var periodCache by remember(selectedPeriod) { mutableStateOf(selectedPeriod) }

    var isEditMode by remember(startInEditMode) { mutableStateOf(startInEditMode) }
    var showFinishConfirm by remember { mutableStateOf(false) }

    val totalDays =
        remember(startDate, endDate) {
            if (endDate != null) ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1 else 30
        }

    fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())

    val startDateAsDate = remember(startDate) { startDate.toDate() }
    val endDateAsDate = remember(endDate) { endDate?.toDate() }
    val totalSpent = budgetState?.totalSpentInPeriod ?: BigDecimal.ZERO

    val today = remember { LocalDate.now() }
    val daysRemaining =
        remember(startDate, endDate, today) {
            if (endDate == null) totalDays
            else ((ChronoUnit.DAYS.between(today, endDate).toInt() + 1)
                .coerceIn(0, totalDays))
        }

    val available =
        if (totalDays > 0) availablePeriodsFor(totalDays) else listOf(BudgetPeriod.DAILY)

    LaunchedEffect(available, totalDays) {
        logcat {
            "reconcilePeriodCache: current=$periodCache, available=$available, totalDays=$totalDays, start=$startDate, end=$endDate"
        }
        if (periodCache !in available && available.isNotEmpty()) {
            val previous = periodCache
            periodCache = available.first()
            logcat {
                "periodCache auto-adjusted from $previous to $periodCache because previous is not available for totalDays=$totalDays"
            }
            onPeriodSelected(periodCache!!)
        }
    }

    val stepSlideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val stepFadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    AnimatedContent(
        modifier = Modifier.testTag(BUDGET_PERIOD_SHEET_TAG),
        targetState = isEditMode,
        transitionSpec = {
            if (targetState) {
                (
                    slideInHorizontally(
                        initialOffsetX = { it / 3 },
                        animationSpec = stepSlideSpec,
                    ) + fadeIn(stepFadeSpec)
                ).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = stepSlideSpec,
                    ) + fadeOut(stepFadeSpec),
                )
            } else {
                (
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = stepSlideSpec,
                    ) + fadeIn(stepFadeSpec)
                ).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { it / 3 },
                        animationSpec = stepSlideSpec,
                    ) + fadeOut(stepFadeSpec),
                )
            }
        },
        label = "sheetContent",
    ) { editMode ->
        if (editMode) {
            EditBudgetContent(
                budgetSettings = budgetSettings,
                onBack = { isEditMode = false },
                onApply = { newSettings ->
                    onSaveBudget?.invoke(newSettings)
                    isEditMode = false
                },
                pendingExpensesCount = pendingExpensesCount,
            )
        } else {
            ViewBudgetContent(
                budgetSettings = budgetSettings,
                budgetState = budgetState,
                periodCache = periodCache ?: available.first(),
                currencyFormat = currencyFormat,
                currencyCode = currencyCode,
                totalBudget = totalBudget,
                totalSpent = totalSpent,
                totalDays = totalDays,
                daysRemaining = daysRemaining,
                startDateAsDate = startDateAsDate,
                endDateAsDate = endDateAsDate,
                available = available,
                onShowFormula = onShowFormula,
                onPeriodSelected = { p ->
                    logcat { "User selected period chip: $p (previous=$periodCache)" }
                    periodCache = p
                    onPeriodSelected(p)
                },
                onEditClick = {
                    if (onSaveBudget != null) {
                        isEditMode = true
                    } else {
                        onEditBudget?.invoke()
                    }
                },
                onFinishEarlyClick = { showFinishConfirm = true },
                showFinishEarly = onFinishEarly != null && budgetSettings != null,
            )
        }
    }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = {
                Text(
                    stringResource(R.string.finalize_period_question),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
            },
            text = { Text(stringResource(R.string.finalize_period_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFinishConfirm = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onFinishEarly?.invoke()
                    },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(
                        stringResource(R.string.finalize_action),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMediumEmphasized,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showFinishConfirm = false },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelMediumEmphasized,
                    )
                }
            },
        )
    }
}

@Composable
private fun ViewBudgetContent(
    budgetSettings: BudgetSettings?,
    budgetState: BudgetState?,
    periodCache: BudgetPeriod,
    currencyFormat: NumberFormat,
    currencyCode: String,
    totalBudget: BigDecimal,
    totalSpent: BigDecimal,
    totalDays: Int,
    daysRemaining: Int,
    startDateAsDate: Date,
    endDateAsDate: Date?,
    available: List<BudgetPeriod>,
    onPeriodSelected: (BudgetPeriod) -> Unit,
    onEditClick: () -> Unit,
    onFinishEarlyClick: () -> Unit,
    showFinishEarly: Boolean,
    onShowFormula: ((BudgetFormulaRequest) -> Unit)?,
) {
    val view = LocalView.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 32.dp)
                .navigationBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.total_budget),
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
            TooltipIconButton(
                title = stringResource(R.string.edit_budget),
                icon = Icons.Default.Edit,
                onClick = onEditClick,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(BUDGET_PERIOD_EDIT_BUTTON_TAG),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        logcat(BUDGET_PERIOD_SHEET_TAG) {
            "BudgetDisplay input totalBudget=$totalBudget budgetStateTotal=${budgetState?.totalBudget} budgetSettingsTotal=${budgetSettings?.totalBudget} rollOverLimit=${budgetSettings?.rollOverLimit} rollOverCarry=${budgetSettings?.rollOverCarryForward}"
        }

        SpendBudgetCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(bottom = 8.dp),
            budget = budgetState?.totalBudget ?: totalBudget,
            spend = totalSpent,
            currency = currencyCode,
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1.65f)
                        .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                TotalBudgetCard(
                    budget = totalBudget,
                    budgetState = budgetState,
                    budgetSettings = budgetSettings,
                    currencyCode = currencyCode,
                    bigVariant = false,
                    modifier = Modifier.fillMaxWidth(),
                    startDate = startDateAsDate,
                    finishDate = endDateAsDate,
                )
            }

            if (endDateAsDate != null) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    DaysLeftCard(
                        startDate = startDateAsDate,
                        finishDate = endDateAsDate,
                    )
                }
            } else {
                // NOTE: this behavior shouldn't happen, sheet need to render new budget state.
                Card(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(120.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_ending_date),
                            style = MaterialTheme.typography.bodySmallEmphasized,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.budget_split_logic),
            style = MaterialTheme.typography.titleMediumCondensed,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (totalDays > 0 && available.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .testTag(BUDGET_PERIOD_SPLIT_TOGGLE_ROW_TAG),
                horizontalArrangement = Arrangement.spacedBy(
                    ButtonGroupDefaults.ConnectedSpaceBetween
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                available.forEachIndexed { index, p ->
                    val isSelected = p == periodCache
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = {
                            view.confirmFeedback()
                            onPeriodSelected(p)
                        },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            available.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                role = Role.RadioButton
                                selected = isSelected
                            }
                            .testTag(budgetPeriodToggleTag(p)),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    ) {
                        Text(
                            text = periodLabel(p),
                            style = if (isSelected) {
                                MaterialTheme.typography.labelMediumEmphasized
                            } else {
                                MaterialTheme.typography.labelMediumCondensed
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val splitMode = budgetSettings?.splitMode ?: BudgetSplitMode.STATIC
            val formulaTip = stringResource(R.string.budget_formula_tip_hold_pill)
            val openFormula = if (onShowFormula != null && budgetState != null) {
                val request = BudgetFormulaRequest(
                    budgetState, budgetSettings, periodCache, splitMode, currencyCode, tip = formulaTip,
                )
                fun() {
                    view.weakHapticFeedback()
                    onShowFormula(request)
                }
            } else {
                null
            }
            CalculatedSplitCard(
                periodCache = periodCache,
                allocation = budgetState?.allocationFor(periodCache, splitMode) ?: BigDecimal.ZERO,
                splitMode = splitMode,
                currencyFormat = currencyFormat,
                totalBudget = budgetState?.totalBudget ?: BigDecimal.ZERO,
                remaining = (budgetState?.totalBudget ?: BigDecimal.ZERO)
                    .subtract(budgetState?.totalSpentInPeriod ?: BigDecimal.ZERO),
                totalDays = totalDays,
                daysRemaining = budgetState?.daysRemaining ?: 0,
                modifier = Modifier.fillMaxWidth(),
                onClick = openFormula,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        val remaining = totalBudget.subtract(totalSpent)
        if (showFinishEarly && remaining > BigDecimal.ZERO && daysRemaining <= 3) {
            RolloverPreviewBanner(
                remainingAmount = remaining,
                strategy = budgetSettings?.remainingBudgetStrategy ?: RemainingBudgetStrategy.ASK_ALWAYS,
                currencyFormat = currencyFormat,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag(BUDGET_PERIOD_ROLLOVER_PREVIEW_BANNER_TAG),
            )
        }

        if (showFinishEarly) {
            OutlinedButton(
                onClick = onFinishEarlyClick,
                shapes = ButtonDefaults.shapes(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(BUDGET_PERIOD_FINISH_EARLY_BUTTON_TAG),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = stringResource(R.string.finalize_period),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RolloverPreviewBanner(
    remainingAmount: BigDecimal,
    strategy: RemainingBudgetStrategy,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier,
) {
    val message = when (strategy) {
        RemainingBudgetStrategy.SPLIT_EQUALLY -> stringResource(
            R.string.rollover_preview_split_equally,
            currencyFormat.format(remainingAmount),
        )

        RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> stringResource(
            R.string.rollover_preview_add_to_first_day,
            currencyFormat.format(remainingAmount),
        )

        RemainingBudgetStrategy.ASK_ALWAYS -> stringResource(
            R.string.rollover_preview_ask_always,
            currencyFormat.format(remainingAmount),
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmallCondensed,
            )
        }
    }
}

@Composable
fun EditBudgetContent(
    budgetSettings: BudgetSettings?,
    onBack: () -> Unit = {},
    onApply: (BudgetSettings) -> Unit,
    title: String = stringResource(R.string.new_budget_period),
    buttonLabel: String = stringResource(R.string.apply),
    showPreviousValuesChip: Boolean = true,
    pendingExpensesCount: Int = 0,
) {
    val haptic = LocalHapticFeedback.current
    val resources = LocalResources.current
    val dateFormatter =
        remember {
            DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        }

    val budgetFieldLabel = stringResource(R.string.total_budget)

    val pendingNotificationText =
        resources.getQuantityString(
            R.plurals.pending_expense,
            pendingExpensesCount,
            pendingExpensesCount,
        )

    val currentBudget = budgetSettings?.totalBudget ?: BigDecimal.ZERO
    val currentStart = budgetSettings?.startDate ?: LocalDate.now()
    val currentEnd = budgetSettings?.endDate
    val currentCurrency = budgetSettings?.currencyCode ?: "USD"
    val currentStrategy =
        budgetSettings?.remainingBudgetStrategy ?: RemainingBudgetStrategy.ASK_ALWAYS
    val currentSplitMode = budgetSettings?.splitMode ?: BudgetSplitMode.STATIC

    val previousPeriodDays =
        remember(currentStart, currentEnd) {
            if (currentEnd != null) ChronoUnit.DAYS.between(currentStart, currentEnd).toInt() + 1 else 0
        }

    val currencySymbol =
        remember(currentCurrency) {
            SupportedCurrency.findByCode(currentCurrency)?.symbol ?: "$"
        }

    val currencyFractionDigits = remember(currentCurrency) {
        SupportedCurrencyData.findByCode(currentCurrency)?.defaultFractionDigits ?: 2
    }

    var budgetText by remember(currentBudget, currencyFractionDigits) {
        mutableStateOf(
            if (currentBudget > BigDecimal.ZERO) {
                currentBudget.movePointRight(currencyFractionDigits)
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .toBigInteger()
                    .toString()
            } else {
                ""
            },
        )
    }
    var startCache by remember(currentStart) { mutableStateOf(currentStart) }
    var endCache by remember(currentEnd) { mutableStateOf(currentEnd) }
    var currencyCache by remember(currentCurrency) { mutableStateOf(currentCurrency) }
    var strategyCache by remember(currentStrategy) { mutableStateOf(currentStrategy) }
    var splitModeCache by remember(currentSplitMode) { mutableStateOf(currentSplitMode) }

    var showDateSelector by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showBehaviour by remember { mutableStateOf(false) }
    var showPreviousValues by remember { mutableStateOf(false) }

    val parsedBudget = budgetText.toBigDecimalOrNull()?.movePointLeft(currencyFractionDigits)
        ?: BigDecimal.ZERO
    val totalDays = endCache?.let { ChronoUnit.DAYS.between(startCache, it).toInt() + 1 } ?: 0
    val canApply = parsedBudget > BigDecimal.ZERO && totalDays > 0

    val validationMessage =
        when {
            parsedBudget <= BigDecimal.ZERO && budgetText.isNotEmpty() -> {
                stringResource(
                    R.string.budget_validation_major_zero,
                )
            }

            endCache == null -> {
                stringResource(R.string.budget_no_days_defined)
            }

            totalDays < 1 -> {
                stringResource(R.string.budget_less_than_one_day_validation)
            }

            else -> {
                null
            }
        }

    val view = LocalView.current

    fun applySettings() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val periodDays =
            endCache?.let { ChronoUnit.DAYS.between(startCache, it).toInt() + 1 } ?: 1
        val period =
            when {
                periodDays >= 30 -> BudgetPeriod.MONTHLY
                periodDays >= 14 -> BudgetPeriod.BIWEEKLY
                periodDays >= 7 -> BudgetPeriod.WEEKLY
                else -> BudgetPeriod.DAILY
            }
        val newSettings =
            (budgetSettings ?: BudgetSettings.DEFAULT).copy(
                totalBudget = parsedBudget,
                startDate = startCache,
                endDate = endCache,
                daysInPeriod = periodDays,
                currencyCode = currencyCache,
                remainingBudgetStrategy = strategyCache,
                splitMode = splitModeCache,
                period = period,
            )
        logcat("BudgetPeriodSheet") {
            "Apply tapped: budget=$parsedBudget, start=$startCache, end=$endCache, periodDays=$periodDays, resolvedPeriod=$period, strategy=$strategyCache, splitMode=$splitModeCache, currency=$currencyCache"
        }
        onApply(newSettings)
    }

    BackHandler(enabled = showBehaviour) { showBehaviour = false }

    val behaviourSlideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val behaviourFadeSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    AnimatedContent(
        targetState = showBehaviour,
        transitionSpec = {
            val direction = if (targetState) 1 else -1
            (
                slideInHorizontally(animationSpec = behaviourSlideSpec) { direction * it / 3 } +
                    fadeIn(behaviourFadeSpec)
            ).togetherWith(
                slideOutHorizontally(animationSpec = behaviourSlideSpec) { -direction * it / 3 } +
                    fadeOut(behaviourFadeSpec),
            )
        },
        label = "editBudgetStep",
    ) { behaviourStep ->
        if (behaviourStep) {
            BudgetBehaviourContent(
                strategy = strategyCache,
                splitMode = splitModeCache,
                onStrategySelected = { strategyCache = it },
                onSplitModeSelected = { splitModeCache = it },
                applyLabel = buttonLabel,
                onBack = { showBehaviour = false },
                onApply = { applySettings() },
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    textAlign = TextAlign.Center,
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicTextField(
                        value = budgetText,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() }
                            budgetText = filtered
                        },
                        visualTransformation = CurrencyAmountInputVisualTransformation(
                            fractionDigits = currencyFractionDigits,
                        ),
                        textStyle = MaterialTheme.typography.displaySmallCondensed.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box {
                                    if (budgetText.isEmpty()) {
                                        Text(
                                            text = currencySymbol + "",
                                            style =
                                                MaterialTheme.typography.displaySmallCondensed,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = budgetFieldLabel }
                                .testTag(BUDGET_PERIOD_BUDGET_INPUT_TAG),
                    )
                }

                if (showPreviousValuesChip && currentBudget > BigDecimal.ZERO) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        FilterChip(
                            selected = showPreviousValues,
                            onClick = { showPreviousValues = !showPreviousValues },
                            modifier = Modifier.testTag(BUDGET_PERIOD_PREVIOUS_VALUES_TAG),
                            label = {
                                Text(
                                    stringResource(R.string.previous_values),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }

                    AnimatedVisibility(
                        visible = showPreviousValues,
                        enter = fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                            expandVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                        exit = fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                            shrinkVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()),
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = colorButton,
                                ),
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.budget),
                                        style = MaterialTheme.typography.labelSmallCondensed,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = remember(currentBudget, currentCurrency) {
                                            symbolOnlyCurrencyFormat(currentCurrency)
                                                .format(currentBudget)
                                        },
                                        style = MaterialTheme.typography.titleMediumCondensed,
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = stringResource(R.string.period),
                                        style = MaterialTheme.typography.labelSmallCondensed,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = resources.getQuantityString(R.plurals.days, previousPeriodDays, previousPeriodDays),
                                        style = MaterialTheme.typography.titleMediumCondensed,
                                    )
                                }

                                Box(
                                    modifier = Modifier.weight(0.5f),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    TooltipIconButton(
                                        title = stringResource(R.string.apply),
                                        icon = Icons.Default.Check,
                                        onClick = {
                                            budgetText =
                                                if (currentBudget > BigDecimal.ZERO) {
                                                    (
                                                        currentBudget
                                                            .multiply(BigDecimal(100))
                                                            .toBigInteger()
                                                    ).toString()
                                                } else {
                                                    ""
                                                }
                                            if (previousPeriodDays > 0) {
                                                startCache = LocalDate.now()
                                                endCache =
                                                    LocalDate
                                                        .now()
                                                        .plusDays(previousPeriodDays.toLong() - 1)
                                            }
                                            currencyCache = currentCurrency
                                            strategyCache = currentStrategy
                                            showPreviousValues = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val showPreviousPeriodChip = previousPeriodDays > 0 && endCache == null

                ButtonRow(
                    modifier = Modifier.testTag(BUDGET_PERIOD_DATE_ROW_TAG),
                    position = if (showPreviousPeriodChip) {
                        PaddedListItemPosition.Single
                    } else {
                        PaddedListItemPosition.First
                    },
                    icon = Icons.Outlined.CalendarToday,
                    label = stringResource(R.string.budget_duration_label),
                    description =
                        if (endCache != null) {
                            "${startCache.format(dateFormatter)} — ${endCache?.format(dateFormatter)}"
                        } else {
                            stringResource(R.string.budget_no_date_defined)
                        },
                    onClick = { showDateSelector = true },
                ) {
                    if (endCache != null && totalDays > 0) {
                        AccentBadge(resources.getQuantityString(R.plurals.days, totalDays, totalDays))
                    }
                }

                if (showPreviousPeriodChip) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AssistChip(
                            onClick = {
                                startCache = LocalDate.now()
                                endCache = LocalDate.now().plusDays(previousPeriodDays.toLong() - 1)
                            },
                            label = {
                                Text(
                                    stringResource(
                                        R.string.use_previous_period_days,
                                        previousPeriodDays,
                                    ),
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            colors =
                                AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (showPreviousPeriodChip) 12.dp else 3.dp))

                val currencyDisplay = SupportedCurrency.findByCode(currencyCache)
                ButtonRow(
                    modifier = Modifier.testTag(BUDGET_PERIOD_CURRENCY_ROW_TAG),
                    position = if (showPreviousPeriodChip) {
                        PaddedListItemPosition.Single
                    } else {
                        PaddedListItemPosition.Last
                    },
                    icon = Icons.Outlined.MonetizationOn,
                    label = stringResource(R.string.budget_base_currency_label),
                    description = currencyDisplay?.displayName() ?: currencyCache,
                    onClick = { showCurrencyPicker = true },
                ) {
                    Text(
                        text = currencyDisplay?.symbol?.takeIf { it != currencyCache }?.let { "$currencyCache ($it)" }
                            ?: currencyCache,
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (pendingExpensesCount > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = pendingNotificationText,
                            style = MaterialTheme.typography.bodyMediumCondensed,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }

                if (validationMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = validationMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        view.weakHapticFeedback()
                        showBehaviour = true
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = ButtonDefaults.MediumContainerHeight)
                            .testTag(BUDGET_PERIOD_NEXT_BUTTON_TAG),
                    enabled = canApply,
                ) {
                    Text(stringResource(R.string.next), style = MaterialTheme.typography.labelMediumEmphasized)
                }

                Spacer(modifier = Modifier.height(24.dp))

            }
        }
    }

    AnimatedVisibility(
        visible = showDateSelector,
        enter =
            fadeIn(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                slideInHorizontally(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    initialOffsetX = { fullWidth -> fullWidth },
                ),
        exit =
            fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()) +
                slideOutHorizontally(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    targetOffsetX = { fullWidth -> fullWidth },
                ),
    ) {
        FinishDateSelector(
            totalBudget = parsedBudget,
            currencyCode = currencyCache,
            onBackPressed = { showDateSelector = false },
            onApply = { newStart, newEnd, _ ->
                startCache = newStart
                endCache = newEnd
                showDateSelector = false
            },
        )
    }

    if (showCurrencyPicker) {
        CurrencySelectorDialog(
            currentCode = currencyCache,
            onDismiss = { showCurrencyPicker = false },
            onSelect = { code ->
                currencyCache = code
                showCurrencyPicker = false
            },
        )
    }

}

@Composable
private fun TooltipIconButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
        ),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip(
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Assertive
                    paneTitle = title
                },
            ) {
                Text(title)
            }
        },
    ) {
        IconButton(
            onClick = onClick,
            shapes = IconButtonDefaults.shapes(),
            modifier = modifier,
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = tint)
        }
    }
}

@Composable
private fun AccentBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}



@Composable
private fun periodLabel(period: BudgetPeriod): String = stringResource(
    when (period) {
        BudgetPeriod.DAILY -> R.string.budget_period_daily
        BudgetPeriod.WEEKLY -> R.string.budget_period_weekly
        BudgetPeriod.BIWEEKLY -> R.string.budget_period_biweekly
        BudgetPeriod.MONTHLY -> R.string.budget_period_monthly
    }
)

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
@Composable
private fun BudgetPeriodSheetPreview() {
    MinusTheme {
        Surface {
            val pinnedStart = LocalDate.of(2099, 6, 1)
            val pinnedEnd = LocalDate.of(2099, 6, 30)
            BudgetPeriodSheet(
                budgetSettings =
                    BudgetSettings(
                        totalBudget = BigDecimal("17725"),
                        period = BudgetPeriod.DAILY,
                        startDate = pinnedStart,
                        endDate = pinnedEnd,
                        currencyCode = "MXN",
                        daysInPeriod = 30,
                        rollOverEnabled = false,
                        rollOverLimit = null,
                        rollOverCarryForward = false,
                    ),
                budgetState =
                    BudgetState(
                        remainingToday = BigDecimal("17675"),
                        totalSpentToday = BigDecimal("5072"),
                        dailyBudget = BigDecimal("5900"),
                        daysRemaining = 1,
                        progress = 0.03f,
                        isOverBudget = false,
                        totalBudget = BigDecimal("17725"),
                        totalSpentInPeriod = BigDecimal("5072"),
                    ),
                selectedPeriod = BudgetPeriod.DAILY,
                currencyCode = "MXN",
                onPeriodSelected = { },
                onSaveBudget = { },
                onFinishEarly = { },
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=1080px,height=2340px,dpi=440",
)
@Composable
private fun EditModePreview() {
    MinusTheme {
        Surface {
            EditBudgetContent(
                budgetSettings =
                    BudgetSettings(
                        totalBudget = BigDecimal("17725"),
                        period = BudgetPeriod.DAILY,
                        startDate = LocalDate.of(2099, 6, 1),
                        endDate = LocalDate.of(2099, 6, 30),
                        currencyCode = "MXN",
                        daysInPeriod = 30,
                    ),
                onBack = { },
                onApply = { },
                pendingExpensesCount = 3,
            )
        }
    }
}

@Preview(
    showSystemUi = false,
    showBackground = true,
    device = "spec:width=1080px,height=2340px,dpi=440,cutout=double"
)
@Composable
private fun EditEmptyModePreview() {
    MinusTheme {
        EditBudgetContent(
            budgetSettings = BudgetSettings(
                totalBudget = BigDecimal("0"),
                period = BudgetPeriod.DAILY,
                startDate = LocalDate.of(2099, 6, 1),
                endDate = LocalDate.of(2099, 6, 30),
                currencyCode = "MXN",
                daysInPeriod = 30,
            ),
            onBack = { },
            onApply = { },
        )
    }
}

