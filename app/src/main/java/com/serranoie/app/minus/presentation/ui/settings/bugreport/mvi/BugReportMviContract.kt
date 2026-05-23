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
	val currentBehavior: String = "",
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
}

sealed interface BugReportUiIntent {
	data class SelectIssueType(val issueType: BugReportIssueType) : BugReportUiIntent
	data class ChangeCurrentBehavior(val value: String) : BugReportUiIntent
	data class ChangeStep(val index: Int, val value: String) : BugReportUiIntent
	data object AddStep : BugReportUiIntent
	data class RemoveStep(val index: Int) : BugReportUiIntent
	data class FinishStepExit(val stepId: Long) : BugReportUiIntent
	data class ChangeAdditionalInfo(val value: String) : BugReportUiIntent
	data class SelectAttachments(val uris: List<Uri>) : BugReportUiIntent
	data object SubmitReport : BugReportUiIntent
}

sealed interface BugReportUiEffect {
	data class OpenEmailComposer(val uri: Uri, val fileName: String) : BugReportUiEffect
	data class ShowError(val message: String) : BugReportUiEffect
}
