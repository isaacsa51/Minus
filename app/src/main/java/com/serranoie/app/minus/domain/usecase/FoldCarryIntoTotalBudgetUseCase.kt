package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import java.math.BigDecimal
import javax.inject.Inject

class FoldCarryIntoTotalBudgetUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke() {
        if (settingsRepository.getString(DONE_KEY) != null) return

        val settings = budgetRepository.getBudgetSettingsSync()
        val carry = settings?.rollOverLimit?.takeIf { settings.rollOverCarryForward && it > BigDecimal.ZERO }
        if (settings != null && carry != null) {
            budgetRepository.saveBudgetSettings(settings.copy(totalBudget = settings.totalBudget.add(carry)))
        }
        settingsRepository.setString(DONE_KEY, "true")
    }

    companion object {
        const val DONE_KEY = "carry_folded_into_total_budget"
    }
}
