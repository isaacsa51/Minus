package com.serranoie.app.wear.minus.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.serranoie.app.minus.sync.contract.BudgetStatePayload
import com.serranoie.app.wear.minus.R
import com.serranoie.app.wear.minus.presentation.theme.MinusTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class BudgetPeriodKind { DAILY, WEEKLY, BIWEEKLY, MONTHLY, UNKNOWN }

internal data class PeriodOverviewUiState(
    val loading: Boolean = true,
    val hasBudget: Boolean = false,
    /** Currency-formatted (locale-aware) — kept as a string, not a resource. */
    val headlineAmount: String = "",
    val progress: Float = 0f,
    val isOverBudget: Boolean = false,
    val budgetTotalText: String = "",
    val periodStartText: String = "",
    val periodEndText: String = "",
    val period: BudgetPeriodKind = BudgetPeriodKind.UNKNOWN,
    val daysRemaining: Int = 0,
    val currencySymbol: String = "$",
    val symbolAtEnd: Boolean = false,
)

@Composable
internal fun PeriodOverviewScreen(
    state: PeriodOverviewUiState,
    onAddExpense: () -> Unit,
    onManualSync: () -> Unit,
    isSyncing: Boolean = false,
) {
    val listState = rememberLazyListState()

    AppScaffold(timeText = {}) {
        ScreenScaffold(
            scrollState = listState,
            timeText = null,
            edgeButton = {
                EdgeButton(onClick = onAddExpense) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.overview_add_expense),
                    )
                }
            },
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 56.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    when {
                        state.loading && !state.hasBudget ->
                            StatusText(stringResource(R.string.overview_syncing))

                        !state.hasBudget ->
                            StatusText(stringResource(R.string.overview_no_budget))

                        else -> WearBudgetPill(
                            amountText = state.headlineAmount,
                            statusLabel = stringResource(
                                if (state.isOverBudget) {
                                    R.string.overview_over_budget
                                } else {
                                    R.string.overview_left_today
                                },
                            ),
                            progress = state.progress,
                            isOverBudget = state.isOverBudget,
                        )
                    }
                }

                if (state.hasBudget) {
                    item {
                        WearBudgetDisplay(state)
                    }
                }

                item {
                    ManualSyncButton(isSyncing = isSyncing, onClick = onManualSync)
                }
            }
        }
    }
}

@Composable
private fun ManualSyncButton(isSyncing: Boolean, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        enabled = !isSyncing,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = Modifier
            .height(32.dp)
            .width(32.dp),
    ) {
        if (isSyncing) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
        } else {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.overview_manual_sync),
            )
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 24.dp),
    )
}

