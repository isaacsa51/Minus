package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.colorGood
import com.serranoie.app.minus.presentation.ui.theme.colorNotGood
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.expense.StatusBadge
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SubscriptionAvatar
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.font.format.prettyDate
import java.math.BigDecimal
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
    var expandedTransactionId by remember { mutableStateOf<Long?>(null) }

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

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                charges.forEachIndexed { index, charge ->
                    val transactionId = charge.transaction.id
                    SubscriptionDayChargeRow(
                        charge = charge,
                        isToday = date == today,
                        currencyFormat = currencyFormat,
                        position = when {
                            charges.size == 1 -> PaddedListItemPosition.Single
                            index == 0 -> PaddedListItemPosition.First
                            index == charges.lastIndex -> PaddedListItemPosition.Last
                            else -> PaddedListItemPosition.Middle
                        },
                        isExpanded = expandedTransactionId == transactionId,
                        onToggleExpanded = {
                            expandedTransactionId = if (expandedTransactionId == transactionId) null else transactionId
                        },
                        onConfirmPaid = { onConfirmPaid(charge.transaction) },
                        onSkip = { onSkip(charge.transaction) },
                    )
                }
            }
        }
    }
}

private fun chargeRowShape(position: PaddedListItemPosition): Shape = when (position) {
    PaddedListItemPosition.First -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    PaddedListItemPosition.Last -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)
    PaddedListItemPosition.Middle -> RoundedCornerShape(8.dp)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SubscriptionDayChargeRow(
    charge: DayCharge,
    isToday: Boolean,
    currencyFormat: NumberFormat,
    position: PaddedListItemPosition,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onConfirmPaid: () -> Unit,
    onSkip: () -> Unit,
) {
    val transaction = charge.transaction
    val name = transaction.comment.ifBlank { stringResource(R.string.recurrent_ticket_unnamed_subscription) }
    val isActionable = charge.status == ChargeStatus.PENDING && isToday

    val statusLabel = when (charge.status) {
        ChargeStatus.PAID -> stringResource(R.string.subscriptions_status_paid)
        ChargeStatus.SKIPPED -> stringResource(R.string.subscriptions_status_skipped)
        ChargeStatus.PENDING -> if (isToday) stringResource(R.string.subscriptions_due_today_label) else null
    }
    val statusColor = when (charge.status) {
        ChargeStatus.PAID -> colorGood
        ChargeStatus.SKIPPED -> MaterialTheme.colorScheme.onSurfaceVariant
        ChargeStatus.PENDING -> colorNotGood
    }
    val shape = chargeRowShape(position)

    SharedTransitionLayout {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "charge_row_${transaction.id}",
            ) { expanded ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleExpanded),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SubscriptionAvatar(
                            label = name.first().uppercaseChar().toString(),
                            transactionId = transaction.id,
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState(key = "day_charge_avatar_${transaction.id}"),
                                animatedVisibilityScope = this@AnimatedContent,
                            ),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMediumCondensed,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.sharedElement(
                                    rememberSharedContentState(key = "day_charge_name_${transaction.id}"),
                                    animatedVisibilityScope = this@AnimatedContent,
                                ),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (statusLabel != null) {
                                StatusBadge(text = statusLabel, color = statusColor)
                            } else {
                                Text(
                                    text = frequencyText(transaction.recurrentFrequency),
                                    style = MaterialTheme.typography.bodySmallCondensed,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currencyFormat.format(transaction.amount),
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState(key = "day_charge_amount_${transaction.id}"),
                                animatedVisibilityScope = this@AnimatedContent,
                            ),
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(20.dp),
                        )
                    }

                    if (expanded) {
                        Spacer(modifier = Modifier.height(12.dp))
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

                            if (isActionable) {
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmallCondensed,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmallCondensed,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun previewTransaction(id: Long, name: String, amount: String, note: String = "") = Transaction(
    id = id,
    amount = BigDecimal(amount),
    comment = name,
    note = note,
    date = LocalDate.now().atStartOfDay(),
    isRecurrent = true,
    recurrentFrequency = RecurrentFrequency.MONTHLY,
)

/** One row per remaining case — only today's PENDING charge expose the confirm/skip actions when expanded. */
@Preview(showBackground = true)
@Composable
private fun SubscriptionDayChargeRowPreview() {
    MinusTheme {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            SubscriptionDayChargeRow(
                charge = DayCharge(previewTransaction(1, "Netflix", "16.99"), ChargeStatus.PENDING),
                isToday = true,
                currencyFormat = NumberFormat.getCurrencyInstance(),
                position = PaddedListItemPosition.First,
                isExpanded = true,
                onToggleExpanded = {},
                onConfirmPaid = {},
                onSkip = {},
            )
            SubscriptionDayChargeRow(
                charge = DayCharge(previewTransaction(2, "Spotify", "9.99"), ChargeStatus.PENDING),
                isToday = false,
                currencyFormat = NumberFormat.getCurrencyInstance(),
                position = PaddedListItemPosition.Middle,
                isExpanded = false,
                onToggleExpanded = {},
                onConfirmPaid = {},
                onSkip = {},
            )
            SubscriptionDayChargeRow(
                charge = DayCharge(previewTransaction(3, "Gym membership", "49.99", note = "Annual plan, paid upfront"), ChargeStatus.SKIPPED),
                isToday = false,
                currencyFormat = NumberFormat.getCurrencyInstance(),
                position = PaddedListItemPosition.Middle,
                isExpanded = true,
                onToggleExpanded = {},
                onConfirmPaid = {},
                onSkip = {},
            )
            SubscriptionDayChargeRow(
                charge = DayCharge(previewTransaction(4, "iCloud+", "2.99"), ChargeStatus.PAID),
                isToday = false,
                currencyFormat = NumberFormat.getCurrencyInstance(),
                position = PaddedListItemPosition.Last,
                isExpanded = false,
                onToggleExpanded = {},
                onConfirmPaid = {},
                onSkip = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Single charge, today")
@Composable
private fun SubscriptionDayDetailsSheetPreview() {
    val today = LocalDate.now()
    MinusTheme {
        SubscriptionDayDetailsSheet(
            date = today,
            charges = listOf(DayCharge(previewTransaction(1, "Netflix", "16.99"), ChargeStatus.PENDING)),
            currencyFormat = NumberFormat.getCurrencyInstance(),
            onConfirmPaid = {},
            onSkip = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true, name = "Multiple charges, mixed statuses")
@Composable
private fun SubscriptionDayDetailsSheetMultiplePreview() {
    val pastDay = LocalDate.now().minusDays(5)
    MinusTheme {
        SubscriptionDayDetailsSheet(
            date = pastDay,
            charges = listOf(
                DayCharge(previewTransaction(1, "Netflix", "16.99"), ChargeStatus.PAID),
                DayCharge(previewTransaction(2, "Spotify", "9.99"), ChargeStatus.SKIPPED),
                DayCharge(previewTransaction(3, "Gym membership", "49.99", note = "Annual plan, paid upfront"), ChargeStatus.PAID),
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(),
            onConfirmPaid = {},
            onSkip = {},
            onDismiss = {},
        )
    }
}
