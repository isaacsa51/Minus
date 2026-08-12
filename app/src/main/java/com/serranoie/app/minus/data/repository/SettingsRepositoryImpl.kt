package com.serranoie.app.minus.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.ContrastMode
import com.serranoie.app.minus.domain.model.FirstLaunchTutorialStage
import com.serranoie.app.minus.domain.model.PeriodMappingMode
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.SavingsSplitPreset
import com.serranoie.app.minus.domain.model.ThemeMode
import com.serranoie.app.minus.domain.model.TypographyMode
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.time.CURRENT_PERIOD_ROLLOVER_AMOUNT_KEY_NAME
import com.serranoie.app.minus.domain.time.CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD_KEY_NAME
import com.serranoie.app.minus.domain.time.PENDING_ROLLOVER_AMOUNT_KEY_NAME
import com.serranoie.app.minus.domain.time.PENDING_ROLLOVER_STRATEGY_KEY_NAME
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
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
const val COLOR_SCHEME_KEY_NAME = "app_color_scheme"
const val LANGUAGE_KEY_NAME = "language"
const val DYNAMIC_COLOR_KEY_NAME = "dynamic_color_enabled"
const val CREDIT_QUICK_TOGGLE_FEATURE_KEY_NAME = "credit_quick_toggle_feature_enabled"
const val SHOW_PAST_TRANSACTIONS_KEY_NAME = "show_past_transactions"
const val ROUNDED_FONT_KEY_NAME = "rounded_font_enabled"
const val CATEGORY_PICKER_DIRECT_POPUP_KEY_NAME = "category_picker_direct_popup_enabled"
const val CATEGORY_GRID_MODE_KEY_NAME = "category_grid_mode_enabled"
const val RECURRENT_PAYMENTS_VIEW_MODE_KEY_NAME = "recurrent_payments_view_mode"
const val EARLY_FINISH_ACTIVE_KEY_NAME = "early_finish_active"
const val EARLY_FINISH_ACTUAL_DATE_KEY_NAME = "early_finish_actual_date_millis"
const val EARLY_FINISH_ORIGINAL_END_DATE_KEY_NAME = "early_finish_original_end_date_millis"
const val CURRENT_PERIOD_STARTED_AT_KEY_NAME = "current_period_started_at_millis"
const val CURRENT_PERIOD_ID_KEY_NAME = "current_period_id"
const val BUDGET_SPLIT_VIEW_PERIOD_KEY_NAME = "budget_split_view_period"
const val SAVINGS_PRESET_KEY_NAME = "savings_preset"
const val SAVINGS_NEEDS_PCT_KEY_NAME = "savings_needs_pct"
const val SAVINGS_WANTS_PCT_KEY_NAME = "savings_wants_pct"
const val SAVINGS_SAVINGS_PCT_KEY_NAME = "savings_savings_pct"
const val SAVINGS_GOAL_AMOUNT_KEY_NAME = "savings_goal_amount"
const val SAVINGS_GOAL_MONTHS_KEY_NAME = "savings_goal_months"
const val TUTORIAL_BOX_COMPLETED_KEY_NAME = "tutorial_box_completed"
const val ANALYTICS_TUTORIAL_COMPLETED_KEY_NAME = "analytics_tutorial_completed"
const val ANALYTICS_SPENDS_TUTORIAL_COMPLETED_KEY_NAME = "analytics_spends_tutorial_completed"

private val ONBOARDING_COMPLETED = booleanPreferencesKey(ONBOARDING_COMPLETED_KEY_NAME)
private val EARLY_FINISH_ACTIVE = booleanPreferencesKey(EARLY_FINISH_ACTIVE_KEY_NAME)
private val EARLY_FINISH_ACTUAL_DATE = longPreferencesKey(EARLY_FINISH_ACTUAL_DATE_KEY_NAME)
private val EARLY_FINISH_ORIGINAL_END_DATE =
    longPreferencesKey(EARLY_FINISH_ORIGINAL_END_DATE_KEY_NAME)
