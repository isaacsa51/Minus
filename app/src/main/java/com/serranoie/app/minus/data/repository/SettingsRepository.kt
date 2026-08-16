package com.serranoie.app.minus.data.repository

import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.ContrastMode
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

    suspend fun setPeriodEndAlreadyHandled(handled: Boolean)

    suspend fun setCurrentPeriod(periodId: Long, startedAt: Long)

    suspend fun setCurrentPeriodRollover(amount: BigDecimal, carryForward: Boolean)

    suspend fun setPendingRollover(amount: BigDecimal, strategy: RemainingBudgetStrategy)

    suspend fun clearPendingRollover()

    suspend fun setNotificationTime(hour: Int, minute: Int)

    suspend fun setRecurrentNotificationTime(hour: Int, minute: Int)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setTypographyMode(mode: TypographyMode)

    suspend fun setContrastMode(mode: ContrastMode)

    suspend fun setAppColorScheme(colorScheme: com.serranoie.app.minus.domain.model.AppColorScheme)

    suspend fun setLanguage(language: String)

    suspend fun setDynamicColorEnabled(enabled: Boolean)

    suspend fun setRoundedFontEnabled(enabled: Boolean)

    suspend fun setCreditQuickToggleEnabled(enabled: Boolean)

    suspend fun setShowPastTransactions(enabled: Boolean)

    suspend fun setCategoryPickerDirectPopupEnabled(enabled: Boolean)

    suspend fun setCategoryGridModeEnabled(enabled: Boolean)

    suspend fun setTutorialBoxCompleted(completed: Boolean)

    suspend fun setAnalyticsTutorialCompleted(completed: Boolean)

    suspend fun setAnalyticsSpendsTutorialCompleted(completed: Boolean)

    suspend fun setPeriodMappingMode(mode: com.serranoie.app.minus.domain.model.PeriodMappingMode)

    suspend fun setFirstLaunchTutorialStage(stage: com.serranoie.app.minus.domain.model.FirstLaunchTutorialStage)

    suspend fun setRecurrentPaymentsViewMode(mode: com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode)

    suspend fun setBudgetSplitViewPeriod(period: BudgetPeriod)

    fun observeBudgetEndDate(): Flow<Long?>

    suspend fun setBudgetEndDate(millis: Long?)

    fun observeMidnightTransitionOccurred(): Flow<Boolean>

    suspend fun setMidnightTransitionOccurred(occurred: Boolean)

    suspend fun persistLastPeriodSnapshot(periodEndDateMillis: Long, remainingAmount: BigDecimal)

    suspend fun getLastPeriodEnd(): Long?

    suspend fun getRemainingFromLastPeriod(): BigDecimal

    suspend fun getPendingRollover(): Pair<BigDecimal, com.serranoie.app.minus.domain.model.RemainingBudgetStrategy?>

    suspend fun setSavingsPreferences(prefs: SavingsPreferences)

    suspend fun clearEarlyFinish()

    suspend fun resetTutorials()

    suspend fun getString(key: String): String?

    suspend fun setString(key: String, value: String)

    suspend fun getLastSeenVersionCode(): Long?

    suspend fun setLastSeenVersionCode(code: Long)

    suspend fun resetLastSeenVersionCode()

    suspend fun clearLastPeriodSnapshot()
}
