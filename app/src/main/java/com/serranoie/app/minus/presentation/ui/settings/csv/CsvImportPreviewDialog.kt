package com.serranoie.app.minus.presentation.ui.settings.csv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.data.csv.CsvImportPreview
import com.serranoie.app.minus.presentation.ui.settings.CsvImportDialog
import com.serranoie.app.minus.presentation.util.font.format.formatCurrency
import com.serranoie.app.minus.presentation.util.font.format.formatLocalized

@Composable
fun CsvImportPreviewDialog(
    dialog: CsvImportDialog?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit
        CsvImportDialog.Unreadable -> AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    stringResource(R.string.csv_import_unreadable_title),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                )
            },
            text = {
                Text(
                    stringResource(R.string.csv_import_unreadable_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.ok), style = MaterialTheme.typography.labelMediumEmphasized)
                }
            },
        )
        is CsvImportDialog.Preview -> AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    stringResource(R.string.csv_import_preview_title),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                )
            },
            text = { PreviewSummary(dialog.summary) },
            confirmButton = {
                Button(onClick = onConfirm, enabled = !dialog.summary.blocked) {
                    Text(
                        stringResource(R.string.csv_import_preview_confirm),
                        style = MaterialTheme.typography.labelMediumEmphasized,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), style = MaterialTheme.typography.labelMediumEmphasized)
                }
            },
        )
    }
}

@Composable
private fun PreviewSummary(summary: CsvImportPreview) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val start = summary.periodStart
        val end = summary.periodEnd
        if (start != null && end != null) {
            SummaryRow(
                stringResource(R.string.csv_import_preview_period),
                "${start.formatLocalized("d MMM")} – ${end.formatLocalized("d MMM yyyy")}",
            )
        }
        summary.budgetTotal?.let {
            SummaryRow(stringResource(R.string.csv_import_preview_budget), formatCurrency(it, summary.currencyCode))
        }
        SummaryRow(
            stringResource(
                if (summary.replacesAllData) {
                    R.string.csv_import_preview_transactions
                } else {
                    R.string.csv_import_preview_transactions_to_add
                },
            ),
            summary.currentPeriodTransactions.toString(),
        )
        if (summary.replacesAllData) {
            SummaryRow(stringResource(R.string.csv_import_preview_completed), summary.completedPeriods.toString())
        }
        SummaryRow(stringResource(R.string.csv_import_preview_recurring), summary.recurringPayments.toString())
        if (summary.discarded > 0) {
            SummaryRow(stringResource(R.string.csv_import_preview_discarded), summary.discarded.toString())
        }
        Text(
            text = stringResource(
                when {
                    summary.blocked -> R.string.csv_import_preview_blocked
                    summary.replacesAllData -> R.string.csv_import_preview_replace_warning
                    else -> R.string.csv_import_preview_append
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = if (summary.blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
