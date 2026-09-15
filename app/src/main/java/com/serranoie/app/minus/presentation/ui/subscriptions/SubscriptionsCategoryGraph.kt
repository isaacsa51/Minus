package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.expense.subscriptionPalette
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate

private data class SubscriptionBar(
    val transactionId: Long,
    val label: String,
    val amount: BigDecimal
)

/**
 * "Category" view of the same [chargesByDay] the calendar renders — one bar per subscription,
 * summed across every occurrence in the period, with the subscription's name as the x-axis label
 * instead of a date. Plain weighted-height boxes rather than a Canvas chart: with only a handful
 * of subscriptions there's no time series to scroll through, just N proportional bars.
 */
@Composable
internal fun SubscriptionsCategoryGraph(
    chargesByDay: Map<LocalDate, List<DayCharge>>,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier,
) {
    val bars = remember(chargesByDay) {
        chargesByDay.values.flatten()
            .groupBy { it.transaction.id }
            .map { (transactionId, charges) ->
                SubscriptionBar(
                    transactionId = transactionId,
                    label = charges.first().transaction.comment.ifBlank { "?" },
                    amount = charges.sumOf { it.transaction.amount },
                )
            }
            .sortedByDescending { it.amount }
    }
    if (bars.isEmpty()) return

    val maxAmount = remember(bars) { bars.maxOf { it.amount } }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            bars.forEach { bar ->
                val heightFraction =
                    (bar.amount.toDouble() / maxAmount.toDouble()).toFloat().coerceIn(0.08f, 1f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        text = currencyFormat.format(bar.amount),
                        style = MaterialTheme.typography.labelSmallCondensed,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .fillMaxHeight(heightFraction)
                            .background(
                                subscriptionPalette(bar.transactionId).main,
                                MaterialTheme.shapes.small,
                            ),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            bars.forEach { bar ->
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelSmallCondensed.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun previewCharge(id: Long, name: String, amount: String, status: ChargeStatus) = DayCharge(
    transaction = Transaction(
        id = id,
        amount = BigDecimal(amount),
        comment = name,
        date = LocalDate.now().atStartOfDay(),
        isRecurrent = true,
        recurrentFrequency = RecurrentFrequency.MONTHLY,
    ),
    status = status,
)

@Preview(showBackground = true, name = "Few subscriptions")
@Composable
private fun SubscriptionsCategoryGraphPreview() {
    val today = LocalDate.now()
    MinusTheme {
        SubscriptionsCategoryGraph(
            chargesByDay = mapOf(
                today to listOf(
                    previewCharge(1, "Netflix", "16.99", ChargeStatus.PENDING),
                    previewCharge(2, "Spotify", "9.99", ChargeStatus.PAID),
                    previewCharge(3, "Gym", "49.99", ChargeStatus.PENDING),
                ),
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
    }
}

@Preview(showBackground = true, name = "Many subscriptions")
@Composable
private fun SubscriptionsCategoryGraphManyPreview() {
    val today = LocalDate.now()
    MinusTheme {
        SubscriptionsCategoryGraph(
            chargesByDay = mapOf(
                today to listOf(
                    previewCharge(1, "Netflix", "16.99", ChargeStatus.PENDING),
                    previewCharge(2, "Spotify", "9.99", ChargeStatus.PAID),
                    previewCharge(3, "Gym membership", "49.99", ChargeStatus.PENDING),
                    previewCharge(4, "iCloud+", "2.99", ChargeStatus.PAID),
                    previewCharge(5, "YouTube Premium", "13.99", ChargeStatus.PENDING),
                    previewCharge(6, "Disney+", "7.99", ChargeStatus.PAID),
                ),
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
    }
}
