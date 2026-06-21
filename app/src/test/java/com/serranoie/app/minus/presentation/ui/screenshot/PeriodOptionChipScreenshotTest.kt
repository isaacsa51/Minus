package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.ui.Modifier
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.presentation.ui.theme.component.PeriodOptionChip
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class PeriodOptionChipScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun periodOptionChipRender() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                PeriodOptionChip(
                    period = BudgetPeriod.MONTHLY,
                    budgetPreview = BigDecimal("900"),
                    currencyCode = "USD",
                    isSelected = true,
                    onClick = {},
                    modifier = Modifier
                )
            }
        }
    }
}