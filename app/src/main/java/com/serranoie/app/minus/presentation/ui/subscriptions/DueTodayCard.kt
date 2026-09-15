package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorNotGood
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedExpandableItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.expense.StatusBadge
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SubscriptionAvatar
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
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
    var isExpanded by remember { mutableStateOf(false) }
    val transaction = item.transaction
    val name = transaction.comment.ifBlank { stringResource(R.string.recurrent_ticket_unnamed_subscription) }

    CustomPaddedExpandableItem(
        isExpanded = isExpanded,
        onToggleExpanded = { isExpanded = !isExpanded },
        position = PaddedListItemPosition.Single,
        background = colorNotGood.copy(alpha = 0.16f),
        borderStroke = BorderStroke(1.dp, colorNotGood.copy(alpha = 0.5f)),
        modifier = modifier,
        defaultContent = {
            SubscriptionAvatar(label = name.first().uppercaseChar().toString(), transactionId = transaction.id)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMediumCondensed,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                StatusBadge(
                    text = stringResource(R.string.subscriptions_due_today_label),
                    color = colorNotGood,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currencyFormat.format(transaction.amount),
                style = MaterialTheme.typography.titleSmallEmphasized,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp),
            )
        },
        expandedContent = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow(
                    label = stringResource(R.string.frequency),
                    value = frequencyText(transaction.recurrentFrequency),
                )
                if (transaction.note.isNotBlank()) {
                    DetailRow(
                        label = stringResource(R.string.extra_note_label),
                        value = transaction.note,
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onConfirmPaid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.subscriptions_mark_as_paid),
                            style = MaterialTheme.typography.labelSmallEmphasized,
                        )
                    }
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.subscriptions_skip_this_cycle),
                            style = MaterialTheme.typography.labelSmallEmphasized,
                        )
                    }
                }
            }
        },
    )
}

@PreviewLightDark
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
