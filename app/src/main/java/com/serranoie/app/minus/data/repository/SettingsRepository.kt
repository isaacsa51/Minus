package com.serranoie.app.minus.data.repository

import com.serranoie.app.minus.domain.model.ThemeMode
import com.serranoie.app.minus.domain.model.TypographyMode
import com.serranoie.app.minus.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user settings and preferences.
 * Provides a clean interface for loading and saving user preferences from DataStore.
 */
interface SettingsRepository {

    /**
     * Observe changes to user settings.
     */
    fun observeSettings(): Flow<UserSettings>

    /**
     * Get current settings synchronously (first emission).
     */
    suspend fun getSettings(): UserSettings

    /**
     * Save whether onboarding has been completed.
     */
    suspend fun setOnboardingCompleted(completed: Boolean)

    /**
     * Save early finish state.
     */
    suspend fun setEarlyFinishActive(active: Boolean, actualDate: Long, originalEndDate: Long)

    /**
     * Save current period information.
     */
    suspend fun setCurrentPeriod(periodId: Long, startedAt: Long)

    /**
     * Save notification time preferences.
     */
    suspend fun setNotificationTime(hour: Int, minute: Int)

    /**
     * Save theme mode preference.
     */
    suspend fun setThemeMode(mode: ThemeMode)

    /**
     * Save typography mode preference.
     */
    suspend fun setTypographyMode(mode: TypographyMode)

    /**
     * Save dynamic color preference.
     */
    suspend fun setDynamicColorEnabled(enabled: Boolean)

    /**
     * Clear early finish state.
     */
    suspend fun clearEarlyFinish()
}