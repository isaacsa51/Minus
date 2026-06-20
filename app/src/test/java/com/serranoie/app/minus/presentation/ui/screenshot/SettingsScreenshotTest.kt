package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.PeriodMappingMode
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.ui.settings.Settings
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class SettingsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun settingsDefaultState() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                SettingsPreview(
                    currentTheme = "system",
                    currentTypography = "default",
                    isMaterialYouEnabled = true,
                    isCreditQuickToggleFeatureEnabled = false,
                    recurrentPaymentsViewMode = RecurrentPaymentsViewMode.HORIZONTAL_LIST,
                    notificationHour = 19,
                    notificationMinute = 0,
                    recurrentNotificationHour = 8,
                    recurrentNotificationMinute = 0,
                    exactAlarmEnabled = false,
                    periodMappingMode = PeriodMappingMode.ACTIVE_BUDGET,
                )
            }
        }
    }

    @Test
    fun settingsDarkThemeWithCreditFeature() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                SettingsPreview(
                    currentTheme = "dark",
                    currentTypography = "default",
                    isMaterialYouEnabled = false,
                    isCreditQuickToggleFeatureEnabled = true,
                    recurrentPaymentsViewMode = RecurrentPaymentsViewMode.VERTICAL_LIST,
                    notificationHour = 20,
                    notificationMinute = 30,
                    recurrentNotificationHour = 9,
                    recurrentNotificationMinute = 0,
                    exactAlarmEnabled = true,
                    periodMappingMode = PeriodMappingMode.CALENDAR_BUCKET,
                )
            }
        }
    }

    @Composable
    private fun SettingsPreview(
        currentTheme: String,
        currentTypography: String,
        isMaterialYouEnabled: Boolean,
        isCreditQuickToggleFeatureEnabled: Boolean,
        recurrentPaymentsViewMode: RecurrentPaymentsViewMode,
        notificationHour: Int,
        notificationMinute: Int,
        recurrentNotificationHour: Int,
        recurrentNotificationMinute: Int,
        exactAlarmEnabled: Boolean,
        periodMappingMode: PeriodMappingMode,
    ) {
        MinusTheme {
            Settings(
                modifier = Modifier.fillMaxSize(),
                currentTheme = currentTheme,
                currentTypography = currentTypography,
                isMaterialYouEnabled = isMaterialYouEnabled,
                isCreditQuickToggleFeatureEnabled = isCreditQuickToggleFeatureEnabled,
                recurrentPaymentsViewMode = recurrentPaymentsViewMode,
                notificationHour = notificationHour,
                notificationMinute = notificationMinute,
                recurrentNotificationHour = recurrentNotificationHour,
                recurrentNotificationMinute = recurrentNotificationMinute,
                exactAlarmEnabled = exactAlarmEnabled,
                onThemeChange = {},
                onTypographyChange = {},
                onMaterialYouToggle = {},
                onCreditQuickToggleFeatureToggle = {},
                onRecurrentPaymentsViewModeChange = {},
                onNotificationTimeChange = { _, _ -> },
                onRecurrentNotificationTimeChange = { _, _ -> },
                onOpenExactAlarmSettings = {},
                periodMappingMode = periodMappingMode,
                onPeriodMappingModeChange = {},
                onExportCsv = {},
                onImportCsv = {},
                onResetTutorial = {},
                onBugReportClick = {},
                onBack = {},
            )
        }
    }
}
