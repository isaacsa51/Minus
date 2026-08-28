package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.budget.graphs.CategoryDayEntry
import com.serranoie.app.minus.presentation.ui.theme.component.budget.graphs.MultiCategoryChart
import com.serranoie.app.minus.presentation.ui.theme.component.charts.baseColors
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class MultiCategoryChartScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 0.1,
    )

    private val groceries = baseColors[0]
    private val transport = baseColors[1]

    // A wide window with large totals — the scenario that used to cram overlapping "$X" labels
    // above every bar.
    private fun manyDaysEntries(start: LocalDate): List<CategoryDayEntry> = listOf(
        CategoryDayEntry(start, "Groceries", BigDecimal("800"), groceries),
        CategoryDayEntry(start.plusDays(1), "Transport", BigDecimal("50"), transport),
        CategoryDayEntry(start.plusDays(6), "Groceries", BigDecimal("300"), groceries),
        CategoryDayEntry(start.plusDays(6), "Transport", BigDecimal("2000"), transport),
        CategoryDayEntry(start.plusDays(12), "Groceries", BigDecimal("2500"), groceries),
        CategoryDayEntry(start.plusDays(13), "Transport", BigDecimal("99"), transport),
        CategoryDayEntry(start.plusDays(19), "Groceries", BigDecimal("15000"), groceries),
        CategoryDayEntry(start.plusDays(20), "Transport", BigDecimal("74"), transport),
    )

    @Test
    fun multiCategoryChart_noStaticTotalLabels() {
        Locale.setDefault(Locale.US)
        val start = LocalDate.of(2026, 5, 1)
        val startDate = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val entries = manyDaysEntries(start)
        val dayTotals = entries.groupBy { it.date }.mapValues { (_, e) -> e.sumOf { it.amount } }

        paparazzi.snapshot {
            MinusTheme {
                Box(Modifier.padding(16.dp)) {
                    MultiCategoryChart(
                        entries = entries,
                        dayTotals = dayTotals,
                        currencyCode = "USD",
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        startDate = startDate,
                        windowIndex = 0,
                        scrollStep = 30,
                        dataSize = 30,
                        dateFormatter = DateTimeFormatter.ofPattern("dd MMM"),
                    )
                }
            }
        }
    }

    @Test
    fun multiCategoryChart_tooltipOnTappedBar() {
        Locale.setDefault(Locale.US)
        val start = LocalDate.of(2026, 5, 1)
        val startDate = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val entries = manyDaysEntries(start)
        val dayTotals = entries.groupBy { it.date }.mapValues { (_, e) -> e.sumOf { it.amount } }

        paparazzi.snapshot {
            MinusTheme {
                Box(Modifier.padding(16.dp)) {
                    MultiCategoryChart(
                        entries = entries,
                        dayTotals = dayTotals,
                        currencyCode = "USD",
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        startDate = startDate,
                        windowIndex = 0,
                        scrollStep = 30,
                        dataSize = 30,
                        dateFormatter = DateTimeFormatter.ofPattern("dd MMM"),
                        forcedTooltipDate = start.plusDays(19),
                    )
                }
            }
        }
    }
}
