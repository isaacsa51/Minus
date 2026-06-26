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
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ChangelogGateViewModel @Inject constructor(
    private val evaluator: ChangelogTriggerEvaluator,
) : ViewModel() {

    private val _pendingRelease = MutableStateFlow<VersionRelease?>(null)
    val pendingRelease: StateFlow<VersionRelease?> = _pendingRelease.asStateFlow()

    fun evaluate(currentVersionCode: Int) {
        viewModelScope.launch {
            when (val decision = evaluator(currentVersionCode)) {
                is ChangelogDecision.Show -> _pendingRelease.value = decision.release
                is ChangelogDecision.Skip -> _pendingRelease.value = null
            }
        }
    }

    fun dismissSheet() {
        _pendingRelease.value = null
    }
}

@Composable
fun ChangelogGate(
    currentVersionCode: Int,
    viewModel: ChangelogGateViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val pending by viewModel.pendingRelease.collectAsStateWithLifecycle()
    LaunchedEffect(currentVersionCode) {
        viewModel.evaluate(currentVersionCode)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        pending?.let { release ->
            ChangelogBottomSheet(
                release = release,
                onDismiss = viewModel::dismissSheet,
            )
        }
    }
}
