package com.serranoie.app.minus.presentation.ui.editor.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.time.CalendarSystem
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.component.date.PersianDateCaption
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.font.format.formatCurrencySymbolOnly
import com.serranoie.app.minus.presentation.util.font.format.localizedFormatter
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecurrentExpenseDialog(
    budgetSettings: BudgetSettings?,
    onDismiss: () -> Unit,
    onConfirm: (frequency: RecurrentFrequency, endDate: LocalDate, subscriptionDay: Int?, fallbackComment: String) -> Unit,
    fallbackComments: Map<RecurrentFrequency, String>,
    modifier: Modifier = Modifier,
    budgetState: BudgetState? = null,
    amount: BigDecimal? = null,
    today: LocalDate = LocalDate.now(),
    initialFrequency: RecurrentFrequency = RecurrentFrequency.MONTHLY,
) {
    val budgetEndDate = budgetSettings?.getPeriodEndDate() ?: today.plusDays(30)

    var selectedFrequency by remember { mutableStateOf(initialFrequency) }
    var selectedDay by remember {
        mutableIntStateOf(CalendarSystem.current().dayOfMonth(today).coerceIn(MinSubscriptionDay, MaxSubscriptionDay))
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val defaultEndDate = remember(budgetEndDate) { budgetEndDate.plusMonths(3) }
    var selectedEndDate by remember { mutableStateOf(defaultEndDate) }
    var plannedDate by remember { mutableStateOf(today.plusDays(1)) }
    val isOnce = selectedFrequency == RecurrentFrequency.ONCE

    if (showDatePicker) {
        val maxSelectableDate = today.plusMonths(12)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (if (isOnce) plannedDate else selectedEndDate).atStartOfDay(ZoneOffset.UTC)
                .toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC)
                        .toLocalDate()
                    return date.isAfter(today) && !date.isAfter(maxSelectableDate)
                }
            }
        )

        DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        if (isOnce) plannedDate = picked else selectedEndDate = picked
                    }
                    showDatePicker = false
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }, dismissButton = {
            TextButton(
                onClick = { showDatePicker = false },
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        }) {
            DatePicker(state = datePickerState)
            PersianDateCaption(datePickerState.selectedDateMillis)
        }
    }

    val dateFormatter = remember { localizedFormatter("dd MMM yyyy", Locale.getDefault()) }

    AlertDialog(modifier = modifier, onDismissRequest = onDismiss, title = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(if (isOnce) R.string.planned_expense_title else R.string.recurrent_expense),
                style = MaterialTheme.typography.titleLargeEmphasized
            )
        }
    }, text = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                stringResource(R.string.recurrent_expense_frequency_subtitle),
                style = MaterialTheme.typography.labelMediumCondensed
            )

            val options = listOf(
                stringResource(R.string.recurrent_frequency_once),
                stringResource(R.string.recurrent_frequency_weekly),
                stringResource(R.string.recurrent_frequency_biweekly),
                stringResource(R.string.recurrent_frequency_monthly)
            )
            val frequencies = listOf(
                RecurrentFrequency.ONCE,
                RecurrentFrequency.WEEKLY,
                RecurrentFrequency.BIWEEKLY,
                RecurrentFrequency.MONTHLY,
            )
            val selectedIndex = frequencies.indexOf(selectedFrequency).coerceAtLeast(0)

            Row(
                Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                val modifiers =
                    listOf(Modifier.weight(1f), Modifier.weight(1f), Modifier.weight(1.25f), Modifier.weight(1.1f))

                options.forEachIndexed { index, label ->
                    ToggleButton(
                        checked = selectedIndex == index,
                        onCheckedChange = { selectedFrequency = frequencies[index] },
                        modifier = modifiers[index].semantics { role = Role.RadioButton },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        Text(label, modifier = Modifier.basicMarquee())
                    }
                }
            }

            if (selectedFrequency == RecurrentFrequency.MONTHLY) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { selectedDay-- },
                            enabled = selectedDay > MinSubscriptionDay,
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = stringResource(R.string.previous_day),
                            )
                        }

                        Text(
                            text = stringResource(R.string.monthly_on_day_format, selectedDay),
                            style = MaterialTheme.typography.titleMedium
                        )

                        IconButton(
                            onClick = { selectedDay++ },
                            enabled = selectedDay < MaxSubscriptionDay,
                            shapes = IconButtonDefaults.shapes(),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.next_day),
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(if (isOnce) R.string.planned_on_date_label else R.string.limit_date),
                style = MaterialTheme.typography.labelMediumCondensed,
            )

            OutlinedTextField(
                value = (if (isOnce) plannedDate else selectedEndDate).let(dateFormatter),
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(stringResource(R.string.date_placeholder)) },
                trailingIcon = {
                    IconButton(
                        onClick = { showDatePicker = true },
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = stringResource(R.string.change_date),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            val preview = if (isOnce) plannedPreview(budgetSettings, budgetState, amount, plannedDate) else null
            val isWarning = preview?.isNegative == true
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(
                    1.dp,
                    if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                ),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isWarning) MaterialTheme.colorScheme.errorContainer else Color.Transparent,
                    contentColor = if (isWarning) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
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
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isOnce) {
                                plannedSummary(preview, plannedDate, dateFormatter)
                            } else {
                                buildRecurrentSummary(
                                    frequency = selectedFrequency,
                                    selectedDay = selectedDay,
                                    selectedEndDate = selectedEndDate,
                                    formatter = dateFormatter,
                                )
                            },
                            style = MaterialTheme.typography.bodySmallCondensed,
                        )
                        if (isOnce) {
                            preview?.amounts()?.let { (before, after) ->
                                PlannedAmountChange(before, after, budgetSettings?.currencyCode.orEmpty())
                            }
                        }
                    }
                }
            }
        }
    }, confirmButton = {
        Button(
            shapes = ButtonDefaults.shapes(),
            onClick = {
                onConfirm(
                    selectedFrequency,
                    if (isOnce) plannedDate else selectedEndDate,
                    if (selectedFrequency == RecurrentFrequency.MONTHLY) selectedDay else null,
                    fallbackComments[selectedFrequency] ?: "",
                )
            }
        ) {
            Text(
                stringResource(R.string.save),
                style = MaterialTheme.typography.labelMediumEmphasized
            )
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
            Text(
                stringResource(R.string.cancel),
                style = MaterialTheme.typography.labelMediumEmphasized
            )
        }
    })
}

