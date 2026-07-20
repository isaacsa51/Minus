@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.analytics.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.formatCurrencySymbolOnly
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PastPeriodsBottomSheet(
    periods: List<ArchivedBudget>,
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
                    style = MaterialTheme.typography.bodyLarge,
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

                StatusChip(period)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.past_periods_spent_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrencySymbolOnly(period.spentAmount, period.currencyCode),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (period.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.past_periods_limit_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "of ${formatCurrencySymbolOnly(period.totalBudget, period.currencyCode)}",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val progress = (period.spentAmount.toFloat() / period.totalBudget.toFloat()).coerceIn(0f, 1f)
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = if (period.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

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
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
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
    val samplePeriods = listOf(
        ArchivedBudget(
            periodId = 1L,
            totalBudget = BigDecimal("1000.00"),
            spentAmount = BigDecimal("850.00"),
            startDate = LocalDate.now().minusDays(30),
            endDate = LocalDate.now().minusDays(1),
            currencyCode = "USD",
            periodType = BudgetPeriod.MONTHLY
        ),
        ArchivedBudget(
            periodId = 2L,
            totalBudget = BigDecimal("500.00"),
            spentAmount = BigDecimal("600.00"),
            startDate = LocalDate.now().minusDays(45),
            endDate = LocalDate.now().minusDays(31),
            currencyCode = "USD",
            periodType = BudgetPeriod.BIWEEKLY
        ),
        ArchivedBudget(
            periodId = 3L,
            totalBudget = BigDecimal("1200.00"),
            spentAmount = BigDecimal("1200.00"),
            startDate = LocalDate.now().minusDays(60),
            endDate = LocalDate.now().minusDays(46),
            currencyCode = "USD",
            periodType = BudgetPeriod.MONTHLY
        )
    )

    MinusTheme {
        Surface {
            PastPeriodsBottomSheet(
                periods = samplePeriods,
                onPeriodClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PastPeriodsBottomSheetEmptyPreview() {
    MinusTheme {
        Surface {
            PastPeriodsBottomSheet(
                periods = emptyList(),
                onPeriodClick = {}
            )
        }
    }
}