@Composable
private fun WearBudgetDisplay(state: PeriodOverviewUiState) {
    val daysLeftText = if (state.daysRemaining <= 0) {
        stringResource(R.string.overview_ends_today)
    } else {
        pluralStringResource(R.plurals.overview_days_left, state.daysRemaining, state.daysRemaining)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(state.period.titleRes()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.budgetTotalText,
            style = MaterialTheme.typography.numeralSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Column(
                modifier = Modifier.padding(end = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RailDot()
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                RailDot()
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = state.periodStartText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                DaysLeftPill(daysLeftText)
                Text(
                    text = state.periodEndText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RailDot() {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
    )
}

@Composable
private fun DaysLeftPill(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

@StringRes
private fun BudgetPeriodKind.titleRes(): Int = when (this) {
    BudgetPeriodKind.DAILY -> R.string.overview_budget_title_daily
    BudgetPeriodKind.WEEKLY -> R.string.overview_budget_title_weekly
    BudgetPeriodKind.BIWEEKLY -> R.string.overview_budget_title_biweekly
    BudgetPeriodKind.MONTHLY -> R.string.overview_budget_title_monthly
    BudgetPeriodKind.UNKNOWN -> R.string.overview_budget_title_generic
}


internal fun BudgetStatePayload?.toOverviewUiState(): PeriodOverviewUiState {
    if (this == null) return PeriodOverviewUiState(loading = true, hasBudget = false)
    if (!hasBudget) return PeriodOverviewUiState(loading = false, hasBudget = false)

    val headlineSource = if (isOverBudget) remainingInPeriod else remainingToday
    return PeriodOverviewUiState(
        loading = false,
        hasBudget = true,
        headlineAmount = formatMoney(currencySymbol, symbolAtEnd, headlineSource),
        progress = progress,
        isOverBudget = isOverBudget,
        budgetTotalText = formatMoney(currencySymbol, symbolAtEnd, totalBudget),
        periodStartText = formatDay(periodStartEpochDay),
        periodEndText = formatDay(periodEndEpochDay),
        period = budgetPeriodKind(period),
        daysRemaining = daysRemaining,
        currencySymbol = currencySymbol,
        symbolAtEnd = symbolAtEnd,
    )
}

private fun budgetPeriodKind(raw: String): BudgetPeriodKind = when (raw.uppercase(Locale.ROOT)) {
    "DAILY" -> BudgetPeriodKind.DAILY
    "WEEKLY" -> BudgetPeriodKind.WEEKLY
    "BIWEEKLY" -> BudgetPeriodKind.BIWEEKLY
    "MONTHLY" -> BudgetPeriodKind.MONTHLY
    else -> BudgetPeriodKind.UNKNOWN
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

private fun formatDay(epochDay: Long): String {
    if (epochDay <= 0L) return "—"
    return LocalDate.ofEpochDay(epochDay).format(dateFormatter)
}

internal fun formatMoney(symbol: String, symbolAtEnd: Boolean, rawAmount: String): String {
    val amount = rawAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val negative = amount.signum() < 0
    val magnitude = amount.abs().setScale(2, RoundingMode.HALF_UP).stripTrailingZeros()
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        minimumFractionDigits = if (magnitude.scale() <= 0) 0 else magnitude.scale().coerceAtMost(2)
        maximumFractionDigits = 2
    }
    val body = formatter.format(magnitude)
    val withSymbol = if (symbolAtEnd) "$body$symbol" else "$symbol$body"
    return if (negative) "-$withSymbol" else withSymbol
}

@Preview(device = "id:wearos_small_round", showBackground = true, showSystemUi = true)
@Composable
private fun PeriodOverviewPreview() {
    MinusTheme {
        PeriodOverviewScreen(
            state = PeriodOverviewUiState(
                loading = false,
                hasBudget = true,
                headlineAmount = "$42.50",
                progress = 0.42f,
                isOverBudget = false,
                budgetTotalText = "$800.00",
                periodStartText = "Sep 1",
                periodEndText = "Sep 30",
                period = BudgetPeriodKind.MONTHLY,
                daysRemaining = 12,
            ),
            onAddExpense = {},
            onManualSync = {},
        )
    }
}

@Preview(device = "id:wearos_small_round", showBackground = true, showSystemUi = true)
@Composable
private fun PeriodOverviewNoBudgetPreview() {
    MinusTheme {
        PeriodOverviewScreen(
            state = PeriodOverviewUiState(loading = false, hasBudget = false),
            onAddExpense = {},
            onManualSync = {},
        )
    }
}

@Preview(device = "id:wearos_small_round", showBackground = true, showSystemUi = true)
@Composable
private fun PeriodOverviewSyncingPreview() {
    MinusTheme {
        PeriodOverviewScreen(
            state = PeriodOverviewUiState(
                loading = false,
                hasBudget = true,
                headlineAmount = "$42.50",
                progress = 0.42f,
                isOverBudget = false,
                budgetTotalText = "$800.00",
                periodStartText = "Sep 1",
                periodEndText = "Sep 30",
                period = BudgetPeriodKind.MONTHLY,
                daysRemaining = 12,
            ),
            onAddExpense = {},
            onManualSync = {},
            isSyncing = true,
        )
    }
}
