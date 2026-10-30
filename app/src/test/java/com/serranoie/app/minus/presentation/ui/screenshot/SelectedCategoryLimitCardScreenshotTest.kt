package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.serranoie.app.minus.presentation.ui.editor.category.SelectedCategoryLimitCard
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class SelectedCategoryLimitCardScreenshotTest {
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
            windowStart = LocalDate.of(2026, 11, 9),
            windowEnd = LocalDate.of(2026, 11, 15),
            pace = pace,
        )

    @Test
    fun selectedCategoryLimitCard() {
        paparazzi.snapshot {
            MinusTheme {
                Surface {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        SelectedCategoryLimitCard(
                            status = status("Transport", "30", "120", CategoryLimitUnit.PERIOD, CategoryLimitPace.ON_TRACK),
                            pendingAmount = BigDecimal("15"),
                            currencyCode = "USD",
                        )
                        SelectedCategoryLimitCard(
                            status = status("Coffee", "48", "50", CategoryLimitUnit.WEEKLY, CategoryLimitPace.AHEAD),
                            pendingAmount = BigDecimal("12"),
                            currencyCode = "USD",
                        )
                    }
                }
            }
        }
    }
}
