package com.serranoie.app.minus.presentation.ui.theme.component.budget.formula

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.navigation.predictiveDismiss
import com.serranoie.app.minus.navigation.rememberPredictiveDismiss
import com.serranoie.app.minus.presentation.ui.editor.sheets.labelRes
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate

@Stable
class BudgetFormulaHostState internal constructor(
    internal val sharedTransitionScope: SharedTransitionScope,
) {
    var request: BudgetFormulaRequest? by mutableStateOf(null)
        private set
    var sourceKey: String? by mutableStateOf(null)
        private set
    var shown: Boolean by mutableStateOf(false)
        private set

    fun show(sourceKey: String, request: BudgetFormulaRequest) {
        this.sourceKey = sourceKey
        this.request = request
        shown = true
    }

    fun dismiss() {
        shown = false
    }

    internal fun isShowing(sourceKey: String): Boolean = shown && this.sourceKey == sourceKey
}

val LocalBudgetFormulaHost = compositionLocalOf<BudgetFormulaHostState?> { null }

@Composable
fun BudgetFormulaHost(content: @Composable () -> Unit) {
    SharedTransitionLayout {
        val host = remember { BudgetFormulaHostState(this) }
        CompositionLocalProvider(LocalBudgetFormulaHost provides host) {
            Box {
                content()
                BudgetFormulaOverlay(host, Modifier.matchParentSize())
            }
        }
    }
}

@Composable
fun BudgetFormulaSource(
    key: String,
    modifier: Modifier = Modifier,
    content: @Composable (sharedModifier: Modifier, showFormula: ((BudgetFormulaRequest) -> Unit)?) -> Unit,
) {
    val host = LocalBudgetFormulaHost.current
    val hidden = host?.isShowing(key) == true
    var slotSize by remember { mutableStateOf(IntSize.Zero) }
    val slotModifier = if (hidden) {
        Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(slotSize.width, slotSize.height) { placeable.place(0, 0) }
        }
    } else {
        Modifier.onSizeChanged { slotSize = it }
    }
    Box(modifier.then(slotModifier)) {
        AnimatedVisibility(visible = !hidden, enter = fadeIn(), exit = fadeOut()) {
            val sharedModifier = host?.let {
                with(it.sharedTransitionScope) {
                    Modifier.sharedBounds(
                        rememberSharedContentState(key),
                        animatedVisibilityScope = this@AnimatedVisibility,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    )
                }
            } ?: Modifier
            val showFormula = remember(host, key) {
                host?.let { { request: BudgetFormulaRequest -> it.show(key, request) } }
            }
            content(sharedModifier, showFormula)
        }
    }
}

@Composable
private fun SharedTransitionScope.BudgetFormulaOverlay(
    host: BudgetFormulaHostState,
    modifier: Modifier
) {
    val dismiss = rememberPredictiveDismiss(enabled = host.shown) { host.dismiss() }

    AnimatedVisibility(
        visible = host.shown,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val request = host.request ?: return@AnimatedVisibility
        val sourceKey = host.sourceKey ?: return@AnimatedVisibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { host.dismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .predictiveDismiss(dismiss)
                    .padding(24.dp)
                    .widthIn(max = 400.dp)
                    .sharedBounds(
                        rememberSharedContentState(sourceKey),
                        animatedVisibilityScope = this@AnimatedVisibility,
                        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {},
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                BudgetFormulaContent(request)
            }
        }
    }
}

