package com.serranoie.app.minus.presentation.ui.settings.bugreport

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.serranoie.app.minus.BuildConfig
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportIssueType
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BugReportZipGenerator @Inject constructor(
	@param:ApplicationContext private val context: Context,
) {

	suspend fun generate(state: BugReportUiState): GeneratedBugReport = withContext(Dispatchers.IO) {
		val reportDir = File(context.cacheDir, BUG_REPORT_CACHE_DIR).apply { mkdirs() }
		val fileName = buildFileName(state.selectedIssueType)
		val zipFile = File(reportDir, fileName)

		ZipOutputStream(zipFile.outputStream().buffered()).use { zipOutput ->
			zipOutput.putNextEntry(ZipEntry(REPORT_MARKDOWN_FILE_NAME))
			zipOutput.write(buildMarkdown(state).toByteArray(Charsets.UTF_8))
			zipOutput.closeEntry()

			state.selectedAttachmentUris.forEachIndexed { index, uri ->
				val attachmentName = buildAttachmentEntryName(index, uri)
				zipOutput.putNextEntry(ZipEntry("attachments/$attachmentName"))
				context.contentResolver.openInputStream(uri)?.use { input ->
					input.copyTo(zipOutput)
				}
				zipOutput.closeEntry()
			}
		}

		val uri = FileProvider.getUriForFile(
			context,
			"${context.packageName}.fileprovider",
			zipFile,
		)
		GeneratedBugReport(uri = uri, fileName = fileName)
	}

	private fun buildFileName(issueType: BugReportIssueType): String {
		val prefix = when (issueType) {
			BugReportIssueType.BugReport -> "bug"
			BugReportIssueType.FeatureRequest -> "request"
		}
		val date = LocalDate.now().format(FILE_DATE_FORMATTER).lowercase(Locale.US)
		return "${prefix}_report_on_v${BuildConfig.VERSION_NAME}-$date.zip"
	}

	private fun buildMarkdown(state: BugReportUiState): String {
		val issueType = when (state.selectedIssueType) {
			BugReportIssueType.BugReport -> "Bug Report"
			BugReportIssueType.FeatureRequest -> "Feature Request"
		}
		val behaviorTitle = when (state.selectedIssueType) {
			BugReportIssueType.BugReport -> "What is happening?"
			BugReportIssueType.FeatureRequest -> "What should change?"
		}
		val steps = state.reproductionSteps
			.filter { it.visible && it.value.isNotBlank() }
			.mapIndexed { index, step -> "${index + 1}. ${step.value.trim()}" }
			.ifEmpty { listOf("No steps provided.") }
			.joinToString(separator = "\n")
		val attachments = state.selectedAttachmentUris
			.mapIndexed { index, uri -> "- attachments/${buildAttachmentEntryName(index, uri)}" }
			.ifEmpty { listOf("No attachments selected.") }
			.joinToString(separator = "\n")

		return buildString {
			appendLine("# Minus Feedback Report")
			appendLine()
			appendLine("## Metadata")
			appendLine("- Issue type: $issueType")
			appendLine("- App version: v${BuildConfig.VERSION_NAME}")
			appendLine("- Generated on: ${LocalDate.now()}")
			appendLine()
			appendLine("## $behaviorTitle")
			appendLine(state.currentBehavior.takeIf { it.isNotBlank() }?.trim() ?: "No description provided.")
			appendLine()
			appendLine("## Steps to reproduce")
			appendLine(steps)
			appendLine()
			appendLine("## Other information")
			appendLine(state.additionalInfo.takeIf { it.isNotBlank() }?.trim() ?: "No additional information provided.")
			appendLine()
			appendLine("## Attachments")
			appendLine(attachments)
		}
	}

	private fun buildAttachmentEntryName(index: Int, uri: Uri): String {
		val fallbackName = "attachment_${index + 1}"
		val displayName = queryDisplayName(uri).orEmpty().ifBlank { fallbackName }
		return sanitizeZipEntryName(displayName).ifBlank { fallbackName }
	}

	private fun queryDisplayName(uri: Uri): String? {
		return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
			?.use { cursor ->
				if (!cursor.moveToFirst()) return@use null
				val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
				if (nameIndex < 0) null else cursor.getString(nameIndex)
			}
	}

	private fun sanitizeZipEntryName(value: String): String {
		val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
			.replace("\\p{Mn}+".toRegex(), "")
		return normalized
			.replace("[^A-Za-z0-9._-]".toRegex(), "_")
			.trim('_')
	}

	companion object {
		private const val BUG_REPORT_CACHE_DIR = "bug_reports"
		private const val REPORT_MARKDOWN_FILE_NAME = "report.md"
		private val FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.US)
	}
}

data class GeneratedBugReport(
	val uri: Uri,
	val fileName: String,
)
