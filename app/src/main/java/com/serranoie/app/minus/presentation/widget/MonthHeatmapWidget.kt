@file:OptIn(ExperimentalGlancePreviewApi::class)

package com.serranoie.app.minus.presentation.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.serranoie.app.minus.R
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.YearMonth
import java.util.Locale
import java.time.format.TextStyle as JavaTextStyle

internal data class MonthWidgetCellState(
    val dayNumber: Int,
    val ratio: Float,
    val hasSpending: Boolean,
)

private data class MonthWidgetCalendarCellState(
    val dayNumber: Int?,
    val ratio: Float,
    val hasSpending: Boolean,
)

private const val MONTH_WIDGET_GRID_COLUMNS = 7
private const val MONTH_WIDGET_GRID_ROWS = 6
private const val MONTH_WIDGET_MAX_DAYS = 31

private val monthWidgetYearKey = intPreferencesKey("month_heatmap_year")
private val monthWidgetMonthKey = intPreferencesKey("month_heatmap_month")
private val monthWidgetTotalSpentKey = intPreferencesKey("month_heatmap_total_spent")
private val monthWidgetCurrencyKey = stringPreferencesKey("month_heatmap_currency")

private fun monthWidgetRatioKey(day: Int) = floatPreferencesKey("month_heatmap_ratio_$day")

private fun monthWidgetHasSpendingKey(day: Int) =
    intPreferencesKey("month_heatmap_has_spending_$day")

private fun monthWidgetHeatColorRes(
    ratio: Float,
    hasSpending: Boolean,
): Int {
    if (!hasSpending) return R.color.widget_heatmap_empty_dark

    val normalized = ratio.coerceIn(0f, 1.4f)
    return when {
        normalized <= 0.2f -> R.color.widget_heatmap_good_1
        normalized <= 0.45f -> R.color.widget_heatmap_good_2
        normalized <= 0.7f -> R.color.widget_heatmap_warn_1
        normalized <= 0.95f -> R.color.widget_heatmap_warn_2
        normalized <= 1.2f -> R.color.widget_heatmap_bad_1
        else -> R.color.widget_heatmap_bad_2
    }
}

private fun monthWidgetTitle(yearMonth: YearMonth): String {
    val month = yearMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
    return month.uppercase(Locale.getDefault())
}

private fun monthWidgetFirstDayOffset(yearMonth: YearMonth): Int {
    val first = yearMonth.atDay(1).dayOfWeek
    return (first.value - DayOfWeek.MONDAY.value + 7) % 7
}

private fun buildMonthWidgetCalendarCells(
    yearMonth: YearMonth,
    monthCells: List<MonthWidgetCellState>,
): List<MonthWidgetCalendarCellState> {
    val daysInMonth = yearMonth.lengthOfMonth()
    val offset = monthWidgetFirstDayOffset(yearMonth)
    val byDay = monthCells.associateBy { it.dayNumber }

    return List(MONTH_WIDGET_GRID_COLUMNS * MONTH_WIDGET_GRID_ROWS) { index ->
        val day = index - offset + 1
        if (day in 1..daysInMonth) {
            val cell = byDay[day]
            MonthWidgetCalendarCellState(
                dayNumber = day,
                ratio = cell?.ratio ?: 0f,
                hasSpending = cell?.hasSpending == true,
            )
        } else {
            MonthWidgetCalendarCellState(dayNumber = null, ratio = 0f, hasSpending = false)
        }
    }
}

