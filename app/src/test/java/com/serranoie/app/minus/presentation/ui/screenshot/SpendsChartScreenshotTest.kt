package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.charts.SpendsChart
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class SpendsChartScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun spendsChartLine() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                SpendsChart(
                    spends = sampleTransactions(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp),
                )
            }
        }
    }

    @Test
    fun outlinedSpendsChart() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                com.serranoie.app.minus.presentation.ui.theme.component.charts.OutlinedSpendsChart(
                    spends = sampleTransactions(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp),
                    graphBackgroundColor = Color.Transparent,
                    graphOutlineColor = Color(0xFF6750A4),
                    graphColor = Color(0xFF6750A4),
                )
            }
        }
    }

    private fun sampleTransactions(): List<Transaction> {
        val today = LocalDate.of(2026, 1, 15)
        return listOf(
            Transaction(id = 1L, amount = BigDecimal("12.50"), comment = "Lunch", date = today.minusDays(7).atTime(12, 0), periodId = 7L),
            Transaction(id = 2L, amount = BigDecimal("45.00"), comment = "Groceries", date = today.minusDays(6).atTime(18, 0), periodId = 7L),
            Transaction(id = 3L, amount = BigDecimal("8.25"), comment = "Coffee", date = today.minusDays(5).atTime(9, 0), periodId = 7L),
            Transaction(id = 4L, amount = BigDecimal("32.00"), comment = "Gas", date = today.minusDays(4).atTime(8, 0), periodId = 7L),
            Transaction(id = 5L, amount = BigDecimal("15.99"), comment = "Subscription", date = today.minusDays(3).atTime(10, 0), periodId = 7L),
            Transaction(id = 6L, amount = BigDecimal("22.00"), comment = "Dinner", date = today.minusDays(2).atTime(19, 30), periodId = 7L),
        )
    }
}
