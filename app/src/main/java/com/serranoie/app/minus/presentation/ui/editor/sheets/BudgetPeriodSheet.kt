@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.editor.sheets

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repartition
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.ui.onboarding.FinishDateSelector
import com.serranoie.app.minus.presentation.ui.onboarding.availablePeriodsFor
import com.serranoie.app.minus.presentation.ui.onboarding.budgetForPeriod
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.colorButton
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetDisplay
import com.serranoie.app.minus.presentation.ui.theme.component.budget.SpendBudgetCard
import com.serranoie.app.minus.presentation.ui.theme.component.date.DaysLeftCard
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.CurrencyAmountInputVisualTransformation
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
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
const val BUDGET_PERIOD_STRATEGY_ROW_TAG = "BudgetPeriodSheet.StrategyRow"
const val BUDGET_PERIOD_CURRENCY_ROW_TAG = "BudgetPeriodSheet.CurrencyRow"
fun budgetPeriodCardTag(period: BudgetPeriod) = "BudgetPeriodSheet.Period.${period.name}"

@Composable
fun BudgetPeriodSheet(
    budgetSettings: BudgetSettings?,
    budgetState: BudgetState?,
    selectedPeriod: BudgetPeriod = budgetSettings?.period ?: BudgetPeriod.DAILY,
    currencyCode: String,
    onPeriodSelected: (BudgetPeriod) -> Unit,
    onSaveBudget: ((BudgetSettings) -> Unit)? = null,
    onEditBudget: (() -> Unit)? = null,
    onFinishEarly: (() -> Unit)? = null,
    startInEditMode: Boolean = false,
    pendingExpensesCount: Int = 0,
) {
    val haptic = LocalHapticFeedback.current
    val currencyFormat = remember(currencyCode) {
        symbolOnlyCurrencyFormat(currencyCode, maximumFractionDigits = 0)
    }

    val startDate = budgetSettings?.startDate ?: LocalDate.now()
    val endDate = budgetSettings?.endDate
    val totalBudget = budgetSettings?.totalBudget ?: BigDecimal.ZERO
    var periodCache by remember(selectedPeriod) { mutableStateOf(selectedPeriod) }

    var isEditMode by remember(startInEditMode) { mutableStateOf(startInEditMode) }
    var showFinishConfirm by remember { mutableStateOf(false) }

    val totalDays = remember(startDate, endDate) {
        if (endDate != null) ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1 else 30
    }

    fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())

    val startDateAsDate = remember(startDate) { startDate.toDate() }
    val endDateAsDate = remember(endDate) { endDate?.toDate() }
    val totalSpent = budgetState?.totalSpentInPeriod ?: BigDecimal.ZERO

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
            onPeriodSelected(periodCache)
        }
    }

    AnimatedContent(
        modifier = Modifier.testTag(BUDGET_PERIOD_SHEET_TAG),
        targetState = isEditMode,
        transitionSpec = {
            if (targetState) {
                (
                    slideInHorizontally(
                        initialOffsetX = { it / 3 }, animationSpec = tween(300)
                    ) + fadeIn(tween(250, delayMillis = 50))
                    ).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 }, animationSpec = tween(300)
                    ) + fadeOut(tween(200))
                )
            } else {
                (
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 }, animationSpec = tween(300)
                    ) + fadeIn(tween(250, delayMillis = 50))
                    ).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { it / 3 }, animationSpec = tween(300)
                    ) + fadeOut(tween(200))
                )
            }
        },
        label = "sheetContent"
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
                periodCache = periodCache,
                currencyFormat = currencyFormat,
                currencyCode = currencyCode,
                totalBudget = totalBudget,
                totalSpent = totalSpent,
                totalDays = totalDays,
                startDateAsDate = startDateAsDate,
                endDateAsDate = endDateAsDate,
                available = available,
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
                    style = MaterialTheme.typography.titleMediumEmphasized
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
                ) {
                    Text(
                        stringResource(R.string.finalize_action),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMediumEmphasized
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirm = false }) {
                    Text(
                        stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelMediumEmphasized
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
    startDateAsDate: Date,
    endDateAsDate: Date?,
    available: List<BudgetPeriod>,
    onPeriodSelected: (BudgetPeriod) -> Unit,
    onEditClick: () -> Unit,
    onFinishEarlyClick: () -> Unit,
    showFinishEarly: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp, bottom = 32.dp)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.total_budget),
                style = MaterialTheme.typography.titleLargeEmphasized,
                fontWeight = FontWeight.W500,
            )
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag(BUDGET_PERIOD_EDIT_BUTTON_TAG)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_budget),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        logcat(BUDGET_PERIOD_SHEET_TAG) {
            "BudgetDisplay input totalBudget=$totalBudget budgetStateTotal=${budgetState?.totalBudget} budgetSettingsTotal=${budgetSettings?.totalBudget} rollOverLimit=${budgetSettings?.rollOverLimit} rollOverCarry=${budgetSettings?.rollOverCarryForward}"
        }

        SpendBudgetCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(bottom = 8.dp),
            budget = totalBudget,
            spend = totalSpent,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1.65f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                BudgetDisplay(
                    budget = totalBudget,
                    budgetState = budgetState,
                    budgetSettings = budgetSettings,
                    currencyCode = currencyCode,
                    bigVariant = false,
                    modifier = Modifier.fillMaxWidth(),
                    startDate = startDateAsDate,
                    finishDate = endDateAsDate
                )
            }

            if (endDateAsDate != null) {
                Box(
                    modifier = Modifier
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
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_ending_date),
                            style = MaterialTheme.typography.bodySmallEmphasized,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.budget_split_logic),
            style = MaterialTheme.typography.titleMediumCondensed,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (totalDays > 0) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                available.chunked(2).forEach { rowPeriods ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowPeriods.forEach { p ->
                            val isSelected = p == periodCache
                            val preview = if (totalBudget > BigDecimal.ZERO && totalDays > 0) {
                                budgetForPeriod(totalBudget, totalDays, p)
                            } else {
                                BigDecimal.ZERO
                            }

                            CompactPeriodCard(
                                period = p,
                                budgetAmount = preview,
                                currencyFormat = currencyFormat,
                                isSelected = isSelected,
                                onClick = { onPeriodSelected(p) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag(budgetPeriodCardTag(p))
                            )
                        }
                        if (rowPeriods.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        if (showFinishEarly) {
            OutlinedButton(
                onClick = onFinishEarlyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(BUDGET_PERIOD_FINISH_EARLY_BUTTON_TAG),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25F))
            ) {
                Text(
                    text = stringResource(R.string.finalize_period),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    textAlign = TextAlign.Center,
                    lineHeight = TextUnit(0.925f, TextUnitType.Em),
                )
            }
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
    val resources = LocalContext.current.resources
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMMM", Locale("es", "ES"))
    }

    val pendingNotificationText = resources.getQuantityString(
        R.plurals.pending_expense,
        pendingExpensesCount,
        pendingExpensesCount
    )

    val currentBudget = budgetSettings?.totalBudget ?: BigDecimal.ZERO
    val currentStart = budgetSettings?.startDate ?: LocalDate.now()
    val currentEnd = budgetSettings?.endDate
    val currentCurrency = budgetSettings?.currencyCode ?: "USD"
    val currentStrategy =
        budgetSettings?.remainingBudgetStrategy ?: RemainingBudgetStrategy.ASK_ALWAYS

    val previousPeriodDays = remember(currentStart, currentEnd) {
        if (currentEnd != null) ChronoUnit.DAYS.between(currentStart, currentEnd).toInt() + 1 else 0
    }

    val currencySymbol = remember(currentCurrency) {
        SupportedCurrency.findByCode(currentCurrency)?.symbol ?: "$"
    }

    var budgetText by remember(currentBudget) {
        mutableStateOf(
            if (currentBudget > BigDecimal.ZERO) {
                (currentBudget.multiply(BigDecimal(100)).toBigInteger()).toString()
            } else {
                ""
            }
        )
    }
    var startCache by remember(currentStart) { mutableStateOf(currentStart) }
    var endCache by remember(currentEnd) { mutableStateOf(currentEnd) }
    var currencyCache by remember(currentCurrency) { mutableStateOf(currentCurrency) }
    var strategyCache by remember(currentStrategy) { mutableStateOf(currentStrategy) }

    var showDateSelector by remember { mutableStateOf(false) }
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var showStrategyPicker by remember { mutableStateOf(false) }
    var showPreviousValues by remember { mutableStateOf(false) }

    val parsedBudget = budgetText.toBigDecimalOrNull()?.divide(BigDecimal(100)) ?: BigDecimal.ZERO
    val totalDays = endCache?.let { ChronoUnit.DAYS.between(startCache, it).toInt() + 1 } ?: 0
    val canApply = parsedBudget > BigDecimal.ZERO && totalDays > 0

    val validationMessage = when {
        parsedBudget <= BigDecimal.ZERO && budgetText.isNotEmpty() -> stringResource(
            R.string.budget_validation_major_zero
        )

        endCache == null -> stringResource(R.string.budget_no_days_defined)
        totalDays < 1 -> stringResource(R.string.budget_less_than_one_day_validation)
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .navigationBarsPadding(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLargeEmphasized,
            fontWeight = FontWeight.W500,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier
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
                visualTransformation = CurrencyAmountInputVisualTransformation(),
                textStyle = MaterialTheme.typography.titleMediumCondensed.copy(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
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
                                    style = MaterialTheme.typography.titleMediumCondensed.copy(
                                        fontSize = 48.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                )
                            }
                            innerTextField()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(BUDGET_PERIOD_BUDGET_INPUT_TAG),
            )
        }

        if (showPreviousValuesChip && currentBudget > BigDecimal.ZERO) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                AssistChip(
                    onClick = { showPreviousValues = !showPreviousValues },
                    modifier = Modifier.testTag(BUDGET_PERIOD_PREVIOUS_VALUES_TAG),
                    label = {
                        Text(
                            stringResource(R.string.previous_values),
                            style = MaterialTheme.typography.labelMediumCondensed
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                )
            }

            if (showPreviousValues) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = colorButton,
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Presupuesto",
                                style = MaterialTheme.typography.labelSmallCondensed,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$currencySymbol${currentBudget.toPlainString()}",
                                style = MaterialTheme.typography.titleMediumCondensed,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.period),
                                style = MaterialTheme.typography.labelSmallCondensed,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$previousPeriodDays días",
                                style = MaterialTheme.typography.titleMediumCondensed,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier.weight(0.5f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            IconButton(
                                onClick = {
                                    budgetText = if (currentBudget > BigDecimal.ZERO) {
                                        (
                                            currentBudget.multiply(BigDecimal(100))
                                                .toBigInteger()
                                            ).toString()
                                    } else {
                                        ""
                                    }
                                    if (previousPeriodDays > 0) {
                                        startCache = LocalDate.now()
                                        endCache = LocalDate.now()
                                            .plusDays(previousPeriodDays.toLong() - 1)
                                    }
                                    currencyCache = currentCurrency
                                    strategyCache = currentStrategy
                                    showPreviousValues = false
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Aplicar",
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsRow(
            modifier = Modifier.testTag(BUDGET_PERIOD_DATE_ROW_TAG),
            icon = Icons.Outlined.DateRange,
            label = if (endCache != null) {
                "${startCache.format(dateFormatter)} — ${endCache?.format(dateFormatter)}"
            } else {
                stringResource(R.string.budget_no_date_defined)
            },
            onClick = { showDateSelector = true },
        )

        if (previousPeriodDays > 0 && endCache == null) {
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
                                previousPeriodDays
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingsRow(
            modifier = Modifier.testTag(BUDGET_PERIOD_STRATEGY_ROW_TAG),
            icon = Icons.Default.Repartition,
            label = stringResource(R.string.remaining_budget_label),
            trailingText = when (strategyCache) {
                RemainingBudgetStrategy.ASK_ALWAYS -> stringResource(R.string.strategy_ask_always)
                RemainingBudgetStrategy.SPLIT_EQUALLY -> stringResource(R.string.strategy_split_equally)
                RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> stringResource(R.string.strategy_add_to_first_day)
            },
            onClick = { showStrategyPicker = true },
        )

        Spacer(modifier = Modifier.height(8.dp))

        val currencyDisplay = SupportedCurrency.findByCode(currencyCache)
        SettingsRow(
            modifier = Modifier.testTag(BUDGET_PERIOD_CURRENCY_ROW_TAG),
            icon = Icons.Default.Edit,
            label = stringResource(R.string.currency_label),
            trailingText = if (currencyDisplay != null) {
                "${currencyDisplay.symbol} ${currencyDisplay.code}"
            } else {
                currencyCache
            },
            onClick = { showCurrencyPicker = true },
        )

        if (pendingExpensesCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
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
                    fontWeight = FontWeight.Medium,
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
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = validationMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val periodDays =
                    endCache?.let { ChronoUnit.DAYS.between(startCache, it).toInt() + 1 } ?: 1
                val period = when {
                    periodDays >= 30 -> BudgetPeriod.MONTHLY
                    periodDays >= 14 -> BudgetPeriod.BIWEEKLY
                    periodDays >= 7 -> BudgetPeriod.WEEKLY
                    else -> BudgetPeriod.DAILY
                }
                val newSettings = (budgetSettings ?: BudgetSettings.DEFAULT).copy(
                    totalBudget = parsedBudget,
                    startDate = startCache,
                    endDate = endCache,
                    daysInPeriod = periodDays,
                    currencyCode = currencyCache,
                    remainingBudgetStrategy = strategyCache,
                    period = period,
                )
                logcat {
                    "Apply tapped: budget=$parsedBudget, start=$startCache, end=$endCache, periodDays=$periodDays, resolvedPeriod=$period, strategy=$strategyCache, currency=$currencyCache"
                }
                onApply(newSettings)
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .testTag(BUDGET_PERIOD_APPLY_BUTTON_TAG),
            enabled = canApply,
        ) {
            Text(buttonLabel, style = MaterialTheme.typography.labelMediumEmphasized)
        }
    }

    AnimatedVisibility(
        visible = showDateSelector,
        enter = fadeIn(animationSpec = tween(220)) + slideInHorizontally(
            animationSpec = tween(300),
            initialOffsetX = { fullWidth -> fullWidth },
        ),
        exit = fadeOut(animationSpec = tween(160)) + slideOutHorizontally(
            animationSpec = tween(240),
            targetOffsetX = { fullWidth -> fullWidth },
        ),
    ) {
        FinishDateSelector(
            totalBudget = parsedBudget,
            currencyCode = currencyCache,
            onBackPressed = { showDateSelector = false },
            onApply = { newStart, newEnd, selectedPeriod ->
                startCache = newStart
                endCache = newEnd
                showDateSelector = false
            },
        )
    }

    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            currentCode = currencyCache,
            onDismiss = { showCurrencyPicker = false },
            onSelect = { code ->
                currencyCache = code
                showCurrencyPicker = false
            }
        )
    }

    if (showStrategyPicker) {
        StrategyPickerDialog(
            currentStrategy = strategyCache,
            onDismiss = { showStrategyPicker = false },
            onSelect = { strategy ->
                strategyCache = strategy
                showStrategyPicker = false
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    trailingText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CurrencyPickerDialog(
    currentCode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.select_currency),
                style = MaterialTheme.typography.titleLargeEmphasized
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                SupportedCurrency.ALL.forEach { currency ->
                    val isSelected = currency.code == currentCode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(currency.code) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = currency.symbol,
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currency.code,
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            Text(
                                text = currency.name,
                                style = MaterialTheme.typography.bodySmallCondensed,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.currency_selected),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.close),
                    style = MaterialTheme.typography.labelMediumEmphasized
                )
            }
        },
    )
}

