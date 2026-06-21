package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.TextRow
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class TextRowScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun textRowWithIconAndDescription() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                TextRow(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Home,
                    text = "Home Address",
                    description = "123 Main Street, City",
                )
            }
        }
    }

    @Test
    fun textRowWithEndCaption() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                TextRow(
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Home,
                    text = "Settings",
                    endCaption = "Enabled",
                )
            }
        }
    }
}
