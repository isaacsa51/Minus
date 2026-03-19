package com.serranoie.app.minus.presentation.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.editor.category.FocusController
import com.serranoie.app.minus.presentation.editor.category.CategoryToolbar
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditMode
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditorState
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Numpad
import com.serranoie.app.minus.presentation.util.prettyDate
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat

/**
 * Screen for editing a transaction.
 * Shows date/time picker, amount editor with numpad, and category input.
 * For recurring transactions, allows editing billing day, end date, and frequency via bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    transaction: Transaction,
    budgetStartDate: LocalDate,
    budgetEndDate: LocalDate,
    currencyCode: String = "USD",
    onCancel: () -> Unit = {},
    onSave: (
        newAmount: BigDecimal,
        newComment: String,
        newDateTime: LocalDateTime,
        newIsRecurrent: Boolean,
        newFrequency: RecurrentFrequency?,
        newEndDate: LocalDate?,
        newSubscriptionDay: Int?
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val currencyFormat = symbolOnlyCurrencyFormat(currencyCode)
    val scope = rememberCoroutineScope()

    // State for editing
    var editedAmount by remember { mutableStateOf(transaction.amount.toString()) }
    var editedComment by remember { mutableStateOf(transaction.comment) }
    var editedDate by remember { mutableStateOf(transaction.date?.toLocalDate() ?: LocalDate.now()) }
    var editedTime by remember { 
        mutableStateOf(transaction.date?.toLocalTime() ?: LocalTime.now()) 
    }

    // Recurring-specific state
    var isRecurrent by remember { mutableStateOf(transaction.isRecurrent) }
    var selectedFrequency by remember { 
        mutableStateOf(transaction.recurrentFrequency ?: RecurrentFrequency.MONTHLY) 
    }
    var subscriptionDay by remember { 
        mutableIntStateOf(transaction.subscriptionDay ?: transaction.date?.dayOfMonth ?: 1) 
    }
    var recurrentEndDate by remember { 
        mutableStateOf(transaction.recurrentEndDate?.toLocalDate() ?: budgetEndDate.plusMonths(3)) 
    }
    
    // Dialog/bottom sheet visibility states
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRecurrentBottomSheet by remember { mutableStateOf(false) }

    // Focus controller for tagging toolbar
    val focusController = remember { FocusController() }

    // Editor state for numpad
    val editorState = remember(editedAmount) {
        EditorState(
            mode = EditMode.EDIT,
            rawSpentValue = editedAmount,
            stage = EditStage.EDIT_SPENT,
            currentComment = editedComment
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top bar with cancel button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancelar edición",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = if (transaction.isRecurrent) "Editar gasto recurrente" else "Editar gasto",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Date and Time display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date picker button
            Text(
                text = prettyDate(
                    editedDate.atStartOfDay(),
                    forceShowDate = true,
                    showTime = false,
                    human = false
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showDatePicker = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )

            Text(
                text = "•",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Time picker button
            Text(
                text = String.format("%02d:%02d", editedTime.hour, editedTime.minute),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showTimePicker = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Amount display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            val formattedAmount = remember(editedAmount) {
                try {
                    val value = editedAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    currencyFormat.format(value)
                } catch (e: Exception) {
                    editedAmount
                }
            }

            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Recurring Configuration Button (only show if was already recurrent)
        if (transaction.isRecurrent) {
            Button(
                onClick = { showRecurrentBottomSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isRecurrent) "Configurar recurrencia" else "Hacer recurrente",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    if (isRecurrent) {
                        val freqText = when (selectedFrequency) {
                            RecurrentFrequency.WEEKLY -> "Semanal"
                            RecurrentFrequency.BIWEEKLY -> "Quincenal"
                            RecurrentFrequency.MONTHLY -> "Mensual"
                        }
                        Text(
                            text = freqText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            CategoryToolbar(
                tags = emptyList(),
                currentComment = editedComment,
                stage = EditStage.EDIT_SPENT,
                onCommentUpdate = { editedComment = it },
                editorFocusController = focusController,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Numpad
        Numpad(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f),
            editorState = editorState,
            onNumberInput = { digit ->
                // Handle number input - replace if currently "0"
                editedAmount = if (editedAmount == "0") {
                    digit.toString()
                } else {
                    editedAmount + digit.toString()
                }
            },
            onDotInput = {
                // Add decimal point if not already present
                if (!editedAmount.contains(".")) {
                    editedAmount = editedAmount + "."
                }
            },
            onBackspace = {
                // Remove last character
                editedAmount = editedAmount.dropLast(1).ifEmpty { "0" }
            },
            onBackspaceLongPress = {
                // Clear all
                editedAmount = "0"
            },
            onApply = {
                // Save the edited transaction with all recurring details
                val newAmount = editedAmount.toBigDecimalOrNull() ?: transaction.amount
                val frequency = if (isRecurrent) selectedFrequency else null
                val endDate = if (isRecurrent) recurrentEndDate else null
                val subDay = if (isRecurrent && selectedFrequency == RecurrentFrequency.MONTHLY) {
                    subscriptionDay
                } else null
                
                onSave(
                    newAmount,
                    editedComment,
                    editedDate.atTime(editedTime),
                    isRecurrent,
                    frequency,
                    endDate,
                    subDay
                )
            },
            onDelete = {
                // Just cancel in edit mode
                onCancel()
            }
        )
    }

    // Date Picker Dialog (for transaction date)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editedDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            // Ensure date is within budget period
                            editedDate = when {
                                selectedDate.isBefore(budgetStartDate) -> budgetStartDate
                                selectedDate.isAfter(budgetEndDate) -> budgetEndDate
                                else -> selectedDate
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = editedTime.hour,
            initialMinute = editedTime.minute
        )

        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Seleccionar hora",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    TimePicker(state = timePickerState)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancelar")
                        }

                        TextButton(
                            onClick = {
                                editedTime = LocalTime.of(
                                    timePickerState.hour,
                                    timePickerState.minute
                                )
                                showTimePicker = false
                            }
                        ) {
                            Text("Aceptar")
                        }
                    }
                }
            }
        }
    }
    
    // Recurring Configuration Bottom Sheet
    if (showRecurrentBottomSheet) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        
        ModalBottomSheet(
            onDismissRequest = { showRecurrentBottomSheet = false },
            sheetState = sheetState
        ) {
            RecurrentConfigBottomSheetContent(
                isRecurrent = isRecurrent,
                onIsRecurrentChange = { isRecurrent = it },
                selectedFrequency = selectedFrequency,
                onFrequencyChange = { selectedFrequency = it },
                subscriptionDay = subscriptionDay,
                onSubscriptionDayChange = { subscriptionDay = it },
                recurrentEndDate = recurrentEndDate,
                onEndDateChange = { recurrentEndDate = it },
                onDismiss = { 
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showRecurrentBottomSheet = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurrentConfigBottomSheetContent(
    isRecurrent: Boolean,
    onIsRecurrentChange: (Boolean) -> Unit,
    selectedFrequency: RecurrentFrequency,
    onFrequencyChange: (RecurrentFrequency) -> Unit,
    subscriptionDay: Int,
    onSubscriptionDayChange: (Int) -> Unit,
    recurrentEndDate: LocalDate,
    onEndDateChange: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var showFrequencyDropdown by remember { mutableStateOf(false) }
    var showDayDropdown by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Configurar recurrencia",
                style = MaterialTheme.typography.headlineSmall
            )
            
            TextButton(onClick = onDismiss) {
                Text("Listo")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Is Recurrent Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gasto recurrente",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = isRecurrent,
                onCheckedChange = onIsRecurrentChange
            )
        }
        
        if (isRecurrent) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Frequency Selection
            ExposedDropdownMenuBox(
                expanded = showFrequencyDropdown,
                onExpandedChange = { showFrequencyDropdown = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = when (selectedFrequency) {
                        RecurrentFrequency.WEEKLY -> "Semanal (cada 7 días)"
                        RecurrentFrequency.BIWEEKLY -> "Quincenal (cada 14 días)"
                        RecurrentFrequency.MONTHLY -> "Mensual"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frecuencia") },
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFrequencyDropdown) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                
                ExposedDropdownMenu(
                    expanded = showFrequencyDropdown,
                    onDismissRequest = { showFrequencyDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Semanal (cada 7 días)") },
                        onClick = {
                            onFrequencyChange(RecurrentFrequency.WEEKLY)
                            showFrequencyDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Quincenal (cada 14 días)") },
                        onClick = {
                            onFrequencyChange(RecurrentFrequency.BIWEEKLY)
                            showFrequencyDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Mensual") },
                        onClick = {
                            onFrequencyChange(RecurrentFrequency.MONTHLY)
                            showFrequencyDropdown = false
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Billing Day (for monthly)
            if (selectedFrequency == RecurrentFrequency.MONTHLY) {
                ExposedDropdownMenuBox(
                    expanded = showDayDropdown,
                    onExpandedChange = { showDayDropdown = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "$subscriptionDay",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Día de cobro") },
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
                                    onSubscriptionDayChange(day)
                                    showDayDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // End Date
            OutlinedTextField(
                value = recurrentEndDate.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Finaliza el") },
                trailingIcon = {
                    TextButton(onClick = { showEndDatePicker = true }) {
                        Text("Cambiar")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
    
    // End Date Picker Dialog
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = recurrentEndDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onEndDateChange(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            )
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun TransactionEditScreenPreview() {
    MinusTheme {
        TransactionEditScreen(
            transaction = Transaction(
                id = 1L,
                amount = BigDecimal("50.00"),
                comment = "ani",
                date = LocalDateTime.now(),
                isDeleted = false
            ),
            budgetStartDate = LocalDate.now().minusDays(15),
            budgetEndDate = LocalDate.now().plusDays(15),
            currencyCode = "USD",
            onCancel = {},
            onSave = { _, _, _, _, _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun TransactionEditScreenRecurringPreview() {
    MinusTheme {
        TransactionEditScreen(
            transaction = Transaction(
                id = 1L,
                amount = BigDecimal("99.00"),
                comment = "Netflix",
                date = LocalDateTime.now(),
                isDeleted = false,
                isRecurrent = true,
                recurrentFrequency = RecurrentFrequency.MONTHLY,
                subscriptionDay = 15,
                recurrentEndDate = LocalDateTime.now().plusMonths(6)
            ),
            budgetStartDate = LocalDate.now().minusDays(15),
            budgetEndDate = LocalDate.now().plusDays(15),
            currencyCode = "USD",
            onCancel = {},
            onSave = { _, _, _, _, _, _, _ -> }
        )
    }
}