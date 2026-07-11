package com.serranoie.app.minus.presentation.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.PeriodMappingMode
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.usecase.UpdatePeriodEndNotificationTimeUseCase
import com.serranoie.app.minus.presentation.CATEGORY_GRID_MODE_KEY
import com.serranoie.app.minus.presentation.CATEGORY_PICKER_DIRECT_POPUP_KEY
import com.serranoie.app.minus.presentation.CREDIT_QUICK_TOGGLE_FEATURE_KEY
import com.serranoie.app.minus.presentation.DYNAMIC_COLOR_KEY
import com.serranoie.app.minus.presentation.RECURRENT_NOTIFICATION_HOUR_KEY
import com.serranoie.app.minus.presentation.RECURRENT_NOTIFICATION_MINUTE_KEY
import com.serranoie.app.minus.presentation.THEME_MODE_KEY
import com.serranoie.app.minus.presentation.TYPOGRAPHY_MODE_KEY
import com.serranoie.app.minus.presentation.appTheme
import com.serranoie.app.minus.presentation.appTypography
import com.serranoie.app.minus.presentation.dynamicColorEnabled
import com.serranoie.app.minus.presentation.settingsDataStore
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.ui.settings.csv.CsvTransferManager
import com.serranoie.app.minus.presentation.ui.theme.ThemeMode
import com.serranoie.app.minus.presentation.ui.theme.TypographyMode
import com.serranoie.app.minus.presentation.ui.tutorial.FIRST_LAUNCH_TUTORIAL_STAGE_KEY
import com.serranoie.app.minus.presentation.ui.tutorial.FirstLaunchTutorialStage
import com.serranoie.app.minus.presentation.ui.tutorial.PERIOD_MAPPING_MODE_KEY
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.logcat
import javax.inject.Inject
import android.provider.Settings as AndroidSettings

data class SettingsUiState(
    val currentTheme: String = "System",
    val currentTypography: String = "Expressive",
    val isMaterialYouEnabled: Boolean = false,
    val isCreditQuickToggleEnabled: Boolean = false,
    val isCategoryPickerDirectPopupEnabled: Boolean = false,
    val isCategoryGridModeEnabled: Boolean = false,
    val recurrentPaymentsViewMode: RecurrentPaymentsViewMode = RecurrentPaymentsViewMode.VERTICAL_LIST,
    val notificationHour: Int = 9,
    val notificationMinute: Int = 0,
    val recurrentNotificationHour: Int = 8,
    val recurrentNotificationMinute: Int = 0,
    val exactAlarmEnabled: Boolean = true,
    val notificationPermissionGranted: Boolean = false,
    val periodMappingMode: PeriodMappingMode = PeriodMappingMode.ACTIVE_BUDGET,
    val savingsPreferences: SavingsPreferences = SavingsPreferences.DEFAULT,
)

sealed interface SettingsUiEffect {
    data object NavigateToBugReport : SettingsUiEffect
    data object NavigateBack : SettingsUiEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val updateNotificationTimeUseCase: UpdatePeriodEndNotificationTimeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = MutableStateFlow<SettingsUiEffect?>(null)
    val effects: StateFlow<SettingsUiEffect?> = _effects.asStateFlow()

    private var csvTransferManager: CsvTransferManager? = null
    private var importLauncher: ActivityResultLauncher<Array<String>>? = null

    init {
        loadPreferences()
        refreshNotificationPermission()
    }

