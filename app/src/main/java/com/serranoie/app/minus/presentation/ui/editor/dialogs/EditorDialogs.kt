package com.serranoie.app.minus.presentation.ui.editor.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import logcat.logcat

private const val TAG = "CreditCutoffDayDialog"

@Composable
fun CreditCutoffDayDialog(
    initialDay: Int = 15,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var cutoffDayInput by remember(initialDay) { 
        logcat(TAG) { "Opening dialog with initialDay=$initialDay" }
        mutableStateOf(initialDay.toString()) 
    }
    val cutoffDay = cutoffDayInput.toIntOrNull()
    val isValid = cutoffDay != null && cutoffDay in 1..31
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = {
            logcat(TAG) { "Dismissed by onDismissRequest" }
            onDismiss()
        },
        title = {
            Text(
                text = stringResource(R.string.credit_cutoff_dialog_title),
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.credit_cutoff_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = cutoffDayInput,
                    onValueChange = { value ->
                        if (value.length <= 2 && value.all { it.isDigit() }) {
                            logcat(TAG) { "Input changed: '$cutoffDayInput' -> '$value'" }
                            cutoffDayInput = value
                        }
                    },
                    label = { Text(stringResource(R.string.credit_cutoff_dialog_label)) },
                    singleLine = true,
                    isError = cutoffDayInput.isNotBlank() && !isValid,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValid) {
                                logcat(TAG) { "Confirmed via keyboard Done action: $cutoffDay" }
                                onConfirm(cutoffDay!!)
                                focusManager.clearFocus()
                            }
                        }
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (isValid) {
                        logcat(TAG) { "Confirmed via Save button: $cutoffDay" }
                        onConfirm(cutoffDay!!)
                    }
                }, 
                enabled = isValid
            ) {
                Text(
                    stringResource(R.string.save),
                    style = MaterialTheme.typography.labelSmallEmphasized,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                logcat(TAG) { "Dismissed via Cancel button" }
                onDismiss()
            }) {
                Text(
                    stringResource(R.string.cancel),
                    style = MaterialTheme.typography.labelSmallEmphasized,
                )
            }
        },
    )
}
