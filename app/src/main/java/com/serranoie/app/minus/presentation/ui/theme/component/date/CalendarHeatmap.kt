package com.serranoie.app.minus.presentation.ui.theme.component.date

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.colorBad
import com.serranoie.app.minus.presentation.ui.theme.colorGood
import com.serranoie.app.minus.presentation.ui.theme.colorNotGood
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.getWeek
import com.serranoie.app.minus.presentation.util.prettyWeekDay
import com.serranoie.app.minus.presentation.util.prettyYearMonth
import com.serranoie.app.minus.presentation.util.toDate
import com.serranoie.app.minus.presentation.util.toLocalDate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Date
import java.util.Locale

data class SpendingDay(
    val date: Date,
    val spends: List<Transaction>,
    val budget: BigDecimal,
    val spending: BigDecimal,
)

@Composable
fun CalendarHeatmap(
    modifier: Modifier = Modifier,
    budget: BigDecimal,
    transactions: List<Transaction>,
    startDate: Date,
    finishDate: Date,
    actualFinishDate: Date? = null,
    onDayClick: (LocalDate) -> Unit = {},
) {
    val context = LocalContext.current
    val spendingDays = remember(transactions) {
        val days: MutableMap<LocalDate, SpendingDay> = mutableMapOf()
        val groupedByDate = transactions.filter { it.date != null && !it.isDeleted }
            .groupBy { it.date!!.toLocalDate() }

        groupedByDate.forEach { (date, txs) ->
            val totalSpending = txs.sumOf { it.amount }
            days[date] = SpendingDay(
                date = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                spends = txs,
                budget = budget,
                spending = totalSpending,
            )
        }

        days
    }
    val maxSpendsPerDay = remember(spendingDays) {
        spendingDays.values.maxOfOrNull { it.spends.size }?.coerceAtLeast(1) ?: 1
    }

    val calendarState = remember(startDate, finishDate, actualFinishDate) {
        CalendarState(
            context = context,
            disableBeforeDate = startDate,
            disableAfterDate = actualFinishDate ?: finishDate,
        )
    }

    Card(
        modifier = modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(
            containerColor = combineColors(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant,
                t = 0.3f,
            ),
        )
    ) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
            Icon(
                modifier = Modifier
                    .padding(top = 0.5.dp)
                    .size(14.dp),
                imageVector = Icons.Rounded.Info,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                modifier = Modifier.basicMarquee(),
                text = stringResource(R.string.calendar_heatmap_info_text),
                style = MaterialTheme.typography.labelSmallCondensed.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f),
                    fontWeight = FontWeight.W100,
                ),
            )
        }
        Layout(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            measurePolicy = verticalGridMeasurePolicy(7),
            content = {
                val calendarUiState = calendarState.calendarUiState.value

                calendarState.listMonths.forEach { month ->
                    MonthHeader(
                        modifier = Modifier.layoutId("fullWidth"), yearMonth = month.yearMonth
                    )

                    DaysOfWeek(
                        modifier = Modifier.layoutId("fullWidth")
                    )

                    month.weeks.forEach { week ->
                        val beginningWeek = week.yearMonth.atDay(1).plusWeeks(week.number.toLong())
                        val currentDay =
                            beginningWeek.with(TemporalAdjusters.previousOrSame(getWeek()[0]))

                        if (currentDay.plusDays(6)
                                .isAfter(calendarUiState.disabledBefore) && currentDay.isBefore(
                                calendarUiState.disabledAfter!!.plusDays(1)
                            )
                        ) {
                            WeekRow(
                                modifier = Modifier.layoutId("fullWidth"),
                                week = week,
                                calendarUiState = calendarUiState,
                                spendingDays = spendingDays,
                                budget = budget,
                                maxSpendsPerDay = maxSpendsPerDay,
                                onDayClick = onDayClick,
                            )
                        }
                    }
                }
            })

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp, bottom = 12.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            HeatmapLegend()
        }
    }

}

