package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditMode
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditorState
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Numpad
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class NumpadScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun numpadIdle() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Numpad(
                    modifier = Modifier,
                    editorState = EditorState(
                        mode = EditMode.ADD,
                        rawSpentValue = "42",
                        stage = EditStage.IDLE,
                        currentSpent = "42",
                        currentComment = "",
                        editedTransaction = null,
                    ),
                    isCalculation = false,
                )
            }
        }
    }

    @Test
    fun numpadCalculationMode() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Numpad(
                    modifier = Modifier,
                    editorState = EditorState(
                        mode = EditMode.ADD,
                        rawSpentValue = "18.50+6.25",
                        stage = EditStage.EDIT_SPENT,
                        currentSpent = "24.75",
                        currentComment = "",
                        editedTransaction = null,
                    ),
                    isCalculation = true,
                )
            }
        }
    }
}
