package com.serranoie.app.minus.presentation.ui.history.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.prettyDate
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.Locale

@Composable
internal fun TransactionDetailDialog(
    transaction: Transaction?,
    currencyFormat: NumberFormat,
    readOnly: Boolean,
    isDismissingTransactionDialog: Boolean,
    onDismissStart: () -> Unit,
    onDismiss: () -> Unit,
    onMarkAsPaid: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    if (transaction == null) return

    val withoutDate = stringResource(R.string.without_date)
    val descriptionLabel = stringResource(R.string.description)
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
        add(descriptionLabel to transaction.comment.ifEmpty { noName })
        add(dateLabel to transactionDateText)
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
                    )
                )
            }
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = EnterTransition.None,
        exit = ExitTransition.None,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (!isDismissingTransactionDialog) {
                            onDismissStart()
                            onDismiss()
                        }
                    },
            )
            TransactionDetailTicketCard(
                transaction = transaction,
                totalAmountText = currencyFormat.format(transaction.amount),
                details = details,
                onMarkAsPaid = onMarkAsPaid,
                onEdit = { onEdit(transaction) },
                onDelete = { onDelete(transaction) },
                readOnly = readOnly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .align(Alignment.Center),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewTransactionDetailDialog() {
    MinusTheme {
        TransactionDetailDialog(
            transaction = Transaction(
                id = 1L,
                amount = BigDecimal("25.99"),
                comment = "Netflix",
                date = LocalDateTime.of(2026, 6, 18, 14, 30),
                isRecurrent = false,
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
            readOnly = false,
            isDismissingTransactionDialog = false,
            onDismissStart = {},
            onDismiss = {},
            onMarkAsPaid = {},
            onEdit = {},
            onDelete = {},
        )
    }
}

@Preview
@Composable
private fun PreviewTransactionDetailDialogRecurrent() {
    MinusTheme {
        TransactionDetailDialog(
            transaction = Transaction(
                id = 2L,
                amount = BigDecimal("9.99"),
                comment = "A weird montly subscription that has a loong value as a name",
                date = LocalDateTime.of(2026, 7, 1, 8, 0),
                isRecurrent = true,
                recurrentFrequency = RecurrentFrequency.MONTHLY,
                recurrentEndDate = null,
                subscriptionDay = 1,
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
            readOnly = true,
            isDismissingTransactionDialog = false,
            onDismissStart = {},
            onDismiss = {},
            onMarkAsPaid = {},
            onEdit = {},
            onDelete = {},
        )
    }
}
