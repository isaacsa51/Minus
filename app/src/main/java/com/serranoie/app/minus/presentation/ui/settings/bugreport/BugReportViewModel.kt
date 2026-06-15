package com.serranoie.app.minus.presentation.ui.settings.bugreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportIssueType
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportStep
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportUiEffect
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportUiIntent
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.asLog
import logcat.logcat
import javax.inject.Inject

@HiltViewModel
class BugReportViewModel @Inject constructor(
	private val zipGenerator: BugReportZipGenerator,
) : ViewModel() {

	private val _uiState = MutableStateFlow(BugReportUiState())
	val uiState: StateFlow<BugReportUiState> = _uiState.asStateFlow()

	private val _effects = MutableSharedFlow<BugReportUiEffect>()
	val effects: SharedFlow<BugReportUiEffect> = _effects.asSharedFlow()

	private var nextStepId = 3L

	fun onIntent(intent: BugReportUiIntent) {
		when (intent) {
			is BugReportUiIntent.SelectIssueType -> _uiState.update {
				it.copy(
					selectedIssueType = intent.issueType,
					showReproductionSteps = intent.issueType == BugReportIssueType.BugReport
				)
			}
			is BugReportUiIntent.ChangeTitle -> _uiState.update {
				it.copy(title = intent.value)
			}
			is BugReportUiIntent.ChangeDescription -> _uiState.update {
				it.copy(description = intent.value)
			}
			is BugReportUiIntent.ChangeProposedSolution -> _uiState.update {
				it.copy(proposedSolution = intent.value)
			}
			is BugReportUiIntent.ChangeAlternativesConsidered -> _uiState.update {
				it.copy(alternativesConsidered = intent.value)
			}
			is BugReportUiIntent.ToggleReproductionSteps -> _uiState.update {
				it.copy(showReproductionSteps = intent.visible)
			}
			is BugReportUiIntent.ChangeStep -> updateStep(intent.index, intent.value)
			BugReportUiIntent.AddStep -> addStep()
			is BugReportUiIntent.RemoveStep -> removeStep(intent.index)
			is BugReportUiIntent.FinishStepExit -> finishStepExit(intent.stepId)
			is BugReportUiIntent.ChangeAdditionalInfo -> _uiState.update {
				it.copy(additionalInfo = intent.value)
			}
			is BugReportUiIntent.SelectAttachments -> _uiState.update {
				it.copy(selectedAttachmentUris = intent.uris)
			}
			BugReportUiIntent.SubmitReport -> submitReport()
		}
	}

	private fun updateStep(index: Int, value: String) {
		_uiState.update { state ->
			val steps = state.reproductionSteps.toMutableList()
			val step = steps.getOrNull(index) ?: return@update state
			steps[index] = step.copy(value = value)
			state.copy(reproductionSteps = steps)
		}
	}

	private fun addStep() {
		_uiState.update { state ->
			state.copy(reproductionSteps = state.reproductionSteps + BugReportStep(id = nextStepId++))
		}
	}

	private fun removeStep(index: Int) {
		_uiState.update { state ->
			val steps = state.reproductionSteps.toMutableList()
			val step = steps.getOrNull(index) ?: return@update state
			if (steps.size > 1) {
				steps[index] = step.copy(visible = false)
			} else {
				steps[index] = step.copy(value = "")
			}
			state.copy(reproductionSteps = steps)
		}
	}

	private fun finishStepExit(stepId: Long) {
		_uiState.update { state ->
			state.copy(reproductionSteps = state.reproductionSteps.filterNot { it.id == stepId && !it.visible })
		}
	}

	private fun submitReport() {
		val currentState = _uiState.value
		if (!currentState.canSubmit) return

		viewModelScope.launch {
			_uiState.update { it.copy(isGeneratingReport = true) }
			runCatching { zipGenerator.generate(currentState) }
				.onSuccess { report ->
					val titlePrefix = if (currentState.selectedIssueType == BugReportIssueType.BugReport) {
						"[Bug]: "
					} else {
						"[Feature]: "
					}
					_effects.emit(
						BugReportUiEffect.OpenEmailComposer(
							uri = report.uri,
							fileName = report.fileName,
							title = "$titlePrefix${currentState.title}"
						)
					)
				}
				.onFailure { throwable ->
					logcat { "Failed to generate bug report: ${throwable.asLog()}" }
					_effects.emit(BugReportUiEffect.ShowError(throwable.message ?: "Failed to generate report"))
				}
			_uiState.update { it.copy(isGeneratingReport = false) }
		}
	}
}
