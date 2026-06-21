package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.RolloverDialog
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class RolloverDialogScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun rolloverDialogDefault() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                RolloverDialog(
                    remainingAmount = BigDecimal("125.50"),
                    currencyCode = "USD",
                    periodLabel = "January 2026",
                    spentAmount = BigDecimal("774.50"),
                    onSplitEqually = {},
                    onCarryToNextDay = {},
                    onDismiss = {}
                )
            }
        }
    }
}