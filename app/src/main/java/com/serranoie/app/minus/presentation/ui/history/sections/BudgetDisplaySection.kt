package com.serranoie.app.minus.presentation.ui.history.sections

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetDisplay
import logcat.logcat
import java.math.BigDecimal
import java.time.ZoneId
import java.util.Date
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import java.time.LocalDate

internal fun LazyListScope.budgetDisplaySection(
    budgetState: BudgetState?,
    budgetSettings: BudgetSettings?,
    currencyCode: String,
    debtAdjustedBalance: BigDecimal? = null,
) {
    item("budget-display") {
        val startDate = budgetSettings?.startDate?.let {
            Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
        } ?: Date()

        val finishDate = budgetSettings?.getPeriodEndDate()?.let {
            Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
        }

        val budget = budgetState?.totalBudget ?: BigDecimal.ZERO
        logcat("History") {
            "BudgetDisplay input budget=$budget budgetStateTotal=${budgetState?.totalBudget} budgetSettingsTotal=${budgetSettings?.totalBudget} rollOverLimit=${budgetSettings?.rollOverLimit} rollOverCarry=${budgetSettings?.rollOverCarryForward}"
        }

        BudgetDisplay(
            budget = budget,
            budgetState = budgetState,
            budgetSettings = budgetSettings,
            currencyCode = currencyCode,
            bigVariant = true,
            modifier = Modifier.fillMaxWidth(),
            startDate = startDate,
            finishDate = finishDate,
            debtAdjustedBalance = debtAdjustedBalance,
        )
    }
}

@PreviewLightDark
@Composable
private fun BudgetDisplaySectionPreview() {
    val today = LocalDate.now()
    MinusTheme {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            budgetDisplaySection(
                budgetState = BudgetState(
                    remainingToday = BigDecimal("50.00"),
                    totalSpentToday = BigDecimal("10.00"),
                    dailyBudget = BigDecimal("50.00"),
                    daysRemaining = 15,
                    progress = 0.2f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("1500.00"),
                    totalSpentInPeriod = BigDecimal("250.00"),
                ),
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("1500.00"),
                    period = BudgetPeriod.MONTHLY,
                    startDate = today.minusDays(15),
                    endDate = today.plusDays(15),
                    currencyCode = "USD",
                    daysInPeriod = 31,
                ),
                currencyCode = "USD",
            )
        }
    }
}
