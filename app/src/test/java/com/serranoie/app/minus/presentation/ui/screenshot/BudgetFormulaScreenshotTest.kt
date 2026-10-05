package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.budget.formula.BudgetFormulaContent
import com.serranoie.app.minus.presentation.ui.theme.component.budget.formula.BudgetFormulaRequest
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class BudgetFormulaScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    private val reserved = listOf(
        Transaction(amount = BigDecimal("4152.00"), comment = "terreno", date = LocalDate.of(2026, 10, 5).atStartOfDay()),
        Transaction(amount = BigDecimal("400.00"), comment = "gym", date = LocalDate.of(2026, 10, 11).atStartOfDay()),
    )

    private val carryOverState = BudgetState(
        remainingToday = BigDecimal("-3986.64"),
        totalSpentToday = BigDecimal.ZERO,
        dailyBudget = BigDecimal("535.13"),
        daysRemaining = 12,
        progress = 1f,
        isOverBudget = false,
        totalBudget = BigDecimal("8027.00"),
        totalSpentInPeriod = BigDecimal("6127.17"),
        totalSpentThisWeek = BigDecimal("6127.17"),
        periodTotalDays = 15,
        reservedCharges = reserved,
        splitBudget = BigDecimal("8027.00"),
    )

    private val settings = BudgetSettings(
        totalBudget = BigDecimal("8027.00"),
        period = BudgetPeriod.BIWEEKLY,
        startDate = LocalDate.of(2026, 10, 1),
    )

    @Test
    fun budgetFormulaCarryOverWithReservedCharges() {
        snapshot(
            BudgetFormulaRequest(
                budgetState = carryOverState,
                budgetSettings = settings,
                viewPeriod = BudgetPeriod.DAILY,
                splitMode = BudgetSplitMode.CARRY_OVER,
                currencyCode = "MXN",
            )
        )
    }

    @Test
    fun budgetFormulaStaticWeeklyWithReservedCharges() {
        snapshot(
            BudgetFormulaRequest(
                budgetState = carryOverState.copy(totalSpentThisWeek = BigDecimal("5332.17")),
                budgetSettings = settings,
                viewPeriod = BudgetPeriod.WEEKLY,
                splitMode = BudgetSplitMode.STATIC,
                currencyCode = "MXN",
            )
        )
    }

    @Test
    fun budgetFormulaDynamicWeeklyHealthy() {
        snapshot(
            BudgetFormulaRequest(
                budgetState = carryOverState.copy(
                    remainingToday = BigDecimal("1200.00"),
                    totalSpentInPeriod = BigDecimal("1580.17"),
                    totalSpentThisWeek = BigDecimal("980.00"),
                    reservedCharges = reserved,
                ),
                budgetSettings = settings,
                viewPeriod = BudgetPeriod.WEEKLY,
                splitMode = BudgetSplitMode.DYNAMIC,
                currencyCode = "MXN",
            )
        )
    }

    private fun snapshot(request: BudgetFormulaRequest) {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot { FormulaCard(request) }
    }

    @Composable
    private fun FormulaCard(request: BudgetFormulaRequest) {
        MinusTheme(darkTheme = true) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.scrim)
                    .padding(24.dp)
            ) {
                Card(
                    modifier = Modifier.widthIn(max = 400.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    BudgetFormulaContent(request)
                }
            }
        }
    }
}
