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
import com.serranoie.app.minus.data.repository.BudgetRepository
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
import com.serranoie.app.minus.presentation.isAmoledEnabled
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.ui.settings.csv.CsvTransferManager
import com.serranoie.app.minus.presentation.util.CensorManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val isRoundedFontEnabled: Boolean = true,
    val isAmoledEnabled: Boolean = false,
    val isCreditQuickToggleEnabled: Boolean = false,
    val showPastTransactions: Boolean = true,
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
    val creditCardCutoffDay: Int? = null,
)

sealed interface SettingsUiEffect {
    data object NavigateToBugReport : SettingsUiEffect
    data object NavigateBack : SettingsUiEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val budgetRepository: BudgetRepository,
    private val updateNotificationTimeUseCase: UpdatePeriodEndNotificationTimeUseCase,
    private val censorManager: CensorManager,
) : ViewModel() {

    private val _notificationPermissionGranted = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeSettings(),
        budgetRepository.getBudgetSettings(),
        censorManager.isCensored,
        _notificationPermissionGranted
    ) { settings, budgetSettings, isCensored, permissionGranted ->
        SettingsUiState(
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
            isRoundedFontEnabled = settings.isRoundedFontEnabled,
            isAmoledEnabled = settings.isAmoledEnabled,
            isCreditQuickToggleEnabled = settings.isCreditQuickToggleEnabled,
            showPastTransactions = settings.showPastTransactions,
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
            notificationPermissionGranted = permissionGranted,
            isCensored = isCensored,
            periodMappingMode = settings.periodMappingMode,
            savingsPreferences = settings.savingsPreferences,
            creditCardCutoffDay = budgetSettings?.creditCardCutoffDay
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    private val _effects = MutableStateFlow<SettingsUiEffect?>(null)
    val effects: StateFlow<SettingsUiEffect?> = _effects.asStateFlow()

    private var csvTransferManager: CsvTransferManager? = null
    private var importLauncher: ActivityResultLauncher<Array<String>>? = null

    init {
        refreshNotificationPermission()
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
        _notificationPermissionGranted.value = granted
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

    fun onThemeChange(themeMode: String) {
        val newMode = when (themeMode) {
            "Light" -> ThemeMode.LIGHT
            "Dark" -> ThemeMode.NIGHT
            else -> ThemeMode.SYSTEM
        }
        context.appTheme = newMode
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
        viewModelScope.launch {
            settingsRepository.setContrastMode(newMode)
        }
    }

    fun onColorSchemeChange(colorScheme: AppColorScheme) {
        context.appColorScheme = colorScheme
        viewModelScope.launch {
            settingsRepository.setAppColorScheme(colorScheme)
        }
    }

    fun onLanguageChange(language: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun onMaterialYouToggle() {
        val newValue = !uiState.value.isMaterialYouEnabled
        context.dynamicColorEnabled = newValue
        viewModelScope.launch {
            settingsRepository.setDynamicColorEnabled(newValue)
        }
    }

    fun onRoundedFontToggle() {
        val newValue = !uiState.value.isRoundedFontEnabled
        viewModelScope.launch {
            settingsRepository.setRoundedFontEnabled(newValue)
        }
    }

    fun onAmoledToggle() {
        val newValue = !uiState.value.isAmoledEnabled
        context.isAmoledEnabled = newValue
        viewModelScope.launch {
            settingsRepository.setAmoledEnabled(newValue)
        }
    }

    fun onCensorModeToggle() {
        censorManager.setCensored(!uiState.value.isCensored)
    }

    fun onCreditQuickToggleFeatureToggle() {
        val newValue = !uiState.value.isCreditQuickToggleEnabled
        viewModelScope.launch {
            settingsRepository.setCreditQuickToggleEnabled(newValue)
        }
    }

    fun onCutoffDayChange(day: Int) {
        viewModelScope.launch {
            val currentSettings = budgetRepository.getBudgetSettingsSync()
            if (currentSettings != null) {
                budgetRepository.saveBudgetSettings(currentSettings.copy(creditCardCutoffDay = day))
            }
        }
    }

    fun onShowPastTransactionsToggle() {
        val newValue = !uiState.value.showPastTransactions
        viewModelScope.launch {
            settingsRepository.setShowPastTransactions(newValue)
        }
    }

    fun onCategoryPickerDirectPopupFeatureToggle() {
        val newValue = !uiState.value.isCategoryPickerDirectPopupEnabled
        viewModelScope.launch {
            settingsRepository.setCategoryPickerDirectPopupEnabled(newValue)
        }
    }

    fun onCategoryGridModeToggle() {
        val newValue = !uiState.value.isCategoryGridModeEnabled
        viewModelScope.launch {
            settingsRepository.setCategoryGridModeEnabled(newValue)
        }
    }

    fun onRecurrentPaymentsViewModeChange(mode: RecurrentPaymentsViewMode) {
        viewModelScope.launch {
            settingsRepository.setRecurrentPaymentsViewMode(mode)
        }
    }

    fun onNotificationTimeChange(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setNotificationTime(hour, minute)
            updateNotificationTimeUseCase(hour, minute)
        }
    }

    fun onRecurrentNotificationTimeChange(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setRecurrentNotificationTime(hour, minute)
            updateNotificationTimeUseCase.updateRecurrentNotificationTime(hour, minute)
        }
    }

    fun onPeriodMappingModeChange(mode: PeriodMappingMode) {
        viewModelScope.launch {
            settingsRepository.setPeriodMappingMode(mode)
        }
    }

    fun onSavingsPreferencesChange(prefs: SavingsPreferences) {
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
