package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.BarSegment
import com.serranoie.app.minus.presentation.ui.theme.component.LinearSavingBar
import com.serranoie.app.minus.presentation.ui.theme.component.budget.graphs.ChartLegend
import com.serranoie.app.minus.presentation.ui.theme.component.budget.graphs.LegendEntry
import com.serranoie.app.minus.presentation.ui.theme.component.date.CalendarState
import com.serranoie.app.minus.presentation.ui.theme.component.expense.subscriptionPalette
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.font.format.toDate
import com.serranoie.app.minus.presentation.util.haptic.HapticUtil.performVirtualKeyHaptic
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/**
 * "Weekly" view of the same [chargesByDay] the whole-period calendar renders, as a single card:
 * one week of pill-shaped day cells (with a colored dot per charge, using the same per-subscription
 * palette as the month grid) plus a spending breakdown for just that week.
 */
@Composable
internal fun SubscriptionsWeeklyCalendar(
    periodStart: LocalDate,
    periodEnd: LocalDate,
    chargesByDay: Map<LocalDate, List<DayCharge>>,
    currencyFormat: NumberFormat,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
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
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM dd", locale) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(
                    targetState = weekIndex,
                    modifier = Modifier.weight(1f),
                    transitionSpec = weekSlideTransitionSpec(),
                    label = "weekHeaderSlide",
                ) { animatedIndex ->
                    val animatedWeek = renderedWeeks[animatedIndex]
                    val animatedWeekEnd = animatedWeek.startDate.plusDays(6)
                    val animatedWeekCharges = remember(chargesByDay, animatedWeek) {
                        (0..6).flatMap { chargesByDay[animatedWeek.startDate.plusDays(it.toLong())].orEmpty() }
                    }
                    Column {
                        FlowRow(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "${dateFormatter.format(animatedWeek.startDate)} – ${
                                    dateFormatter.format(
                                        animatedWeekEnd
                                    )
                                }",
                                style = MaterialTheme.typography.titleMediumEmphasized,
                            )

                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = pluralStringResource(
                                R.plurals.subscriptions_weekly_bills_scheduled,
                                animatedWeekCharges.size,
                                animatedWeekCharges.size,
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(
                    onClick = {
                        weekIndex--
                        performVirtualKeyHaptic(view)
                    },
                    enabled = weekIndex > 0,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        weekIndex++
                        performVirtualKeyHaptic(view)
                    },
                    enabled = weekIndex < renderedWeeks.lastIndex,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = weekIndex,
                transitionSpec = weekSlideTransitionSpec(),
                label = "weekBodySlide",
            ) { animatedIndex ->
                val animatedWeek = renderedWeeks[animatedIndex]
                val animatedWeekCharges = remember(chargesByDay, animatedWeek) {
                    (0..6).flatMap { chargesByDay[animatedWeek.startDate.plusDays(it.toLong())].orEmpty() }
                }
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (dayOfWeekColumn in 0 until 7) {
                            val day = animatedWeek.startDate.plusDays(dayOfWeekColumn.toLong())
                            SubscriptionsWeeklyDayPill(
                                day = day,
                                locale = locale,
                                charges = chargesByDay[day].orEmpty(),
                                isToday = day == today,
                                isOutsidePeriod = day.isBefore(periodStart) || day.isAfter(periodEnd),
                                shape = weeklyDayPillShape(
                                    isFirst = dayOfWeekColumn == 0,
                                    isLast = dayOfWeekColumn == 6,
                                ),
                                onClick = {
                                    onDayClick(day)
                                    performVirtualKeyHaptic(view)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                            )
                        }
                    }

                    if (animatedWeekCharges.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Spacer(modifier = Modifier.height(16.dp))

                        SubscriptionsWeeklyCategoryDistribution(
                            charges = animatedWeekCharges,
                            currencyFormat = currencyFormat,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** Slides the week content left/right depending on whether the user paged forward or back. */
private fun weekSlideTransitionSpec(): AnimatedContentTransitionScope<Int>.() -> ContentTransform = {
    val direction = if (targetState > initialState) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
    val slideSpec = tween<IntOffset>(durationMillis = 450, easing = EaseInOut)
    val fadeSpec = tween<Float>(durationMillis = 450, easing = EaseInOut)
    (slideIntoContainer(direction, animationSpec = slideSpec) + fadeIn(fadeSpec)) togetherWith
        (slideOutOfContainer(direction, animationSpec = slideSpec) + fadeOut(fadeSpec))
}

@Composable
private fun weeklyDayPillShape(isFirst: Boolean, isLast: Boolean): Shape {
    val small = MaterialTheme.shapes.small
    if (!isFirst && !isLast) return small

    val extraLarge = MaterialTheme.shapes.extraLarge
    return small.copy(
        topStart = if (isFirst) extraLarge.topStart else small.topStart,
        bottomStart = if (isFirst) extraLarge.bottomStart else small.bottomStart,
        topEnd = if (isLast) extraLarge.topEnd else small.topEnd,
        bottomEnd = if (isLast) extraLarge.bottomEnd else small.bottomEnd,
    )
}

@Composable
private fun SubscriptionsWeeklyDayPill(
    day: LocalDate,
    locale: Locale,
    charges: List<DayCharge>,
    isToday: Boolean,
    isOutsidePeriod: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .then(
                if (isOutsidePeriod) {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape
                    )
                } else {
                    Modifier.background(
                        if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape,
                    )
                }
            )
            .then(if (charges.isNotEmpty()) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val onDayColor = if (isToday) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).uppercase(locale),
            style = MaterialTheme.typography.labelSmallCondensed,
            color = onDayColor,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        )

        if (charges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                charges.take(3).forEach { charge ->
                    val dotAlpha = when (charge.status) {
                        ChargeStatus.SKIPPED -> 0.45f
                        ChargeStatus.PAID, ChargeStatus.PENDING -> 1f
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                subscriptionPalette(charge.transaction.id).main.copy(alpha = dotAlpha),
                                CircleShape,
                            ),
                    )
                }
                if (charges.size > 3) {
                    Text(
                        text = "+${charges.size - 3}",
                        style = MaterialTheme.typography.labelSmallCondensed,
                        color = onDayColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionsWeeklyCategoryDistribution(
    charges: List<DayCharge>,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier,
) {
    val bars = remember(charges) {
        charges.groupBy { it.transaction.id }
            .map { (transactionId, group) ->
                Triple(
                    transactionId,
                    group.first().transaction.comment.ifBlank { "?" },
                    group.sumOf { it.transaction.amount },
                )
            }
            .sortedByDescending { it.third }
    }
    val total = remember(bars) { bars.sumOf { it.third } }
    if (total <= BigDecimal.ZERO) return

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.subscriptions_weekly_category_distribution_title),
                style = MaterialTheme.typography.labelMediumCondensed,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.subscriptions_weekly_total_suffix,
                    currencyFormat.format(total)
                ),
                style = MaterialTheme.typography.titleSmallEmphasized,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearSavingBar(
            segments = bars.map { (transactionId, _, amount) ->
                BarSegment(
                    weight = (amount.toDouble() / total.toDouble()).toFloat(),
                    color = subscriptionPalette(transactionId).main,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        ChartLegend(
            entries = bars.map { (transactionId, name, amount) ->
                val percent = (amount.toDouble() / total.toDouble() * 100).roundToInt()
                LegendEntry(
                    label = "$name $percent%",
                    color = subscriptionPalette(transactionId).main,
                )
            },
        )
    }
}

private fun previewWeeklyCharge(id: Long, name: String, amount: String, status: ChargeStatus) =
    DayCharge(
        transaction = Transaction(
            id = id,
            amount = BigDecimal(amount),
            comment = name,
            date = LocalDate.now().atStartOfDay(),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        status = status,
    )

/** Current week, mid-month period (so the pagination arrows have somewhere to go). */
@Preview(showBackground = true, name = "Busy week")
@Composable
private fun SubscriptionsWeeklyCalendarPreview() {
    val today = LocalDate.now()
    MinusTheme {
        SubscriptionsWeeklyCalendar(
            periodStart = today.withDayOfMonth(1),
            periodEnd = today.withDayOfMonth(today.lengthOfMonth()),
            chargesByDay = mapOf(
                today to listOf(previewWeeklyCharge(1, "Netflix", "16.99", ChargeStatus.PENDING)),
                today.plusDays(1) to listOf(
                    previewWeeklyCharge(2, "Gym membership", "49.99", ChargeStatus.PENDING),
                    previewWeeklyCharge(3, "iCloud+", "2.99", ChargeStatus.PAID),
                ),
                today.minusDays(1) to listOf(
                    previewWeeklyCharge(
                        4,
                        "Disney+",
                        "7.99",
                        ChargeStatus.PAID
                    )
                ),
                today.plusDays(3) to listOf(
                    previewWeeklyCharge(
                        5,
                        "Spotify",
                        "9.99",
                        ChargeStatus.SKIPPED
                    )
                ),
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(),
            onDayClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** A quiet week with a single charge and no adjacent-week pagination (period == this one week). */
@Preview(showBackground = true, name = "Quiet week")
@Composable
private fun SubscriptionsWeeklyCalendarQuietPreview() {
    val today = LocalDate.now()
    MinusTheme {
        SubscriptionsWeeklyCalendar(
            periodStart = today.minusDays(3),
            periodEnd = today.plusDays(3),
            chargesByDay = mapOf(
                today to listOf(previewWeeklyCharge(1, "Netflix", "16.99", ChargeStatus.PENDING)),
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(),
            onDayClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
