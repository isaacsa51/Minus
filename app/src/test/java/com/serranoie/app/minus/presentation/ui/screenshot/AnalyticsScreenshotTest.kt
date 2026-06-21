package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.LocalWindowSize
import com.serranoie.app.minus.presentation.ui.analytics.Analytics
import com.serranoie.app.minus.presentation.ui.analytics.AnalyticsActions
import com.serranoie.app.minus.presentation.ui.analytics.AnalyticsState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class AnalyticsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun analyticsActivePeriod() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            AnalyticsPreview(
                state = sampleActivePeriodState(),
            )
        }
    }

    @Test
    fun analyticsFinishedPeriod() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            AnalyticsPreview(
                state = sampleFinishedPeriodState(),
            )
        }
    }
}

class AnalyticsTabletScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_C,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun analyticsTabletWideLayout() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            AnalyticsPreview(
                state = sampleActivePeriodState(),
                windowSizeClass = WindowWidthSizeClass.Expanded,
            )
        }
    }
}

@Composable
private fun AnalyticsPreview(
    state: AnalyticsState,
    windowSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
) {
    CompositionLocalProvider(
        LocalWindowSize provides windowSizeClass,
        LocalWindowInsets provides PaddingValues(0.dp),
    ) {
        MinusTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                Analytics(
                    state = state,
                    actions = AnalyticsActions(
                        onCreateNewPeriod = {},
                        onClose = {},
                        onExportCSV = {},
                    ),
                    activityResultRegistryOwner = null,
                )
            }
        }
    }
}

private fun sampleActivePeriodState(): AnalyticsState {
    val today = LocalDate.of(2026, 1, 15)
    val start = today.withDayOfMonth(1)
    val end = today.withDayOfMonth(30)

    val settings = BudgetSettings(
        totalBudget = BigDecimal("900.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = start,
        endDate = end,
        currencyCode = "USD",
        daysInPeriod = 30,
    )
    val budgetState = BudgetState(
        remainingToday = BigDecimal("31.25"),
        totalSpentToday = BigDecimal("18.75"),
        dailyBudget = BigDecimal("50.00"),
        daysRemaining = 12,
        progress = 0.58f,
        isOverBudget = false,
        totalBudget = BigDecimal("900.00"),
        totalSpentInPeriod = BigDecimal("522.45"),
    )

    val transactions = listOf(
        Transaction(
            id = 101L,
            amount = BigDecimal("18.75"),
            comment = "Lunch",
            date = today.minusDays(1).atTime(12, 30),
            periodId = 7L,
        ),
        Transaction(
            id = 102L,
            amount = BigDecimal("42.30"),
            comment = "Groceries",
            date = today.minusDays(2).atTime(18, 20),
            periodId = 7L,
        ),
        Transaction(
            id = 103L,
            amount = BigDecimal("6.25"),
            comment = "Coffee",
            date = today.minusDays(3).atTime(9, 15),
            periodId = 7L,
        ),
        Transaction(
            id = 104L,
            amount = BigDecimal("25.00"),
            comment = "Gas",
            date = today.minusDays(4).atTime(8, 0),
            periodId = 7L,
        ),
        Transaction(
            id = 105L,
            amount = BigDecimal("89.99"),
            comment = "Subscription",
            date = today.minusDays(5).atTime(10, 0),
            periodId = 7L,
        ),
    )

    return AnalyticsState(
        periodFinished = false,
        transactions = transactions,
        spends = transactions,
        wholeBudget = BigDecimal("900.00"),
        currencyCode = "USD",
        budgetSettingsForDisplay = settings,
        budgetStateForDisplay = budgetState,
        showRolloverStyleInBudgetDisplay = false,
        isLoading = false,
        startPeriodDate = fixedDate(start),
        finishPeriodDate = fixedDate(end),
        extraAffordableDaysFromRemaining = 0,
    )
}

private fun sampleFinishedPeriodState(): AnalyticsState {
    val start = LocalDate.of(2025, 12, 1)
    val end = LocalDate.of(2025, 12, 31)
    val actualEnd = LocalDate.of(2025, 12, 28)

    val settings = BudgetSettings(
        totalBudget = BigDecimal("900.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = start,
        endDate = end,
        currencyCode = "USD",
        daysInPeriod = 31,
    )
    val budgetState = BudgetState(
        remainingToday = BigDecimal("0.00"),
        totalSpentToday = BigDecimal("900.00"),
        dailyBudget = BigDecimal("50.00"),
        daysRemaining = 0,
        progress = 1.0f,
        isOverBudget = false,
        totalBudget = BigDecimal("900.00"),
        totalSpentInPeriod = BigDecimal("900.00"),
    )

    val transactions = listOf(
        Transaction(
            id = 201L,
            amount = BigDecimal("120.50"),
            comment = "Groceries",
            date = actualEnd.atTime(17, 0),
            periodId = 6L,
        ),
        Transaction(
            id = 202L,
            amount = BigDecimal("45.00"),
            comment = "Dining out",
            date = actualEnd.minusDays(1).atTime(19, 30),
            periodId = 6L,
        ),
    )

    return AnalyticsState(
        periodFinished = true,
        transactions = transactions,
        spends = transactions,
        wholeBudget = BigDecimal("900.00"),
        currencyCode = "USD",
        budgetSettingsForDisplay = settings,
        budgetStateForDisplay = budgetState,
        showRolloverStyleInBudgetDisplay = true,
        isLoading = false,
        startPeriodDate = fixedDate(start),
        finishPeriodDate = fixedDate(end),
        finishPeriodActualDate = fixedDate(actualEnd),
        extraAffordableDaysFromRemaining = 3,
    )
}

private fun fixedDate(localDate: LocalDate): Date = Date.from(
    localDate.atStartOfDay().toInstant(ZoneOffset.UTC)
)
