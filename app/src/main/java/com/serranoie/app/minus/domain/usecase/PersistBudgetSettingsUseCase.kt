package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.presentation.budget.BudgetPeriodManager
import com.serranoie.app.minus.presentation.budget.PeriodBoundaryResult
import javax.inject.Inject

class PersistBudgetSettingsUseCase @Inject constructor(
	private val periodManager: BudgetPeriodManager,
) {
	suspend operator fun invoke(
		settings: BudgetSettings,
		forceNewPeriodBoundary: Boolean,
	): PeriodBoundaryResult {
		return periodManager.persistBudgetSettings(
			settings = settings,
			forceNewPeriodBoundary = forceNewPeriodBoundary,
		)
	}
}
