package com.serranoie.app.minus.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.ThemeMode
import com.serranoie.app.minus.domain.model.TypographyMode
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.time.CURRENT_PERIOD_ROLLOVER_AMOUNT_KEY_NAME
import com.serranoie.app.minus.domain.time.CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD_KEY_NAME
import com.serranoie.app.minus.domain.time.PENDING_ROLLOVER_AMOUNT_KEY_NAME
import com.serranoie.app.minus.domain.time.PENDING_ROLLOVER_STRATEGY_KEY_NAME
import com.serranoie.app.minus.presentation.settingsDataStore
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

const val SETTINGS_DATASTORE_NAME = "settings"
const val ONBOARDING_COMPLETED_KEY_NAME = "onboarding_completed"
const val NOTIFICATION_HOUR_KEY_NAME = "notification_hour"
const val NOTIFICATION_MINUTE_KEY_NAME = "notification_minute"
const val RECURRENT_NOTIFICATION_HOUR_KEY_NAME = "recurrent_notification_hour"
const val RECURRENT_NOTIFICATION_MINUTE_KEY_NAME = "recurrent_notification_minute"
const val THEME_MODE_KEY_NAME = "theme_mode"
const val TYPOGRAPHY_MODE_KEY_NAME = "typography_mode"
const val DYNAMIC_COLOR_KEY_NAME = "dynamic_color_enabled"
const val CREDIT_QUICK_TOGGLE_FEATURE_KEY_NAME = "credit_quick_toggle_feature_enabled"
const val CATEGORY_PICKER_DIRECT_POPUP_KEY_NAME = "category_picker_direct_popup_enabled"
const val CATEGORY_GRID_MODE_KEY_NAME = "category_grid_mode_enabled"
const val RECURRENT_PAYMENTS_VIEW_MODE_KEY_NAME = "recurrent_payments_view_mode"
const val EARLY_FINISH_ACTIVE_KEY_NAME = "early_finish_active"
const val EARLY_FINISH_ACTUAL_DATE_KEY_NAME = "early_finish_actual_date_millis"
const val EARLY_FINISH_ORIGINAL_END_DATE_KEY_NAME = "early_finish_original_end_date_millis"
const val CURRENT_PERIOD_STARTED_AT_KEY_NAME = "current_period_started_at_millis"
const val CURRENT_PERIOD_ID_KEY_NAME = "current_period_id"
const val BUDGET_SPLIT_VIEW_PERIOD_KEY_NAME = "budget_split_view_period"

private val ONBOARDING_COMPLETED = booleanPreferencesKey(ONBOARDING_COMPLETED_KEY_NAME)
private val EARLY_FINISH_ACTIVE = booleanPreferencesKey(EARLY_FINISH_ACTIVE_KEY_NAME)
private val EARLY_FINISH_ACTUAL_DATE = longPreferencesKey(EARLY_FINISH_ACTUAL_DATE_KEY_NAME)
private val EARLY_FINISH_ORIGINAL_END_DATE =
    longPreferencesKey(EARLY_FINISH_ORIGINAL_END_DATE_KEY_NAME)
private val CURRENT_PERIOD_STARTED_AT = longPreferencesKey(CURRENT_PERIOD_STARTED_AT_KEY_NAME)
private val CURRENT_PERIOD_ID = longPreferencesKey(CURRENT_PERIOD_ID_KEY_NAME)
private val NOTIFICATION_HOUR = intPreferencesKey(NOTIFICATION_HOUR_KEY_NAME)
private val NOTIFICATION_MINUTE = intPreferencesKey(NOTIFICATION_MINUTE_KEY_NAME)
private val THEME_MODE = stringPreferencesKey(THEME_MODE_KEY_NAME)
private val TYPOGRAPHY_MODE = stringPreferencesKey(TYPOGRAPHY_MODE_KEY_NAME)
private val DYNAMIC_COLOR = booleanPreferencesKey(DYNAMIC_COLOR_KEY_NAME)
private val RECURRENT_PAYMENTS_VIEW_MODE =
    stringPreferencesKey(RECURRENT_PAYMENTS_VIEW_MODE_KEY_NAME)
private val CURRENT_PERIOD_ROLLOVER_AMOUNT =
    stringPreferencesKey(CURRENT_PERIOD_ROLLOVER_AMOUNT_KEY_NAME)
private val CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD =
    booleanPreferencesKey(CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD_KEY_NAME)