@Composable
private fun StrategyPickerDialog(
    currentStrategy: RemainingBudgetStrategy,
    onDismiss: () -> Unit,
    onSelect: (RemainingBudgetStrategy) -> Unit,
) {
    val strategies = listOf(
        RemainingBudgetStrategy.ASK_ALWAYS,
        RemainingBudgetStrategy.SPLIT_EQUALLY,
        RemainingBudgetStrategy.ADD_TO_FIRST_DAY,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.strategy_dialog_title),
                style = MaterialTheme.typography.titleLargeEmphasized
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.strategy_dialog_description),
                    style = MaterialTheme.typography.bodySmallCondensed,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                strategies.forEach { strategy ->
                    val isSelected = strategy == currentStrategy
                    val title = when (strategy) {
                        RemainingBudgetStrategy.ASK_ALWAYS -> stringResource(R.string.strategy_ask_always)
                        RemainingBudgetStrategy.SPLIT_EQUALLY -> stringResource(R.string.strategy_split_equally)
                        RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> stringResource(R.string.strategy_add_to_first_day)
                    }
                    val description = when (strategy) {
                        RemainingBudgetStrategy.ASK_ALWAYS -> stringResource(R.string.strategy_ask_always_desc)
                        RemainingBudgetStrategy.SPLIT_EQUALLY -> stringResource(R.string.strategy_split_equally_desc)
                        RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> stringResource(
                            R.string.strategy_add_to_first_day_desc
                        )
                    }
                    OutlinedCard(
                        onClick = { onSelect(strategy) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            },
                        ),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = 0.3f
                                )
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMediumEmphasized,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.close),
                    style = MaterialTheme.typography.labelMediumEmphasized
                )
            }
        },
    )
}

