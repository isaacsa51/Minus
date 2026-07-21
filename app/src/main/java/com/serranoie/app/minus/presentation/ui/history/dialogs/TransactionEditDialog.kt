package com.serranoie.app.minus.presentation.ui.history.dialogs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.edit.TransactionEditScreen
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
internal fun TransactionEditDialog(
    transaction: Transaction?,
    budgetStartDate: LocalDate,
    budgetEndDate: LocalDate,
    currencyCode: String,
    tags: List<String> = emptyList(),
    isCreditQuickToggleEnabled: Boolean = false,
    creditCardCutoffDay: Int? = null,
    onUpdateCreditCutoffDay: (Int) -> Unit = {},
    onCancel: () -> Unit,
    onSave: (Transaction) -> Unit,
) {
    if (transaction == null) return

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            TransactionEditScreen(
                transaction = transaction,
                budgetStartDate = budgetStartDate,
                budgetEndDate = budgetEndDate,
                currencyCode = currencyCode,
                tags = tags,
                isCreditQuickToggleEnabled = isCreditQuickToggleEnabled,
                creditCardCutoffDay = creditCardCutoffDay,
                onUpdateCreditCutoffDay = onUpdateCreditCutoffDay,
                onCancel = onCancel,
                onSave = { newAmount, newComment, newDateTime, newIsRecurrent, newFrequency, newEndDate, newSubscriptionDay, newIsCredit ->
                    val updatedTransaction = transaction.copy(
                        id = transaction.sourceTransactionId ?: transaction.id,
                        amount = newAmount,
                        comment = newComment,
                        date = newDateTime,
                        isRecurrent = newIsRecurrent,
                        recurrentFrequency = newFrequency,
                        recurrentEndDate = newEndDate?.atStartOfDay(),
                        subscriptionDay = newSubscriptionDay,
                        isCredit = newIsCredit,
                        sourceTransactionId = null,
                    )
                    onSave(updatedTransaction)
                },
            )
        }
    }
}

@Preview
@Composable
private fun PreviewTransactionEditDialog() {
    MinusTheme {
        TransactionEditDialog(
            transaction = Transaction(
                id = 1L,
                amount = BigDecimal("45.00"),
                comment = "Grocery run",
                date = LocalDateTime.of(2026, 6, 20, 10, 30),
            ),
            budgetStartDate = LocalDate.of(2026, 6, 1),
            budgetEndDate = LocalDate.of(2026, 6, 30),
            currencyCode = "USD",
            onCancel = {},
            onSave = {},
        )
    }
}
