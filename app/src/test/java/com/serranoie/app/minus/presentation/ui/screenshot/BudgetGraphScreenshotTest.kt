package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.analytics.AnalyticsState
import com.serranoie.app.minus.presentation.ui.analytics.GraphGranularity
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetGraph
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetGraphViewMode
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

class BudgetGraphScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 0.1,
    )

    private fun fixedDate(year: Int, month: Int, day: Int): Date {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    private fun sampleSettings(start: LocalDate, end: LocalDate) = BudgetSettings(
        totalBudget = BigDecimal("1000"),
        period = BudgetPeriod.MONTHLY,
        startDate = start,
        endDate = end,
        currencyCode = "USD",
    )

    private fun mockTransactions(start: LocalDate): List<Transaction> {
        return listOf(
            Transaction(amount = BigDecimal("150.00"), date = start.atTime(10, 0)),
            Transaction(amount = BigDecimal("85.50"), date = start.plusDays(5).atTime(10, 0)),
            Transaction(amount = BigDecimal("120.00"), date = start.plusDays(10).atTime(10, 0)),
            Transaction(amount = BigDecimal("300.00"), date = start.plusDays(20).atTime(10, 0))
        )
    }

    @Test
    fun budgetGraph_daysView_cumulative() {
        Locale.setDefault(Locale.US)
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 8, 30)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetGraph(
                        state = AnalyticsState(
                            spends = mockTransactions(start),
                            startPeriodDate = fixedDate(2026, 8, 1),
                            finishPeriodDate = fixedDate(2026, 8, 30),
                            budgetSettingsForDisplay = sampleSettings(start, end),
                            graphGranularity = GraphGranularity.DAYS,
                            currencyCode = "USD"
                        ),
                        onGranularityChanged = {},
                    )
                }
            }
        }
    }

    @Test
    fun budgetGraph_categoriesView() {
        Locale.setDefault(Locale.US)
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 8, 7)
        val categories = listOf(
            Category(id = 1L, name = "Groceries"),
            Category(id = 2L, name = "Transport"),
        )
        val spends = listOf(
            Transaction(amount = BigDecimal("150.00"), date = start.atTime(10, 0), categoryId = 1L),
            Transaction(amount = BigDecimal("85.50"), date = start.plusDays(2).atTime(10, 0), categoryId = 2L),
            Transaction(amount = BigDecimal("60.00"), date = start.plusDays(2).atTime(18, 0), categoryId = 1L),
            Transaction(amount = BigDecimal("120.00"), date = start.plusDays(5).atTime(10, 0), categoryId = null),
        )
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetGraph(
                        state = AnalyticsState(
                            spends = spends,
                            categories = categories,
                            startPeriodDate = fixedDate(2026, 8, 1),
                            finishPeriodDate = fixedDate(2026, 8, 7),
                            budgetSettingsForDisplay = sampleSettings(start, end),
                            graphGranularity = GraphGranularity.DAYS,
                            currencyCode = "USD"
                        ),
                        onGranularityChanged = {},
                        initialViewMode = BudgetGraphViewMode.CATEGORIES,
                    )
                }
            }
        }
    }

    @Test
    fun budgetGraph_categoriesView_selectedDayHighlight() {
        Locale.setDefault(Locale.US)
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 8, 7)
        val categories = listOf(
            Category(id = 1L, name = "Groceries"),
            Category(id = 2L, name = "Transport"),
        )
        val spends = listOf(
            Transaction(amount = BigDecimal("150.00"), date = start.atTime(10, 0), categoryId = 1L),
            Transaction(amount = BigDecimal("85.50"), date = start.plusDays(2).atTime(10, 0), categoryId = 2L),
            Transaction(amount = BigDecimal("60.00"), date = start.plusDays(2).atTime(18, 0), categoryId = 1L),
            Transaction(amount = BigDecimal("120.00"), date = start.plusDays(5).atTime(10, 0), categoryId = null),
        )
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetGraph(
                        state = AnalyticsState(
                            spends = spends,
                            categories = categories,
                            startPeriodDate = fixedDate(2026, 8, 1),
                            finishPeriodDate = fixedDate(2026, 8, 7),
                            budgetSettingsForDisplay = sampleSettings(start, end),
                            graphGranularity = GraphGranularity.DAYS,
                            currencyCode = "USD"
                        ),
                        onGranularityChanged = {},
                        initialViewMode = BudgetGraphViewMode.CATEGORIES,
                        selectedDate = start.plusDays(2),
                    )
                }
            }
        }
    }

    @Test
    fun budgetGraph_categoriesView_empty() {
        Locale.setDefault(Locale.US)
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 8, 7)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetGraph(
                        state = AnalyticsState(
                            spends = emptyList(),
                            startPeriodDate = fixedDate(2026, 8, 1),
                            finishPeriodDate = fixedDate(2026, 8, 7),
                            budgetSettingsForDisplay = sampleSettings(start, end),
                            graphGranularity = GraphGranularity.DAYS,
                            currencyCode = "USD"
                        ),
                        onGranularityChanged = {},
                        initialViewMode = BudgetGraphViewMode.CATEGORIES,
                    )
                }
            }
        }
    }

    @Test
    fun budgetGraph_totalView() {
        Locale.setDefault(Locale.US)
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 8, 30)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    BudgetGraph(
                        state = AnalyticsState(
                            spends = mockTransactions(start),
                            startPeriodDate = fixedDate(2026, 8, 1),
                            finishPeriodDate = fixedDate(2026, 8, 30),
                            budgetSettingsForDisplay = sampleSettings(start, end),
                            graphGranularity = GraphGranularity.TOTAL,
                            currencyCode = "USD"
                        ),
                        onGranularityChanged = {}
                    )
                }
            }
        }
    }
}
