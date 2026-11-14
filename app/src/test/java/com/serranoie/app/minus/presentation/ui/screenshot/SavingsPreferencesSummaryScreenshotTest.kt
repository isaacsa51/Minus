package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.presentation.ui.settings.savings.savingsPreferencesSummary
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class SavingsPreferencesSummaryScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun summaryWithAndWithoutGoal() {
        paparazzi.snapshot {
            MinusTheme {
                Surface {
                    Column(Modifier.padding(16.dp)) {
                        Text(savingsPreferencesSummary(SavingsPreferences(), "USD"), style = MaterialTheme.typography.bodySmall)
                        Text(
                            savingsPreferencesSummary(SavingsPreferences(savingsGoalAmount = BigDecimal("15000")), "USD"),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            savingsPreferencesSummary(
                                SavingsPreferences(savingsGoalAmount = BigDecimal("15000"), savingsGoalMonths = 6),
                                "USD",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
