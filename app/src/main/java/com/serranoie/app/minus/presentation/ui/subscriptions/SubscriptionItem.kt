package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SubscriptionAvatar
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.isNightMode
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.haptic.HapticUtil.performUIHaptic
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * A self-contained subscription row: avatar, name, relative due date, amount, and every action
 * (mark paid, skip, edit, delete) always visible in one row.
 */
@Composable
internal fun SubscriptionItem(
    item: UpcomingRecurrentItem,
    currencyFormat: NumberFormat,
    onMarkAsPaid: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderStroke: BorderStroke? = null,
    accentColor: Color? = null,
) {
    val transaction = item.transaction
    val view = LocalView.current
    val locale = LocalConfiguration.current.locales[0]
    val name =
        transaction.comment.ifBlank { stringResource(R.string.recurrent_ticket_unnamed_subscription) }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }

    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), item.nextChargeDate)
    val subtitle = when {
        daysUntil == 0L -> stringResource(R.string.upcoming_recurrent_today)
        daysUntil == 1L -> stringResource(R.string.upcoming_recurrent_tomorrow)
        daysUntil < 7 -> stringResource(
            R.string.subscriptions_item_due_in_days,
            daysUntil,
            dateFormatter.format(item.nextChargeDate),
        )

        else -> stringResource(
            R.string.subscriptions_item_due_in_weeks,
            daysUntil / 7,
            dateFormatter.format(item.nextChargeDate),
        )
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderStroke,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubscriptionAvatar(
                    label = name.first().uppercaseChar().toString(),
                    transactionId = transaction.id
                )
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currencyFormat.format(transaction.amount),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            val nightMode = isNightMode()
            val darkenedAccent = accentColor?.let {
                if (nightMode) {
                    Color(
                        red = it.red + (1f - it.red) * 0.5f,
                        green = it.green + (1f - it.green) * 0.5f,
                        blue = it.blue + (1f - it.blue) * 0.5f,
                    )
                } else {
                    Color(red = it.red * 0.55f, green = it.green * 0.55f, blue = it.blue * 0.55f)
                }
            }
            val markPaidContainer = accentColor ?: MaterialTheme.colorScheme.secondary
            val markPaidContent =
                if (accentColor != null) Color.Black else MaterialTheme.colorScheme.onSecondary
            val tonalContainer =
                accentColor?.copy(alpha = 0.3f) ?: MaterialTheme.colorScheme.surfaceVariant
            val tonalContent = darkenedAccent ?: MaterialTheme.colorScheme.onSurfaceVariant
            val editTint = darkenedAccent ?: MaterialTheme.colorScheme.secondary

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        onMarkAsPaid()
                        performUIHaptic(view)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = markPaidContainer,
                        contentColor = markPaidContent,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.subscriptions_item_mark_paid_short),
                        style = MaterialTheme.typography.labelSmallEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Button(
                    onClick =  {
                        onSkip()
                        performUIHaptic(view)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tonalContainer,
                        contentColor = tonalContent,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.subscriptions_item_skip_short),
                        style = MaterialTheme.typography.labelSmallEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                FilledIconButton(
                    onClick = {
                        onEdit()
                        performUIHaptic(view)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = tonalContainer,
                        contentColor = editTint,
                    ),
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit),
                        modifier = Modifier.size(16.dp),
                        tint = editTint,
                    )
                }

                FilledIconButton(
                    onClick = {
                        onDelete()
                        performUIHaptic(view)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun previewItem(
    id: Long,
    name: String,
    amount: String,
    daysAhead: Long,
    frequency: RecurrentFrequency
) =
    UpcomingRecurrentItem(
        transaction = Transaction(
            id = id,
            amount = BigDecimal(amount),
            comment = name,
            date = LocalDate.now().atStartOfDay(),
            isRecurrent = true,
            recurrentFrequency = frequency,
        ),
        nextChargeDate = LocalDate.now().plusDays(daysAhead),
        isInCurrentPeriod = true,
    )

@PreviewLightDark
@Composable
private fun SubscriptionItemPreview() {
    MinusTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SubscriptionItem(
                item = previewItem(1, "YouTube Family", "159", 11, RecurrentFrequency.MONTHLY),
                currencyFormat = NumberFormat.getCurrencyInstance(),
                onMarkAsPaid = {},
                onSkip = {},
                onEdit = {},
                onDelete = {},
                modifier = Modifier.fillMaxWidth(),
            )
            SubscriptionItem(
                item = previewItem(2, "Fitness First Pro", "500", 13, RecurrentFrequency.MONTHLY),
                currencyFormat = NumberFormat.getCurrencyInstance(),
                onMarkAsPaid = {},
                onSkip = {},
                onEdit = {},
                onDelete = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
