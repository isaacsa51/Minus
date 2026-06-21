package com.serranoie.app.minus.presentation.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.EntryPointAccessors

@Composable
fun SettingsScreen(
    onNavigateToBugReport: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        viewModel.onImportResult(uri)
    }

    LaunchedEffect(Unit) {
        val manager = EntryPointAccessors
            .fromApplication(context.applicationContext, CsvTransferEntryPoint::class.java)
            .csvTransferManager()
        viewModel.setCsvTransferManager(manager)
        viewModel.setImportLauncher(importLauncher)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsUiEffect.NavigateToBugReport -> {
                    viewModel.consumeEffect()
                    onNavigateToBugReport()
                }
                is SettingsUiEffect.NavigateBack -> {
                    viewModel.consumeEffect()
                    onNavigateBack()
                }
                null -> { /* no-op */ }
            }
        }
    }

    Settings(
        currentTheme = uiState.currentTheme,
        currentTypography = uiState.currentTypography,
        isMaterialYouEnabled = uiState.isMaterialYouEnabled,
        isCreditQuickToggleFeatureEnabled = uiState.isCreditQuickToggleEnabled,
        recurrentPaymentsViewMode = uiState.recurrentPaymentsViewMode,
        notificationHour = uiState.notificationHour,
        notificationMinute = uiState.notificationMinute,
        recurrentNotificationHour = uiState.recurrentNotificationHour,
        recurrentNotificationMinute = uiState.recurrentNotificationMinute,
        exactAlarmEnabled = uiState.exactAlarmEnabled,
        onThemeChange = viewModel::onThemeChange,
        onTypographyChange = viewModel::onTypographyChange,
        onMaterialYouToggle = viewModel::onMaterialYouToggle,
        onCreditQuickToggleFeatureToggle = viewModel::onCreditQuickToggleFeatureToggle,
        onRecurrentPaymentsViewModeChange = viewModel::onRecurrentPaymentsViewModeChange,
        onNotificationTimeChange = viewModel::onNotificationTimeChange,
        onRecurrentNotificationTimeChange = viewModel::onRecurrentNotificationTimeChange,
        onOpenExactAlarmSettings = viewModel::onOpenExactAlarmSettings,
        periodMappingMode = uiState.periodMappingMode,
        onPeriodMappingModeChange = viewModel::onPeriodMappingModeChange,
        onExportCsv = viewModel::onExportCsv,
        onImportCsv = viewModel::onImportCsv,
        onResetTutorial = viewModel::onResetTutorial,
        onBugReportClick = viewModel::onBugReportClick,
        onBack = viewModel::onBack,
    )
}
