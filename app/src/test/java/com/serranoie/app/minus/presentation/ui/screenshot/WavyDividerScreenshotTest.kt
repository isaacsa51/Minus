package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.WavyDivider
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class WavyDividerScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun wavyDivider() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    WavyDivider(
                        text = "Recurrent payments next period",
                        modifier = Modifier.fillMaxWidth(),
                        horizontalPadding = 16.dp,
                        amplitude = 4f,
                        wavelength = 45f,
                        strokeWidth = 4f
                    )
                }
            }
        }
    }
}