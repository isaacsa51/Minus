package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetDisplay
import com.serranoie.app.minus.presentation.ui.theme.component.budget.CountDaysChip
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class BudgetDisplayScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    private fun fixedDate(year: Int, month: Int, day: Int): Date {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC))
    }

    @Test
    fun budgetDisplayHealthy() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetDisplay(
                        budget = BigDecimal("500.00"),
                        budgetState = BudgetState(
                            remainingToday = BigDecimal("45.50"),
                            totalSpentToday = BigDecimal("12.50"),
                            dailyBudget = BigDecimal("58.00"),
                            daysRemaining = 15,
                            progress = 0.21f,
                            isOverBudget = false,
                            totalBudget = BigDecimal("500.00"),
                            totalSpentInPeriod = BigDecimal("100.00")
                        ),
                        budgetSettings = BudgetSettings(
                            totalBudget = BigDecimal("500.00"),
                            period = BudgetPeriod.MONTHLY,
                            startDate = LocalDate.of(2026, 1, 1),
                            currencyCode = "USD"
                        ),
                        currencyCode = "USD",
                        bigVariant = true,
                        startDate = fixedDate(2026, 1, 1),
                        finishDate = fixedDate(2026, 1, 30)
                    )
                }
            }
        }
    }

    @Test
    fun budgetDisplayOverBudget() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetDisplay(
                        budget = BigDecimal("300.00"),
                        budgetState = BudgetState(
                            remainingToday = BigDecimal("-15.30"),
                            totalSpentToday = BigDecimal("73.30"),
                            dailyBudget = BigDecimal("58.00"),
                            daysRemaining = 3,
                            progress = 1.15f,
                            isOverBudget = true,
                            totalBudget = BigDecimal("300.00"),
                            totalSpentInPeriod = BigDecimal("345.30")
                        ),
                        budgetSettings = BudgetSettings(
                            totalBudget = BigDecimal("300.00"),
                            period = BudgetPeriod.WEEKLY,
                            startDate = LocalDate.of(2026, 1, 1),
                            currencyCode = "USD"
                        ),
                        currencyCode = "USD",
                        bigVariant = true,
                        startDate = fixedDate(2026, 1, 1),
                        finishDate = fixedDate(2026, 1, 4),
                        actualFinishDate = fixedDate(2025, 12, 31)
                    )
                }
            }
        }
    }

    @Test
    fun countDaysChipDefault() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    CountDaysChip(
                        modifier = Modifier,
                        fromDate = fixedDate(2026, 1, 1),
                        toDate = fixedDate(2026, 1, 15)
                    )
                }
            }
        }
    }

    @Test
    fun countDaysChipWithExtraDays() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    CountDaysChip(
                        modifier = Modifier,
                        fromDate = fixedDate(2026, 1, 1),
                        toDate = fixedDate(2026, 1, 15),
                        extraDays = 2
                    )
                }
            }
        }
    }
}