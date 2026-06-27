package com.serranoie.app.minus.presentation.ui.editor.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R

@Composable
internal fun CreditCutoffDayDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var cutoffDayInput by remember { mutableStateOf("15") }
    val cutoffDay = cutoffDayInput.toIntOrNull()
    val isValid = cutoffDay != null && cutoffDay in 1..31

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.credit_cutoff_dialog_title),
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(R.string.credit_cutoff_dialog_message))
                OutlinedTextField(
                    value = cutoffDayInput,
                    onValueChange = { value ->
                        cutoffDayInput = value.filter { it.isDigit() }.take(2)
                    },
                    label = { Text(stringResource(R.string.credit_cutoff_dialog_label)) },
                    singleLine = true,
                    isError = cutoffDayInput.isNotBlank() && !isValid,
                )
            }
        },
        confirmButton = {
            Button(onClick = { cutoffDay?.let(onConfirm) }, enabled = isValid) {
                Text(
                    stringResource(R.string.save),
                    style = MaterialTheme.typography.labelSmallEmphasized,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelSmallEmphasized,
                )
            }
        },
    )
}