@Composable
private fun HeatmapLegend() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.heatmap_legend_less),
            style = MaterialTheme.typography.labelSmallCondensed,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.width(4.dp))

        listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f).forEach { ratio ->
            val color = if (ratio == 0f) Color.Transparent else {
                val normalizedHeat = ratio.coerceIn(0f, 1.2f)
                val heatColor = when {
                    normalizedHeat <= 0.5f -> lerp(colorGood, colorNotGood, normalizedHeat / 0.5f)
                    normalizedHeat <= 1.0f -> lerp(
                        colorNotGood,
                        colorBad,
                        (normalizedHeat - 0.5f) / 0.5f
                    )

                    else -> colorBad
                }
                val heatAlpha = 0.25f + (normalizedHeat * 0.55f)
                heatColor.copy(alpha = heatAlpha)
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .padding(1.dp)
                    .background(color, shape = RoundedCornerShape(2.dp))
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.heatmap_legend_more),
            style = MaterialTheme.typography.labelSmallCondensed,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
internal fun MonthHeader(modifier: Modifier = Modifier, yearMonth: YearMonth) {
    Row(modifier = modifier.height(CELL_SIZE), verticalAlignment = Alignment.Bottom) {
        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            text = prettyYearMonth(yearMonth),
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

fun verticalGridMeasurePolicy(columns: Int) = MeasurePolicy { measurables, constraints ->
    val cellWidth = constraints.maxWidth / columns
    val cells = emptyList<Int>().toMutableList()
    var cellsCount = 0

    val placeables = measurables.mapIndexed { index, it ->
        cells.add(if (it.layoutId == "fullWidth") columns else 1)
        cellsCount += cells[index]

        it.measure(
            constraints.copy(
                maxWidth = cellWidth * cells[index],
            )
        )
    }


    layout(
        constraints.maxWidth,
        ((cellsCount + columns - 1) / columns) * CELL_SIZE.roundToPx(),
    ) {
        var cellsOffset = 0

        placeables.forEachIndexed { index, it ->
            val cellIndex = (cells.getOrNull(index - 1) ?: 0) + cellsOffset

            cellsOffset = cellIndex
            it.place(
                cellWidth * (cellIndex % columns), CELL_SIZE.roundToPx() * (cellIndex / columns), 0f
            )
        }
    }
}


@Composable
internal fun DaysOfWeek(modifier: Modifier = Modifier) {
    val week = getWeek()

    Row(modifier = modifier) {
        for (day in week) {
            DayOfWeekHeading(
                day = prettyWeekDay(day), modifier = Modifier.weight(1f)
            )
        }
    }
}


data class Week(
    val number: Int, val yearMonth: YearMonth
)

@Composable
fun WeekRow(
    modifier: Modifier = Modifier,
    week: Week,
    calendarUiState: CalendarUiState,
    spendingDays: Map<LocalDate, SpendingDay>,
    budget: BigDecimal,
    maxSpendsPerDay: Int,
    onDayClick: (LocalDate) -> Unit,
) {
    val beginningWeek = week.yearMonth.atDay(1).plusWeeks(week.number.toLong())
    var currentDay = beginningWeek.with(TemporalAdjusters.previousOrSame(getWeek()[0]))

    Box(modifier.fillMaxWidth()) {
        Row {
            for (i in 0..6) {
                if (currentDay.month == week.yearMonth.month) {
                    val spendingDay = spendingDays[currentDay]
                    val dayBudget = spendingDay?.budget ?: budget
                    val daySpending = spendingDay?.spending ?: BigDecimal.ZERO
                    val amountRatio = if (dayBudget > BigDecimal.ZERO) {
                        (daySpending.toFloat() / dayBudget.toFloat()).coerceAtLeast(0f)
                    } else 0f
                    val daySpendsCount = spendingDay?.spends?.size ?: 0
                    val countRatio = if (maxSpendsPerDay > 0) {
                        (daySpendsCount.toFloat() / maxSpendsPerDay.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val heatIntensity = (amountRatio * 0.6f + countRatio * 0.4f).coerceIn(0f, 1.4f)

                    DayCell(
                        modifier = Modifier.weight(1f),
                        calendarState = calendarUiState,
                        day = currentDay,
                        onDayClicked = onDayClick,
                        spendingRatio = heatIntensity,
                        hasSpending = spendingDay != null,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(CELL_SIZE)
                            .weight(1f)
                    )
                }
                currentDay = currentDay.plusDays(1)
            }
        }
    }
}

@Composable
internal fun DayOfWeekHeading(day: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(CELL_SIZE)
            .widthIn(min = CELL_SIZE)
            .fillMaxWidth()
            .background(Color.Transparent), contentAlignment = Alignment.Center
    ) {
        Text(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            text = day,
            style = MaterialTheme.typography.labelSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5F),
        )
    }
}

@Composable
private fun DayContainer(
    modifier: Modifier = Modifier,
    current: Boolean = false,
    disabled: Boolean = false,
    onSelect: () -> Unit = { },
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .height(CELL_SIZE)
            .widthIn(min = CELL_SIZE)
            .fillMaxWidth()
            .background(Color.Transparent)
            .clickable(
                onClick = { onSelect() },
                enabled = !disabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = false,
                    radius = CELL_SIZE / 2,
                )
            ), contentAlignment = Alignment.Center
    ) {
        if (current) {
            Box(
                modifier = modifier
                    .height(CELL_SIZE - 8.dp)
                    .width(CELL_SIZE - 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    )
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
internal fun Day(
    day: LocalDate,
    calendarState: CalendarUiState,
    onDayClicked: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val disabled =
        calendarState.disabledBefore?.let { day.isBefore(it) } == true || calendarState.disabledAfter?.let {
            day.isAfter(it)
        } == true
    val selected = !disabled
    val current = day == LocalDate.now()

    DayContainer(
        modifier = modifier,
        current = current,
        disabled = disabled,
        onSelect = { onDayClicked(day) },
    ) {

        Text(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center),
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when (true) {
                current -> MaterialTheme.colorScheme.onPrimaryContainer
                selected -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = if (disabled) 0.3F else 1F)
            },
        )
    }
}

@Composable
internal fun DayCell(
    day: LocalDate,
    calendarState: CalendarUiState,
    onDayClicked: (LocalDate) -> Unit,
    spendingRatio: Float,
    hasSpending: Boolean,
    modifier: Modifier = Modifier
) {
    val disabled =
        calendarState.disabledBefore?.let { day.isBefore(it) } == true || calendarState.disabledAfter?.let {
            day.isAfter(it)
        } == true
    val current = day == LocalDate.now()

    val normalizedHeat = spendingRatio.coerceIn(0f, 1.2f)
    val heatColor = when {
        normalizedHeat <= 0.5f -> lerp(colorGood, colorNotGood, normalizedHeat / 0.5f)
        normalizedHeat <= 1.0f -> lerp(colorNotGood, colorBad, (normalizedHeat - 0.5f) / 0.5f)
        else -> colorBad
    }
    val heatAlpha = when {
        normalizedHeat <= 0f -> 0f
        normalizedHeat <= 1f -> 0.25f + (normalizedHeat * 0.55f)
        else -> 0.85f
    }
    val backgroundColor = when {
        disabled || !hasSpending -> Color.Transparent
        else -> heatColor.copy(alpha = heatAlpha)
    }

    Box(
        modifier = modifier
            .size(CELL_SIZE)
            .padding(2.dp)
            .background(
                color = backgroundColor, shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                onClick = { onDayClicked(day) },
                enabled = !disabled,
            )
            .border(
                width = if (current) 2.dp else 1.dp,
                color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(4.dp)
            ), contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMediumCondensed,
            color = when {
                disabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                hasSpending && spendingRatio > 1.0f -> MaterialTheme.colorScheme.onError
                hasSpending -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

internal val CELL_SIZE = 32.dp

data class CalendarUiState(
    val disabledBefore: LocalDate? = null,
    val disabledAfter: LocalDate? = null,
)

data class Month(
    val yearMonth: YearMonth, val weeks: List<Week>
)

class CalendarState(
    context: Context,
    disableBeforeDate: Date,
    disableAfterDate: Date,
) {
    val calendarUiState = mutableStateOf(
        CalendarUiState(
            disabledBefore = disableBeforeDate.toLocalDate(),
            disabledAfter = disableAfterDate.toLocalDate(),
        )
    )

    val listMonths: List<Month> = generateMonths(disableBeforeDate, disableAfterDate)

    private fun generateMonths(startDate: Date, endDate: Date): List<Month> {
        val start = startDate.toLocalDate()
        val end = endDate.toLocalDate()
        val months = mutableListOf<Month>()

        var current = YearMonth.from(start)
        val endMonth = YearMonth.from(end)

        while (!current.isAfter(endMonth)) {
            val weeks = mutableListOf<Week>()
            val firstDayOfMonth = current.atDay(1)
            val lastDayOfMonth = current.atEndOfMonth()

            // Calculate week numbers in this month
            var currentWeekDay = firstDayOfMonth
            while (!currentWeekDay.isAfter(lastDayOfMonth)) {
                val weekNumber = currentWeekDay.get(
                    WeekFields.of(Locale.getDefault()).weekOfMonth()
                ) - 1
                if (weekNumber >= 0 && weeks.none { it.number == weekNumber && it.yearMonth == current }) {
                    weeks.add(Week(weekNumber, current))
                }
                currentWeekDay = currentWeekDay.plusDays(7)
            }

            months.add(Month(current, weeks.sortedBy { it.number }))
            current = current.plusMonths(1)
        }

        return months
    }

    fun isDisabledDay(day: LocalDate): Boolean {
        val state = calendarUiState.value
        return state.disabledBefore?.let { day.isBefore(it) } == true || state.disabledAfter?.let {
            day.isAfter(
                it
            )
        } == true
    }

    fun isCurrentDay(day: LocalDate): Boolean {
        return day == LocalDate.now()
    }

    fun isDateInSelectedPeriod(day: LocalDate): Boolean {
        return !isDisabledDay(day)
    }
}

@Preview(name = "Calendar Heatmap - Full 2 Weeks", widthDp = 420)
@Preview(
    name = "Calendar Heatmap - Full 2 Weeks Dark Mode",
    widthDp = 420,
    showSystemUi = false,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun PreviewCalendarHeatmap() {
    val start = LocalDate.now().minusDays(13)
    val mockTransactions = buildList {
        val countByDay = listOf(0, 1, 2, 3, 1, 4, 6, 8, 5, 2, 7, 3, 1, 4)
        val amountByDay = listOf(0, 12, 18, 26, 10, 45, 70, 130, 60, 22, 105, 35, 14, 52)

        countByDay.forEachIndexed { index, count ->
            repeat(count) { txIndex ->
                val dayAmount = amountByDay[index]
                val splitAmount = if (count > 0) {
                    BigDecimal(dayAmount).divide(
                        BigDecimal(count), 2, java.math.RoundingMode.HALF_UP
                    )
                } else {
                    BigDecimal.ZERO
                }
                add(
                    Transaction(
                        amount = splitAmount,
                        date = start.plusDays(index.toLong()).atTime(8 + (txIndex % 10), 0),
                        comment = "Mock #$index-$txIndex",
                    )
                )
            }
        }
    }

    MinusTheme {
        CalendarHeatmap(
            budget = BigDecimal(200),
            transactions = mockTransactions,
            startDate = start.toDate(),
            finishDate = start.plusDays(13).toDate(),
        )
    }
}
