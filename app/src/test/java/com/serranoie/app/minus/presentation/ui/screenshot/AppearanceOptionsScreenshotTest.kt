package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.runtime.Composable
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.settings.SettingsUiState
import com.serranoie.app.minus.presentation.ui.settings.appearance.AppearanceOptionsScreen
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class AppearanceOptionsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun appearanceOptionsScreen() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                AppearanceContent(materialYou = false, amoled = true)
            }
        }
    }

    @Composable
    private fun AppearanceContent(materialYou: Boolean, amoled: Boolean) {
        AppearanceOptionsScreen(
            state = SettingsUiState(
                isMaterialYouEnabled = materialYou,
                isAmoledEnabled = amoled,
            ),
            onThemeChange = {},
            onTypographyChange = {},
            onContrastChange = {},
            onColorSchemeChange = {},
            onLanguageChange = {},
            onMaterialYouToggle = {},
            onRoundedFontToggle = {},
            onAmoledToggle = {},
            onBack = {},
        )
    }
}