private val PENDING_ROLLOVER_AMOUNT = stringPreferencesKey(PENDING_ROLLOVER_AMOUNT_KEY_NAME)
private val PENDING_ROLLOVER_STRATEGY = stringPreferencesKey(PENDING_ROLLOVER_STRATEGY_KEY_NAME)
private val BUDGET_SPLIT_VIEW_PERIOD = stringPreferencesKey(BUDGET_SPLIT_VIEW_PERIOD_KEY_NAME)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SettingsRepository {

    override fun observeSettings(): Flow<UserSettings> {
        return context.settingsDataStore.data.map { preferences ->
            UserSettings(
                onboardingCompleted = preferences[ONBOARDING_COMPLETED] ?: false,
                earlyFinishActive = preferences[EARLY_FINISH_ACTIVE] ?: false,
                earlyFinishActualDate = preferences[EARLY_FINISH_ACTUAL_DATE] ?: 0L,
                earlyFinishOriginalEndDate = preferences[EARLY_FINISH_ORIGINAL_END_DATE] ?: 0L,
                currentPeriodStartedAt = preferences[CURRENT_PERIOD_STARTED_AT] ?: 0L,
                currentPeriodId = preferences[CURRENT_PERIOD_ID] ?: 0L,
                notificationHour = preferences[NOTIFICATION_HOUR]
                    ?: UserSettings.DEFAULT_NOTIFICATION_HOUR,
                notificationMinute = preferences[NOTIFICATION_MINUTE]
                    ?: UserSettings.DEFAULT_NOTIFICATION_MINUTE,
                themeMode = preferences[THEME_MODE]?.toThemeMode() ?: ThemeMode.SYSTEM,
                typographyMode = preferences[TYPOGRAPHY_MODE]?.toTypographyMode()
                    ?: TypographyMode.EXPRESSIVE,
                dynamicColorEnabled = preferences[DYNAMIC_COLOR] ?: false,
                recurrentPaymentsViewMode = RecurrentPaymentsViewMode.fromName(
                    preferences[RECURRENT_PAYMENTS_VIEW_MODE]
                ),
                budgetSplitViewPeriod = preferences[BUDGET_SPLIT_VIEW_PERIOD]?.let { name ->
                    try { BudgetPeriod.valueOf(name) } catch (_: Exception) { null }
                }
            )
        }
    }

    override suspend fun getSettings(): UserSettings {
        return observeSettings().first()
    }

    override fun observeCurrentPeriodRollover(): Flow<Pair<BigDecimal, Boolean>> {
        return context.settingsDataStore.data.map { preferences ->
            val amount = preferences[CURRENT_PERIOD_ROLLOVER_AMOUNT]
                ?.toBigDecimalOrNull()
                ?: BigDecimal.ZERO
            val carryForward = preferences[CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD] ?: false
            amount to carryForward
        }
    }

    override fun observeCurrentPeriodBoundary(): Flow<Pair<Long, Long>> {
        return context.settingsDataStore.data.map { preferences ->
            val startedAt = preferences[CURRENT_PERIOD_STARTED_AT] ?: 0L
            val periodId = preferences[CURRENT_PERIOD_ID] ?: 0L
            startedAt to periodId
        }
    }

    override suspend fun getCurrentPeriodId(): Long {
        return context.settingsDataStore.data.first()[CURRENT_PERIOD_ID] ?: 0L
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun setEarlyFinishActive(
        active: Boolean,
        actualDate: Long,
        originalEndDate: Long
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[EARLY_FINISH_ACTIVE] = active
            preferences[EARLY_FINISH_ACTUAL_DATE] = actualDate
            preferences[EARLY_FINISH_ORIGINAL_END_DATE] = originalEndDate
        }
    }

    override suspend fun setCurrentPeriod(periodId: Long, startedAt: Long) {
        context.settingsDataStore.edit { preferences ->
            preferences[CURRENT_PERIOD_ID] = periodId
            preferences[CURRENT_PERIOD_STARTED_AT] = startedAt
        }
    }

    override suspend fun setCurrentPeriodRollover(amount: BigDecimal, carryForward: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[CURRENT_PERIOD_ROLLOVER_AMOUNT] = amount.toPlainString()
            preferences[CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD] = carryForward
        }
    }

    override suspend fun setPendingRollover(amount: BigDecimal, strategy: RemainingBudgetStrategy) {
        context.settingsDataStore.edit { preferences ->
            preferences[PENDING_ROLLOVER_AMOUNT] = amount.toPlainString()
            preferences[PENDING_ROLLOVER_STRATEGY] = strategy.name
        }
    }

    override suspend fun clearPendingRollover() {
        context.settingsDataStore.edit { preferences ->
            preferences.remove(PENDING_ROLLOVER_AMOUNT)
            preferences.remove(PENDING_ROLLOVER_STRATEGY)
        }
    }

    override suspend fun setNotificationTime(hour: Int, minute: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[NOTIFICATION_HOUR] = hour
            preferences[NOTIFICATION_MINUTE] = minute
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    override suspend fun setTypographyMode(mode: TypographyMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[TYPOGRAPHY_MODE] = mode.name
        }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR] = enabled
        }
    }

    override suspend fun setRecurrentPaymentsViewMode(mode: RecurrentPaymentsViewMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[RECURRENT_PAYMENTS_VIEW_MODE] = mode.name
        }
    }

    override suspend fun setBudgetSplitViewPeriod(period: BudgetPeriod) {
        context.settingsDataStore.edit { preferences ->
            preferences[BUDGET_SPLIT_VIEW_PERIOD] = period.name
        }
    }

    override suspend fun clearEarlyFinish() {
        context.settingsDataStore.edit { preferences ->
            preferences[EARLY_FINISH_ACTIVE] = false
            preferences[EARLY_FINISH_ACTUAL_DATE] = 0L
            preferences[EARLY_FINISH_ORIGINAL_END_DATE] = 0L
        }
    }

    private fun String.toThemeMode(): ThemeMode {
        return try {
            ThemeMode.valueOf(this)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    private fun String.toTypographyMode(): TypographyMode {
        return try {
            TypographyMode.valueOf(this)
        } catch (_: Exception) {
            TypographyMode.EXPRESSIVE
        }
    }
}
