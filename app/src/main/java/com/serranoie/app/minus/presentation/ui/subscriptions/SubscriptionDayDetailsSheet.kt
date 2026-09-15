package com.serranoie.app.minus.presentation.ui.subscriptions

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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.colorBad
import com.serranoie.app.minus.presentation.ui.theme.colorGood
import com.serranoie.app.minus.presentation.ui.theme.colorNotGood
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.font.format.prettyDate
import java.text.NumberFormat
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubscriptionDayDetailsSheet(
    date: LocalDate,
    charges: List<DayCharge>,
    currencyFormat: NumberFormat,
    onConfirmPaid: (Transaction) -> Unit,
    onSkip: (Transaction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { LocalDate.now() }

    LaunchedEffect(Unit) {
        sheetState.show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = prettyDate(date = date.atStartOfDay(), showTime = false, human = true),
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
            Spacer(modifier = Modifier.height(16.dp))

            charges.forEachIndexed { index, charge ->
                SubscriptionDayChargeRow(
                    charge = charge,
                    isToday = date == today,
                    currencyFormat = currencyFormat,
                    onConfirmPaid = { onConfirmPaid(charge.transaction) },
                    onSkip = { onSkip(charge.transaction) },
                )
                if (index != charges.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SubscriptionDayChargeRow(
    charge: DayCharge,
    isToday: Boolean,
    currencyFormat: NumberFormat,
    onConfirmPaid: () -> Unit,
    onSkip: () -> Unit,
) {
    val transaction = charge.transaction
    val name = transaction.comment.ifBlank { stringResource(R.string.recurrent_ticket_unnamed_subscription) }
    val isActionable = charge.status == ChargeStatus.MISSED ||
        (charge.status == ChargeStatus.PENDING && isToday)

    val statusLabel = when (charge.status) {
        ChargeStatus.PAID -> stringResource(R.string.subscriptions_status_paid)
        ChargeStatus.SKIPPED -> stringResource(R.string.subscriptions_status_skipped)
        ChargeStatus.MISSED -> stringResource(R.string.subscriptions_status_missed)
        ChargeStatus.PENDING -> if (isToday) stringResource(R.string.subscriptions_due_today_label) else null
    }
    val statusColor = when (charge.status) {
        ChargeStatus.PAID -> colorGood
        ChargeStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
        ChargeStatus.MISSED -> colorBad
        ChargeStatus.PENDING -> colorNotGood
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RecurringItemIndicator(
                label = name.first().uppercaseChar().toString(),
                transactionId = transaction.id,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMediumCondensed,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = frequencyText(transaction.recurrentFrequency),
                    style = MaterialTheme.typography.bodyMediumCondensed,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = currencyFormat.format(transaction.amount),
                style = MaterialTheme.typography.titleSmallEmphasized,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (statusLabel != null) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelMediumCondensed,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (isActionable) {
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
