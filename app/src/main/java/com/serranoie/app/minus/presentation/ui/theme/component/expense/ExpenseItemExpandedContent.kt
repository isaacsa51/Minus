package com.serranoie.app.minus.presentation.ui.theme.component.expense

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelLargeCondensed
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.calculateDaysToCutoff
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.Locale
import com.serranoie.app.minus.presentation.util.prettyDate

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ExpenseItemExpandedContent(
    transaction: Transaction,
    currencyFormat: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsPaid: () -> Unit,
    readOnly: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    creditCardCutoffDay: Int? = null,
) {
    val withoutDate = stringResource(R.string.without_date)
    val noName = stringResource(R.string.no_name)
    val dateLabel = stringResource(R.string.date)
    val frequencyLabel = stringResource(R.string.frequency)
    val chargeDayLabel = stringResource(R.string.charge_day)

    val transactionDateText = transaction.date?.let { date ->
        prettyDate(date, showTime = true, forceHideDate = false, human = true)
    } ?: withoutDate
    val recurrenceLabel = when (transaction.recurrentFrequency) {
        RecurrentFrequency.WEEKLY -> stringResource(R.string.recurrent_frequency_weekly)
        RecurrentFrequency.BIWEEKLY -> stringResource(R.string.recurrent_frequency_biweekly)
        RecurrentFrequency.MONTHLY -> stringResource(R.string.recurrent_frequency_monthly)
        null -> ""
    }

    val details = buildList {
        add(stringResource(R.string.description) to transaction.comment.ifEmpty { noName })
        add(dateLabel to transactionDateText)

        if (transaction.isCredit && creditCardCutoffDay != null) {
            val daysToCutoff = calculateDaysToCutoff(creditCardCutoffDay)
            val cutoffValue = if (daysToCutoff == 0) {
                stringResource(R.string.today)
            } else {
                stringResource(R.string.upcoming_recurrent_in_days, daysToCutoff.toLong())
            }
            add(stringResource(R.string.credit_cutoff_dialog_label) to cutoffValue)
        }

        if (transaction.isRecurrent && recurrenceLabel.isNotEmpty()) {
            add(frequencyLabel to recurrenceLabel)
        }
        transaction.subscriptionDay?.let { day ->
            if (transaction.isRecurrent) {
                val dayLabel = stringResource(R.string.day_number, day)
                add(chargeDayLabel to dayLabel)
            }
        }
        transaction.recurrentEndDate?.let { endDate ->
            if (transaction.isRecurrent) {
                val recurrenceEndLabel = stringResource(R.string.recurrence_end)
                add(
                    recurrenceEndLabel to prettyDate(
                        endDate,
                        showTime = false,
                        forceHideDate = false,
                        human = true,
                    ),
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.ticket_total_amount),
            style = MaterialTheme.typography.labelLargeCondensed,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Text(
            text = currencyFormat.format(transaction.amount),
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .censor()
                .then(
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            Modifier.sharedElement(
                                rememberSharedContentState(key = "amount_${transaction.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    } else Modifier
                )
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        details.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmallCondensed,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmallCondensed,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (label == stringResource(R.string.description) && sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedElement(
                                        rememberSharedContentState(key = "comment_${transaction.id}"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            } else Modifier
                        ),
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (transaction.isRecurrent) {
            Button(
                onClick = onMarkAsPaid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.mark_as_paid),
                    style = MaterialTheme.typography.labelSmallEmphasized,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                onClick = onEdit,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.edit), style = MaterialTheme.typography.labelSmallEmphasized)
            }

            if (!readOnly) {
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(stringResource(R.string.delete), style = MaterialTheme.typography.labelSmallEmphasized)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpenseItemExpandedContentNormalPreview() {
    MinusTheme {
        ExpenseItemExpandedContent(
            transaction = Transaction(
                id = 1L,
                amount = BigDecimal("150.50"),
                comment = "Compra en supermercado",
                date = LocalDateTime.now(),
                isDeleted = false,
                isRecurrent = false,
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
            onEdit = {},
            onDelete = {},
            onMarkAsPaid = {},
            readOnly = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpenseItemExpandedContentRecurrentPreview() {
    MinusTheme {
        ExpenseItemExpandedContent(
            transaction = Transaction(
                id = 2L,
                amount = BigDecimal("50.00"),
                comment = "Suscripción Mensual",
                date = LocalDateTime.now(),
                isDeleted = false,
                isRecurrent = true,
                recurrentFrequency = RecurrentFrequency.MONTHLY,
                subscriptionDay = 15,
                recurrentEndDate = LocalDateTime.now().plusMonths(6)
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
            onEdit = {},
            onDelete = {},
            onMarkAsPaid = {},
            readOnly = false,
        )
    }
}
