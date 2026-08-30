package com.serranoie.app.minus.presentation.ui.theme.component.date

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.isRoundedFontEnabled
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorBad
import com.serranoie.app.minus.presentation.ui.theme.colorGood
import com.serranoie.app.minus.presentation.ui.theme.colorNotGood
import com.serranoie.app.minus.presentation.ui.theme.googleSansFlex
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.getWeek
import com.serranoie.app.minus.presentation.util.font.format.prettyWeekDay
import com.serranoie.app.minus.presentation.util.font.format.prettyYearMonth
import com.serranoie.app.minus.presentation.util.font.format.toDate
import com.serranoie.app.minus.presentation.util.font.format.toLocalDate
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
    val intensity: Float = 0f,
)

data class Week(
    val startDate: LocalDate,
    val yearMonth: YearMonth
)

data class Month(
    val yearMonth: YearMonth,
    val weeks: List<Week>
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
    val cardContainerColor = combineColors(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant,
        t = 0.3f,
    )
    val spendingDays = remember(transactions, budget) {
        val days: MutableMap<LocalDate, SpendingDay> = mutableMapOf()
        val groupedByDate = transactions.filter { it.date != null && !it.isDeleted }
            .groupBy { it.date!!.toLocalDate() }

        val maxSpendsCount = groupedByDate.values.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1

        groupedByDate.forEach { (date, txs) ->
            val totalSpending = txs.sumOf { it.amount }

            val amountRatio = if (budget > BigDecimal.ZERO) {
                (totalSpending.toFloat() / budget.toFloat()).coerceAtLeast(0f)
            } else 0f
            val countRatio = (txs.size.toFloat() / maxSpendsCount.toFloat()).coerceIn(0f, 1f)
            val intensity = (amountRatio * 0.6f + countRatio * 0.4f).coerceIn(0f, 1.4f)

            days[date] = SpendingDay(
                date = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                spends = txs,
                budget = budget,
                spending = totalSpending,
                intensity = intensity
            )
        }

        days
    }
    val maxSpending = remember(spendingDays) {
        spendingDays.values.maxOfOrNull { it.spending } ?: BigDecimal.ZERO
    }

    val locale = LocalConfiguration.current.locales[0]
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }

    val calendarState = remember(startDate, finishDate, actualFinishDate, locale) {
        CalendarState(
            context = context,
            disableBeforeDate = startDate,
            disableAfterDate = actualFinishDate ?: finishDate,
            locale = locale
        )
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor,
        )
    ) {
        Layout(
            modifier = Modifier.padding(18.dp),
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
                        val currentDay = week.startDate
                        val endOfWeek = currentDay.plusDays(6)
                        val periodStart = calendarUiState.disabledBefore
                        val periodEnd = calendarUiState.disabledAfter

                        if (periodStart != null && periodEnd != null &&
                            !endOfWeek.isBefore(periodStart) &&
                            !currentDay.isAfter(periodEnd)
                        ) {
                            WeekRow(
                                modifier = Modifier.layoutId("fullWidth"),
                                week = week,
                                calendarUiState = calendarUiState,
                                spendingDays = spendingDays,
                                maxSpending = maxSpending,
                                cardContainerColor = cardContainerColor,
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
            val normalizedHeat = ratio.coerceIn(0f, 1.2f)
            val boxScale = calculateHeatScale(normalizedHeat)

            val color = if (ratio == 0f) Color.Transparent else {
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
                    .zIndex(normalizedHeat)
                    .scale(boxScale)
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

@Composable
fun WeekRow(
    modifier: Modifier = Modifier,
    week: Week,
    calendarUiState: CalendarUiState,
    spendingDays: Map<LocalDate, SpendingDay>,
    maxSpending: BigDecimal,
    cardContainerColor: Color,
    onDayClick: (LocalDate) -> Unit,
) {
    var currentDay = week.startDate

    Box(modifier.fillMaxWidth()) {
        Row {
            for (i in 0..6) {
                if (currentDay.month == week.yearMonth.month) {
                    val spendingDay = spendingDays[currentDay]
                    val heatIntensity = spendingDay?.intensity ?: 0f

                    DayCell(
                        modifier = Modifier.weight(1f),
                        calendarState = calendarUiState,
                        day = currentDay,
                        onDayClicked = onDayClick,
                        spendingRatio = heatIntensity,
                        hasSpending = spendingDay != null,
                        isHighestDay = spendingDay?.spending == maxSpending && maxSpending > BigDecimal.ZERO,
                        cardContainerColor = cardContainerColor,
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
    cardContainerColor: Color,
    modifier: Modifier = Modifier,
    isHighestDay: Boolean = false,
) {
    val context = LocalContext.current
    val isRounded = remember(context) { context.isRoundedFontEnabled }

    val disabled =
        calendarState.disabledBefore?.let { day.isBefore(it) } == true || calendarState.disabledAfter?.let {
            day.isAfter(it)
        } == true
    val current = day == LocalDate.now()

    val animatedHeat by animateFloatAsState(
        targetValue = spendingRatio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "HeatBounce"
    )

    val normalizedHeat = animatedHeat.coerceIn(0f, 1.2f)
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

    val backgroundScale = remember(normalizedHeat, isHighestDay) {
        if (isHighestDay) calculateHeatScale(normalizedHeat).coerceAtLeast(1.15f) else 1.0f
    }

    val baseStyle = MaterialTheme.typography.bodyMedium
    val dynamicStyle = remember(animatedHeat, isRounded, isHighestDay) {
        if (isHighestDay) {
            calculateDynamicHeatStyle(animatedHeat, isRounded, baseStyle)
        } else {
            baseStyle
        }
    }

    Box(
        modifier = modifier
            .size(CELL_SIZE)
            .zIndex(normalizedHeat)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onDayClicked(day) },
                enabled = !disabled,
            ),
        contentAlignment = Alignment.Center
    ) {
        val effectiveScale = if (hasSpending && !disabled) backgroundScale else 1.0f

        if (hasSpending && !disabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .scale(effectiveScale)
                    .background(
                        color = backgroundColor, shape = MaterialTheme.shapes.extraSmall
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .border(
                    width = if (current) 2.dp else 1.dp,
                    color = if (current) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                    },
                    shape = MaterialTheme.shapes.extraSmall
                )
        )

        Text(
            text = day.dayOfMonth.toString(),
            style = dynamicStyle,
            fontStyle = if (disabled) FontStyle.Italic else FontStyle.Normal,
            color = when {
                disabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                hasSpending && spendingRatio > 1.0f -> MaterialTheme.colorScheme.onErrorContainer
                hasSpending -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

private fun calculateHeatScale(intensity: Float): Float {
    return if (intensity <= 1.0f) 1.0f
    else lerpFloat(1.0f, 1.35f, (intensity - 1.0f) / 0.2f)
}

@OptIn(ExperimentalTextApi::class)
private fun calculateDynamicHeatStyle(
    intensity: Float,
    isRounded: Boolean,
    baseStyle: TextStyle
): TextStyle {
    val normalized = intensity.coerceIn(0f, 1.2f)
    val weight = lerpFloat(400f, 800f, normalized).toInt()
    val width = lerpFloat(80f, 125f, normalized)
    val animatedFontSize = (baseStyle.fontSize.value * lerpFloat(1.0f, 1.25f, normalized)).sp

    return baseStyle.copy(
        fontSize = animatedFontSize,
        fontFamily = googleSansFlex(
            weight = weight,
            width = width,
            isRounded = isRounded
        )
    )
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

internal val CELL_SIZE = 32.dp

data class CalendarUiState(
    val disabledBefore: LocalDate? = null,
    val disabledAfter: LocalDate? = null,
)

class CalendarState(
    context: Context,
    disableBeforeDate: Date,
    disableAfterDate: Date,
    locale: Locale,
) {
    val calendarUiState = mutableStateOf(
        CalendarUiState(
            disabledBefore = disableBeforeDate.toLocalDate(),
            disabledAfter = disableAfterDate.toLocalDate(),
        )
    )

    val listMonths: List<Month> = generateMonths(disableBeforeDate, disableAfterDate, locale)

    private fun generateMonths(startDate: Date, endDate: Date, locale: Locale): List<Month> {
        val start = startDate.toLocalDate()
        val end = endDate.toLocalDate()
        val months = mutableListOf<Month>()

        val weekFields = WeekFields.of(locale)

        var currentMonth = YearMonth.from(start)
        val endMonth = YearMonth.from(end)

        while (!currentMonth.isAfter(endMonth)) {
            val weeks = mutableListOf<Week>()
            val firstDayOfMonth = currentMonth.atDay(1)
            val lastDayOfMonth = currentMonth.atEndOfMonth()

            var d = firstDayOfMonth
            while (!d.isAfter(lastDayOfMonth)) {
                val weekStart = d.with(TemporalAdjusters.previousOrSame(weekFields.firstDayOfWeek))
                if (weeks.none { it.startDate == weekStart }) {
                    weeks.add(Week(weekStart, currentMonth))
                }
                d = d.plusDays(1)
            }

            months.add(Month(currentMonth, weeks.sortedBy { it.startDate }))
            currentMonth = currentMonth.plusMonths(1)
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

@Preview(name = "Heatmap - Short Period (Aug 29-31)", widthDp = 420)
@Composable
private fun PreviewHeatmapShortPeriod() {
    val start = LocalDate.of(2026, 8, 29)
    val end = LocalDate.of(2026, 8, 31)
    MinusTheme {
        CalendarHeatmap(
            budget = BigDecimal(1000),
            transactions = listOf(
                Transaction(amount = BigDecimal(100), date = start.atTime(12, 0)),
                Transaction(amount = BigDecimal(200), date = end.atTime(12, 0))
            ),
            startDate = start.toDate(),
            finishDate = end.toDate(),
        )
    }
}

@Preview(name = "Heatmap - Two Months", widthDp = 420)
@Composable
private fun PreviewHeatmapTwoMonths() {
    val start = LocalDate.of(2026, 6, 15)
    val end = LocalDate.of(2026, 7, 15)
    MinusTheme {
        CalendarHeatmap(
            budget = BigDecimal(1000),
            transactions = listOf(
                Transaction(amount = BigDecimal(50), date = start.atTime(12, 0)),
                Transaction(amount = BigDecimal(150), date = end.atTime(12, 0))
            ),
            startDate = start.toDate(),
            finishDate = end.toDate(),
        )
    }
}

@Preview(name = "Heatmap - Year Cross", widthDp = 420)
@Composable
private fun PreviewHeatmapYearCross() {
    val start = LocalDate.of(2025, 12, 28)
    val end = LocalDate.of(2026, 1, 5)
    MinusTheme {
        CalendarHeatmap(
            budget = BigDecimal(1000),
            transactions = listOf(
                Transaction(amount = BigDecimal(100), date = start.atTime(12, 0)),
                Transaction(amount = BigDecimal(200), date = end.atTime(12, 0))
            ),
            startDate = start.toDate(),
            finishDate = end.toDate(),
        )
    }
}
