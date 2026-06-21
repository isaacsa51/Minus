package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.charts.CategoryUsage
import com.serranoie.app.minus.presentation.ui.theme.component.charts.DonutChart
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class DonutChartScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun donutChartDefault() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                DonutChart(
                    items = listOf(
                        CategoryUsage(name = "Groceries", amount = BigDecimal("120.50")),
                        CategoryUsage(name = "Dining", amount = BigDecimal("85.00")),
                        CategoryUsage(name = "Gas", amount = BigDecimal("57.00")),
                        CategoryUsage(name = "Subscriptions", amount = BigDecimal("42.00")),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(16.dp),
                )
            }
        }
    }
}
