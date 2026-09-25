package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.android.resources.NightMode
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.theme.BudgetStatusColors
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.budget.pill.BudgetPill
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

@Composable
private fun PillRow(spent: String, budget: String, remaining: String, over: Boolean) {
    BudgetPill(
        budgetState = BudgetState(
            remainingToday = BigDecimal(remaining),
            totalSpentToday = BigDecimal(spent),
            dailyBudget = BigDecimal(budget),
            daysRemaining = 15,
            progress = 0f,
            isOverBudget = over,
            totalBudget = BigDecimal(budget),
            totalSpentInPeriod = BigDecimal(spent),
        ),
        budgetSettings = BudgetSettings(
            totalBudget = BigDecimal(budget),
            period = BudgetPeriod.DAILY,
            startDate = LocalDate.now(),
            currencyCode = "USD",
        ),
        viewPeriod = BudgetPeriod.DAILY,
        currencyCode = "USD",
        onOpenBudgetSheet = { },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
    )
}

@Composable
internal fun BudgetPillPaletteGallery() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        PillRow(spent = "12.50", budget = "500.00", remaining = "110.00", over = false)
        PillRow(spent = "125.00", budget = "500.00", remaining = "80.00", over = false)
        PillRow(spent = "250.00", budget = "500.00", remaining = "37.00", over = false)
        PillRow(spent = "375.00", budget = "500.00", remaining = "20.00", over = false)
        PillRow(spent = "480.00", budget = "500.00", remaining = "5.00", over = false)
        PillRow(spent = "550.00", budget = "500.00", remaining = "-50.00", over = true)
    }
}

class BudgetPillPaletteScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun budgetPillPalette_light() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme { BudgetPillPaletteGallery() }
        }
    }

    @Test
    fun budgetPillPalette_overriddenScale() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme(
                budgetStatusColors = BudgetStatusColors(
                    good = Color(0xFF4FC3F7),
                    notGood = Color(0xFF9575CD),
                    bad = Color(0xFFF06292),
                ),
            ) { BudgetPillPaletteGallery() }
        }
    }
}

class BudgetPillPaletteNightScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT),
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun budgetPillPalette_dark() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme { BudgetPillPaletteGallery() }
        }
    }
}
