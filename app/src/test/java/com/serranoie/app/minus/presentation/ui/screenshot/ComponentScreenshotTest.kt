package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.StatCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetPill
import com.serranoie.app.minus.presentation.ui.theme.component.charts.CategoriesChartCard
import com.serranoie.app.minus.presentation.ui.theme.component.charts.SpendsChart
import com.serranoie.app.minus.presentation.ui.theme.component.date.DaysLeftCard
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class ComponentScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun statCardStandard() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                StatCard(
                    value = "$1,234.56",
                    label = "Total Spent",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }
    }

    @Test
    fun statCardWithCrossedValue() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                StatCard(
                    value = "$850.00",
                    label = "Remaining",
                    crossedValue = "$1,200.00",
                    crossedValueColor = Color(0xFFE57373),
                    valueOffsetWhenCrossedY = -4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }
    }

    @Test
    fun budgetPillHealthyBudget() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                BudgetPill(
                    budgetState = BudgetState(
                        remainingToday = BigDecimal("31.25"),
                        totalSpentToday = BigDecimal("18.75"),
                        dailyBudget = BigDecimal("50.00"),
                        daysRemaining = 12,
                        progress = 0.58f,
                        isOverBudget = false,
                        totalBudget = BigDecimal("900.00"),
                        totalSpentInPeriod = BigDecimal("522.45"),
                    ),
                    budgetSettings = BudgetSettings(
                        totalBudget = BigDecimal("900.00"),
                        period = BudgetPeriod.MONTHLY,
                        startDate = LocalDate.of(2026, 1, 1),
                        endDate = LocalDate.of(2026, 1, 30),
                        currencyCode = "USD",
                        daysInPeriod = 30,
                    ),
                    currencyCode = "USD",
                    viewPeriod = BudgetPeriod.DAILY,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    @Test
    fun budgetPillOverBudget() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                BudgetPill(
                    budgetState = BudgetState(
                        remainingToday = BigDecimal("-8.50"),
                        totalSpentToday = BigDecimal("58.50"),
                        dailyBudget = BigDecimal("50.00"),
                        daysRemaining = 3,
                        progress = 1.17f,
                        isOverBudget = true,
                        totalBudget = BigDecimal("300.00"),
                        totalSpentInPeriod = BigDecimal("345.30"),
                    ),
                    budgetSettings = BudgetSettings(
                        totalBudget = BigDecimal("300.00"),
                        period = BudgetPeriod.MONTHLY,
                        startDate = LocalDate.of(2026, 1, 1),
                        endDate = LocalDate.of(2026, 1, 30),
                        currencyCode = "USD",
                        daysInPeriod = 30,
                    ),
                    currencyCode = "USD",
                    viewPeriod = BudgetPeriod.MONTHLY,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    @Test
    fun daysLeftCardMidMonth() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                DaysLeftCard(
                    startDate = fixedDate(2026, 1, 1),
                    finishDate = fixedDate(2026, 1, 30),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    @Test
    fun spendsChartWithTransactions() {
        Locale.setDefault(Locale.US)

        val today = LocalDate.of(2026, 1, 15)
        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = BigDecimal("12.50"),
                comment = "Lunch",
                date = today.minusDays(7).atTime(12, 0),
                periodId = 7L,
            ),
            Transaction(
                id = 2L,
                amount = BigDecimal("45.00"),
                comment = "Groceries",
                date = today.minusDays(6).atTime(18, 0),
                periodId = 7L,
            ),
            Transaction(
                id = 3L,
                amount = BigDecimal("8.25"),
                comment = "Coffee",
                date = today.minusDays(5).atTime(9, 0),
                periodId = 7L,
            ),
            Transaction(
                id = 4L,
                amount = BigDecimal("32.00"),
                comment = "Gas",
                date = today.minusDays(4).atTime(8, 0),
                periodId = 7L,
            ),
            Transaction(
                id = 5L,
                amount = BigDecimal("15.99"),
                comment = "Subscription",
                date = today.minusDays(3).atTime(10, 0),
                periodId = 7L,
            ),
            Transaction(
                id = 6L,
                amount = BigDecimal("22.00"),
                comment = "Dinner",
                date = today.minusDays(2).atTime(19, 30),
                periodId = 7L,
            ),
            Transaction(
                id = 7L,
                amount = BigDecimal("67.50"),
                comment = "Groceries",
                date = today.minusDays(1).atTime(17, 0),
                periodId = 7L,
            ),
            Transaction(
                id = 8L,
                amount = BigDecimal("18.75"),
                comment = "Lunch",
                date = today.atTime(12, 30),
                periodId = 7L,
            ),
        )

        paparazzi.snapshot {
            MinusTheme {
                SpendsChart(
                    spends = transactions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(16.dp),
                )
            }
        }
    }

    @Test
    fun categoriesChartCardWithTransactions() {
        Locale.setDefault(Locale.US)

        val today = LocalDate.of(2026, 1, 15)
        val transactions = listOf(
            Transaction(
                id = 1L,
                amount = BigDecimal("120.50"),
                comment = "Groceries",
                date = today.minusDays(1).atTime(17, 0),
                periodId = 7L,
            ),
            Transaction(
                id = 2L,
                amount = BigDecimal("85.00"),
                comment = "Groceries",
                date = today.minusDays(3).atTime(18, 30),
                periodId = 7L,
            ),
            Transaction(
                id = 3L,
                amount = BigDecimal("45.00"),
                comment = "Dining out",
                date = today.minusDays(2).atTime(19, 0),
                periodId = 7L,
            ),
            Transaction(
                id = 4L,
                amount = BigDecimal("32.00"),
                comment = "Gas",
                date = today.minusDays(4).atTime(8, 0),
                periodId = 7L,
            ),
            Transaction(
                id = 5L,
                amount = BigDecimal("25.00"),
                comment = "Gas",
                date = today.minusDays(6).atTime(8, 30),
                periodId = 7L,
            ),
            Transaction(
                id = 6L,
                amount = BigDecimal("18.75"),
                comment = "Lunch",
                date = today.atTime(12, 30),
                periodId = 7L,
            ),
            Transaction(
                id = 7L,
                amount = BigDecimal("15.99"),
                comment = "Subscription",
                date = today.minusDays(5).atTime(10, 0),
                periodId = 7L,
            ),
            Transaction(
                id = 8L,
                amount = BigDecimal("8.25"),
                comment = "Coffee",
                date = today.minusDays(2).atTime(9, 0),
                periodId = 7L,
            ),
            Transaction(
                id = 9L,
                amount = BigDecimal("12.50"),
                comment = "Lunch",
                date = today.minusDays(7).atTime(12, 0),
                periodId = 7L,
            ),
        )

        paparazzi.snapshot {
            MinusTheme {
                CategoriesChartCard(
                    spends = transactions,
                    currency = "USD",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }
    }

    @Test
    fun multipleStatCardsRow() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    StatCard(
                        value = "$522",
                        label = "Spent",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = "$378",
                        label = "Remaining",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = "15",
                        label = "Days left",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    private fun fixedDate(year: Int, month: Int, day: Int): Date {
        return Date.from(
            LocalDate.of(year, month, day)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
        )
    }
}
