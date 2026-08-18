package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.budget.ChartDateRange
import com.serranoie.app.minus.presentation.ui.theme.component.date.CalendarHeatmap
import com.serranoie.app.minus.presentation.ui.theme.component.date.SpendingDay
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class CalendarHeatmapScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun calendarHeatmap() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                CalendarHeatmap(
                    budget = BigDecimal("50.00"),
                    transactions = sampleTransactions(),
                    startDate = fixedDate(2026, 1, 1),
                    finishDate = fixedDate(2026, 1, 30),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp),
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun calendarHeatmap_withHighlightedRange() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                CalendarHeatmap(
                    budget = BigDecimal("50.00"),
                    transactions = sampleTransactions(),
                    startDate = fixedDate(2026, 1, 1),
                    finishDate = fixedDate(2026, 1, 30),
                    highlightedRange = ChartDateRange(
                        start = LocalDate.of(2026, 1, 12),
                        end = LocalDate.of(2026, 1, 18),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp),
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun calendarHeatmap_withSelectedDay() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                CalendarHeatmap(
                    budget = BigDecimal("50.00"),
                    transactions = sampleTransactions(),
                    startDate = fixedDate(2026, 1, 1),
                    finishDate = fixedDate(2026, 1, 30),
                    highlightedRange = ChartDateRange(
                        start = LocalDate.of(2026, 1, 20),
                        end = LocalDate.of(2026, 1, 20),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp),
                )
            }
        }
    }

    private fun sampleTransactions(): List<Transaction> = listOf(
        Transaction(id = 1L, amount = BigDecimal("18.75"), comment = "Lunch", date = LocalDateTime.of(2026, 1, 12, 12, 0), periodId = 7L),
        Transaction(id = 2L, amount = BigDecimal("45.00"), comment = "Groceries", date = LocalDateTime.of(2026, 1, 13, 18, 0), periodId = 7L),
        Transaction(id = 3L, amount = BigDecimal("8.25"), comment = "Coffee", date = LocalDateTime.of(2026, 1, 14, 9, 0), periodId = 7L),
    )

    private fun fixedDate(year: Int, month: Int, day: Int): Date = Date.from(
        LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC)
    )
}