@Composable
private fun CompactPeriodCard(
    period: BudgetPeriod,
    budgetAmount: BigDecimal,
    currencyFormat: NumberFormat,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    OutlinedCard(
        modifier = modifier.clickable(onClick = onClick),
        onClick = onClick,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        ),
        colors = CardDefaults.outlinedCardColors(containerColor = backgroundColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (period) {
                    BudgetPeriod.DAILY -> stringResource(R.string.budget_period_daily)
                    BudgetPeriod.WEEKLY -> stringResource(R.string.budget_period_weekly)
                    BudgetPeriod.BIWEEKLY -> stringResource(R.string.budget_period_biweekly)
                    BudgetPeriod.MONTHLY -> stringResource(R.string.budget_period_monthly)
                },
                style = if (isSelected) {
                    MaterialTheme.typography.bodySmallEmphasized
                } else {
                    MaterialTheme.typography.bodySmallCondensed
                },
                fontWeight = FontWeight.Medium,
                color = textColor
            )

            Text(
                text = currencyFormat.format(budgetAmount),
                style = MaterialTheme.typography.bodySmallCondensed,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f),
                modifier = Modifier.censor()
            )
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun BudgetPeriodSheetPreview() {
    MinusTheme {
        Surface {
            BudgetPeriodSheet(
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("17725"),
                    period = BudgetPeriod.DAILY,
                    startDate = LocalDate.now().minusDays(29),
                    endDate = LocalDate.now(),
                    currencyCode = "MXN",
                    daysInPeriod = 30,
                    rollOverEnabled = false,
                    rollOverLimit = null,
                    rollOverCarryForward = false
                ),
                budgetState = BudgetState(
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
                onFinishEarly = { }
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=1080px,height=2340px,dpi=440"
)
@Composable
private fun EditModePreview() {
    MinusTheme {
        Surface {
            EditBudgetContent(
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("17725"),
                    period = BudgetPeriod.DAILY,
                    startDate = LocalDate.now().minusDays(29),
                    endDate = LocalDate.now(),
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
                startDate = LocalDate.now().minusDays(29),
                endDate = LocalDate.now(),
                currencyCode = "MXN",
                daysInPeriod = 30,
            ),
            onBack = { },
            onApply = { },
        )
    }
}
