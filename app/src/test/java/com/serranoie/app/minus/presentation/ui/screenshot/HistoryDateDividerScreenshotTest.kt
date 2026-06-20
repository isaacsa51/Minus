package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.date.HistoryDateDivider
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class HistoryDateDividerScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun historyDateDividerExpanded() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                HistoryDateDivider(
                    date = LocalDate.of(2026, 1, 15),
                    isExpanded = true,
                    totalAmount = BigDecimal("125.75"),
                    currencyCode = "\$"
                )
            }
        }
    }
}