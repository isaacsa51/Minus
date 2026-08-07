package com.serranoie.app.minus.presentation.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.logcat
import javax.inject.Inject

private const val TAG = "ISAAC:Onboarding"

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<OnboardingUiEffect>()
    val effects: SharedFlow<OnboardingUiEffect> = _effects.asSharedFlow()

    fun processIntent(intent: OnboardingUiIntent) {
        logcat(TAG) { "processIntent: $intent (state before: isCompleted=${_uiState.value.isCompleted})" }
        when (intent) {
            is OnboardingUiIntent.OnWelcomeDismissed -> handleWelcomeDismissed()
        }
    }

    private fun handleWelcomeDismissed() {
        logcat(TAG) { "handleWelcomeDismissed: setting onboarding_completed=true" }
        viewModelScope.launch {
            try {
                settingsRepository.setOnboardingCompleted(true)
                _uiState.update { it.copy(isCompleted = true) }
                logcat(TAG) { "handleWelcomeDismissed: emitted OnboardingCompleted" }
                _effects.emit(OnboardingUiEffect.OnboardingCompleted)
            } catch (e: Exception) {
                logcat(TAG) { "handleWelcomeDismissed failed: ${e.message}" }
                _effects.emit(OnboardingUiEffect.OnboardingFailed(e.message ?: "Unknown error"))
            }
        }
    }
}
