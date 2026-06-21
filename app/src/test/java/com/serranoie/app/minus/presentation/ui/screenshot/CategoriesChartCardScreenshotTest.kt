package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.charts.CategoriesChartCard
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class CategoriesChartCardScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun categoriesChartCard() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                CategoriesChartCard(
                    spends = sampleTransactions(),
                    currency = "USD",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp),
                )
            }
        }
    }

    private fun sampleTransactions(): List<Transaction> {
        val today = LocalDate.of(2026, 1, 15)
        return listOf(
            Transaction(id = 1L, amount = BigDecimal("120.50"), comment = "Groceries", date = today.minusDays(1).atTime(17, 0), periodId = 7L),
            Transaction(id = 2L, amount = BigDecimal("85.00"), comment = "Groceries", date = today.minusDays(3).atTime(18, 30), periodId = 7L),
            Transaction(id = 3L, amount = BigDecimal("45.00"), comment = "Dining out", date = today.minusDays(2).atTime(19, 0), periodId = 7L),
            Transaction(id = 4L, amount = BigDecimal("32.00"), comment = "Gas", date = today.minusDays(4).atTime(8, 0), periodId = 7L),
            Transaction(id = 5L, amount = BigDecimal("25.00"), comment = "Gas", date = today.minusDays(6).atTime(8, 30), periodId = 7L),
            Transaction(id = 6L, amount = BigDecimal("18.75"), comment = "Lunch", date = today.atTime(12, 30), periodId = 7L),
        )
    }
}
