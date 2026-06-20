package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetPill
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class BudgetPillScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun budgetPillHealthy() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetPill(
                        budgetState = BudgetState(
                            remainingToday = BigDecimal("110.00"),
                            totalSpentToday = BigDecimal("12.50"),
                            dailyBudget = BigDecimal("122.50"),
                            daysRemaining = 15,
                            progress = 0.1f,
                            isOverBudget = false,
                            totalBudget = BigDecimal("500.00"),
                            totalSpentInPeriod = BigDecimal("12.50")
                        ),
                        budgetSettings = BudgetSettings(
                            totalBudget = BigDecimal("500.00"),
                            period = BudgetPeriod.DAILY,
                            startDate = LocalDate.now(),
                            currencyCode = "USD"
                        ),
                        viewPeriod = BudgetPeriod.DAILY,
                        currencyCode = "USD",
                        onOpenSettings = { },
                        onOpenBudgetSheet = { },
                    )
                }
            }
        }
    }

    @Test
    fun budgetPillOverBudget() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetPill(
                        budgetState = BudgetState(
                            remainingToday = BigDecimal("-50.00"),
                            totalSpentToday = BigDecimal("150.00"),
                            dailyBudget = BigDecimal("100.00"),
                            daysRemaining = 15,
                            progress = 1.0f,
                            isOverBudget = true,
                            totalBudget = BigDecimal("500.00"),
                            totalSpentInPeriod = BigDecimal("150.00")
                        ),
                        budgetSettings = BudgetSettings(
                            totalBudget = BigDecimal("1500.00"),
                            period = BudgetPeriod.DAILY,
                            startDate = LocalDate.now(),
                            currencyCode = "USD"
                        ),
                        viewPeriod = BudgetPeriod.DAILY,
                        currencyCode = "USD",
                        onOpenSettings = { },
                        onOpenBudgetSheet = { },
                    )
                }
            }
        }
    }
}