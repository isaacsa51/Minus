package com.serranoie.app.minus.presentation.ui.settings.features

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.presentation.notification.scan.ExpenseNotificationListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ScannableApp(
    val packageName: String,
    val label: String,
)

data class NotificationScanUiState(
    val accessGranted: Boolean = false,
    val enabled: Boolean = false,
    val selectedPackages: Set<String> = emptySet(),
    val apps: List<ScannableApp> = emptyList(),
    val query: String = "",
    val loadingApps: Boolean = true,
)

@HiltViewModel
class NotificationScanViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val accessGranted = MutableStateFlow(isAccessGranted())
    private val apps = MutableStateFlow<List<ScannableApp>?>(null)
    private val query = MutableStateFlow("")

    val uiState: StateFlow<NotificationScanUiState> = combine(
        settingsRepository.observeSettings(),
        accessGranted,
        apps,
        query,
    ) { settings, granted, loadedApps, searchQuery ->
        NotificationScanUiState(
            accessGranted = granted,
            enabled = settings.notificationScanEnabled,
            selectedPackages = settings.notificationScanPackages,
            apps = loadedApps.orEmpty(),
            query = searchQuery,
            loadingApps = loadedApps == null,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationScanUiState(),
    )

    init {
        loadApps()
    }

    fun refreshAccess() {
        accessGranted.value = isAccessGranted()
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onEnabledToggle() {
        val enabled = !uiState.value.enabled
        viewModelScope.launch {
            settingsRepository.setNotificationScanEnabled(enabled)
            if (enabled) {
                ExpenseNotificationListener.requestRebindIfGranted(context)
            }
        }
    }

    fun onAppToggle(packageName: String) {
        val current = uiState.value.selectedPackages
        val updated = if (packageName in current) current - packageName else current + packageName
        viewModelScope.launch {
            settingsRepository.setNotificationScanPackages(updated)
        }
    }

    fun openAccessSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun isAccessGranted(): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    private fun loadApps() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val packageManager = context.packageManager
                val launchable = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                packageManager.queryIntentActivities(launchable, 0)
                    .mapNotNull { it.activityInfo?.applicationInfo }
                    .filter { it.packageName != context.packageName }
                    .distinctBy { it.packageName }
                    .map {
                        ScannableApp(
                            packageName = it.packageName,
                            label = packageManager.getApplicationLabel(it).toString(),
                        )
                    }
                    .sortedBy { it.label.lowercase() }
            }
            apps.value = loaded
        }
    }
}
