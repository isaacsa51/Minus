package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import javax.inject.Inject

class ObserveCurrentPeriodRolloverUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {
    operator fun invoke(): Flow<Pair<BigDecimal, Boolean>> {
        return settingsRepository.observeCurrentPeriodRollover()
    }
}
