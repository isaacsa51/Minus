package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.currency.ConversionPreferences
import com.serranoie.app.minus.domain.currency.ExchangeRate
import com.serranoie.app.minus.presentation.ui.currency.CurrencyPeriodSummaryContent
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class CurrencyConversionScreenshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5, renderingMode = SessionParams.RenderingMode.SHRINK)
    @Test fun pairedReferenceTotals() {
        paparazzi.snapshot {
            MinusTheme { Surface {
                CurrencyPeriodSummaryContent("USD", BigDecimal("100"), BigDecimal("12.50"),
                    ConversionPreferences("BRL", referenceRates = listOf(ExchangeRate("USD", "BRL", BigDecimal("4.9962"), "2026-10-08", false))), false, Modifier.padding(16.dp))
            } }
        }
    }
    @Test fun pairedManualTotalsShowSavedDate() {
        paparazzi.snapshot {
            MinusTheme { Surface {
                CurrencyPeriodSummaryContent("EUR", BigDecimal("200"), BigDecimal("20"),
                    ConversionPreferences("BRL", manualRates = listOf(ExchangeRate("EUR", "BRL", BigDecimal("6"), "2026-10-07", true))), false, Modifier.padding(16.dp))
            } }
        }
    }
    @Test fun missingRate() {
        paparazzi.snapshot {
            MinusTheme { Surface { CurrencyPeriodSummaryContent("USD", BigDecimal("100"), BigDecimal("12.50"), ConversionPreferences("BRL"), false, Modifier.padding(16.dp)) } }
        }
    }
}

class CurrencyConversionFrenchScreenshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "fr"), renderingMode = SessionParams.RenderingMode.SHRINK)
    @Test fun localizedManualTotalsAndDate() {
        paparazzi.snapshot {
            MinusTheme { Surface {
                CurrencyPeriodSummaryContent("EUR", BigDecimal("200"), BigDecimal("20.50"),
                    ConversionPreferences("BRL", manualRates = listOf(ExchangeRate("EUR", "BRL", BigDecimal("6"), "2026-10-07", true))), false, Modifier.padding(16.dp))
            } }
        }
    }
}

class CurrencyConversionSpanishScreenshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "es"), renderingMode = SessionParams.RenderingMode.SHRINK)
    @Test fun localizedManualTotalsAndDate() {
        paparazzi.snapshot {
            MinusTheme { Surface {
                CurrencyPeriodSummaryContent("EUR", BigDecimal("200"), BigDecimal("20.50"),
                    ConversionPreferences("BRL", manualRates = listOf(ExchangeRate("EUR", "BRL", BigDecimal("6"), "2026-10-07", true))), false, Modifier.padding(16.dp))
            } }
        }
    }
}
