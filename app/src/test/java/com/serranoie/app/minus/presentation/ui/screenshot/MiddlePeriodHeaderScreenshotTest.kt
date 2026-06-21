package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.MiddlePeriodHeader
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class MiddlePeriodHeaderScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun middlePeriodHeader() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MiddlePeriodHeader(
                            modifier = Modifier.fillMaxWidth(),
                            onClose = {},
                        )
                    }
                }
            }
        }
    }
}