@Composable
fun BudgetFormulaContent(request: BudgetFormulaRequest, modifier: Modifier = Modifier) {
    val rows = remember(request) { buildBudgetFormula(request) }
    val currencyFormat =
        remember(request.currencyCode) { symbolOnlyCurrencyFormat(request.currencyCode) }
    val periodName = stringResource(request.viewPeriod.nameRes())
    val modeName = stringResource(request.splitMode.labelRes())

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.budget_formula_title),
            style = MaterialTheme.typography.titleMediumEmphasized,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.budget_formula_subtitle, modeName, periodName),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        rows.forEachIndexed { index, row ->
            val isResult = index == rows.lastIndex
            if (index == 0) {
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Text(
                text = row.captionText(periodName, request.viewPeriod),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            FormulaRowView(row, currencyFormat, highlightResult = isResult)
        }
        request.tip?.let { tip ->
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.labelSmallCondensed,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FormulaRowView(
    row: FormulaRow,
    currencyFormat: NumberFormat,
    highlightResult: Boolean
) {
    val style = MaterialTheme.typography.titleMediumCondensed.copy(fontFeatureSettings = "tnum")
    val resultTint = resultColor(row.result, highlightResult)
    val charges = row.terms.filterIsInstance<FormulaTerm.Charge>()
    if (charges.isNotEmpty()) {
        ChargeSumView(charges, row.result, currencyFormat, style, resultTint)
        return
    }
    if (row.terms.any { it is FormulaTerm.Amount && it.label != null }) {
        LedgerRowView(row, currencyFormat, style, resultTint)
        return
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        row.terms.withOps().forEach { (term, op) ->
            FormulaTermView(term, op, currencyFormat, style)
        }
        Text(text = "=", style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = currencyFormat.format(row.result),
            style = style,
            color = resultTint,
            modifier = Modifier.censor(),
        )
    }
}

@Composable
private fun ChargeSumView(
    charges: List<FormulaTerm.Charge>,
    total: BigDecimal,
    currencyFormat: NumberFormat,
    style: TextStyle,
    resultTint: Color,
) {
    val chargeTint = MaterialTheme.colorScheme.secondary
    val single = charges.size == 1
    LedgerView(
        lines = charges.mapIndexed { index, charge ->
            LedgerLine(
                label = charge.label(),
                amount = (if (index > 0) "+ " else "") + currencyFormat.format(charge.transaction.amount),
                tint = if (single) resultTint else chargeTint,
            )
        },
        total = if (single) null else LedgerLine("", currencyFormat.format(total), resultTint),
        style = style,
        ruleTint = chargeTint,
    )
}

@Composable
private fun LedgerRowView(
    row: FormulaRow,
    currencyFormat: NumberFormat,
    style: TextStyle,
    resultTint: Color,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LedgerView(
            lines = row.terms.withOps().mapIndexedNotNull { index, (term, op) ->
                val amount = term as? FormulaTerm.Amount ?: return@mapIndexedNotNull null
                LedgerLine(
                    label = amount.label?.let { stringResource(it.textRes()) }.orEmpty(),
                    amount = (if (index == 0) "" else "$op ") + currencyFormat.format(amount.value),
                    tint = termColor(amount.value, op),
                )
            },
            total = LedgerLine(
                label = stringResource(R.string.budget_formula_label_total),
                amount = "= " + currencyFormat.format(row.result),
                tint = resultTint,
            ),
            style = style,
            ruleTint = MaterialTheme.colorScheme.onSurface,
        )
        negativeReason(row)?.let { reason ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = reason,
                style = MaterialTheme.typography.labelSmallCondensed,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun negativeReason(row: FormulaRow): String? {
    if (row.result.signum() >= 0) return null
    val biggest = row.terms.withOps()
        .mapNotNull { (term, op) -> (term as? FormulaTerm.Amount)?.takeIf { op == "−" } }
        .maxByOrNull { it.value } ?: return null
    return stringResource(
        when (biggest.label) {
            FormulaLabel.RECURRING -> R.string.budget_formula_negative_recurring
            FormulaLabel.CARRIED -> R.string.budget_formula_negative_carried
            else -> R.string.budget_formula_negative_spent
        }
    )
}

private data class LedgerLine(val label: String, val amount: String, val tint: Color)

@Composable
private fun LedgerView(
    lines: List<LedgerLine>,
    total: LedgerLine?,
    style: TextStyle,
    ruleTint: Color,
) {
    Column(
        modifier = Modifier.width(IntrinsicSize.Max),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        lines.forEach { line -> LedgerLineView(line, style) }
        if (total != null) {
            HorizontalDivider(thickness = 2.dp, color = ruleTint)
            LedgerLineView(total, style)
        }
    }
}

@Composable
private fun LedgerLineView(line: LedgerLine, style: TextStyle) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = line.label,
            style = MaterialTheme.typography.labelSmallCondensed,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 180.dp),
        )
        Text(
            text = line.amount,
            style = style,
            color = line.tint,
            modifier = Modifier.censor(),
        )
    }
}

private fun FormulaLabel.textRes(): Int = when (this) {
    FormulaLabel.BUDGET -> R.string.budget_formula_label_budget
    FormulaLabel.CARRIED -> R.string.budget_formula_label_carried
    FormulaLabel.RECURRING -> R.string.budget_formula_label_recurring
    FormulaLabel.REBALANCED -> R.string.budget_formula_label_rebalanced
    FormulaLabel.SPENT -> R.string.budget_formula_label_spent
}

@Composable
private fun FormulaTerm.Charge.label(): String = transaction.comment.ifBlank {
    stringResource(
        when (transaction.recurrentFrequency) {
            RecurrentFrequency.WEEKLY -> R.string.recurrent_ticket_weekly_unnamed
            RecurrentFrequency.BIWEEKLY -> R.string.recurrent_ticket_biweekly_unnamed
            else -> R.string.recurrent_ticket_monthly_unnamed
        }
    )
}

private fun List<FormulaTerm>.withOps(): List<Pair<FormulaTerm, String?>> {
    var op: String? = null
    return map { term ->
        if (term is FormulaTerm.Op) op = term.symbol
        term to op
    }
}

@Composable
private fun opColor(op: String?, neutral: Color = LocalContentColor.current): Color = when (op) {
    "+" -> MaterialTheme.colorScheme.secondary
    "−" -> MaterialTheme.colorScheme.tertiary
    else -> neutral
}

@Composable
private fun termColor(value: BigDecimal, op: String?): Color =
    if (value.signum() < 0) MaterialTheme.colorScheme.error else opColor(op)

@Composable
private fun resultColor(value: BigDecimal, highlight: Boolean): Color = when {
    value.signum() < 0 -> MaterialTheme.colorScheme.error
    highlight -> MaterialTheme.colorScheme.primary
    else -> LocalContentColor.current
}

@Composable
private fun FormulaTermView(
    term: FormulaTerm,
    op: String?,
    currencyFormat: NumberFormat,
    style: TextStyle,
) {
    when (term) {
        is FormulaTerm.Amount -> Text(
            text = currencyFormat.format(term.value),
            style = style,
            color = termColor(term.value, op),
            modifier = Modifier.censor(),
        )

        is FormulaTerm.Op -> Text(
            text = term.symbol,
            style = style,
            color = opColor(term.symbol, MaterialTheme.colorScheme.onSurfaceVariant),
        )

        is FormulaTerm.Count -> Text(
            text = term.label(),
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        is FormulaTerm.Charge -> Text(
            text = currencyFormat.format(term.transaction.amount),
            style = style,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.censor(),
        )

        is FormulaTerm.Fraction -> Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = currencyFormat.format(term.numerator),
                style = style,
                color = termColor(term.numerator, op),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .censor(),
            )
            HorizontalDivider(thickness = 2.dp, color = LocalContentColor.current)
            Text(
                text = term.denominator.label(),
                style = style,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FormulaTerm.Count.label(): String = pluralStringResource(
    when (unit) {
        BudgetPeriod.DAILY -> R.plurals.days
        BudgetPeriod.WEEKLY -> R.plurals.budget_formula_weeks
        BudgetPeriod.BIWEEKLY -> R.plurals.budget_formula_biweeks
        BudgetPeriod.MONTHLY -> R.plurals.budget_formula_months
    },
    n,
    n,
)

@Composable
private fun FormulaRow.captionText(periodName: String, period: BudgetPeriod): String =
    when (caption) {
        FormulaCaption.SURPLUS_SPLIT -> stringResource(R.string.budget_formula_caption_surplus_split)
        FormulaCaption.SURPLUS_FIRST_DAY -> stringResource(R.string.budget_formula_caption_surplus_first_day)
        FormulaCaption.ADJUSTMENTS -> stringResource(R.string.budget_formula_caption_adjustments)
        FormulaCaption.PER_DAY -> stringResource(R.string.budget_formula_caption_per_day)
        FormulaCaption.PER_PERIOD -> stringResource(
            R.string.budget_formula_caption_per_period,
            periodName
        )

        FormulaCaption.REMAINING_BUDGET -> stringResource(R.string.budget_formula_caption_remaining_budget)
        FormulaCaption.SPREAD_OVER_LEFT -> stringResource(R.string.budget_formula_caption_spread_over_left)
        FormulaCaption.LEFT -> stringResource(R.string.budget_formula_caption_left)
        FormulaCaption.RESERVED -> stringResource(R.string.budget_formula_caption_reserved)
        FormulaCaption.CARRIED -> stringResource(R.string.budget_formula_caption_carried)
        FormulaCaption.SPREAD_LEFTOVER -> stringResource(R.string.budget_formula_caption_spread_leftover)
        FormulaCaption.NEXT_BLOCK -> stringResource(
            when (period) {
                BudgetPeriod.DAILY -> R.string.budget_pill_next_daily
                BudgetPeriod.WEEKLY -> R.string.budget_pill_next_weekly
                BudgetPeriod.BIWEEKLY -> R.string.budget_pill_next_biweekly
                BudgetPeriod.MONTHLY -> R.string.budget_pill_next_monthly
            },
            "",
        ).trim()
    }

private fun BudgetPeriod.nameRes(): Int = when (this) {
    BudgetPeriod.DAILY -> R.string.budget_period_daily
    BudgetPeriod.WEEKLY -> R.string.budget_period_weekly
    BudgetPeriod.BIWEEKLY -> R.string.budget_period_biweekly
    BudgetPeriod.MONTHLY -> R.string.budget_period_monthly
}

@Preview(showBackground = true)
@Composable
private fun BudgetFormulaContentPreview() {
    MinusTheme {
        BudgetFormulaContent(
            request = BudgetFormulaRequest(
                budgetState = BudgetState(
                    remainingToday = BigDecimal.ZERO,
                    totalSpentToday = BigDecimal("20.00"),
                    dailyBudget = BigDecimal("33.33"),
                    daysRemaining = 20,
                    progress = 0f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("1050.00"),
                    totalSpentInPeriod = BigDecimal("400.00"),
                    totalSpentThisWeek = BigDecimal("150.00"),
                    periodTotalDays = 30,
                    reservedCharges = listOf(
                        Transaction(
                            amount = BigDecimal("15.00"),
                            comment = "Netflix",
                            date = LocalDate.of(2026, 9, 25).atStartOfDay(),
                        ),
                        Transaction(
                            amount = BigDecimal("30.00"),
                            comment = "Gym",
                            date = LocalDate.of(2026, 9, 28).atStartOfDay(),
                        ),
                        Transaction(
                            amount = BigDecimal("9.99"),
                            comment = "Spotify",
                            date = LocalDate.of(2026, 9, 29).atStartOfDay(),
                        ),
                    ),
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("1000.00"),
                    period = BudgetPeriod.MONTHLY,
                    startDate = LocalDate.of(2026, 9, 1),
                    rollOverLimit = BigDecimal("100.00"),
                    rollOverCarryForward = false,
                ),
                viewPeriod = BudgetPeriod.WEEKLY,
                splitMode = BudgetSplitMode.DYNAMIC,
                currencyCode = "USD",
            )
        )
    }
}
