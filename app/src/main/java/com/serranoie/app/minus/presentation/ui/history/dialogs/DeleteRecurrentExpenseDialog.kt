package com.serranoie.app.minus.presentation.ui.history.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import java.math.BigDecimal
import java.time.LocalDateTime

@Composable
internal fun DeleteRecurrentExpenseDialog(
    transaction: Transaction?,
    onDismiss: () -> Unit,
    onConfirm: (Transaction) -> Unit,
) {
    if (transaction == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.delete_recurrent_expense_title),
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
        },
        text = {
            Column {
                val recurrentExpenseName = transaction.comment.ifEmpty {
                    stringResource(R.string.delete_recurrent_expense_fallback_name)
                }
                Text(
                    text = stringResource(
                        R.string.delete_recurrent_expense_message,
                        recurrentExpenseName,
                    ),
                    style = MaterialTheme.typography.bodyMediumCondensed,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.delete_recurrent_expense_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(transaction) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(
                    stringResource(R.string.delete),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelMediumEmphasized,
                )
            }
        },
    )
}

@Preview
@Composable
private fun PreviewDeleteRecurrentExpenseDialog() {
    MinusTheme {
        DeleteRecurrentExpenseDialog(
            transaction = Transaction(
                id = 1L,
                amount = BigDecimal("14.99"),
                comment = "Amazon Prime",
                date = LocalDateTime.of(2026, 7, 1, 0, 0),
                isRecurrent = true,
            ),
            onDismiss = {},
            onConfirm = {},
        )
    }
}

@Preview
@Composable
private fun PreviewDeleteRecurrentExpenseDialogUnnamed() {
    MinusTheme {
        DeleteRecurrentExpenseDialog(
            transaction = Transaction(
                id = 2L,
                amount = BigDecimal("9.99"),
                comment = "",
                date = LocalDateTime.of(2026, 7, 1, 0, 0),
                isRecurrent = true,
            ),
            onDismiss = {},
            onConfirm = {},
        )
    }
}
