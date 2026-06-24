package com.serranoie.app.minus.presentation.ui.history.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RecurrenceConfigSheet(
    isRecurrent: Boolean,
    selectedFrequency: RecurrentFrequency,
    subscriptionDay: Int,
    recurrentEndDate: LocalDate,
    onSaveConfiguration: (
        isRecurrent: Boolean, frequency: RecurrentFrequency, subscriptionDay: Int, endDate: LocalDate
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var showEndDatePicker by remember { mutableStateOf(false) }
    var localIsRecurrent by remember(isRecurrent) { mutableStateOf(isRecurrent) }
    var localSelectedFrequency by remember(selectedFrequency) { mutableStateOf(selectedFrequency) }
    var localSubscriptionDay by remember(subscriptionDay) { mutableIntStateOf(subscriptionDay) }
    var localRecurrentEndDate by remember(recurrentEndDate) { mutableStateOf(recurrentEndDate) }
    val today = LocalDate.now()
    val maxSelectableDate = today.plusMonths(12)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.configure_recurrence),
                style = MaterialTheme.typography.titleMediumEmphasized,
            )

            Switch(
                checked = localIsRecurrent, onCheckedChange = { localIsRecurrent = it })
        }

        if (localIsRecurrent) {
            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.recurrent_expense_frequency_subtitle),
                style = MaterialTheme.typography.labelMediumCondensed
            )

            val options = listOf(
                stringResource(R.string.recurrent_frequency_weekly),
                stringResource(R.string.recurrent_frequency_biweekly),
                stringResource(R.string.recurrent_frequency_monthly)
            )
            val frequencies = listOf(
                RecurrentFrequency.WEEKLY,
                RecurrentFrequency.BIWEEKLY,
                RecurrentFrequency.MONTHLY,
            )
            val selectedIndex = frequencies.indexOf(localSelectedFrequency).coerceAtLeast(0)

            Row(
                Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                val modifiers =
                    listOf(Modifier.weight(1f), Modifier.weight(1.5f), Modifier.weight(1f))

                options.forEachIndexed { index, label ->
                    ToggleButton(
                        checked = selectedIndex == index,
                        onCheckedChange = { localSelectedFrequency = frequencies[index] },
                        modifier = modifiers[index].semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMediumCondensed)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (localSelectedFrequency == RecurrentFrequency.MONTHLY) {
                MonthlySubscriptionDayRow(
                    day = localSubscriptionDay,
                    onDayChange = { localSubscriptionDay = it },
                )
                Spacer(Modifier.height(16.dp))
            }

            Text(
                text = stringResource(R.string.limit_date),
                style = MaterialTheme.typography.labelMediumCondensed,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = localRecurrentEndDate.format(dateFormatter),
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(stringResource(R.string.date_placeholder)) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            showEndDatePicker = true
                        })
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = Color.Transparent
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        text = buildRecurrentSummary(
                            frequency = localSelectedFrequency,
                            selectedDay = localSubscriptionDay,
                            selectedEndDate = localRecurrentEndDate,
                            formatter = dateFormatter,
                        ),
                        style = MaterialTheme.typography.bodySmallCondensed,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(), onClick = {
                onSaveConfiguration(
                    localIsRecurrent,
                    localSelectedFrequency,
                    localSubscriptionDay,
                    localRecurrentEndDate,
                )
                onDismiss()
            }) {
            Text(
                text = stringResource(R.string.save),
                style = MaterialTheme.typography.labelSmallEmphasized
            )
        }

        Spacer(Modifier.height(12.dp))
    }

    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localRecurrentEndDate.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli(), selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    return date.isAfter(today) && !date.isAfter(maxSelectableDate)
                }
            })

        DatePickerDialog(onDismissRequest = { showEndDatePicker = false }, confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        localRecurrentEndDate =
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                                .toLocalDate()
                    }
                    showEndDatePicker = false
                }) {
                Text(stringResource(R.string.accept))
            }
        }, dismissButton = {
            TextButton(onClick = { showEndDatePicker = false }) {
                Text(stringResource(R.string.cancel))
            }
        }) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun MonthlySubscriptionDayRow(
    day: Int,
    onDayChange: (Int) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onDayChange((day - 1).coerceAtLeast(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.previous_day),
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = stringResource(R.string.monthly_on_day_format, day),
                style = MaterialTheme.typography.titleSmallEmphasized
            )

            IconButton(onClick = { onDayChange((day + 1).coerceAtMost(31)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_day),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun buildRecurrentSummary(
    frequency: RecurrentFrequency,
    selectedDay: Int,
    selectedEndDate: LocalDate,
    formatter: DateTimeFormatter
): String {
    val formattedDate = selectedEndDate.format(formatter)
    return when (frequency) {
        RecurrentFrequency.WEEKLY -> stringResource(R.string.summary_weekly_format, formattedDate)
        RecurrentFrequency.BIWEEKLY -> stringResource(
            R.string.summary_biweekly_format, formattedDate
        )

        RecurrentFrequency.MONTHLY -> stringResource(
            R.string.summary_monthly_format, selectedDay, formattedDate
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun RecurrenceConfigSheetPreview() {
    MinusTheme {
        RecurrenceConfigSheet(
            isRecurrent = true,
            selectedFrequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 15,
            recurrentEndDate = LocalDate.now().plusMonths(6),
            onSaveConfiguration = { _, _, _, _ -> },
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecurrenceConfigSheetWeeklyPreview() {
    MinusTheme {
        RecurrenceConfigSheet(
            isRecurrent = true,
            selectedFrequency = RecurrentFrequency.WEEKLY,
            subscriptionDay = 1,
            recurrentEndDate = LocalDate.now().plusMonths(3),
            onSaveConfiguration = { _, _, _, _ -> },
            onDismiss = {},
        )
    }
}
