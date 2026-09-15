package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.colorNotGood
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate

@Composable
internal fun DueTodayCard(
    item: UpcomingRecurrentItem,
    currencyFormat: NumberFormat,
    onConfirmPaid: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = item.transaction.comment.ifBlank { stringResource(R.string.recurrent_ticket_unnamed_subscription) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colorNotGood.copy(alpha = 0.16f)),
        border = BorderStroke(1.dp, colorNotGood.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RecurringItemIndicator(
                    label = name.first().uppercaseChar().toString(),
                    transactionId = item.transaction.id,
                    backgroundOverride = colorNotGood.copy(alpha = 0.3f),
                    contentColorOverride = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMediumCondensed,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = frequencyText(item.transaction.recurrentFrequency),
                        style = MaterialTheme.typography.bodyMediumCondensed,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = currencyFormat.format(item.transaction.amount),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.subscriptions_due_today_label),
                style = MaterialTheme.typography.labelMediumCondensed,
                fontWeight = FontWeight.Bold,
                color = colorNotGood,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onConfirmPaid) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.subscriptions_mark_as_paid),
                        style = MaterialTheme.typography.labelMediumCondensed,
                    )
                }
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(R.string.subscriptions_skip_this_cycle),
                        style = MaterialTheme.typography.labelMediumCondensed,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DueTodayCardPreview() {
    val today = LocalDate.now()
    val sampleItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("16.99"),
            comment = "Netflix",
            date = today.minusMonths(1).atStartOfDay(),
            isDeleted = false,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today,
        isInCurrentPeriod = true,
    )
    MinusTheme {
        DueTodayCard(
            item = sampleItem,
            currencyFormat = NumberFormat.getCurrencyInstance(),
            onConfirmPaid = {},
            onSkip = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