    fun refreshNotificationPermission() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-Tiramisu devices grant by default.
            true
        }
        logcat("ISAAC:Settings") { "refreshNotificationPermission -> granted=$granted" }
        _uiState.update { it.copy(notificationPermissionGranted = granted) }
    }

    fun onOpenAppSettings() {
        logcat("ISAAC:Settings") { "onOpenAppSettings" }
        val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun setCsvTransferManager(manager: CsvTransferManager) {
        csvTransferManager = manager
    }

    fun setImportLauncher(launcher: ActivityResultLauncher<Array<String>>) {
        importLauncher = launcher
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        currentTheme = when (settings.themeMode) {
                            com.serranoie.app.minus.domain.model.ThemeMode.LIGHT -> "Light"
                            com.serranoie.app.minus.domain.model.ThemeMode.NIGHT -> "Dark"
                            else -> "System"
                        },
                        currentTypography = when (settings.typographyMode) {
                            com.serranoie.app.minus.domain.model.TypographyMode.DEFAULT -> "Default"
                            com.serranoie.app.minus.domain.model.TypographyMode.CONDENSED -> "Condensed"
                            else -> "Expressive"
                        },
                        isMaterialYouEnabled = settings.dynamicColorEnabled,
                        recurrentPaymentsViewMode = settings.recurrentPaymentsViewMode,
                        notificationHour = settings.notificationHour,
                        notificationMinute = settings.notificationMinute,
                        recurrentNotificationHour = settings.notificationHour, // Fixed to match repo if needed, but repo has separate? No, repo only has one.
                        recurrentNotificationMinute = settings.notificationMinute,
                        exactAlarmEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val alarmManager =
                                context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            alarmManager.canScheduleExactAlarms()
                        } else true,
                        savingsPreferences = settings.savingsPreferences,
                    )
                }
            }
        }
        viewModelScope.launch {
            context.settingsDataStore.data.collect { prefs ->
                _uiState.update {
                    it.copy(
                        isCreditQuickToggleEnabled = prefs[CREDIT_QUICK_TOGGLE_FEATURE_KEY]
                            ?: false,
                        isCategoryPickerDirectPopupEnabled = prefs[CATEGORY_PICKER_DIRECT_POPUP_KEY]
                            ?: false,
                        isCategoryGridModeEnabled = prefs[CATEGORY_GRID_MODE_KEY] ?: false,
                        periodMappingMode = try {
                            PeriodMappingMode.valueOf(
                                prefs[PERIOD_MAPPING_MODE_KEY] ?: ""
                            )
                        } catch (_: Exception) {
                            PeriodMappingMode.ACTIVE_BUDGET
                        },
                    )
                }
            }
        }
    }

    fun onThemeChange(themeMode: String) {
        val newMode = when (themeMode) {
            "Light" -> ThemeMode.LIGHT
            "Dark" -> ThemeMode.NIGHT
            else -> ThemeMode.SYSTEM
        }
        context.appTheme = newMode
        _uiState.update { it.copy(currentTheme = themeMode) }
        viewModelScope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[THEME_MODE_KEY] = newMode.toString()
            }
        }
    }

    fun onTypographyChange(typographyMode: String) {
        val newMode = when (typographyMode) {
            "Default" -> TypographyMode.DEFAULT
            "Condensed" -> TypographyMode.CONDENSED
            else -> TypographyMode.EXPRESSIVE
        }
        context.appTypography = newMode
        _uiState.update { it.copy(currentTypography = typographyMode) }
        viewModelScope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[TYPOGRAPHY_MODE_KEY] = newMode.toString()
            }
        }
    }

    fun onMaterialYouToggle() {
        val newValue = !_uiState.value.isMaterialYouEnabled
        context.dynamicColorEnabled = newValue
        _uiState.update { it.copy(isMaterialYouEnabled = newValue) }
        viewModelScope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[DYNAMIC_COLOR_KEY] = newValue
            }
        }
    }

    fun onCreditQuickToggleFeatureToggle() {
        val newValue = !_uiState.value.isCreditQuickToggleEnabled
        _uiState.update { it.copy(isCreditQuickToggleEnabled = newValue) }
        viewModelScope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[CREDIT_QUICK_TOGGLE_FEATURE_KEY] = newValue
            }
        }
    }

    fun onCategoryPickerDirectPopupFeatureToggle() {
        val newValue = !_uiState.value.isCategoryPickerDirectPopupEnabled
        _uiState.update { it.copy(isCategoryPickerDirectPopupEnabled = newValue) }
        viewModelScope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[CATEGORY_PICKER_DIRECT_POPUP_KEY] = newValue
            }
        }
    }

    fun onCategoryGridModeToggle() {
        val newValue = !_uiState.value.isCategoryGridModeEnabled
        _uiState.update { it.copy(isCategoryGridModeEnabled = newValue) }
        viewModelScope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[CATEGORY_GRID_MODE_KEY] = newValue
            }
        }
    }

    fun onRecurrentPaymentsViewModeChange(mode: RecurrentPaymentsViewMode) {
        _uiState.update { it.copy(recurrentPaymentsViewMode = mode) }
        viewModelScope.launch {
            settingsRepository.setRecurrentPaymentsViewMode(mode)
        }
    }

    fun onNotificationTimeChange(hour: Int, minute: Int) {
        _uiState.update { it.copy(notificationHour = hour, notificationMinute = minute) }
        viewModelScope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[RECURRENT_NOTIFICATION_HOUR_KEY] = hour
                prefs[RECURRENT_NOTIFICATION_MINUTE_KEY] = minute
            }
            updateNotificationTimeUseCase(hour, minute)
        }
    }

    fun onRecurrentNotificationTimeChange(hour: Int, minute: Int) {
        _uiState.update {
            it.copy(
                recurrentNotificationHour = hour, recurrentNotificationMinute = minute
            )
        }
        viewModelScope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[RECURRENT_NOTIFICATION_HOUR_KEY] = hour
                prefs[RECURRENT_NOTIFICATION_MINUTE_KEY] = minute
            }
            updateNotificationTimeUseCase.updateRecurrentNotificationTime(hour, minute)
        }
    }

    fun onPeriodMappingModeChange(mode: PeriodMappingMode) {
        _uiState.update { it.copy(periodMappingMode = mode) }
        viewModelScope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[PERIOD_MAPPING_MODE_KEY] = mode.name
            }
        }
    }

    fun onSavingsPreferencesChange(prefs: SavingsPreferences) {
        _uiState.update { it.copy(savingsPreferences = prefs) }
        viewModelScope.launch {
            settingsRepository.setSavingsPreferences(prefs)
        }
    }

    fun onOpenExactAlarmSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
            }
        } else {
            Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
            }
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun onExportCsv() {
        viewModelScope.launch {
            csvTransferManager?.exportAndShareCsv()
        }
    }

    fun onImportCsv() {
        importLauncher?.launch(arrayOf("text/*", "text/csv", "application/csv"))
    }

    fun onImportResult(uri: Uri?) {
        uri ?: return
        viewModelScope.launch {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            csvTransferManager?.enqueueImport(uri.toString())
        }
    }

    fun onResetTutorial() {
        viewModelScope.launch {
            context.settingsDataStore.edit { prefs ->
                prefs[FIRST_LAUNCH_TUTORIAL_STAGE_KEY] =
                    FirstLaunchTutorialStage.TAP_ANY_NUMBER.name
            }
        }
    }

    fun onBugReportClick() {
        _effects.value = SettingsUiEffect.NavigateToBugReport
    }

    fun onBack() {
        _effects.value = SettingsUiEffect.NavigateBack
    }

    fun consumeEffect() {
        _effects.value = null
    }
}
