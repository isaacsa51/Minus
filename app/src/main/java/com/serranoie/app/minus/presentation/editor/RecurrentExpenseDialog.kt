package com.serranoie.app.minus.presentation.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Dialog for configuring recurrent expense settings.
 * User selects the billing day (1-31) and the system automatically
 * determines the frequency and creates appropriate notifications.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrentExpenseDialog(
    budgetSettings: BudgetSettings?,
    onDismiss: () -> Unit,
    onConfirm: (frequency: RecurrentFrequency, endDate: LocalDate, subscriptionDay: Int?) -> Unit
) {
    val today = LocalDate.now()
    val budgetEndDate = budgetSettings?.getPeriodEndDate() ?: today.plusDays(30)
    val budgetStartDate = budgetSettings?.startDate ?: today

    var selectedFrequency by remember { mutableStateOf(RecurrentFrequency.MONTHLY) }

    var selectedDay by remember { mutableIntStateOf(today.dayOfMonth.coerceIn(1, 28)) }
    var showDayDropdown by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val defaultEndDate = remember(budgetEndDate) { budgetEndDate.plusMonths(3) }
    var selectedEndDate by remember { mutableStateOf(defaultEndDate) }

    if (showDatePicker) {
        val maxSelectableDate = today.plusMonths(12) // Allow up to 1 year
        
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                    return date.isAfter(today) && !date.isAfter(maxSelectableDate)
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedEndDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Agregar gasto recurrente",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Configura cómo se repetirá este gasto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Frecuencia", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurrentFrequency.entries.forEach { freq ->
                        val isSelected = selectedFrequency == freq
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFrequency = freq },
                            label = {
                                Text(
                                    when (freq) {
                                        RecurrentFrequency.WEEKLY -> "Semanal"
                                        RecurrentFrequency.BIWEEKLY -> "Quincenal"
                                        RecurrentFrequency.MONTHLY -> "Mensual"
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (selectedFrequency == RecurrentFrequency.MONTHLY) {
                    ExposedDropdownMenuBox(
                        expanded = showDayDropdown,
                        onExpandedChange = { showDayDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = "Día $selectedDay del mes",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Día de cobro") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDayDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = showDayDropdown,
                            onDismissRequest = { showDayDropdown = false }
                        ) {
                            (1..31).forEach { day ->
                                DropdownMenuItem(
                                    text = { Text("$day") },
                                    onClick = {
                                        selectedDay = day
                                        showDayDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text("Finaliza suscripción", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = selectedEndDate.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha de finalización") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    singleLine = true,
                    enabled = false
                )
                TextButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Seleccionar fecha")
                }

                // Summary / explanation card
                Surface(
                    color = MaterialTheme.colorScheme.surfaceDim,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Resumen",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildRecurrenceExplanation(
                                frequency = selectedFrequency,
                                selectedDay = selectedDay,
                                budgetStartDate = budgetSettings?.startDate ?: today,
                                budgetEndDate = budgetEndDate,
                                today = today
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedFrequency, selectedEndDate, selectedDay) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun buildRecurrenceExplanation(
    frequency: RecurrentFrequency,
    selectedDay: Int,
    budgetStartDate: LocalDate,
    budgetEndDate: LocalDate,
    today: LocalDate
): String {
    var firstChargeDate = today.withDayOfMonth(selectedDay.coerceAtMost(today.lengthOfMonth()))
    if (firstChargeDate.isBefore(today) || firstChargeDate.isEqual(today)) {
        firstChargeDate = firstChargeDate.plusMonths(1)
        val maxDay = firstChargeDate.lengthOfMonth()
        if (selectedDay > maxDay) {
            firstChargeDate = firstChargeDate.withDayOfMonth(maxDay)
        }
    }
    
    val isInCurrentPeriod = !firstChargeDate.isBefore(budgetStartDate) && !firstChargeDate.isAfter(budgetEndDate)
    val periodText = if (isInCurrentPeriod) "actual" else "siguiente"

    return when (frequency) {
        RecurrentFrequency.WEEKLY ->
            "Este gasto se cobrará cada 7 días comenzando desde el día $selectedDay. " +
                "Primer cobro: $firstChargeDate (en el período presupuestario $periodText)."
        RecurrentFrequency.BIWEEKLY ->
            "Este gasto se cobrará cada 14 días comenzando desde el día $selectedDay. " +
                "Primer cobro: $firstChargeDate (en el período presupuestario $periodText)."
        RecurrentFrequency.MONTHLY ->
            "Este gasto se cobrará mensualmente el día $selectedDay de cada mes. " +
                "Primer cobro: $firstChargeDate (en el período presupuestario $periodText)."
    }
}

@Preview(showBackground = true)
@Composable
fun RecurrentExpenseDialogPreview() {
    MinusTheme {
        RecurrentExpenseDialog(
            budgetSettings = BudgetSettings(
                totalBudget = BigDecimal("500.00"),
                period = BudgetPeriod.MONTHLY,
                startDate = LocalDate.now(),
                currencyCode = "USD"
            ),
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}
