package com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi

import android.net.Uri

enum class BugReportIssueType {
	BugReport,
	FeatureRequest,
}

data class BugReportStep(
	val id: Long,
	val value: String = "",
	val visible: Boolean = true,
)

data class BugReportUiState(
	val selectedIssueType: BugReportIssueType = BugReportIssueType.BugReport,
	val title: String = "",
	val description: String = "",
	val proposedSolution: String = "",
	val alternativesConsidered: String = "",
	val showReproductionSteps: Boolean = true,
	val reproductionSteps: List<BugReportStep> = listOf(
		BugReportStep(id = 0L),
		BugReportStep(id = 1L),
		BugReportStep(id = 2L),
	),
	val additionalInfo: String = "",
	val selectedAttachmentUris: List<Uri> = emptyList(),
	val isGeneratingReport: Boolean = false,
) {
	val attachmentCount: Int = selectedAttachmentUris.size
	val hasTitle: Boolean = title.isNotBlank()
	val hasDescription: Boolean = description.isNotBlank()
	val hasProposedSolution: Boolean = proposedSolution.isNotBlank()
	val hasReproductionSteps: Boolean = reproductionSteps.any { it.visible && it.value.isNotBlank() }
	
	val canSubmit: Boolean = hasTitle && hasDescription && 
			(selectedIssueType != BugReportIssueType.FeatureRequest || hasProposedSolution) &&
			(selectedIssueType != BugReportIssueType.BugReport || !showReproductionSteps || hasReproductionSteps) && 
			!isGeneratingReport
}

sealed interface BugReportUiIntent {
	data class SelectIssueType(val issueType: BugReportIssueType) : BugReportUiIntent
	data class ChangeTitle(val value: String) : BugReportUiIntent
	data class ChangeDescription(val value: String) : BugReportUiIntent
	data class ChangeProposedSolution(val value: String) : BugReportUiIntent
	data class ChangeAlternativesConsidered(val value: String) : BugReportUiIntent
	data class ToggleReproductionSteps(val visible: Boolean) : BugReportUiIntent
	data class ChangeStep(val index: Int, val value: String) : BugReportUiIntent
	data object AddStep : BugReportUiIntent
	data class RemoveStep(val index: Int) : BugReportUiIntent
	data class FinishStepExit(val stepId: Long) : BugReportUiIntent
	data class ChangeAdditionalInfo(val value: String) : BugReportUiIntent
	data class SelectAttachments(val uris: List<Uri>) : BugReportUiIntent
	data class RemoveAttachment(val uri: Uri) : BugReportUiIntent
	data object SubmitReport : BugReportUiIntent
}

sealed interface BugReportUiEffect {
	data class OpenEmailComposer(val uri: Uri, val fileName: String, val title: String, val body: String) : BugReportUiEffect
	data class ShowError(val message: String) : BugReportUiEffect
}
