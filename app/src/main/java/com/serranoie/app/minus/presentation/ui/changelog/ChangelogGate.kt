package com.serranoie.app.minus.presentation.ui.changelog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.domain.model.changelog.ChangelogDecision
import com.serranoie.app.minus.domain.model.changelog.VersionRelease
import com.serranoie.app.minus.domain.usecase.ChangelogTriggerEvaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import logcat.logcat
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "ISAAC:ChangelogGate"

@HiltViewModel
class ChangelogGateViewModel @Inject constructor(
    private val evaluator: ChangelogTriggerEvaluator,
) : ViewModel() {

    private val _pendingRelease = MutableStateFlow<VersionRelease?>(null)
    val pendingRelease: StateFlow<VersionRelease?> = _pendingRelease.asStateFlow()

    fun evaluate(currentVersionCode: Int) {
        logcat(TAG) { "evaluate(currentVersionCode=$currentVersionCode) called" }
        viewModelScope.launch {
            when (val decision = evaluator(currentVersionCode)) {
                is ChangelogDecision.Show -> {
                    logcat(TAG) { "decision=Show release=${decision.release.versionName}" }
                    _pendingRelease.value = decision.release
                }
                is ChangelogDecision.Skip -> {
                    logcat(TAG) { "decision=Skip -> clearing pendingRelease" }
                    _pendingRelease.value = null
                }
            }
        }
    }

    fun dismissSheet() {
        logcat(TAG) { "dismissSheet called -> clearing pendingRelease" }
        _pendingRelease.value = null
    }
}

@Composable
fun ChangelogGate(
    currentVersionCode: Int,
    viewModel: ChangelogGateViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    logcat(TAG) { "ChangelogGate composed (currentVersionCode=$currentVersionCode)" }
    val pending by viewModel.pendingRelease.collectAsStateWithLifecycle()
    LaunchedEffect(currentVersionCode) {
        logcat(TAG) { "ChangelogGate LaunchedEffect -> viewModel.evaluate($currentVersionCode)" }
        viewModel.evaluate(currentVersionCode)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        pending?.let { release ->
            logcat(TAG) { "Rendering ChangelogBottomSheet for ${release.versionName}" }
            ChangelogBottomSheet(
                release = release,
                onDismiss = viewModel::dismissSheet,
            )
        }
    }
}