private const val MinSubscriptionDay = 1
private const val MaxSubscriptionDay = 28

@Composable
private fun buildRecurrentSummary(
    frequency: RecurrentFrequency,
    selectedDay: Int,
    selectedEndDate: LocalDate,
    formatter: (LocalDate) -> String
): String {
    val formattedDate = selectedEndDate.let(formatter)
    return when (frequency) {
        RecurrentFrequency.ONCE -> stringResource(R.string.summary_once_format, formattedDate)
        RecurrentFrequency.WEEKLY -> stringResource(R.string.summary_weekly_format, formattedDate)
        RecurrentFrequency.BIWEEKLY -> stringResource(
            R.string.summary_biweekly_format,
            formattedDate
        )

        RecurrentFrequency.MONTHLY -> stringResource(
            R.string.summary_monthly_format,
            selectedDay,
            formattedDate
        )
    }
}

@Composable
private fun plannedSummary(
    preview: PlannedPreview?,
    day: LocalDate,
    formatter: (LocalDate) -> String,
): String = when (preview) {
    is PlannedPreview.Daily, is PlannedPreview.LeftInPeriod -> stringResource(R.string.planned_preview_held)
    is PlannedPreview.OtherPeriod -> stringResource(R.string.planned_preview_other_period, formatter(preview.day))
    null -> stringResource(R.string.summary_once_format, formatter(day))
}

private fun PlannedPreview.amounts(): Pair<BigDecimal, BigDecimal>? = when (this) {
    is PlannedPreview.Daily -> before to after
    is PlannedPreview.LeftInPeriod -> before to after
    is PlannedPreview.OtherPeriod -> null
}

@Composable
private fun PlannedAmountChange(before: BigDecimal, after: BigDecimal, currencyCode: String) {
    val beforeText = formatCurrencySymbolOnly(before, currencyCode)
    val afterText = formatCurrencySymbolOnly(after, currencyCode)
    val description = stringResource(R.string.planned_preview_changes_to, beforeText, afterText)
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(beforeText, style = MaterialTheme.typography.titleSmall)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        Text(afterText, style = MaterialTheme.typography.titleSmall)
    }
}

@Preview(showBackground = true)
@Composable
private fun RecurrentExpenseDialogPreview() {
    MinusTheme {
        RecurrentExpenseDialog(
            budgetSettings = BudgetSettings(
                totalBudget = BigDecimal("500.00"),
                period = BudgetPeriod.MONTHLY,
                startDate = LocalDate.now(),
                currencyCode = "USD"
            ),
            onDismiss = {},
            onConfirm = { _, _, _, _ -> },
            fallbackComments = mapOf(
                RecurrentFrequency.WEEKLY to "Weekly subscription without name",
                RecurrentFrequency.BIWEEKLY to "Biweekly subscription without name",
                RecurrentFrequency.MONTHLY to "Monthly subscription without name",
            ),
        )
    }
}
