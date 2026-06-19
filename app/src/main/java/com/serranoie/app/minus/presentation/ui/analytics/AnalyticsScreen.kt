package com.serranoie.app.minus.presentation.ui.analytics

import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.presentation.settingsDataStore

@Composable
fun AnalyticsScreen(
    activityResultRegistryOwner: ActivityResultRegistryOwner?,
    onNavigateToMainWithWallet: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        context.settingsDataStore.data.collect { prefs: Preferences ->
            viewModel.updatePrefsSnapshot(prefs)
        }
    }

    LaunchedEffect(uiState.displayState) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AnalyticsUiEffect.NavigateToMainWithWallet -> {
                    viewModel.consumeEffect()
                    onNavigateToMainWithWallet()
                }
                is AnalyticsUiEffect.NavigateToMain -> {
                    viewModel.consumeEffect()
                    onNavigateToMain()
                }
                null -> { /* no-op */ }
            }
        }
    }

    Analytics(
        state = uiState.displayState,
        actions = AnalyticsActions(
            onCreateNewPeriod = {
                viewModel.onCreateNewPeriod()
            },
            onClose = {
                viewModel.onClose()
            },
        ),
        activityResultRegistryOwner = activityResultRegistryOwner,
    )
}
