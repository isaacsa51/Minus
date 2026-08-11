@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.analytics.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.font.calcAdaptiveFont
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.formatCurrencySymbolOnly
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PastPeriodsBottomSheet(
    periods: List<ArchivedBudget>,
    allTransactions: List<Transaction> = emptyList(),
    onPeriodClick: (ArchivedBudget) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = stringResource(R.string.past_periods_title),
            style = MaterialTheme.typography.titleLargeEmphasized,
            modifier = Modifier.padding(16.dp)
        )

        if (periods.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.past_periods_empty),
                    style = MaterialTheme.typography.bodyLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(periods) { period ->
                    ArchivedPeriodCard(
                        period = period,
                        allTransactions = allTransactions,
                        onClick = { onPeriodClick(period) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivedPeriodCard(
    period: ArchivedBudget,
    allTransactions: List<Transaction>,
    onClick: () -> Unit
) {
    val isVirtual = period.periodId < 0L

    val periodTransactions = remember(period, allTransactions) {
        val filteredById =
            allTransactions.filter { it.periodId == period.periodId && !it.isDeleted && period.periodId > 0 }
        if (filteredById.isNotEmpty()) {
            filteredById
        } else {
            allTransactions.filter { tx ->
                val date = tx.date?.toLocalDate() ?: return@filter false
                !date.isBefore(period.startDate) && !date.isAfter(period.endDate) && !tx.isDeleted
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = combineColors(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant,
                t = 0.3f,
            ),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${
                                period.startDate.format(
                                    DateTimeFormatter.ofPattern("dd MMM")
                                )
                            } - ${
                                period.endDate.format(
                                    DateTimeFormatter.ofPattern("dd MMM yyyy")
                                )
                            }",
                            style = MaterialTheme.typography.titleSmallEmphasized,
                        )
                    }
                }

                if (!isVirtual) {
                    StatusChip(period)
                }
            }

            val density = LocalDensity.current
            val spentFormatted = formatCurrencySymbolOnly(period.spentAmount, period.currencyCode)
            val budgetFormatted = formatCurrencySymbolOnly(period.totalBudget, period.currencyCode)

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.past_periods_spent_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BoxWithConstraints {
                        val adaptiveFontSize = calcAdaptiveFont(
                            height = 100f,
                            width = with(density) { maxWidth.toPx() },
                            minFontSize = 14.sp,
                            maxFontSize = 24.sp,
                            text = spentFormatted,
                            style = MaterialTheme.typography.titleLargeEmphasized
                        )
                        Text(
                            text = spentFormatted,
                            style = MaterialTheme.typography.titleLargeEmphasized.copy(
                                fontSize = adaptiveFontSize,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (!isVirtual && period.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (!isVirtual) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .width(1.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.budget),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BoxWithConstraints {
                            val adaptiveFontSize = calcAdaptiveFont(
                                height = 100f,
                                width = with(density) { maxWidth.toPx() },
                                minFontSize = 14.sp,
                                maxFontSize = 24.sp,
                                text = budgetFormatted,
                                style = MaterialTheme.typography.titleLargeEmphasized
                            )
                            Text(
                                text = budgetFormatted,
                                style = MaterialTheme.typography.titleLargeEmphasized.copy(
                                    fontSize = adaptiveFontSize
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            CategoryDistributionBar(
                transactions = periodTransactions,
                totalBudget = if (isVirtual) null else period.totalBudget,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryDistributionBar(
    transactions: List<Transaction>,
    totalBudget: BigDecimal?,
    modifier: Modifier = Modifier
) {
    val uncategorizedLabel = stringResource(R.string.categories_chart_uncategorized)
    val recurrentLabel = stringResource(R.string.tutorial_recurrent_title)

    val data = remember(transactions, totalBudget, uncategorizedLabel, recurrentLabel) {
        val recurrentTotal = transactions.filter { it.isRecurrent }.sumOf { it.amount }
        val categoryTotals = transactions
            .filter { !it.isRecurrent }
            .groupBy { it.comment.trim().ifEmpty { uncategorizedLabel } }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(4)

        val totalSpent = transactions.sumOf { it.amount }
        val base = totalBudget ?: totalSpent.coerceAtLeast(BigDecimal.ONE)
        val isOverBudget = totalBudget != null && totalSpent > totalBudget

        val segments = mutableListOf<DistributionSegment>()

        // Recurrent first
        if (recurrentTotal > BigDecimal.ZERO) {
            segments.add(
                DistributionSegment(
                    name = recurrentLabel,
                    amount = recurrentTotal,
                    isRecurrent = true
                )
            )
        }

        // Categories
        categoryTotals.forEach { (name, amount) ->
            segments.add(
                DistributionSegment(
                    name = name,
                    amount = amount,
                    isRecurrent = false
                )
            )
        }

        val totalWeight = base.coerceAtLeast(totalSpent).toFloat()

        DistributionData(
            segments = segments,
            totalSpent = totalSpent,
            totalBudget = totalBudget,
            totalWeight = totalWeight,
            isOverBudget = isOverBudget
        )
    }

    val colors = listOf(
        Color(0xFF5FC7E7),
        Color(0xFF75E584),
        Color(0xFFFFD386),
        Color(0xFFEF7564),
        Color(0xFF64B5F6),
        Color(0xFFAED581),
        Color(0xFFFFB74D),
        Color(0xFFBA68C8),
        Color(0xFF4DB6AC),
        Color(0xFF9575CD),
        Color(0xFFF06292),
    )
    val recurrentColor = MaterialTheme.colorScheme.outline

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                data.segments.forEachIndexed { index, segment ->
                    val weight = (segment.amount.toFloat() / data.totalWeight).coerceAtLeast(0.01f)
                    val color =
                        if (segment.isRecurrent) recurrentColor else colors.getOrElse(index - if (data.segments.any { it.isRecurrent }) 1 else 0) { MaterialTheme.colorScheme.outlineVariant }

                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }

                if (data.totalBudget != null && data.totalSpent < data.totalBudget) {
                    val remaining = (data.totalBudget - data.totalSpent).toFloat()
                    if (remaining > 0) {
                        Box(
                            modifier = Modifier
                                .weight(remaining / data.totalWeight)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(data.segments) { index, segment ->
                val color =
                    if (segment.isRecurrent) recurrentColor else colors.getOrElse(index - if (data.segments.any { it.isRecurrent }) 1 else 0) { MaterialTheme.colorScheme.outlineVariant }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = segment.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class DistributionSegment(
    val name: String,
    val amount: BigDecimal,
    val isRecurrent: Boolean
)

private data class DistributionData(
    val segments: List<DistributionSegment>,
    val totalSpent: BigDecimal,
    val totalBudget: BigDecimal?,
    val totalWeight: Float,
    val isOverBudget: Boolean
)

@Composable
private fun StatusChip(period: ArchivedBudget) {
    val (text, color, icon) = when {
        period.isOverBudget -> Triple(
            stringResource(R.string.past_periods_status_over_budget),
            MaterialTheme.colorScheme.error,
            Icons.Default.Error
        )

        period.savedAmount > BigDecimal.ZERO -> Triple(
            stringResource(
                R.string.past_periods_status_saved,
                formatCurrencySymbolOnly(period.savedAmount, period.currencyCode)
            ), Color(0xFF4CAF50), Icons.Default.Savings
        )

        else -> Triple(
            stringResource(R.string.past_periods_status_on_track),
            MaterialTheme.colorScheme.primary,
            Icons.Default.CheckCircle
        )
    }

    Surface(
        color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMediumCondensed,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PastPeriodsBottomSheetPreview() {
    val today = LocalDate.now()
    val sampleTransactions = listOf(
        Transaction(
            amount = BigDecimal("150.00"),
            comment = "Netflix",
            isRecurrent = true,
            date = today.minusDays(5).atStartOfDay()
        ),
        Transaction(
            amount = BigDecimal("50.00"),
            comment = "Food",
            date = today.minusDays(5).atStartOfDay()
        ),
        Transaction(
            amount = BigDecimal("20.00"),
            comment = "Coffee",
            date = today.minusDays(5).atStartOfDay()
        ),
        Transaction(
            amount = BigDecimal("100.00"),
            comment = "Rent",
            isRecurrent = true,
            date = today.minusDays(40).atStartOfDay()
        ),
        Transaction(
            amount = BigDecimal("30.00"),
            comment = "Gas",
            date = today.minusDays(70).atStartOfDay()
        ),
        Transaction(
            amount = BigDecimal("80.00"),
            comment = "Internet",
            date = LocalDate.of(2026, 6, 15).atStartOfDay()
        ),
        Transaction(
            amount = BigDecimal("70.00"),
            comment = "Other",
            date = LocalDate.of(2026, 6, 20).atStartOfDay()
        ),
    )
    val samplePeriods = listOf(
        ArchivedBudget(
            periodId = 1L,
            totalBudget = BigDecimal("1000.00"),
            spentAmount = BigDecimal("850.00"),
            startDate = today.minusDays(30),
            endDate = today.minusDays(1),
            currencyCode = "USD",
            periodType = BudgetPeriod.MONTHLY
        ), ArchivedBudget(
            periodId = -202606L,
            totalBudget = BigDecimal("1000.00"),
            spentAmount = BigDecimal("150.00"),
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 30),
            currencyCode = "USD",
            periodType = BudgetPeriod.MONTHLY
        ), ArchivedBudget(
            periodId = 2L,
            totalBudget = BigDecimal("500.00"),
            spentAmount = BigDecimal("600.00"),
            startDate = today.minusDays(45),
            endDate = today.minusDays(31),
            currencyCode = "USD",
            periodType = BudgetPeriod.BIWEEKLY
        )
    )

    MinusTheme {
        Surface {
            PastPeriodsBottomSheet(
                periods = samplePeriods,
                allTransactions = sampleTransactions,
                onPeriodClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PastPeriodsBottomSheetEmptyPreview() {
    MinusTheme {
        Surface {
            PastPeriodsBottomSheet(
                periods = emptyList(), onPeriodClick = {})
        }
    }
}
