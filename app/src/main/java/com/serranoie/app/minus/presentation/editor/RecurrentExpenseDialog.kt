package com.serranoie.app.minus.presentation.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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

    // Day of month selection (1-31) - always available
    var selectedDay by remember { mutableIntStateOf(today.dayOfMonth.coerceIn(1, 28)) }
    var showDayDropdown by remember { mutableStateOf(false) }
    
    // Calculate suggested frequency based on selected day and budget period
    val suggestedFrequency by remember(selectedDay, budgetStartDate, budgetEndDate, today) {
        derivedStateOf {
            calculateSuggestedFrequency(selectedDay, budgetStartDate, budgetEndDate, today)
        }
    }
    
    // End date - extend to ensure at least one billing cycle
    var showDatePicker by remember { mutableStateOf(false) }
    val defaultEndDate = remember(suggestedFrequency, selectedDay, budgetEndDate) {
        // Always extend end date to cover at least 3 months for any subscription
        // This ensures multiple billing cycles occur
        budgetEndDate.plusMonths(3)
    }
    var selectedEndDate by remember { mutableStateOf(defaultEndDate) }

    // Date picker dialog
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
            Text(
                text = "Configure Recurrent Expense",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Day of month selection - always shown as primary input
                Text(
                    text = "Billing Day of Month:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                ExposedDropdownMenuBox(
                    expanded = showDayDropdown,
                    onExpandedChange = { showDayDropdown = it }
                ) {
                    OutlinedTextField(
                        value = "$selectedDay",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day of month") },
                        trailingIcon = { 
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDayDropdown) 
                        },
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

                // Show calculated frequency and explanation
                Spacer(modifier = Modifier.height(8.dp))
                
                val frequency = suggestedFrequency
                val recurrenceText = when (frequency) {
                    RecurrentFrequency.WEEKLY -> "Weekly (every 7 days)"
                    RecurrentFrequency.BIWEEKLY -> "Biweekly (every 14 days)"
                    RecurrentFrequency.MONTHLY -> "Monthly"
                }
                
                Text(
                    text = "Recurrence: $recurrenceText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
                
                // User-friendly explanation
                val explanation = buildRecurrenceExplanation(
                    frequency = frequency,
                    selectedDay = selectedDay,
                    budgetStartDate = budgetStartDate,
                    budgetEndDate = budgetEndDate,
                    today = today
                )
                
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // End date selection
                Text(
                    text = "Subscription ends:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedEndDate.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "You can extend this date for longer subscriptions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val frequency = suggestedFrequency
                    // Pass subscriptionDay for monthly, null for weekly/biweekly
                    val subscriptionDay = if (frequency == RecurrentFrequency.MONTHLY) {
                        selectedDay
                    } else null
                    onConfirm(frequency, selectedEndDate, subscriptionDay) 
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Calculate the suggested frequency based on the selected billing day and budget period.
 * - If selected day is within next 7 days: Weekly
 * - If selected day is within next 14 days: Biweekly  
 * - Otherwise: Monthly
 */
private fun calculateSuggestedFrequency(
    selectedDay: Int,
    budgetStartDate: LocalDate,
    budgetEndDate: LocalDate,
    today: LocalDate
): RecurrentFrequency {
    // Calculate the actual date of the selected day in the current or next month
    var targetDate = today.withDayOfMonth(selectedDay.coerceAtMost(today.lengthOfMonth()))
    
    // If that day has already passed this month, move to next month
    if (targetDate.isBefore(today) || targetDate.isEqual(today)) {
        targetDate = targetDate.plusMonths(1)
        // Adjust for months with fewer days
        val maxDay = targetDate.lengthOfMonth()
        if (selectedDay > maxDay) {
            targetDate = targetDate.withDayOfMonth(maxDay)
        }
    }
    
    // Calculate days from today to the target billing date
    val daysUntil = ChronoUnit.DAYS.between(today, targetDate).toInt()
    
    return when {
        daysUntil <= 7 -> RecurrentFrequency.WEEKLY
        daysUntil <= 14 -> RecurrentFrequency.BIWEEKLY
        else -> RecurrentFrequency.MONTHLY
    }
}

/**
 * Build a user-friendly explanation of when the expense will occur.
 */
private fun buildRecurrenceExplanation(
    frequency: RecurrentFrequency,
    selectedDay: Int,
    budgetStartDate: LocalDate,
    budgetEndDate: LocalDate,
    today: LocalDate
): String {
    // Calculate when the first charge will happen
    var firstChargeDate = today.withDayOfMonth(selectedDay.coerceAtMost(today.lengthOfMonth()))
    if (firstChargeDate.isBefore(today) || firstChargeDate.isEqual(today)) {
        firstChargeDate = firstChargeDate.plusMonths(1)
        val maxDay = firstChargeDate.lengthOfMonth()
        if (selectedDay > maxDay) {
            firstChargeDate = firstChargeDate.withDayOfMonth(maxDay)
        }
    }
    
    val isInCurrentPeriod = !firstChargeDate.isBefore(budgetStartDate) && !firstChargeDate.isAfter(budgetEndDate)
    val periodText = if (isInCurrentPeriod) "current" else "next"
    
    return when (frequency) {
        RecurrentFrequency.WEEKLY -> 
            "This expense will charge every 7 days starting from day $selectedDay. " +
            "First charge: $firstChargeDate (in the $periodText budget period)."
        RecurrentFrequency.BIWEEKLY -> 
            "This expense will charge every 14 days starting from day $selectedDay. " +
            "First charge: $firstChargeDate (in the $periodText budget period)."
        RecurrentFrequency.MONTHLY -> 
            "This expense will charge monthly on day $selectedDay of each month. " +
            "First charge: $firstChargeDate (in the $periodText budget period)."
    }
}
