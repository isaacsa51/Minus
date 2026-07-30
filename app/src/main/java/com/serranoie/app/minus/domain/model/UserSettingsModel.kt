package com.serranoie.app.minus.domain.model

import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode

data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val earlyFinishActive: Boolean = false,
    val earlyFinishActualDate: Long = 0L,
    val earlyFinishOriginalEndDate: Long = 0L,
    val currentPeriodStartedAt: Long = 0L,
    val currentPeriodId: Long = 0L,
    val notificationHour: Int = DEFAULT_NOTIFICATION_HOUR,
    val notificationMinute: Int = DEFAULT_NOTIFICATION_MINUTE,
    val recurrentNotificationHour: Int = DEFAULT_RECURRENT_NOTIFICATION_HOUR,
    val recurrentNotificationMinute: Int = DEFAULT_RECURRENT_NOTIFICATION_MINUTE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val typographyMode: TypographyMode = TypographyMode.EXPRESSIVE,
    val contrastMode: ContrastMode = ContrastMode.NORMAL,
    val language: String = "en",
    val colorScheme: AppColorScheme = AppColorScheme.BRAND,
    val dynamicColorEnabled: Boolean = false,
    val isCreditQuickToggleEnabled: Boolean = false,
    val categoryPickerDirectPopupEnabled: Boolean = false,
    val categoryGridModeEnabled: Boolean = false,
    val tutorialBoxCompleted: Boolean = false,
    val firstLaunchTutorialStage: FirstLaunchTutorialStage = FirstLaunchTutorialStage.COMPLETED,
    val analyticsTutorialCompleted: Boolean = false,
    val analyticsSpendsTutorialCompleted: Boolean = false,
    val periodMappingMode: PeriodMappingMode = PeriodMappingMode.ACTIVE_BUDGET,
    val recurrentPaymentsViewMode: RecurrentPaymentsViewMode = RecurrentPaymentsViewMode.VERTICAL_LIST,
    val budgetSplitViewPeriod: BudgetPeriod? = null,
    val savingsPreferences: SavingsPreferences = SavingsPreferences.DEFAULT,
) {
    companion object {
        const val DEFAULT_NOTIFICATION_HOUR = 9
        const val DEFAULT_NOTIFICATION_MINUTE = 0
        const val DEFAULT_RECURRENT_NOTIFICATION_HOUR = 8
        const val DEFAULT_RECURRENT_NOTIFICATION_MINUTE = 0

        val DEFAULT = UserSettings()
    }
}

enum class ThemeMode {
    LIGHT,
    NIGHT,
    SYSTEM,
}

enum class TypographyMode {
    SYSTEM,
    DEFAULT,
    CONDENSED,
    EXPRESSIVE,
}

enum class ContrastMode {
    NORMAL,
    MEDIUM,
    HIGH,
}

enum class AppColorScheme {
    BRAND,
    PINK,
    PINK_NEUTRAL,
    RED,
    RED_NEUTRAL,
    BLUE,
    BLUE_NEUTRAL,
    ORANGE,
    ORANGE_NEUTRAL,
    YELLOW,
    YELLOW_NEUTRAL,
    AQUA,
    AQUA_NEUTRAL,
    CYAN,
    CYAN_NEUTRAL,
    PURPLE,
    PURPLE_NEUTRAL,
    GREEN,
}
