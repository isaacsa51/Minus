package com.serranoie.app.minus.presentation.ui.history.sections

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.time.CalendarSystem
import com.serranoie.app.minus.presentation.ui.history.HistorySearchFilter
import com.serranoie.app.minus.presentation.ui.history.HistoryTransactionKind
import com.serranoie.app.minus.presentation.ui.theme.component.date.JalaliDateRangePicker
import com.serranoie.app.minus.presentation.util.font.format.formatLocalized
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

private const val MILLIS_PER_DAY = 86_400_000L
private const val SKELETON_ROWS = 4
private const val SKELETON_PULSE_MILLIS = 700
private const val SKELETON_MIN_ALPHA = 0.45f

@Composable
internal fun rememberLatestDismiss(onDismiss: () -> Unit): () -> Unit {
    val latest by rememberUpdatedState(onDismiss)
    return remember { { latest() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryFilterSheet(
    filter: HistorySearchFilter,
    categories: List<Category>,
    onFilterChange: (HistorySearchFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    var pickingDates by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = rememberLatestDismiss(onDismiss),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        HistoryFilterSheetContent(
            filter = filter,
            categories = categories,
            onFilterChange = onFilterChange,
            onPickDates = { pickingDates = true },
            modifier = Modifier.navigationBarsPadding(),
        )
    }
    if (pickingDates) {
        HistoryFilterDateDialog(
            start = filter.startDate,
            end = filter.endDate,
            onConfirm = { start, end ->
                onFilterChange(filter.copy(startDate = start, endDate = end))
                pickingDates = false
            },
            onDismiss = { pickingDates = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HistoryFilterSheetContent(
    filter: HistorySearchFilter,
    categories: List<Category>,
    onFilterChange: (HistorySearchFilter) -> Unit,
    onPickDates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.history_filter_title),
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onFilterChange(HistorySearchFilter()) }, enabled = filter.isActive) {
                Text(stringResource(R.string.history_filter_reset))
            }
        }

        FilterSection(R.string.history_filter_search_in) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterToggleChip(
                    label = stringResource(R.string.history_filter_descriptions),
                    selected = filter.searchDescriptions,
                    enabled = filter.searchCategoryNames,
                    onClick = { onFilterChange(filter.copy(searchDescriptions = !filter.searchDescriptions)) },
                )
                FilterToggleChip(
                    label = stringResource(R.string.history_filter_category_names),
                    selected = filter.searchCategoryNames,
                    enabled = filter.searchDescriptions,
                    onClick = { onFilterChange(filter.copy(searchCategoryNames = !filter.searchCategoryNames)) },
                )
            }
        }

        FilterSection(R.string.history_filter_type) {
            KindToggleGroup(kind = filter.kind, onKindChange = { onFilterChange(filter.copy(kind = it)) })
        }

        FilterSection(R.string.history_filter_amount) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AmountField(
                    label = stringResource(R.string.history_filter_amount_min),
                    value = filter.minAmount,
                    onValueChange = { onFilterChange(filter.copy(minAmount = it)) },
                    modifier = Modifier.weight(1f),
                )
                AmountField(
                    label = stringResource(R.string.history_filter_amount_max),
                    value = filter.maxAmount,
                    onValueChange = { onFilterChange(filter.copy(maxAmount = it)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        FilterSection(R.string.history_filter_dates) {
            val hasDates = filter.startDate != null || filter.endDate != null
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = hasDates,
                    onClick = onPickDates,
                    label = { Text(dateRangeLabel(filter.startDate, filter.endDate)) },
                    leadingIcon = { Icon(Icons.Rounded.DateRange, contentDescription = null, Modifier.size(18.dp)) },
                )
                if (hasDates) {
                    TextButton(onClick = { onFilterChange(filter.copy(startDate = null, endDate = null)) }) {
                        Text(stringResource(R.string.history_filter_clear_dates))
                    }
                }
            }
        }

        FilterSection(R.string.history_filter_only) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterToggleChip(
                    label = stringResource(R.string.history_filter_recurring),
                    selected = filter.recurringOnly,
                    onClick = { onFilterChange(filter.copy(recurringOnly = !filter.recurringOnly)) },
                )
                FilterToggleChip(
                    label = stringResource(R.string.history_filter_credit),
                    selected = filter.creditOnly,
                    onClick = { onFilterChange(filter.copy(creditOnly = !filter.creditOnly)) },
                )
            }
        }

        if (categories.isNotEmpty()) {
            FilterSection(R.string.history_filter_categories) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        val selected = category.id in filter.categoryIds
                        FilterToggleChip(
                            label = category.name,
                            selected = selected,
                            onClick = {
                                val ids = if (selected) filter.categoryIds - category.id else filter.categoryIds + category.id
                                onFilterChange(filter.copy(categoryIds = ids))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(title: Int, content: @Composable () -> Unit) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
    content()
}

@Composable
private fun FilterToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled || !selected,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun KindToggleGroup(kind: HistoryTransactionKind, onKindChange: (HistoryTransactionKind) -> Unit) {
    val labels = listOf(
        R.string.history_filter_type_all,
        R.string.history_filter_type_expenses,
        R.string.history_filter_type_income,
    )
    val kinds = HistoryTransactionKind.entries
    Row(
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        modifier = Modifier.fillMaxWidth(),
    ) {
        kinds.forEachIndexed { index, option ->
            ToggleButton(
                checked = kind == option,
                onCheckedChange = { onKindChange(option) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                contentPadding = PaddingValues(horizontal = 4.dp),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    kinds.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(stringResource(labels[index]), maxLines = 1)
            }
        }
    }
}

@Composable
private fun AmountField(
    label: String,
    value: BigDecimal?,
    onValueChange: (BigDecimal?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(value?.toPlainString().orEmpty()) }
    LaunchedEffect(value) {
        val parsed = text.toFilterAmount()
        val same = if (parsed == null || value == null) parsed == value else parsed.compareTo(value) == 0
        if (!same) text = value?.toPlainString().orEmpty()
    }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val cleaned = input.filter { it.isDigit() || it == '.' || it == ',' }
            text = cleaned
            onValueChange(cleaned.toFilterAmount())
        },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

private fun String.toFilterAmount(): BigDecimal? = replace(',', '.').toBigDecimalOrNull()

@Composable
private fun dateRangeLabel(start: LocalDate?, end: LocalDate?): String {
    if (start == null && end == null) return stringResource(R.string.history_filter_any_date)
    val locale = Locale.getDefault()
    val format = { date: LocalDate? -> date?.formatLocalized("d MMM yyyy", locale) ?: "…" }
    return stringResource(R.string.history_filter_date_range, format(start), format(end))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryFilterDateDialog(
    start: LocalDate?,
    end: LocalDate?,
    onConfirm: (LocalDate?, LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val persian = CalendarSystem.current() == CalendarSystem.PERSIAN
    var persianRange by remember { mutableStateOf(start to end) }
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = start?.toEpochDay()?.times(MILLIS_PER_DAY),
        initialSelectedEndDateMillis = end?.toEpochDay()?.times(MILLIS_PER_DAY),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (persian) {
                    onConfirm(persianRange.first, persianRange.second ?: persianRange.first)
                } else {
                    val from = state.selectedStartDateMillis?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY) }
                    val to = state.selectedEndDateMillis?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY) }
                    onConfirm(from, to ?: from)
                }
            }) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    ) {
        if (persian) {
            JalaliDateRangePicker(
                startDate = persianRange.first,
                endDate = persianRange.second,
                onSelectionChange = { from, to -> persianRange = from to to },
                modifier = Modifier.weight(1f),
            )
        } else {
            DateRangePicker(
                state = state,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun HistorySearchSkeleton(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "historySearchSkeleton").animateFloat(
        initialValue = 1f,
        targetValue = SKELETON_MIN_ALPHA,
        animationSpec = infiniteRepeatable(tween(SKELETON_PULSE_MILLIS), RepeatMode.Reverse),
        label = "historySearchSkeletonAlpha",
    )
    val bone = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .alpha(pulse),
    ) {
        Box(
            Modifier
                .padding(start = 8.dp, top = 8.dp)
                .width(140.dp)
                .height(14.dp)
                .background(bone, CircleShape),
        )
        repeat(SKELETON_ROWS) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                    .padding(16.dp),
            ) {
                Box(Modifier.size(40.dp).background(bone, CircleShape))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.6f).height(14.dp).background(bone, CircleShape))
                    Box(Modifier.fillMaxWidth(0.35f).height(12.dp).background(bone, CircleShape))
                }
                Spacer(Modifier.width(16.dp))
                Box(Modifier.width(56.dp).height(16.dp).background(bone, CircleShape))
            }
        }
    }
}
