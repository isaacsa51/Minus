package com.serranoie.app.minus.presentation.ui.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.settings.SettingsUiState
import com.serranoie.app.minus.presentation.ui.settings.features.FeatureLabScreen
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class FeatureLabScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun featureLabScreen() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                FeatureLabScreen(
                    state = SettingsUiState(
                        isCreditQuickToggleEnabled = true,
                        showPastTransactions = true,
                        isCategoryPickerDirectPopupEnabled = false,
                        isCategoryGridModeEnabled = false,
                        isExtraNoteEnabled = true,
                    ),
                    onCreditQuickToggle = {},
                    onShowPastTransactionsToggle = {},
                    onCategoryPickerDirectPopupToggle = {},
                    onCategoryGridModeToggle = {},
                    onExtraNoteToggle = {},
                    onBack = {},
                )
            }
        }
    }
}
