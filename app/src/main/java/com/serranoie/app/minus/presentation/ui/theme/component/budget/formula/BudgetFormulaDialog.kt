package com.serranoie.app.minus.presentation.ui.theme.component.budget.formula

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.text.NumberFormat

@OptIn(ExperimentalSharedTransitionApi::class)
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

@OptIn(ExperimentalSharedTransitionApi::class)
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

@OptIn(ExperimentalSharedTransitionApi::class)
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.BudgetFormulaOverlay(host: BudgetFormulaHostState, modifier: Modifier) {
    BackHandler(enabled = host.shown) { host.dismiss() }
    AnimatedVisibility(visible = host.shown, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BudgetFormulaContent(request: BudgetFormulaRequest, modifier: Modifier = Modifier) {
    val rows = remember(request) { buildBudgetFormula(request) }
    val currencyFormat = remember(request.currencyCode) { symbolOnlyCurrencyFormat(request.currencyCode) }
    val periodName = stringResource(request.viewPeriod.nameRes())
    val modeName = stringResource(
        when (request.splitMode) {
            BudgetSplitMode.STATIC -> R.string.budget_formula_mode_static
            BudgetSplitMode.DYNAMIC -> R.string.budget_formula_mode_dynamic
        }
    )

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
            Spacer(modifier = Modifier.height(if (index == 0) 16.dp else 12.dp))
            Text(
                text = row.captionText(periodName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            FormulaRowView(row, currencyFormat, highlightResult = index == rows.lastIndex)
        }
        request.tip?.let { tip ->
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = tip,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FormulaRowView(row: FormulaRow, currencyFormat: NumberFormat, highlightResult: Boolean) {
    val style = MaterialTheme.typography.titleMediumCondensed.copy(
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        row.terms.forEach { term -> FormulaTermView(term, currencyFormat, style) }
        Text(text = "=", style = style)
        Text(
            text = currencyFormat.format(row.result),
            style = style,
            color = if (highlightResult) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            modifier = Modifier.censor(),
        )
    }
}

@Composable
private fun FormulaTermView(term: FormulaTerm, currencyFormat: NumberFormat, style: TextStyle) {
    when (term) {
        is FormulaTerm.Amount -> Text(
            text = currencyFormat.format(term.value),
            style = style,
            modifier = Modifier.censor(),
        )

        is FormulaTerm.Op -> Text(text = term.symbol, style = style)

        is FormulaTerm.Count -> Text(text = term.label(), style = style)

        is FormulaTerm.Fraction -> Column(
            modifier = Modifier.width(IntrinsicSize.Max),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = currencyFormat.format(term.numerator),
                style = style,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .censor(),
            )
            HorizontalDivider(thickness = 2.dp, color = LocalContentColor.current)
            Text(
                text = term.denominator.label(),
                style = style,
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
private fun FormulaRow.captionText(periodName: String): String = when (caption) {
    FormulaCaption.SURPLUS_SPLIT -> stringResource(R.string.budget_formula_caption_surplus_split)
    FormulaCaption.SURPLUS_FIRST_DAY -> stringResource(R.string.budget_formula_caption_surplus_first_day)
    FormulaCaption.ADJUSTMENTS -> stringResource(R.string.budget_formula_caption_adjustments)
    FormulaCaption.PER_DAY -> stringResource(R.string.budget_formula_caption_per_day)
    FormulaCaption.PER_PERIOD -> stringResource(R.string.budget_formula_caption_per_period, periodName)
    FormulaCaption.REMAINING_BUDGET -> stringResource(R.string.budget_formula_caption_remaining_budget)
    FormulaCaption.SPREAD_OVER_LEFT -> stringResource(R.string.budget_formula_caption_spread_over_left)
    FormulaCaption.LEFT -> stringResource(R.string.budget_formula_caption_left)
}

private fun BudgetPeriod.nameRes(): Int = when (this) {
    BudgetPeriod.DAILY -> R.string.budget_period_daily
    BudgetPeriod.WEEKLY -> R.string.budget_period_weekly
    BudgetPeriod.BIWEEKLY -> R.string.budget_period_biweekly
    BudgetPeriod.MONTHLY -> R.string.budget_period_monthly
}
