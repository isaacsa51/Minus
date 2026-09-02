package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.budget.pill.BudgetPill
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
        maxPercentDifference = 10.0,
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
                        onOpenBudgetSheet = { },
                    )
                }
            }
        }
    }

    @Test
    fun budgetPillDynamicMode() {
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
                            totalSpentInPeriod = BigDecimal("12.50"),
                            dailyAllocation = BigDecimal("32.50"),
                            weeklyAllocation = BigDecimal("162.50"),
                            biweeklyAllocation = BigDecimal("487.50"),
                            monthlyAllocation = BigDecimal("487.50"),
                            isTodayOverDailyAllocation = false,
                        ),
                        budgetSettings = BudgetSettings(
                            totalBudget = BigDecimal("500.00"),
                            period = BudgetPeriod.DAILY,
                            startDate = LocalDate.now(),
                            currencyCode = "USD",
                            splitMode = BudgetSplitMode.DYNAMIC,
                        ),
                        viewPeriod = BudgetPeriod.DAILY,
                        currencyCode = "USD",
                        splitMode = BudgetSplitMode.DYNAMIC,
                        onOpenBudgetSheet = { },
                    )
                }
            }
        }
    }

    @Test
    fun budgetPillDynamicOverBudget() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetPill(
                        budgetState = BudgetState(
                            remainingToday = BigDecimal("-25.00"),
                            totalSpentToday = BigDecimal("150.00"),
                            dailyBudget = BigDecimal("100.00"),
                            daysRemaining = 5,
                            progress = 1.0f,
                            isOverBudget = true,
                            totalBudget = BigDecimal("500.00"),
                            totalSpentInPeriod = BigDecimal("600.00")
                        ),
                        budgetSettings = BudgetSettings(
                            totalBudget = BigDecimal("500.00"),
                            period = BudgetPeriod.DAILY,
                            startDate = LocalDate.now(),
                            currencyCode = "USD",
                            splitMode = BudgetSplitMode.DYNAMIC,
                        ),
                        viewPeriod = BudgetPeriod.DAILY,
                        currencyCode = "USD",
                        splitMode = BudgetSplitMode.DYNAMIC,
                        onOpenBudgetSheet = { },
                    )
                }
            }
        }
    }

    @Test
    fun budgetPillDailyExceeded() {
        Locale.setDefault(Locale.US)
        // 1000 budget, 600 spent in period (today: 500), 10 days remaining.
        //   remaining = 400, dailyAllocation = 400/10 = 40.00
        //   totalSpentToday (500) > dailyAllocation (40) → isTodayOverDailyAllocation = true
        //   totalSpentInPeriod (600) < totalBudget (1000) → isOverBudget = false
        // Label should switch to "Daily amount exceeded" (Monto diario excedido in ES).
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetPill(
                        budgetState = BudgetState(
                            remainingToday = BigDecimal("40.00"),
                            totalSpentToday = BigDecimal("500.00"),
                            dailyBudget = BigDecimal("100.00"),
                            daysRemaining = 10,
                            progress = 0.6f,
                            isOverBudget = false,
                            totalBudget = BigDecimal("1000.00"),
                            totalSpentInPeriod = BigDecimal("600.00"),
                            dailyAllocation = BigDecimal("40.00"),
                            isTodayOverDailyAllocation = true,
                        ),
                        budgetSettings = BudgetSettings(
                            totalBudget = BigDecimal("1000.00"),
                            period = BudgetPeriod.DAILY,
                            startDate = LocalDate.now(),
                            currencyCode = "USD",
                            splitMode = BudgetSplitMode.DYNAMIC,
                        ),
                        viewPeriod = BudgetPeriod.DAILY,
                        currencyCode = "USD",
                        splitMode = BudgetSplitMode.DYNAMIC,
                        onOpenBudgetSheet = { },
                    )
                }
            }
        }
    }

    @Test
    fun budgetPillDailyExceededWithProjection() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetPill(
                        budgetState = BudgetState(
                            remainingToday = BigDecimal("-10.00"),
                            totalSpentToday = BigDecimal("50.00"),
                            dailyBudget = BigDecimal("40.00"),
                            daysRemaining = 3,
                            progress = 0.42f,
                            isOverBudget = false,
                            totalBudget = BigDecimal("120.00"),
                            totalSpentInPeriod = BigDecimal("50.00"),
                            nextDailyAllocation = BigDecimal("35.00"),
                        ),
                        budgetSettings = BudgetSettings(
                            totalBudget = BigDecimal("120.00"),
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
            }
        }
    }

    @Test
    fun budgetPillWeeklyExceededWithProjection() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetPill(
                        budgetState = BudgetState(
                            remainingToday = BigDecimal("-500.00"),
                            totalSpentToday = BigDecimal("600.00"),
                            dailyBudget = BigDecimal("47.62"),
                            daysRemaining = 14,
                            progress = 0.6f,
                            isOverBudget = false,
                            totalBudget = BigDecimal("1000.00"),
                            totalSpentInPeriod = BigDecimal("600.00"),
                            totalSpentThisWeek = BigDecimal("600.00"),
                            dailyAllocation = BigDecimal("40.00"),
                            weeklyAllocation = BigDecimal("333.33"),
                            biweeklyAllocation = BigDecimal("466.67"),
                            isTodayOverDailyAllocation = true,
                            nextWeeklyAllocation = BigDecimal("200.00"),
                        ),
                        budgetSettings = BudgetSettings(
                            totalBudget = BigDecimal("1000.00"),
                            period = BudgetPeriod.BIWEEKLY,
                            startDate = LocalDate.now(),
                            currencyCode = "USD",
                            splitMode = BudgetSplitMode.DYNAMIC,
                        ),
                        viewPeriod = BudgetPeriod.WEEKLY,
                        currencyCode = "USD",
                        splitMode = BudgetSplitMode.DYNAMIC,
                        onOpenBudgetSheet = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    )
                }
            }
        }
    }

    @Test
    fun budgetPillDraftCountsDownCenteredAmount() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetPill(
                        budgetState = BudgetState(
                            remainingToday = BigDecimal("40.00"),
                            totalSpentToday = BigDecimal("0.00"),
                            dailyBudget = BigDecimal("40.00"),
                            daysRemaining = 3,
                            progress = 0f,
                            isOverBudget = false,
                            totalBudget = BigDecimal("120.00"),
                            totalSpentInPeriod = BigDecimal("0.00"),
                            periodTotalDays = 3,
                        ),
                        budgetSettings = BudgetSettings(
                            totalBudget = BigDecimal("120.00"),
                            period = BudgetPeriod.DAILY,
                            startDate = LocalDate.now(),
                            currencyCode = "USD",
                        ),
                        viewPeriod = BudgetPeriod.DAILY,
                        currencyCode = "USD",
                        centerRemainingAmount = true,
                        draftAmount = BigDecimal("10"),
                        onOpenBudgetSheet = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    )
                }
            }
        }
    }

    @Test
    fun budgetPillDraftPushesOverDailyAllocation() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetPill(
                        budgetState = BudgetState(
                            remainingToday = BigDecimal("30.00"),
                            totalSpentToday = BigDecimal("10.00"),
                            dailyBudget = BigDecimal("40.00"),
                            daysRemaining = 3,
                            progress = 0.08f,
                            isOverBudget = false,
                            totalBudget = BigDecimal("120.00"),
                            totalSpentInPeriod = BigDecimal("10.00"),
                            periodTotalDays = 3,
                        ),
                        budgetSettings = BudgetSettings(
                            totalBudget = BigDecimal("120.00"),
                            period = BudgetPeriod.DAILY,
                            startDate = LocalDate.now(),
                            currencyCode = "USD",
                        ),
                        viewPeriod = BudgetPeriod.DAILY,
                        currencyCode = "USD",
                        centerRemainingAmount = true,
                        draftAmount = BigDecimal("45"),
                        onOpenBudgetSheet = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    )
                }
            }
        }
    }
}
