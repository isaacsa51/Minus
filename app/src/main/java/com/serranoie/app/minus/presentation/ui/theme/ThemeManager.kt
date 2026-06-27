package com.serranoie.app.minus.presentation.ui.theme

import android.content.Context
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.presentation.appTheme
import com.serranoie.app.minus.presentation.appTypography
import com.serranoie.app.minus.presentation.dynamicColorEnabled
import javax.inject.Inject
import javax.inject.Singleton
import com.serranoie.app.minus.domain.model.ThemeMode as DomainThemeMode
import com.serranoie.app.minus.domain.model.TypographyMode as DomainTypographyMode

@Singleton
class ThemeManager @Inject constructor() {

    fun applyUserSettings(context: Context, settings: UserSettings) {
        context.appTheme = settings.themeMode.toPresentationThemeMode()
        context.appTypography = settings.typographyMode.toPresentationTypographyMode()
        context.dynamicColorEnabled = settings.dynamicColorEnabled
    }

    private fun DomainThemeMode.toPresentationThemeMode(): ThemeMode {
        return when (this) {
            DomainThemeMode.LIGHT -> ThemeMode.LIGHT
            DomainThemeMode.NIGHT -> ThemeMode.NIGHT
            DomainThemeMode.SYSTEM -> ThemeMode.SYSTEM
        }
    }

    private fun DomainTypographyMode.toPresentationTypographyMode(): TypographyMode {
        return when (this) {
            DomainTypographyMode.DEFAULT -> TypographyMode.DEFAULT
            DomainTypographyMode.CONDENSED -> TypographyMode.CONDENSED
            DomainTypographyMode.EXPRESSIVE -> TypographyMode.EXPRESSIVE
        }
    }
}
