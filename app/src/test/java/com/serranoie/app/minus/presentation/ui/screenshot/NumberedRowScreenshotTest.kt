package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.NumberedRow
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class NumberedRowScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun numberedRowWithSubtitle() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                NumberedRow(
                    number = 1,
                    title = "January 2026",
                    subtitle = "\$522.45 spent"
                )
            }
        }
    }
}