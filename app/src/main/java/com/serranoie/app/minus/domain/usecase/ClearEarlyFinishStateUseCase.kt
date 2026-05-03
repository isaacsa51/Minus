package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.presentation.budget.BudgetPeriodManager
import javax.inject.Inject

class ClearEarlyFinishStateUseCase @Inject constructor(
    private val periodManager: BudgetPeriodManager,
) {
    suspend operator fun invoke() {
        periodManager.clearEarlyFinishState()
    }
}
