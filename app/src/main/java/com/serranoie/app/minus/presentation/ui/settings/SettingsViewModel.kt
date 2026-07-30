package com.serranoie.app.minus.presentation.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.AppColorScheme
import com.serranoie.app.minus.domain.model.ContrastMode
import com.serranoie.app.minus.domain.model.PeriodMappingMode
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.ThemeMode
import com.serranoie.app.minus.domain.model.TypographyMode
import com.serranoie.app.minus.domain.usecase.UpdatePeriodEndNotificationTimeUseCase
import com.serranoie.app.minus.presentation.appColorScheme
import com.serranoie.app.minus.presentation.appContrast
import com.serranoie.app.minus.presentation.appTheme
import com.serranoie.app.minus.presentation.appTypography
import com.serranoie.app.minus.presentation.dynamicColorEnabled
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.ui.settings.csv.CsvTransferManager
import com.serranoie.app.minus.presentation.util.CensorManager
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
    val currentContrast: String = "Normal",
    val currentColorScheme: AppColorScheme = AppColorScheme.BRAND,
    val currentLanguage: String = "English",
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
    val isCensored: Boolean = false,
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
    private val censorManager: CensorManager,
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
        observeCensorMode()
    }

    private fun observeCensorMode() {
        viewModelScope.launch {
            censorManager.isCensored.collect { isCensored ->
                _uiState.update { it.copy(isCensored = isCensored) }
            }
        }
    }

    fun refreshNotificationPermission() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
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
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.NIGHT -> "Dark"
                            else -> "System"
                        },
                        currentTypography = when (settings.typographyMode) {
                            TypographyMode.CONDENSED -> "Condensed"
                            TypographyMode.SYSTEM -> "System"
                            else -> "Expressive"
                        },
                        currentContrast = when (settings.contrastMode) {
                            ContrastMode.MEDIUM -> "Medium"
                            ContrastMode.HIGH -> "High"
                            else -> "Normal"
                        },
                        currentColorScheme = settings.colorScheme,
                        isMaterialYouEnabled = settings.dynamicColorEnabled,
                        isCreditQuickToggleEnabled = settings.isCreditQuickToggleEnabled,
                        isCategoryPickerDirectPopupEnabled = settings.categoryPickerDirectPopupEnabled,
                        isCategoryGridModeEnabled = settings.categoryGridModeEnabled,
                        currentLanguage = settings.language,
                        recurrentPaymentsViewMode = settings.recurrentPaymentsViewMode,
                        notificationHour = settings.notificationHour,
                        notificationMinute = settings.notificationMinute,
                        recurrentNotificationHour = settings.recurrentNotificationHour,
                        recurrentNotificationMinute = settings.recurrentNotificationMinute,
                        exactAlarmEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val alarmManager =
                                context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            alarmManager.canScheduleExactAlarms()
                        } else true,
                        periodMappingMode = settings.periodMappingMode,
                        savingsPreferences = settings.savingsPreferences,
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
            settingsRepository.setThemeMode(newMode)
        }
    }

    fun onTypographyChange(typographyMode: String) {
        val newMode = when (typographyMode) {
            "System" -> TypographyMode.SYSTEM
            "Default" -> TypographyMode.DEFAULT
            "Condensed" -> TypographyMode.CONDENSED
            else -> TypographyMode.EXPRESSIVE
        }
        context.appTypography = newMode
        _uiState.update { it.copy(currentTypography = typographyMode) }
        viewModelScope.launch {
            settingsRepository.setTypographyMode(newMode)
        }
    }

    fun onContrastChange(contrastMode: String) {
        val newMode = when (contrastMode) {
            "Medium" -> ContrastMode.MEDIUM
            "High" -> ContrastMode.HIGH
            else -> ContrastMode.NORMAL
        }
        context.appContrast = newMode
        _uiState.update { it.copy(currentContrast = contrastMode) }
        viewModelScope.launch {
            settingsRepository.setContrastMode(newMode)
        }
    }

    fun onColorSchemeChange(colorScheme: AppColorScheme) {
        context.appColorScheme = colorScheme
        _uiState.update { it.copy(currentColorScheme = colorScheme) }
        viewModelScope.launch {
            settingsRepository.setAppColorScheme(colorScheme)
        }
    }

    fun onLanguageChange(language: String) {
        _uiState.update { it.copy(currentLanguage = language) }
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun onMaterialYouToggle() {
        val newValue = !_uiState.value.isMaterialYouEnabled
        context.dynamicColorEnabled = newValue
        _uiState.update { it.copy(isMaterialYouEnabled = newValue) }
        viewModelScope.launch {
            settingsRepository.setDynamicColorEnabled(newValue)
        }
    }

    fun onCensorModeToggle() {
        censorManager.setCensored(!_uiState.value.isCensored)
    }

    fun onCreditQuickToggleFeatureToggle() {
        val newValue = !_uiState.value.isCreditQuickToggleEnabled
        _uiState.update { it.copy(isCreditQuickToggleEnabled = newValue) }
        viewModelScope.launch {
            settingsRepository.setCreditQuickToggleEnabled(newValue)
        }
    }

    fun onCategoryPickerDirectPopupFeatureToggle() {
        val newValue = !_uiState.value.isCategoryPickerDirectPopupEnabled
        _uiState.update { it.copy(isCategoryPickerDirectPopupEnabled = newValue) }
        viewModelScope.launch {
            settingsRepository.setCategoryPickerDirectPopupEnabled(newValue)
        }
    }

    fun onCategoryGridModeToggle() {
        val newValue = !_uiState.value.isCategoryGridModeEnabled
        _uiState.update { it.copy(isCategoryGridModeEnabled = newValue) }
        viewModelScope.launch {
            settingsRepository.setCategoryGridModeEnabled(newValue)
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
            settingsRepository.setNotificationTime(hour, minute)
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
            settingsRepository.setRecurrentNotificationTime(hour, minute)
            updateNotificationTimeUseCase.updateRecurrentNotificationTime(hour, minute)
        }
    }

    fun onPeriodMappingModeChange(mode: PeriodMappingMode) {
        _uiState.update { it.copy(periodMappingMode = mode) }
        viewModelScope.launch {
            settingsRepository.setPeriodMappingMode(mode)
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
            settingsRepository.resetTutorials()
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
