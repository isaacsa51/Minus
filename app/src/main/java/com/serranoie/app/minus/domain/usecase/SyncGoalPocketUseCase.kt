package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.calculator.goalPocketUpdate
import com.serranoie.app.minus.domain.model.SavingsPreferences
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class SyncGoalPocketUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(previous: SavingsPreferences? = null, updated: SavingsPreferences? = null) {
        val prefs = updated ?: settingsRepository.observeSettings().first().savingsPreferences
        val existing = budgetRepository.getSavingsPockets().first().firstOrNull { it.isGoalPocket }
        goalPocketUpdate(existing, previous, prefs, LocalDate.now())?.let { budgetRepository.saveSavingsPocket(it) }
    }
}
