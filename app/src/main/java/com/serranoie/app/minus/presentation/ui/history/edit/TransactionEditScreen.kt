package com.serranoie.app.minus.presentation.ui.history.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.editor.category.CategoryToolbar
import com.serranoie.app.minus.presentation.ui.editor.category.FocusController
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditMode
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditorState
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Numpad
import com.serranoie.app.minus.presentation.ui.theme.displayLargeCondensed
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Transaction as NumpadTransaction

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

    var editedAmount by remember { mutableStateOf(transaction.amount.toString()) }
    var editedComment by remember { mutableStateOf(transaction.comment) }
    var editedDate by remember {
        mutableStateOf(transaction.date?.toLocalDate() ?: LocalDate.now())
    }
    var editedTime by remember {
        mutableStateOf(transaction.date?.toLocalTime() ?: LocalTime.now())
    }

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

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRecurrentBottomSheet by remember { mutableStateOf(false) }

    val focusController = remember { FocusController() }

    var isCalculation by remember { mutableStateOf(false) }

    // Calculate dynamic target height for numpad to take 48% of screen height
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val targetNumpadHeight = screenHeight * 0.48f

    val baseTextStyle = MaterialTheme.typography.displayLargeCondensed.copy(
        fontWeight = FontWeight.W500
    )

    val editorState = remember(editedAmount) {
        EditorState(
            mode = EditMode.EDIT,
            rawSpentValue = editedAmount,
            stage = EditStage.EDIT_SPENT,
            currentSpent = editedAmount,
            currentComment = editedComment,
            editedTransaction = transaction.let {
                NumpadTransaction(
                    id = it.id,
                    amount = it.amount.toPlainString(),
                    comment = it.comment,
                    date = it.date?.atZone(ZoneId.systemDefault())?.toInstant()
                        ?.let { instant -> Date.from(instant) } ?: Date()
                )
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TransactionEditTopBar(
            isRecurrent = transaction.isRecurrent,
            onCancel = onCancel,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RecurrenceToggleButton(
                isRecurrent = isRecurrent,
                selectedFrequency = selectedFrequency,
                subscriptionDay = subscriptionDay,
                onClick = { showRecurrentBottomSheet = true },
            )

            TransactionDateTimeRow(
                date = editedDate,
                time = editedTime,
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            EditAmountDisplay(
                rawAmount = editedAmount,
                currencyFormat = currencyFormat,
                style = baseTextStyle,
                modifier = Modifier.fillMaxWidth(),
            )
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

        Numpad(
            modifier = Modifier
                .fillMaxWidth()
                .height(targetNumpadHeight),
            editorState = editorState,
            onNumberInput = { digit ->
                editedAmount = if (editedAmount == "0") {
                    digit.toString()
                } else {
                    editedAmount + digit.toString()
                }
            },
            onDotInput = {
                val lastChar = editedAmount.lastOrNull()
                if (editedAmount.isEmpty() || (lastChar != null && lastChar in "+-×÷")) {
                    editedAmount += "0."
                } else {
                    val lastOperatorIndex = editedAmount.indexOfLast { it in "+-×÷" }
                    val currentSegment = editedAmount.substring(lastOperatorIndex + 1)
                    if (!currentSegment.contains(".")) {
                        editedAmount += "."
                    }
                }
            },
            onBackspace = {
                editedAmount = editedAmount.dropLast(1).ifEmpty { "0" }
            },
            onBackspaceLongPress = {
                editedAmount = "0"
            },
            onApply = {
                val newAmount = editedAmount.toBigDecimalOrNull() ?: transaction.amount
                val frequency = if (isRecurrent) selectedFrequency else null
                val endDate = if (isRecurrent) recurrentEndDate else null
                val subDay = if (isRecurrent && selectedFrequency == RecurrentFrequency.MONTHLY) {
                    subscriptionDay
                } else {
                    null
                }

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
            isCalculation = isCalculation,
            onCalculationModeChanged = { isCalculation = it },
            onOperatorInput = { operator ->
                val lastChar = editedAmount.lastOrNull()
                if (editedAmount.isNotEmpty() && lastChar != null && lastChar !in "+-×÷" && lastChar != '.') {
                    editedAmount += operator.toString()
                }
            },
            onEqualsInput = {
                val result = evaluateCalculation(editedAmount)
                if (result != null) {
                    editedAmount = result
                }
            },
            onDelete = {
                onCancel()
            }
        )
    }

    if (showDatePicker) {
        EditDatePickerDialog(
            initialDate = editedDate,
            minDate = budgetStartDate,
            maxDate = budgetEndDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { selected ->
                editedDate = when {
                    selected.isBefore(budgetStartDate) -> budgetStartDate
                    selected.isAfter(budgetEndDate) -> budgetEndDate
                    else -> selected
                }
                showDatePicker = false
            },
        )
    }

    if (showTimePicker) {
        EditTimePickerDialog(
            initialHour = editedTime.hour,
            initialMinute = editedTime.minute,
            onDismiss = { showTimePicker = false },
            onTimeSelected = { hour, minute ->
                editedTime = LocalTime.of(hour, minute)
                showTimePicker = false
            },
        )
    }

    if (showRecurrentBottomSheet) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

        ModalBottomSheet(
            onDismissRequest = { showRecurrentBottomSheet = false },
            sheetState = sheetState
        ) {
            RecurrenceConfigSheet(
                isRecurrent = isRecurrent,
                selectedFrequency = selectedFrequency,
                subscriptionDay = subscriptionDay,
                recurrentEndDate = recurrentEndDate,
                onSaveConfiguration = { newIsRecurrent, newFrequency, newSubscriptionDay, newEndDate ->
                    isRecurrent = newIsRecurrent
                    selectedFrequency = newFrequency
                    subscriptionDay = newSubscriptionDay
                    recurrentEndDate = newEndDate
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showRecurrentBottomSheet = false
                    }
                }
            )
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
