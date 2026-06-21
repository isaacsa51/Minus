package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.AutoResizeBasicTextField
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class AutoResizeBasicTextFieldScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun autoResizeBasicTextFieldReadOnly() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    AutoResizeBasicTextField(
                        value = "42.50",
                        onValueChange = {},
                        readOnly = true,
                        containerSize = IntSize.Zero
                    )
                }
            }
        }
    }
}
