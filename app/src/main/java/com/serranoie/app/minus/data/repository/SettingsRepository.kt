package com.serranoie.app.minus.data.repository

import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.ThemeMode
import com.serranoie.app.minus.domain.model.TypographyMode
import com.serranoie.app.minus.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface SettingsRepository {

    fun observeSettings(): Flow<UserSettings>

    fun observeCurrentPeriodRollover(): Flow<Pair<BigDecimal, Boolean>>

    fun observeCurrentPeriodBoundary(): Flow<Pair<Long, Long>>

    suspend fun getCurrentPeriodId(): Long

    suspend fun getSettings(): UserSettings

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setEarlyFinishActive(active: Boolean, actualDate: Long, originalEndDate: Long)

    suspend fun setCurrentPeriod(periodId: Long, startedAt: Long)

    suspend fun setCurrentPeriodRollover(amount: BigDecimal, carryForward: Boolean)

    suspend fun setPendingRollover(amount: BigDecimal, strategy: RemainingBudgetStrategy)

    suspend fun clearPendingRollover()

    suspend fun setNotificationTime(hour: Int, minute: Int)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setTypographyMode(mode: TypographyMode)

    suspend fun setLanguage(language: String)

    suspend fun setDynamicColorEnabled(enabled: Boolean)

    suspend fun setRecurrentPaymentsViewMode(mode: com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode)

    suspend fun setBudgetSplitViewPeriod(period: BudgetPeriod)

    suspend fun setSavingsPreferences(prefs: SavingsPreferences)

    suspend fun clearEarlyFinish()
}