private val CURRENT_PERIOD_STARTED_AT = longPreferencesKey(CURRENT_PERIOD_STARTED_AT_KEY_NAME)
private val CURRENT_PERIOD_ID = longPreferencesKey(CURRENT_PERIOD_ID_KEY_NAME)
private val NOTIFICATION_HOUR = intPreferencesKey(NOTIFICATION_HOUR_KEY_NAME)
private val NOTIFICATION_MINUTE = intPreferencesKey(NOTIFICATION_MINUTE_KEY_NAME)
private val RECURRENT_NOTIFICATION_HOUR = intPreferencesKey(RECURRENT_NOTIFICATION_HOUR_KEY_NAME)
private val RECURRENT_NOTIFICATION_MINUTE = intPreferencesKey(RECURRENT_NOTIFICATION_MINUTE_KEY_NAME)
private val THEME_MODE = stringPreferencesKey(THEME_MODE_KEY_NAME)
private val TYPOGRAPHY_MODE = stringPreferencesKey(TYPOGRAPHY_MODE_KEY_NAME)
private val CONTRAST_MODE = stringPreferencesKey("contrast_mode")
private val COLOR_SCHEME = stringPreferencesKey(COLOR_SCHEME_KEY_NAME)
private val LANGUAGE = stringPreferencesKey(LANGUAGE_KEY_NAME)
private val DYNAMIC_COLOR = booleanPreferencesKey(DYNAMIC_COLOR_KEY_NAME)
private val CREDIT_QUICK_TOGGLE_FEATURE_ENABLED =
    booleanPreferencesKey(CREDIT_QUICK_TOGGLE_FEATURE_KEY_NAME)
private val SHOW_PAST_TRANSACTIONS =
    booleanPreferencesKey(SHOW_PAST_TRANSACTIONS_KEY_NAME)
private val ROUNDED_FONT =
    booleanPreferencesKey(ROUNDED_FONT_KEY_NAME)
private val CATEGORY_PICKER_DIRECT_POPUP_ENABLED =
    booleanPreferencesKey(CATEGORY_PICKER_DIRECT_POPUP_KEY_NAME)
private val CATEGORY_GRID_MODE_ENABLED =
    booleanPreferencesKey(CATEGORY_GRID_MODE_KEY_NAME)
private val TUTORIAL_BOX_COMPLETED =
    booleanPreferencesKey(TUTORIAL_BOX_COMPLETED_KEY_NAME)
private val FIRST_LAUNCH_TUTORIAL_STAGE =
    stringPreferencesKey("first_launch_tutorial_stage")
private val RECURRENT_PAYMENTS_VIEW_MODE =
    stringPreferencesKey(RECURRENT_PAYMENTS_VIEW_MODE_KEY_NAME)
private val CURRENT_PERIOD_ROLLOVER_AMOUNT =
    stringPreferencesKey(CURRENT_PERIOD_ROLLOVER_AMOUNT_KEY_NAME)
private val CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD =
    booleanPreferencesKey(CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD_KEY_NAME)
private val BUDGET_END_DATE =
    longPreferencesKey("budget_end_date_millis")
private val MIDNIGHT_TRANSITION_OCCURRED =
    booleanPreferencesKey("midnight_transition_occurred")
private val LAST_PERIOD_END =
    longPreferencesKey("last_period_end_millis")
private val REMAINING_FROM_LAST_PERIOD =
    stringPreferencesKey("remaining_from_last_period")
