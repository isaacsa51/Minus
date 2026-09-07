package com.serranoie.app.minus.wearsync

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.domain.model.SymbolPosition
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.sync.contract.BudgetStatePayload
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearBudgetStateProvider @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val budgetStateCalculator: BudgetStateCalculator,
) {

    suspend fun currentState(): BudgetStatePayload {
        val settings = budgetRepository.getBudgetSettingsSync()
            ?: return BudgetStatePayload(hasBudget = false)

        val userSettings = settingsRepository.getSettings()
        val allTransactions = budgetRepository.getTransactions().first()
        val paidOccurrences = budgetRepository.getPaidRecurrentOccurrences().first()

        val periodTransactions = budgetStateCalculator.filterPeriodTransactions(
            transactions = allTransactions,
            settings = settings,
            currentPeriodId = userSettings.currentPeriodId,
            currentPeriodStartedAtMillis = userSettings.currentPeriodStartedAt,
        )

        val state = budgetStateCalculator.calculateBudgetState(
            settings = settings,
            transactions = periodTransactions,
            currentDate = LocalDate.now(),
            paidOccurrences = paidOccurrences,
        )

        val currency = SupportedCurrency.findByCode(settings.currencyCode)
        val periodEnd = settings.getPeriodEndDate()
        val remainingInPeriod = state.totalBudget.subtract(state.totalSpentInPeriod)

        return BudgetStatePayload(
            hasBudget = true,
            currencyCode = settings.currencyCode,
            currencySymbol = currency?.symbol ?: "",
            symbolAtEnd = currency?.symbolPosition == SymbolPosition.END,
            period = settings.period.name,
            periodStartEpochDay = settings.startDate.toEpochDay(),
            periodEndEpochDay = periodEnd.toEpochDay(),
            daysRemaining = state.daysRemaining,
            periodTotalDays = state.periodTotalDays,
            totalBudget = state.totalBudget.toPlainString(),
            spentInPeriod = state.totalSpentInPeriod.toPlainString(),
            remainingInPeriod = remainingInPeriod.toPlainString(),
            spentToday = state.totalSpentToday.toPlainString(),
            remainingToday = state.remainingToday.toPlainString(),
            dailyBudget = state.dailyBudget.toPlainString(),
            progress = state.progress,
            isOverBudget = state.isOverBudget,
            generatedAtMillis = System.currentTimeMillis(),
        )
    }
}
