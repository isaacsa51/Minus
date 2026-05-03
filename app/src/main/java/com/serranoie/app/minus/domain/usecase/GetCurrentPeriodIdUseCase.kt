package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.data.repository.SettingsRepository
import javax.inject.Inject

class GetCurrentPeriodIdUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(): Long {
        return settingsRepository.getCurrentPeriodId()
    }
}
