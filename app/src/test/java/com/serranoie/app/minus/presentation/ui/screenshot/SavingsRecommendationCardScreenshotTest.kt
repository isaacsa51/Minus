package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.SavingsRecommendationCard
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale

class SavingsRecommendationCardScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun savingsRecommendationCardUnderSavings() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        SavingsRecommendationCard(
                            budget = BigDecimal("20000"),
                            spends = listOf(
                                Transaction(
                                    id = 1L,
                                    amount = BigDecimal("6000"),
                                    isRecurrent = true,
                                    comment = "Renta",
                                    date = LocalDateTime.now(),
                                    periodId = 7L,
                                ),
                                Transaction(
                                    id = 2L,
                                    amount = BigDecimal("4000"),
                                    isRecurrent = false,
                                    comment = "Súper",
                                    date = LocalDateTime.now(),
                                    periodId = 7L,
                                ),
                            ),
                            currency = "MXN",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun savingsRecommendationCardOverBudget() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        SavingsRecommendationCard(
                            budget = BigDecimal("12000"),
                            spends = listOf(
                                Transaction(
                                    id = 1L,
                                    amount = BigDecimal("8000"),
                                    isRecurrent = true,
                                    comment = "Renta",
                                    date = LocalDateTime.now(),
                                    periodId = 7L,
                                ),
                                Transaction(
                                    id = 2L,
                                    amount = BigDecimal("5000"),
                                    isRecurrent = false,
                                    comment = "Gastos",
                                    date = LocalDateTime.now(),
                                    periodId = 7L,
                                ),
                            ),
                            currency = "MXN",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
