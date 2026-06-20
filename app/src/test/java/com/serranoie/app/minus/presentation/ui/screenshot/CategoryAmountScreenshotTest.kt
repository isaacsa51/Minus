package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.CategoryAmount
import com.serranoie.app.minus.presentation.util.HarmonizedColorPalette
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class CategoryAmountScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun categoryAmountDefault() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                CategoryAmount(
                    value = "Comida",
                    amount = BigDecimal("1234.56"),
                    currency = "MXN",
                    modifier = Modifier,
                )
            }
        }
    }

    @Test
    fun categoryAmountSpecial() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                CategoryAmount(
                    value = "Transporte",
                    amount = BigDecimal("500.00"),
                    isSpecial = true,
                    currency = "MXN",
                    modifier = Modifier,
                )
            }
        }
    }
}
