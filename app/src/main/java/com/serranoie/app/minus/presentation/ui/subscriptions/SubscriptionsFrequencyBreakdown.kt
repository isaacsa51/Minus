package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.BarSegment
import com.serranoie.app.minus.presentation.ui.theme.component.LinearSavingBar
import com.serranoie.app.minus.presentation.ui.theme.component.expense.subscriptionPalette
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import java.math.BigDecimal
import java.text.NumberFormat

/**
 * Small breakdown of the monthly-equivalent commitment by billing frequency (reuses
 * [subscriptionPalette]'s hue-rotation, keyed by [RecurrentFrequency.ordinal] instead of a
 * transaction id, so each frequency gets its own stable, theme-aware color).
 */
@Composable
internal fun SubscriptionsFrequencyBreakdown(
    monthlyTotalByFrequency: Map<RecurrentFrequency, BigDecimal>,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier,
) {
    val entries = remember(monthlyTotalByFrequency) {
        monthlyTotalByFrequency.filterValues { it > BigDecimal.ZERO }
            .entries.sortedByDescending { it.value }
    }
    if (entries.size < 2) return

    val total = remember(entries) { entries.sumOf { it.value } }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.subscriptions_frequency_breakdown_title),
                style = MaterialTheme.typography.labelMediumCondensed,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearSavingBar(
                segments = entries.map { (frequency, amount) ->
                    BarSegment(
                        weight = (amount.toDouble() / total.toDouble()).toFloat(),
                        color = subscriptionPalette(frequency.ordinal.toLong()).main,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                entries.forEach { (frequency, amount) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(subscriptionPalette(frequency.ordinal.toLong()).main, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = frequencyText(frequency),
                                style = MaterialTheme.typography.labelSmallCondensed,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = currencyFormat.format(amount),
                                style = MaterialTheme.typography.labelMediumCondensed,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Two frequencies")
@Composable
private fun SubscriptionsFrequencyBreakdownTwoPreview() {
    MinusTheme {
        SubscriptionsFrequencyBreakdown(
            monthlyTotalByFrequency = mapOf(
                RecurrentFrequency.MONTHLY to BigDecimal("66.98"),
                RecurrentFrequency.WEEKLY to BigDecimal("43.30"),
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "All frequencies")
@Composable
private fun SubscriptionsFrequencyBreakdownAllPreview() {
    MinusTheme {
        SubscriptionsFrequencyBreakdown(
            monthlyTotalByFrequency = mapOf(
                RecurrentFrequency.MONTHLY to BigDecimal("120.50"),
                RecurrentFrequency.BIWEEKLY to BigDecimal("38.00"),
                RecurrentFrequency.WEEKLY to BigDecimal("19.96"),
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(),
            modifier = Modifier.padding(16.dp),
        )
    }
}
