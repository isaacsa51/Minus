package com.serranoie.app.minus.presentation.island

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sameerasw.island.sdk.HostInfo
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.formatCurrencySymbolOnly
import com.serranoie.app.minus.presentation.util.harmonizeWithColor
import com.serranoie.app.minus.presentation.util.withTone
import com.serranoie.app.minus.presentation.widget.exceededLabelRes
import com.serranoie.app.minus.presentation.widget.periodLabelRes
import java.math.BigDecimal
import java.time.LocalDate

private const val STATUS_CHROMA = 2f
private const val STATUS_FILL_TONE = 70.0
private const val STATUS_TRACK_TONE = 24.0
private val GroupOuterCorner = 28.dp
private val GroupInnerCorner = 6.dp
private val GroupGap = 2.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IslandBudgetPage(
    snapshot: IslandBudgetSnapshot,
    info: HostInfo?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    fun px(value: Int?) = with(density) { (value ?: 0).toDp() }
    val sidePadding = px((info?.outsetPx ?: 0) + (info?.expandedPaddingPx ?: 0))

    MinusTheme(darkTheme = true) {
        val progress = if (snapshot.isAlert) 1f else snapshot.spendProgress.coerceIn(0f, 1f)
        val good = MinusTheme.budgetStatus.good
        val notGood = MinusTheme.budgetStatus.notGood
        val bad = MinusTheme.budgetStatus.bad
        val primary = MaterialTheme.colorScheme.primary
        val seed = remember(progress, good, notGood, bad, primary) {
            harmonizeWithColor(combineColors(listOf(good, notGood, bad), progress), primary, STATUS_CHROMA)
        }
        val statusColor = remember(seed) { seed.withTone(STATUS_FILL_TONE) }
        val trackColor = remember(seed) { seed.withTone(STATUS_TRACK_TONE) }
        val quiet = MaterialTheme.colorScheme.onSurfaceVariant

        val inspection = LocalInspectionMode.current
        val animatedProgress = remember { Animatable(if (inspection) progress else 0f) }
        LaunchedEffect(progress) {
            animatedProgress.animateTo(progress, ProgressIndicatorDefaults.ProgressAnimationSpec)
        }
        val extra = islandExtraLine(snapshot)

        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .then(modifier)
                .clip(RoundedCornerShape(px(info?.cornerPx)))
                .background(Color.Black)
                .clickable(
                    onClickLabel = stringResource(R.string.quick_add_open_app),
                    role = Role.Button,
                    onClick = onOpen,
                )
                .semantics(mergeDescendants = true) {},
        ) {
            Column(
                Modifier.padding(start = sidePadding, end = sidePadding, bottom = sidePadding),
                verticalArrangement = Arrangement.spacedBy(GroupGap),
            ) {
                Spacer(Modifier.height(px(info?.cameraClearancePx) - GroupGap))
                val cardColors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
                Card(shape = groupShape(top = true, bottom = false), colors = cardColors) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text(
                            text = stringResource(islandSplitLabel(snapshot)),
                            style = MaterialTheme.typography.labelLarge,
                            color = quiet,
                        )
                        Text(
                            text = islandMoney(islandHeroAmount(snapshot), snapshot.currency),
                            style = MaterialTheme.typography.displaySmallEmphasized,
                            color = statusColor,
                        )
                        Spacer(Modifier.height(12.dp))
                        LinearWavyProgressIndicator(
                            progress = { animatedProgress.value },
                            modifier = Modifier.fillMaxWidth(),
                            color = statusColor,
                            trackColor = trackColor,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.island_page_spent_of,
                                islandMoney(snapshot.splitSpent, snapshot.currency),
                                islandMoney(snapshot.splitBudget, snapshot.currency),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = quiet,
                        )
                    }
                }
                Card(shape = groupShape(top = false, bottom = extra == null), colors = cardColors) {
                    Row(Modifier.fillMaxWidth().padding(16.dp)) {
                        IslandStat(
                            value = islandMoney(snapshot.periodLeft, snapshot.currency),
                            label = stringResource(R.string.island_page_period_left),
                            modifier = Modifier.weight(1f),
                        )
                        IslandStat(
                            value = snapshot.daysLeft.toString(),
                            label = pluralStringResource(R.plurals.island_page_days_left, snapshot.daysLeft),
                            horizontalAlignment = Alignment.End,
                        )
                    }
                }
                extra?.let { (icon, line) ->
                    Card(
                        shape = groupShape(top = false, bottom = true),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(text = line, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun groupShape(top: Boolean, bottom: Boolean): Shape {
    val topCorner = if (top) GroupOuterCorner else GroupInnerCorner
    val bottomCorner = if (bottom) GroupOuterCorner else GroupInnerCorner
    return RoundedCornerShape(topCorner, topCorner, bottomCorner, bottomCorner)
}

@Composable
private fun IslandStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(text = value, style = MaterialTheme.typography.titleLargeEmphasized)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun islandExtraLine(snapshot: IslandBudgetSnapshot): Pair<ImageVector, String>? {
    val charge = snapshot.nextCharge
    if (charge != null) {
        val name = charge.name.ifBlank { stringResource(R.string.island_charge_unnamed) }
        val amount = islandMoney(charge.amount, snapshot.currency)
        val res = if (charge.date == snapshot.date) R.string.island_charge_today else R.string.island_charge_tomorrow
        return Icons.Rounded.EventRepeat to stringResource(res, name, amount)
    }
    if (snapshot.creditOwed.signum() == 1) {
        return Icons.Rounded.CreditCard to
            stringResource(R.string.island_credit_owed, islandMoney(snapshot.creditOwed, snapshot.currency))
    }
    return null
}

internal fun islandHeroAmount(s: IslandBudgetSnapshot): BigDecimal = when {
    s.isOverBudget -> s.periodLeft
    else -> s.overspend()?.negate() ?: s.splitRemaining
}

internal fun islandSplitLabel(s: IslandBudgetSnapshot): Int = when {
    s.isOverBudget -> R.string.budget_pill_over_budget
    s.isOverSplit -> s.viewPeriod.exceededLabelRes()
    else -> s.viewPeriod.periodLabelRes()
}

internal fun islandMoney(value: BigDecimal, currency: String): String =
    if (value < BigDecimal.ZERO) {
        "−" + formatCurrencySymbolOnly(value.negate(), currency)
    } else {
        formatCurrencySymbolOnly(value, currency)
    }

internal val PreviewIslandSnapshot = IslandBudgetSnapshot(
    date = LocalDate.of(2099, 6, 18),
    currency = "EUR",
    viewPeriod = BudgetPeriod.DAILY,
    splitRemaining = BigDecimal("24.50"),
    splitSpent = BigDecimal("15.50"),
    splitBudget = BigDecimal("40"),
    spendProgress = 0.39f,
    isOverBudget = false,
    isOverSplit = false,
    periodLeft = BigDecimal("612.40"),
    daysLeft = 12,
    creditOwed = BigDecimal.ZERO,
    nextCharge = IslandCharge("Netflix", BigDecimal("12.99"), LocalDate.of(2099, 6, 19)),
    startDate = LocalDate.of(2099, 6, 1),
    finishDate = LocalDate.of(2099, 6, 30),
)

internal val PreviewIslandOverSplitSnapshot = PreviewIslandSnapshot.copy(
    splitRemaining = BigDecimal.ZERO,
    splitSpent = BigDecimal("48.20"),
    spendProgress = 1f,
    isOverSplit = true,
    nextCharge = null,
    creditOwed = BigDecimal("120"),
)

internal val PreviewIslandHostInfo = HostInfo(
    hostPackage = "com.sameerasw.essentials",
    expandedPaddingPx = 32,
    cameraClearancePx = 96,
    outsetPx = 8,
    cornerPx = 120,
)

@Preview(widthDp = 380)
@Composable
private fun IslandBudgetPagePreview() {
    IslandBudgetPage(snapshot = PreviewIslandSnapshot, info = PreviewIslandHostInfo, onOpen = {})
}

@Preview(widthDp = 380)
@Composable
private fun IslandBudgetPageOverSplitPreview() {
    IslandBudgetPage(snapshot = PreviewIslandOverSplitSnapshot, info = PreviewIslandHostInfo, onOpen = {})
}
