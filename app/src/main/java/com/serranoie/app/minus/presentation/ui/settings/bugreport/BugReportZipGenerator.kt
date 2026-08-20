package com.serranoie.app.minus.presentation.ui.settings.bugreport

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.serranoie.app.minus.BuildConfig
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportIssueType
import com.serranoie.app.minus.presentation.ui.settings.bugreport.mvi.BugReportUiState
import com.serranoie.app.minus.presentation.util.ErrorLogRecorder
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
	private val errorLogRecorder: ErrorLogRecorder,
) {

	suspend fun generate(state: BugReportUiState): GeneratedBugReport = withContext(Dispatchers.IO) {
		val reportDir = File(context.cacheDir, BUG_REPORT_CACHE_DIR).apply { mkdirs() }
		val fileName = buildFileName(state.selectedIssueType)
		val zipFile = File(reportDir, fileName)
		val markdown = buildMarkdown(state)

		ZipOutputStream(zipFile.outputStream().buffered()).use { zipOutput ->
			zipOutput.putNextEntry(ZipEntry(REPORT_MARKDOWN_FILE_NAME))
			zipOutput.write(markdown.toByteArray(Charsets.UTF_8))
			zipOutput.closeEntry()

			state.selectedAttachmentUris.forEachIndexed { index, uri ->
				val attachmentName = buildAttachmentEntryName(index, uri)
				zipOutput.putNextEntry(ZipEntry("attachments/$attachmentName"))
				context.contentResolver.openInputStream(uri)?.use { input ->
					input.copyTo(zipOutput)
				}
				zipOutput.closeEntry()
			}

			if (errorLogRecorder.hasEntries()) {
				zipOutput.putNextEntry(ZipEntry(ERROR_LOG_ENTRY_NAME))
				zipOutput.write(errorLogRecorder.readAll().toByteArray(Charsets.UTF_8))
				zipOutput.closeEntry()
			}
		}

		val uri = FileProvider.getUriForFile(
			context,
			"${context.packageName}.fileprovider",
			zipFile,
		)
		GeneratedBugReport(uri = uri, fileName = fileName, markdown = markdown)
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
		return when (state.selectedIssueType) {
			BugReportIssueType.BugReport -> buildBugReportMarkdown(state)
			BugReportIssueType.FeatureRequest -> buildFeatureRequestMarkdown(state)
		}
	}

	private fun buildBugReportMarkdown(state: BugReportUiState): String {
		val steps = if (state.showReproductionSteps) {
			state.reproductionSteps
				.filter { it.visible && it.value.isNotBlank() }
				.mapIndexed { index, step -> "${index + 1}. ${step.value.trim()}" }
				.ifEmpty { listOf("1. ", "2. ", "3. ") }
				.joinToString(separator = "\n")
		} else {
			"1. \n2. \n3. "
		}

		val attachments = state.selectedAttachmentUris
			.mapIndexed { index, uri -> "- attachments/${buildAttachmentEntryName(index, uri)}" }
			.ifEmpty { emptyList() }
			.joinToString(separator = "\n")

		return buildString {
			appendLine("# [Bug]: ${state.title}")
			appendLine()
			appendLine("## Description")
			appendLine("<!-- What happened? What did you expect to happen? -->")
			appendLine(state.description.takeIf { it.isNotBlank() }?.trim() ?: "")
			appendLine()
			appendLine("## Steps to reproduce")
			appendLine()
			appendLine(steps)
			appendLine()
			appendLine("## Screenshots or recordings")
			appendLine("<!-- Add screenshots or videos if this is a UI issue. -->")
			if (attachments.isNotBlank()) {
				appendLine(attachments)
			}
			appendLine()
			appendLine("## Environment")
			appendLine("<!-- You can get this holding tap on the version section of the app, if not this sections is populated when creating the bug report from the Bug Report from the app. -->")
			appendLine()
			appendLine(buildAppEnvironmentMetadata())
			appendLine()
			appendLine("## Additional context")
			appendLine("<!-- Logs, exported data details, recurrence/budget period setup, or anything else useful. -->")
			appendLine(state.additionalInfo.takeIf { it.isNotBlank() }?.trim() ?: "")
		}
	}

	private fun buildFeatureRequestMarkdown(state: BugReportUiState): String {
		val attachments = state.selectedAttachmentUris
			.mapIndexed { index, uri -> "- attachments/${buildAttachmentEntryName(index, uri)}" }
			.ifEmpty { emptyList() }
			.joinToString(separator = "\n")

		return buildString {
			appendLine("# [Feature]: ${state.title}")
			appendLine()
			appendLine("## Problem")
			appendLine("<!-- What problem or workflow would this improve? -->")
			appendLine(state.description.takeIf { it.isNotBlank() }?.trim() ?: "")
			appendLine()
			appendLine("## Proposed solution")
			appendLine("<!-- Describe the change you would like to see. -->")
			appendLine(state.proposedSolution.takeIf { it.isNotBlank() }?.trim() ?: "")
			appendLine()
			appendLine("## Alternatives considered")
			appendLine("<!-- Optional: other ways this could work. -->")
			appendLine(state.alternativesConsidered.takeIf { it.isNotBlank() }?.trim() ?: "")
			appendLine()
			appendLine("## Additional context")
			appendLine("<!-- Mockups, screenshots, examples, or related issues. -->")
			appendLine(state.additionalInfo.takeIf { it.isNotBlank() }?.trim() ?: "")
			if (attachments.isNotBlank()) {
				appendLine()
				appendLine("### Screenshots or recordings")
				appendLine(attachments)
			}
			appendLine()
			appendLine("### Metadata")
			appendLine("- App version: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
			appendLine("- Generated on: ${LocalDate.now()}")
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
		private const val ERROR_LOG_ENTRY_NAME = "diagnostics/error_log.txt"
		private val FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("ddMMMyyyy", Locale.US)
	}
}

data class GeneratedBugReport(
	val uri: Uri,
	val fileName: String,
	val markdown: String,
)

fun buildAppEnvironmentMetadata(): String {
	return buildString {
		appendLine("- App version: v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
		buildDeviceMetadata().forEach { (label, value) ->
			appendLine("- $label: $value")
		}
	}.trimEnd()
}

private fun buildDeviceMetadata(): List<Pair<String, String>> {
	return listOf(
		"Device" to listOf(Build.MANUFACTURER, Build.MODEL)
			.filter { it.isNotBlank() }
			.distinctBy { it.lowercase(Locale.US) }
			.joinToString(separator = " ")
			.ifBlank { "Unknown" },
		"Android version" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
		"Android codename" to Build.VERSION.CODENAME,
		"Brand" to Build.BRAND,
		"Product" to Build.PRODUCT,
		"Device name" to Build.DEVICE,
		"Hardware" to Build.HARDWARE,
		"Supported ABIs" to Build.SUPPORTED_ABIS.joinToString(separator = ", "),
		"Locale" to Locale.getDefault().toLanguageTag(),
	)
}
