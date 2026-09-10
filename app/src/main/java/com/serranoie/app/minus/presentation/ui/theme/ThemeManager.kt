package com.serranoie.app.minus.presentation.ui.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.presentation.appColorScheme
import com.serranoie.app.minus.presentation.appTheme
import com.serranoie.app.minus.presentation.appTypography
import com.serranoie.app.minus.presentation.isRoundedFontEnabled
import com.serranoie.app.minus.presentation.isAmoledEnabled
import com.serranoie.app.minus.presentation.dynamicColorEnabled
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor() {

    fun applyUserSettings(context: Context, settings: UserSettings) {
        context.appTheme = settings.themeMode
        context.appTypography = settings.typographyMode
        context.isRoundedFontEnabled = settings.isRoundedFontEnabled
        context.isAmoledEnabled = settings.isAmoledEnabled
        context.appColorScheme = settings.colorScheme
        context.dynamicColorEnabled = settings.dynamicColorEnabled
        val appLocale = if (settings.language == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(settings.language)
        }
        if (AppCompatDelegate.getApplicationLocales() != appLocale) {
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }
}

