package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.component.date.CalendarState
import com.serranoie.app.minus.presentation.ui.theme.component.date.DaysOfWeek
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.font.format.toDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * "Weekly" view of the same [chargesByDay] the whole-period calendar renders, one week at a time
 * with pagination — a single row of 7 cells can afford to be much taller than a 5-row month grid,
 * so each cell shows full (wrapped, not truncated) subscription names instead of dots.
 */
@Composable
internal fun SubscriptionsWeeklyCalendar(
    periodStart: LocalDate,
    periodEnd: LocalDate,
    chargesByDay: Map<LocalDate, List<DayCharge>>,
    onDayClick: (LocalDate) -> Unit,
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
    val renderedWeeks = remember(calendarState, periodStart, periodEnd) {
        calendarState.listMonths.flatMap { month ->
            month.weeks.filter { week ->
                val endOfWeek = week.startDate.plusDays(6)
                !endOfWeek.isBefore(periodStart) && !week.startDate.isAfter(periodEnd)
            }
        }
    }
    if (renderedWeeks.isEmpty()) return

    var weekIndex by remember(periodStart, periodEnd) {
        val currentWeek = renderedWeeks.indexOfFirst { week ->
            !today.isBefore(week.startDate) && !today.isAfter(week.startDate.plusDays(6))
        }
        mutableIntStateOf(currentWeek.coerceAtLeast(0))
    }
    weekIndex = weekIndex.coerceIn(0, renderedWeeks.lastIndex)
    val week = renderedWeeks[weekIndex]
    val weekEnd = week.startDate.plusDays(6)
    val rangeFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            IconButton(
                onClick = { weekIndex-- },
                enabled = weekIndex > 0,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            }
            Text(
                text = "${rangeFormatter.format(week.startDate)} – ${rangeFormatter.format(weekEnd)}",
                style = MaterialTheme.typography.titleSmallEmphasized,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            IconButton(
                onClick = { weekIndex++ },
                enabled = weekIndex < renderedWeeks.lastIndex,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }

        if (renderedWeeks.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                renderedWeeks.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(5.dp)
                            .background(
                                if (index == weekIndex) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                CircleShape,
                            ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        DaysOfWeek()
        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            var day = week.startDate
            for (dayOfWeekColumn in 0 until 7) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(2.dp),
                ) {
                    SubscriptionsWeeklyDayCell(
                        day = day,
                        charges = chargesByDay[day].orEmpty(),
                        isToday = day == today,
                        isOutsidePeriod = day.isBefore(periodStart) || day.isAfter(periodEnd),
                        onClick = { onDayClick(day) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                day = day.plusDays(1)
            }
        }
    }
}

@Composable
private fun SubscriptionsWeeklyDayCell(
    day: LocalDate,
    charges: List<DayCharge>,
    isToday: Boolean,
    isOutsidePeriod: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .then(
                if (isOutsidePeriod) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), shape)
                } else {
                    Modifier.background(
                        if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                        shape,
                    )
                }
            )
            .then(if (charges.isNotEmpty()) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmallCondensed,
            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )
        charges.forEach { charge ->
            val alpha = when (charge.status) {
                ChargeStatus.SKIPPED, ChargeStatus.MISSED -> 0.5f
                ChargeStatus.PAID, ChargeStatus.PENDING -> 1f
            }
            val palette = subscriptionPalette(charge.transaction.id)
            Text(
                text = charge.transaction.comment.ifBlank { stringResource(R.string.recurrent_ticket_unnamed_subscription) },
                style = MaterialTheme.typography.labelSmallCondensed.copy(lineHeight = 12.sp),
                color = palette.main.copy(alpha = alpha),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 3.dp)
                    .fillMaxWidth()
                    .background(palette.main.copy(alpha = alpha * 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 2.dp, vertical = 1.dp),
            )
        }
    }
}
