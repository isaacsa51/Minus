package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.presentation.ui.budget.BudgetPeriodManager
import javax.inject.Inject

class UpdatePeriodEndNotificationTimeUseCase @Inject constructor(
    private val periodManager: BudgetPeriodManager,
) {
    suspend operator fun invoke(hour: Int, minute: Int) {
        periodManager.updatePeriodEndNotificationTime(hour, minute)
    }

    suspend fun updateRecurrentNotificationTime(hour: Int, minute: Int) {
        periodManager.updateRecurrentNotificationTime(hour, minute)
    }
}