@Composable
private fun MonthWidgetDayCell(cell: MonthWidgetCalendarCellState) {
    val hasDay = cell.dayNumber != null

    Box(
        modifier =
            GlanceModifier.fillMaxSize().padding(1.dp).cornerRadius(3.dp).background(
                if (hasDay) {
                    monthWidgetHeatColorRes(
                        cell.ratio,
                        cell.hasSpending,
                    )
                } else {
                    android.R.color.transparent
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (hasDay) {
            Text(
                text = cell.dayNumber.toString(),
                style =
                    TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color =
                            if (cell.hasSpending) {
                                GlanceTheme.colors.surface
                            } else {
                                GlanceTheme.colors.onSurfaceVariant
                            },
                    ),
            )
        }
    }
}

@Composable
internal fun MonthHeatmapContent(
    yearMonth: YearMonth,
    monthCells: List<MonthWidgetCellState>,
    totalSpent: Int,
    modifier: GlanceModifier = GlanceModifier,
    context: Context = LocalContext.current,
    currency: String = "USD",
    totalSpentLabel: String = context.getString(R.string.total_spent),
) {
    val calendarCells = buildMonthWidgetCalendarCells(yearMonth, monthCells)

    Column(
        modifier = modifier.fillMaxSize().padding(6.dp),
    ) {
        Text(
            text = monthWidgetTitle(yearMonth),
            style =
                TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
            modifier = GlanceModifier.padding(bottom = 3.dp),
        )

        Column(modifier = GlanceModifier.fillMaxSize().defaultWeight()) {
            repeat(MONTH_WIDGET_GRID_ROWS) { row ->
                Row(
                    modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                ) {
                    repeat(MONTH_WIDGET_GRID_COLUMNS) { column ->
                        val cell = calendarCells[row * MONTH_WIDGET_GRID_COLUMNS + column]
                        Box(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight().padding(1.dp),
                        ) {
                            MonthWidgetDayCell(cell)
                        }
                    }
                }
            }
        }

        Text(
            text = "$totalSpentLabel: ${formatWidgetCurrency(currency, totalSpent)}",
            style =
                TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
            modifier = GlanceModifier.padding(top = 2.dp),
        )
    }
}

class MonthHeatmapWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val now = YearMonth.now()
        val yearMonth =
            YearMonth.of(
                prefs[monthWidgetYearKey] ?: now.year,
                prefs[monthWidgetMonthKey] ?: now.monthValue,
            )
        val monthCells =
            (1..MONTH_WIDGET_MAX_DAYS).map { day ->
                MonthWidgetCellState(
                    dayNumber = day,
                    ratio = prefs[monthWidgetRatioKey(day)] ?: 0f,
                    hasSpending = (prefs[monthWidgetHasSpendingKey(day)] ?: 0) == 1,
                )
            }
        val totalSpent = prefs[monthWidgetTotalSpentKey] ?: 0
        val currency = prefs[monthWidgetCurrencyKey] ?: "USD"

        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surface)
                    .clickable(actionRunCallback<OpenAppAction>())
                    .padding(8.dp),
        ) {
            MonthHeatmapContent(
                yearMonth = yearMonth,
                monthCells = monthCells,
                totalSpent = totalSpent,
                currency = currency,
            )
        }
    }
}

class MonthHeatmapWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonthHeatmapWidget()
}

suspend fun updateMonthHeatmapWidget(
    context: Context,
    monthData: MonthHeatmapData,
    totalSpent: Int,
    currency: String = "USD",
) {
    val maxTransactions = monthData.days.maxOfOrNull { it.transactionCount }?.coerceAtLeast(1) ?: 1

    val manager = GlanceAppWidgetManager(context)
    val glanceIds = manager.getGlanceIds(MonthHeatmapWidget::class.java)

    glanceIds.forEach { glanceId ->
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[monthWidgetYearKey] = monthData.year
            prefs[monthWidgetMonthKey] = monthData.month
            prefs[monthWidgetTotalSpentKey] = totalSpent
            prefs[monthWidgetCurrencyKey] = currency

            for (day in 1..MONTH_WIDGET_MAX_DAYS) {
                val spending = monthData.days.firstOrNull { it.dayOfMonth == day }
                val ratio =
                    if (spending != null) {
                        val amountRatio =
                            if (spending.budget > BigDecimal.ZERO) {
                                (spending.spending.toFloat() / spending.budget.toFloat()).coerceAtLeast(
                                    0f
                                )
                            } else {
                                0f
                            }
                        val countRatio =
                            if (maxTransactions > 0) {
                                (spending.transactionCount.toFloat() / maxTransactions.toFloat()).coerceIn(
                                    0f,
                                    1f,
                                )
                            } else {
                                0f
                            }
                        (amountRatio * 0.6f + countRatio * 0.4f).coerceIn(0f, 1.4f)
                    } else {
                        0f
                    }

                prefs[monthWidgetRatioKey(day)] = ratio
                prefs[monthWidgetHasSpendingKey(day)] =
                    if ((spending?.transactionCount ?: 0) > 0) 1 else 0
            }
        }
        MonthHeatmapWidget().update(context, glanceId)
    }
}

@Preview(widthDp = 140, heightDp = 140)
@Composable
private fun MonthHeatmapContentPreview() {
    val ym = YearMonth.of(2026, 4)
    val cells =
        remember {
            (1..ym.lengthOfMonth()).map { day ->
                val ratio =
                    when {
                        day % 11 == 0 -> 1.3f
                        day % 7 == 0 -> 0.9f
                        day % 5 == 0 -> 0.65f
                        day % 3 == 0 -> 0.35f
                        else -> 0.1f
                    }
                MonthWidgetCellState(
                    dayNumber = day,
                    ratio = ratio,
                    hasSpending = day % 4 != 0,
                )
            }
        }

    GlanceTheme {
        Box(
            modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface),
        ) {
            MonthHeatmapContent(
                yearMonth = ym,
                monthCells = cells,
                totalSpent = 1250,
                currency = "USD",
            )
        }
    }
}
