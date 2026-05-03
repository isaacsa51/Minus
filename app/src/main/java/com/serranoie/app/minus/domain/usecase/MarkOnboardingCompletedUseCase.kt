package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.data.repository.SettingsRepository
import javax.inject.Inject

class MarkOnboardingCompletedUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke() {
        settingsRepository.setOnboardingCompleted(true)
    }
}
