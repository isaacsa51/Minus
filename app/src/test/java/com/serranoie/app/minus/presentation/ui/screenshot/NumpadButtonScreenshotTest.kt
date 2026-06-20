package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.NumpadButton
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.NumpadButtonType
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class NumpadButtonScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @OptIn(ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun numpadButtonDefaultRender() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                NumpadButton(
                    modifier = Modifier,
                    type = NumpadButtonType.DEFAULT,
                    text = "5",
                    onClick = {}
                )
            }
        }
    }

    @OptIn(ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun numpadButtonPrimaryRender() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                NumpadButton(
                    modifier = Modifier,
                    type = NumpadButtonType.PRIMARY,
                    text = "0",
                    onClick = {}
                )
            }
        }
    }

    @OptIn(ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun numpadButtonOperatorRender() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                NumpadButton(
                    modifier = Modifier,
                    type = NumpadButtonType.OPERATOR,
                    text = "+",
                    onClick = {}
                )
            }
        }
    }
}