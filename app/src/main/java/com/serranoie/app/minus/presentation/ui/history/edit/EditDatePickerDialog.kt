package com.serranoie.app.minus.presentation.ui.history.edit

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.serranoie.app.minus.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditDatePickerDialog(
    initialDate: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate =
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        onDateSelected(selectedDate)
                    }
                }
            ) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }

    @Suppress("UNUSED_VARIABLE")
    val unused = listOf(minDate, maxDate)
}


@Preview(showBackground = true)
@Composable
private fun EditDatePickerDialogPreview() {
    MinusTheme {
        EditDatePickerDialog(
            initialDate = LocalDate.now(),
            minDate = LocalDate.now().minusDays(30),
            maxDate = LocalDate.now().plusDays(30),
            onDismiss = {},
            onDateSelected = {},
        )
    }
}
