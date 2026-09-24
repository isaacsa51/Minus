package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.LeftoverChoice
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditMode
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditorState
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.LeftoverChoiceList
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.Numpad
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
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
    fun numpadThousandsShortcut() {
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
                    showThousandsShortcut = true,
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

    @Test
    fun numpadLeftoverChoice() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme(darkTheme = true) {
                Surface(modifier = Modifier.size(width = 393.dp, height = 360.dp)) {
                    Numpad(
                        editorState = EditorState(
                            mode = EditMode.ADD,
                            rawSpentValue = "",
                            stage = EditStage.IDLE,
                            currentSpent = "",
                            currentComment = "",
                            editedTransaction = null,
                        ),
                        leftContent = {
                            LeftoverChoiceList(
                                amount = BigDecimal("25.00"),
                                remainingToday = BigDecimal("100.00"),
                                dailyBudget = BigDecimal("100.00"),
                                daysRemaining = 8,
                                currencyCode = "USD",
                                selected = LeftoverChoice.SPREAD,
                                onSelect = {},
                            )
                        },
                        backspaceIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                    )
                }
            }
        }
    }
}
