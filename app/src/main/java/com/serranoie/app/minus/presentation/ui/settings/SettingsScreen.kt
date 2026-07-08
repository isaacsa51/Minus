package com.serranoie.app.minus.presentation.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.presentation.ui.settings.csv.CsvTransferEntryPoint
import com.serranoie.app.minus.presentation.util.LocalCensorMode
import dagger.hilt.android.EntryPointAccessors

@Composable
fun SettingsScreen(
    onNavigateToBugReport: () -> Unit,
    onNavigateToChangelog: () -> Unit = {},
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isCensored = LocalCensorMode.current

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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
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
        isCensored = isCensored,
        currentTheme = uiState.currentTheme,
        currentTypography = uiState.currentTypography,
        isMaterialYouEnabled = uiState.isMaterialYouEnabled,
        isCreditQuickToggleFeatureEnabled = uiState.isCreditQuickToggleEnabled,
        isCategoryPickerDirectPopupEnabled = uiState.isCategoryPickerDirectPopupEnabled,
        isCategoryGridModeEnabled = uiState.isCategoryGridModeEnabled,
        onCategoryPickerDirectPopupFeatureToggle = viewModel::onCategoryPickerDirectPopupFeatureToggle,
        onCategoryGridModeToggle = viewModel::onCategoryGridModeToggle,
        recurrentPaymentsViewMode = uiState.recurrentPaymentsViewMode,
        notificationHour = uiState.notificationHour,
        notificationMinute = uiState.notificationMinute,
        recurrentNotificationHour = uiState.recurrentNotificationHour,
        recurrentNotificationMinute = uiState.recurrentNotificationMinute,
        exactAlarmEnabled = uiState.exactAlarmEnabled,
        notificationPermissionGranted = uiState.notificationPermissionGranted,
        onThemeChange = viewModel::onThemeChange,
        onTypographyChange = viewModel::onTypographyChange,
        onMaterialYouToggle = viewModel::onMaterialYouToggle,
        onCreditQuickToggleFeatureToggle = viewModel::onCreditQuickToggleFeatureToggle,
        onRecurrentPaymentsViewModeChange = viewModel::onRecurrentPaymentsViewModeChange,
        onNotificationTimeChange = viewModel::onNotificationTimeChange,
        onRecurrentNotificationTimeChange = viewModel::onRecurrentNotificationTimeChange,
        onOpenExactAlarmSettings = viewModel::onOpenExactAlarmSettings,
        onOpenNotificationSettings = {
            viewModel.onOpenAppSettings()
            viewModel.refreshNotificationPermission()
        },
        periodMappingMode = uiState.periodMappingMode,
        onPeriodMappingModeChange = viewModel::onPeriodMappingModeChange,
        onExportCsv = viewModel::onExportCsv,
        onImportCsv = viewModel::onImportCsv,
        onResetTutorial = viewModel::onResetTutorial,
        onBugReportClick = viewModel::onBugReportClick,
        onNavigateToChangelog = onNavigateToChangelog,
        onBack = viewModel::onBack,
    )
}