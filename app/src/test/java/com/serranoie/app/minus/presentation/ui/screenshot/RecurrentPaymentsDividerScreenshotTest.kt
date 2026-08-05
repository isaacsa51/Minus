package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.expense.RecurrentPaymentsDivider
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class RecurrentPaymentsDividerScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun recurrentPaymentsDividerExpanded() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                RecurrentPaymentsDivider(
                    title = "Recurrent payments this period",
                    isExpanded = true,
                    onToggleClick = {},
                    itemCount = 3,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
        }
    }

    @Test
    fun recurrentPaymentsDividerCollapsed() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                RecurrentPaymentsDivider(
                    title = "Recurrent payments this period",
                    isExpanded = false,
                    onToggleClick = {},
                    itemCount = 3,
                    totalAmount = BigDecimal("4152.00"),
                    currencyCode = "USD",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
        }
    }
}
