package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.calculator.CategoryLimitPace
import com.serranoie.app.minus.domain.calculator.CategoryLimitStatus
import com.serranoie.app.minus.domain.model.CategoryLimit
import com.serranoie.app.minus.domain.model.CategoryLimitUnit
import com.serranoie.app.minus.presentation.ui.analytics.CategoryLimitsCard
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class CategoryLimitsCardScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
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

    private fun status(name: String, spent: String, limit: String, unit: CategoryLimitUnit, pace: CategoryLimitPace) =
        CategoryLimitStatus(
            name = name,
            limit = CategoryLimit(BigDecimal(limit), unit),
            spent = BigDecimal(spent),
            windowStart = LocalDate.of(2026, 11, 1),
            windowEnd = LocalDate.of(2026, 11, 30),
            pace = pace,
        )

    @Test
    fun categoryLimitsCard() {
        paparazzi.snapshot {
            MinusTheme {
                Surface {
                    Box(modifier = Modifier.padding(16.dp)) {
                        CategoryLimitsCard(
                            statuses = listOf(
                                status("Coffee", "62", "50", CategoryLimitUnit.WEEKLY, CategoryLimitPace.OVER),
                                status("Food", "240", "300", CategoryLimitUnit.PERIOD, CategoryLimitPace.AHEAD),
                                status("Transport", "30", "120", CategoryLimitUnit.PERIOD, CategoryLimitPace.ON_TRACK),
                            ),
                            currencyCode = "USD",
                        )
                    }
                }
            }
        }
    }
}
