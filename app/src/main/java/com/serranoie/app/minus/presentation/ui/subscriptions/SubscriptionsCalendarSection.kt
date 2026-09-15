package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.date.CalendarState
import com.serranoie.app.minus.presentation.ui.theme.component.date.DaysOfWeek
import com.serranoie.app.minus.presentation.ui.theme.component.date.MonthHeader
import com.serranoie.app.minus.presentation.ui.theme.component.expense.subscriptionPalette
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.font.format.toDate
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Mirrors [com.serranoie.app.minus.presentation.ui.theme.component.date.CalendarHeatmap]'s
 * week generation: builds the months spanning [periodStart]..[periodEnd] but only renders the
 * weeks that actually overlap that range, instead of a fixed single calendar month.
 */
@Composable
internal fun SubscriptionsCalendarSection(
    periodStart: LocalDate,
    periodEnd: LocalDate,
    chargesByDay: Map<LocalDate, List<DayCharge>>,
    onDayClick: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val today = remember { LocalDate.now() }
    val calendarState = remember(periodStart, periodEnd, locale) {
        CalendarState(
            context = context,
            disableBeforeDate = periodStart.toDate(),
            disableAfterDate = periodEnd.toDate(),
            locale = locale,
        )
    }

    Column(modifier = modifier) {
        calendarState.listMonths.forEach { month ->
            MonthHeader(yearMonth = month.yearMonth)
            DaysOfWeek()

            val renderedWeeks = month.weeks.filter { week ->
                val endOfWeek = week.startDate.plusDays(6)
                !endOfWeek.isBefore(periodStart) && !week.startDate.isAfter(periodEnd)
            }

            renderedWeeks.forEachIndexed { weekIndex, week ->
                val isFirstRow = weekIndex == 0
                val isLastRow = weekIndex == renderedWeeks.lastIndex

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (dayOfWeekColumn in 0 until 7) {
                        // A fresh val per column — capturing a shared `var` here would let every
                        // cell's onClick lambda read whichever date the loop last landed on,
                        // instead of the date that cell actually displayed.
                        val day = week.startDate.plusDays(dayOfWeekColumn.toLong())
                        val isFirstColumn = dayOfWeekColumn == 0
                        val isLastColumn = dayOfWeekColumn == 6
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp),
                        ) {
                            if (day.month == week.yearMonth.month) {
                                CalendarDayCell(
                                    day = day.dayOfMonth,
                                    charges = chargesByDay[day].orEmpty(),
                                    isToday = day == today,
                                    isOutsidePeriod = day.isBefore(periodStart) || day.isAfter(periodEnd),
                                    onClick = { onDayClick(day) },
                                    topLeftRadius = isFirstRow && isFirstColumn,
                                    topRightRadius = isFirstRow && isLastColumn,
                                    bottomLeftRadius = isLastRow && isFirstColumn,
                                    bottomRightRadius = isLastRow && isLastColumn,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Day cells default to [MaterialTheme.shapes.small], but the four corner cells of the rendered
 * grid (whichever day actually lands there — a short first/last week can leave a corner slot
 * blank) swap in [MaterialTheme.shapes.extraLarge] for just that one corner, so the whole grid
 * reads as a single rounded card instead of a sheet of uniform tiles.
 */
@Composable
private fun calendarDayCellShape(
    topLeftRadius: Boolean,
    topRightRadius: Boolean,
    bottomLeftRadius: Boolean,
    bottomRightRadius: Boolean,
): Shape {
    val small = MaterialTheme.shapes.small
    if (!topLeftRadius && !topRightRadius && !bottomLeftRadius && !bottomRightRadius) return small

    val extraLarge = MaterialTheme.shapes.large
    return small.copy(
        topStart = if (topLeftRadius) extraLarge.topStart else small.topStart,
        topEnd = if (topRightRadius) extraLarge.topEnd else small.topEnd,
        bottomEnd = if (bottomRightRadius) extraLarge.bottomEnd else small.bottomEnd,
        bottomStart = if (bottomLeftRadius) extraLarge.bottomStart else small.bottomStart,
    )
}

@Composable
private fun CalendarDayCell(
    day: Int,
    charges: List<DayCharge>,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    isOutsidePeriod: Boolean = false,
    onClick: (() -> Unit)? = null,
    topLeftRadius: Boolean = false,
    topRightRadius: Boolean = false,
    bottomLeftRadius: Boolean = false,
    bottomRightRadius: Boolean = false,
) {
    val cellShape = calendarDayCellShape(topLeftRadius, topRightRadius, bottomLeftRadius, bottomRightRadius)

    Column(
        modifier = modifier
            .then(
                if (isOutsidePeriod) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), cellShape)
                } else {
                    Modifier.background(
                        if (isToday) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        cellShape,
                    )
                }
            )
            .then(
                if (onClick != null && charges.isNotEmpty()) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelSmallCondensed,
            color = if (isToday) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        if (charges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                charges.take(2).forEach { charge ->
                    val dotAlpha = when (charge.status) {
                        ChargeStatus.SKIPPED -> 0.45f
                        ChargeStatus.PAID, ChargeStatus.PENDING -> 1f
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                subscriptionPalette(charge.transaction.id).main.copy(alpha = dotAlpha),
                                CircleShape,
                            ),
                    )
                }
                if (charges.size > 2) {
                    Text(
                        text = "+${charges.size - 2}",
                        style = MaterialTheme.typography.labelSmallCondensed,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun previewDayCharge(id: Long, name: String, status: ChargeStatus) = DayCharge(
    transaction = Transaction(
        id = id,
        amount = BigDecimal("9.99"),
        comment = name,
        date = LocalDate.now().atStartOfDay(),
        isRecurrent = true,
        recurrentFrequency = RecurrentFrequency.MONTHLY,
    ),
    status = status,
)

@Preview(showBackground = true, name = "Busy month")
@Composable
private fun SubscriptionsCalendarSectionPreview() {
    val today = LocalDate.now()
    MinusTheme {
        SubscriptionsCalendarSection(
            periodStart = today.withDayOfMonth(1),
            periodEnd = today.withDayOfMonth(today.lengthOfMonth()),
            chargesByDay = mapOf(
                today to listOf(previewDayCharge(1, "Netflix", ChargeStatus.PENDING)),
                today.minusDays(3) to listOf(previewDayCharge(2, "Spotify", ChargeStatus.PAID)),
                today.minusDays(7) to listOf(previewDayCharge(3, "iCloud+", ChargeStatus.PAID)),
                today.plusDays(5) to listOf(
                    previewDayCharge(4, "Gym", ChargeStatus.PENDING),
                    previewDayCharge(5, "YouTube Premium", ChargeStatus.PENDING),
                    previewDayCharge(6, "Disney+", ChargeStatus.SKIPPED),
                ),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, name = "Empty period")
@Composable
private fun SubscriptionsCalendarSectionEmptyPreview() {
    val today = LocalDate.now()
    MinusTheme {
        SubscriptionsCalendarSection(
            periodStart = today.withDayOfMonth(1),
            periodEnd = today.withDayOfMonth(today.lengthOfMonth()),
            chargesByDay = emptyMap(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