private val PENDING_ROLLOVER_AMOUNT = stringPreferencesKey(PENDING_ROLLOVER_AMOUNT_KEY_NAME)
private val PENDING_ROLLOVER_STRATEGY = stringPreferencesKey(PENDING_ROLLOVER_STRATEGY_KEY_NAME)
private val LAST_SEEN_VERSION_CODE = longPreferencesKey("changelog_last_seen_version_code")
private val PERIOD_MAPPING_MODE = stringPreferencesKey("period_mapping_mode")
private val ANALYTICS_TUTORIAL_COMPLETED = booleanPreferencesKey(ANALYTICS_TUTORIAL_COMPLETED_KEY_NAME)
private val ANALYTICS_SPENDS_TUTORIAL_COMPLETED = booleanPreferencesKey(ANALYTICS_SPENDS_TUTORIAL_COMPLETED_KEY_NAME)
private val BUDGET_SPLIT_VIEW_PERIOD = stringPreferencesKey(BUDGET_SPLIT_VIEW_PERIOD_KEY_NAME)
private val SAVINGS_PRESET = stringPreferencesKey(SAVINGS_PRESET_KEY_NAME)
private val SAVINGS_NEEDS_PCT = intPreferencesKey(SAVINGS_NEEDS_PCT_KEY_NAME)
private val SAVINGS_WANTS_PCT = intPreferencesKey(SAVINGS_WANTS_PCT_KEY_NAME)
private val SAVINGS_SAVINGS_PCT = intPreferencesKey(SAVINGS_SAVINGS_PCT_KEY_NAME)
private val SAVINGS_GOAL_AMOUNT = stringPreferencesKey(SAVINGS_GOAL_AMOUNT_KEY_NAME)
private val SAVINGS_GOAL_MONTHS = intPreferencesKey(SAVINGS_GOAL_MONTHS_KEY_NAME)

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override fun observeSettings(): Flow<UserSettings> {
        return dataStore.data.map { preferences ->
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
                recurrentNotificationHour = preferences[RECURRENT_NOTIFICATION_HOUR]
                    ?: UserSettings.DEFAULT_RECURRENT_NOTIFICATION_HOUR,
                recurrentNotificationMinute = preferences[RECURRENT_NOTIFICATION_MINUTE]
                    ?: UserSettings.DEFAULT_RECURRENT_NOTIFICATION_MINUTE,
                themeMode = preferences[THEME_MODE]?.toThemeMode() ?: ThemeMode.SYSTEM,
                typographyMode = preferences[TYPOGRAPHY_MODE]?.toTypographyMode()
                    ?: TypographyMode.EXPRESSIVE,
                contrastMode = preferences[CONTRAST_MODE]?.toContrastMode()
                    ?: ContrastMode.NORMAL,
                colorScheme = preferences[COLOR_SCHEME]?.toAppColorScheme()
                    ?: com.serranoie.app.minus.domain.model.AppColorScheme.BRAND,
                language = preferences[LANGUAGE] ?: "en",
                dynamicColorEnabled = preferences[DYNAMIC_COLOR] ?: false,
                isRoundedFontEnabled = preferences[ROUNDED_FONT] ?: true,
                showPastTransactions = preferences[SHOW_PAST_TRANSACTIONS] ?: true,
                isCreditQuickToggleEnabled = preferences[CREDIT_QUICK_TOGGLE_FEATURE_ENABLED] ?: false,
                categoryPickerDirectPopupEnabled = preferences[CATEGORY_PICKER_DIRECT_POPUP_ENABLED] ?: false,
                categoryGridModeEnabled = preferences[CATEGORY_GRID_MODE_ENABLED] ?: false,
                tutorialBoxCompleted = preferences[TUTORIAL_BOX_COMPLETED] ?: false,
                firstLaunchTutorialStage = FirstLaunchTutorialStage.from(preferences[FIRST_LAUNCH_TUTORIAL_STAGE]),
                analyticsTutorialCompleted = preferences[ANALYTICS_TUTORIAL_COMPLETED] ?: false,
                analyticsSpendsTutorialCompleted = preferences[ANALYTICS_SPENDS_TUTORIAL_COMPLETED] ?: false,
                periodMappingMode = try {
                    PeriodMappingMode.valueOf(preferences[PERIOD_MAPPING_MODE] ?: "")
                } catch (_: Exception) {
                    PeriodMappingMode.ACTIVE_BUDGET
                },
                recurrentPaymentsViewMode = RecurrentPaymentsViewMode.fromName(
                    preferences[RECURRENT_PAYMENTS_VIEW_MODE]
                ),
                budgetSplitViewPeriod = preferences[BUDGET_SPLIT_VIEW_PERIOD]?.let { name ->
                    try {
                        BudgetPeriod.valueOf(name)
                    } catch (_: Exception) {
                        null
                    }
                },
                savingsPreferences = run {
                    val needsPct =
                        preferences[SAVINGS_NEEDS_PCT] ?: SavingsPreferences.DEFAULT_NEEDS_PCT
                    val wantsPct =
                        preferences[SAVINGS_WANTS_PCT] ?: SavingsPreferences.DEFAULT_WANTS_PCT
                    val savingsPct =
                        preferences[SAVINGS_SAVINGS_PCT] ?: SavingsPreferences.DEFAULT_SAVINGS_PCT
                    val preset = preferences[SAVINGS_PRESET]?.let { name ->
                        runCatching { SavingsSplitPreset.valueOf(name) }.getOrElse {
                            SavingsSplitPreset.fromValues(
                                needsPct, wantsPct, savingsPct
                            )
                        }
                    } ?: SavingsSplitPreset.fromValues(needsPct, wantsPct, savingsPct)
                    SavingsPreferences(
                        preset = preset,
                        needsPct = needsPct,
                        wantsPct = wantsPct,
                        savingsPct = savingsPct,
                        savingsGoalAmount = preferences[SAVINGS_GOAL_AMOUNT]?.toBigDecimalOrNull(),
                        savingsGoalMonths = preferences[SAVINGS_GOAL_MONTHS],
                    )
                })
        }
    }

    override suspend fun getSettings(): UserSettings {
        return observeSettings().first()
    }

    override fun observeCurrentPeriodRollover(): Flow<Pair<BigDecimal, Boolean>> {
        return dataStore.data.map { preferences ->
            val amount =
                preferences[CURRENT_PERIOD_ROLLOVER_AMOUNT]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val carryForward = preferences[CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD] ?: false
            amount to carryForward
        }
    }

    override fun observeCurrentPeriodBoundary(): Flow<Pair<Long, Long>> {
        return dataStore.data.map { preferences ->
            val startedAt = preferences[CURRENT_PERIOD_STARTED_AT] ?: 0L
            val periodId = preferences[CURRENT_PERIOD_ID] ?: 0L
            startedAt to periodId
        }
    }

    override suspend fun getCurrentPeriodId(): Long {
        return dataStore.data.first()[CURRENT_PERIOD_ID] ?: 0L
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun setEarlyFinishActive(
        active: Boolean, actualDate: Long, originalEndDate: Long
    ) {
        dataStore.edit { preferences ->
            preferences[EARLY_FINISH_ACTIVE] = active
            preferences[EARLY_FINISH_ACTUAL_DATE] = actualDate
            preferences[EARLY_FINISH_ORIGINAL_END_DATE] = originalEndDate
        }
    }

    override suspend fun setCurrentPeriod(periodId: Long, startedAt: Long) {
        dataStore.edit { preferences ->
            preferences[CURRENT_PERIOD_ID] = periodId
            preferences[CURRENT_PERIOD_STARTED_AT] = startedAt
        }
    }

    override suspend fun setCurrentPeriodRollover(amount: BigDecimal, carryForward: Boolean) {
        dataStore.edit { preferences ->
            preferences[CURRENT_PERIOD_ROLLOVER_AMOUNT] = amount.toPlainString()
            preferences[CURRENT_PERIOD_ROLLOVER_CARRY_FORWARD] = carryForward
        }
    }

    override suspend fun setPendingRollover(amount: BigDecimal, strategy: RemainingBudgetStrategy) {
        dataStore.edit { preferences ->
            preferences[PENDING_ROLLOVER_AMOUNT] = amount.toPlainString()
            preferences[PENDING_ROLLOVER_STRATEGY] = strategy.name
        }
    }

    override suspend fun clearPendingRollover() {
        dataStore.edit { preferences ->
            preferences.remove(PENDING_ROLLOVER_AMOUNT)
            preferences.remove(PENDING_ROLLOVER_STRATEGY)
        }
    }

    override suspend fun setNotificationTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_HOUR] = hour
            preferences[NOTIFICATION_MINUTE] = minute
        }
    }

    override suspend fun setRecurrentNotificationTime(hour: Int, minute: Int) {
        dataStore.edit { preferences ->
            preferences[RECURRENT_NOTIFICATION_HOUR] = hour
            preferences[RECURRENT_NOTIFICATION_MINUTE] = minute
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    override suspend fun setTypographyMode(mode: TypographyMode) {
        dataStore.edit { preferences ->
            preferences[TYPOGRAPHY_MODE] = mode.name
        }
    }

    override suspend fun setContrastMode(mode: ContrastMode) {
        dataStore.edit { preferences ->
            preferences[CONTRAST_MODE] = mode.name
        }
    }

    override suspend fun setAppColorScheme(colorScheme: com.serranoie.app.minus.domain.model.AppColorScheme) {
        dataStore.edit { preferences ->
            preferences[COLOR_SCHEME] = colorScheme.name
        }
    }

    override suspend fun setLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE] = language
        }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR] = enabled
        }
    }

    override suspend fun setRoundedFontEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ROUNDED_FONT] = enabled
        }
    }

    override suspend fun setCreditQuickToggleEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CREDIT_QUICK_TOGGLE_FEATURE_ENABLED] = enabled
        }
    }

    override suspend fun setShowPastTransactions(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_PAST_TRANSACTIONS] = enabled
        }
    }

    override suspend fun setCategoryPickerDirectPopupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CATEGORY_PICKER_DIRECT_POPUP_ENABLED] = enabled
        }
    }

    override suspend fun setCategoryGridModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CATEGORY_GRID_MODE_ENABLED] = enabled
        }
    }

    override suspend fun setTutorialBoxCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[TUTORIAL_BOX_COMPLETED] = completed
        }
    }

    override suspend fun setAnalyticsTutorialCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ANALYTICS_TUTORIAL_COMPLETED] = completed
        }
    }

    override suspend fun setAnalyticsSpendsTutorialCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ANALYTICS_SPENDS_TUTORIAL_COMPLETED] = completed
        }
    }

    override suspend fun setPeriodMappingMode(mode: PeriodMappingMode) {
        dataStore.edit { preferences ->
            preferences[PERIOD_MAPPING_MODE] = mode.name
        }
    }

    override suspend fun setFirstLaunchTutorialStage(stage: FirstLaunchTutorialStage) {
        dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_TUTORIAL_STAGE] = stage.name
        }
    }

    override suspend fun setRecurrentPaymentsViewMode(mode: RecurrentPaymentsViewMode) {
        dataStore.edit { preferences ->
            preferences[RECURRENT_PAYMENTS_VIEW_MODE] = mode.name
        }
    }

    override suspend fun setBudgetSplitViewPeriod(period: BudgetPeriod) {
        dataStore.edit { preferences ->
            preferences[BUDGET_SPLIT_VIEW_PERIOD] = period.name
        }
    }

    override fun observeBudgetEndDate(): Flow<Long?> {
        return dataStore.data.map { preferences ->
            preferences[BUDGET_END_DATE]
        }
    }

    override suspend fun setBudgetEndDate(millis: Long?) {
        dataStore.edit { preferences ->
            if (millis == null) {
                preferences.remove(BUDGET_END_DATE)
            } else {
                preferences[BUDGET_END_DATE] = millis
            }
        }
    }

    override fun observeMidnightTransitionOccurred(): Flow<Boolean> {
        return dataStore.data.map { it[MIDNIGHT_TRANSITION_OCCURRED] ?: false }
    }

    override suspend fun setMidnightTransitionOccurred(occurred: Boolean) {
        dataStore.edit { it[MIDNIGHT_TRANSITION_OCCURRED] = occurred }
    }

    override suspend fun persistLastPeriodSnapshot(
        periodEndDateMillis: Long,
        remainingAmount: BigDecimal
    ) {
        dataStore.edit { prefs ->
            prefs[LAST_PERIOD_END] = periodEndDateMillis
            prefs[REMAINING_FROM_LAST_PERIOD] = remainingAmount.toPlainString()
        }
    }

    override suspend fun getLastPeriodEnd(): Long? {
        return dataStore.data.first()[LAST_PERIOD_END]
    }

    override suspend fun getRemainingFromLastPeriod(): BigDecimal {
        val raw = dataStore.data.first()[REMAINING_FROM_LAST_PERIOD]
        return raw?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    override suspend fun getPendingRollover(): Pair<BigDecimal, RemainingBudgetStrategy?> {
        val prefs = dataStore.data.first()
        val amount = prefs[PENDING_ROLLOVER_AMOUNT]?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val strategy = prefs[PENDING_ROLLOVER_STRATEGY]?.let {
            runCatching { RemainingBudgetStrategy.valueOf(it) }.getOrNull()
        }
        return amount to strategy
    }

    override suspend fun setSavingsPreferences(prefs: SavingsPreferences) {
        dataStore.edit { preferences ->
            preferences[SAVINGS_PRESET] = prefs.preset.name
            preferences[SAVINGS_NEEDS_PCT] = prefs.needsPct
            preferences[SAVINGS_WANTS_PCT] = prefs.wantsPct
            preferences[SAVINGS_SAVINGS_PCT] = prefs.savingsPct
            val goalAmount = prefs.savingsGoalAmount
            if (goalAmount == null) {
                preferences.remove(SAVINGS_GOAL_AMOUNT)
            } else {
                preferences[SAVINGS_GOAL_AMOUNT] = goalAmount.toPlainString()
            }
            val goalMonths = prefs.savingsGoalMonths
            if (goalMonths == null) {
                preferences.remove(SAVINGS_GOAL_MONTHS)
            } else {
                preferences[SAVINGS_GOAL_MONTHS] = goalMonths
            }
        }
    }

    override suspend fun clearEarlyFinish() {
        dataStore.edit { preferences ->
            preferences[EARLY_FINISH_ACTIVE] = false
            preferences[EARLY_FINISH_ACTUAL_DATE] = 0L
            preferences[EARLY_FINISH_ORIGINAL_END_DATE] = 0L
        }
    }

    override suspend fun resetTutorials() {
        dataStore.edit { preferences ->
            preferences[TUTORIAL_BOX_COMPLETED] = false
            preferences[ANALYTICS_TUTORIAL_COMPLETED] = false
            preferences[FIRST_LAUNCH_TUTORIAL_STAGE] = FirstLaunchTutorialStage.TAP_ANY_NUMBER.name
        }
    }

    override suspend fun getString(key: String): String? {
        return dataStore.data.first()[stringPreferencesKey(key)]
    }

    override suspend fun setString(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    override suspend fun getLastSeenVersionCode(): Long? {
        return dataStore.data.first()[LAST_SEEN_VERSION_CODE]
    }

    override suspend fun setLastSeenVersionCode(code: Long) {
        dataStore.edit { it[LAST_SEEN_VERSION_CODE] = code }
    }

    override suspend fun resetLastSeenVersionCode() {
        dataStore.edit { it.remove(LAST_SEEN_VERSION_CODE) }
    }

    override suspend fun clearLastPeriodSnapshot() {
        dataStore.edit { prefs ->
            prefs.remove(LAST_PERIOD_END)
            prefs.remove(REMAINING_FROM_LAST_PERIOD)
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

    private fun String.toContrastMode(): ContrastMode {
        return try {
            ContrastMode.valueOf(this)
        } catch (_: Exception) {
            ContrastMode.NORMAL
        }
    }

    private fun String.toAppColorScheme(): com.serranoie.app.minus.domain.model.AppColorScheme {
        return try {
            com.serranoie.app.minus.domain.model.AppColorScheme.valueOf(this)
        } catch (_: Exception) {
            com.serranoie.app.minus.domain.model.AppColorScheme.BRAND
        }
    }
}
