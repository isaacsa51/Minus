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
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val typographyMode: TypographyMode = TypographyMode.EXPRESSIVE,
    val dynamicColorEnabled: Boolean = false,
    val recurrentPaymentsViewMode: RecurrentPaymentsViewMode = RecurrentPaymentsViewMode.VERTICAL_LIST,
) {
    companion object {
        const val DEFAULT_NOTIFICATION_HOUR = 9
        const val DEFAULT_NOTIFICATION_MINUTE = 0

        val DEFAULT = UserSettings()
    }
}

enum class ThemeMode {
    LIGHT,
    NIGHT,
    SYSTEM
}

enum class TypographyMode {
    DEFAULT,
    CONDENSED,
    EXPRESSIVE
}
