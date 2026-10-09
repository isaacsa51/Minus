package com.serranoie.app.minus.data.csv

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.presentation.util.ErrorLogRecorder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CsvSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val csvService: MinusCsvService,
    private val settingsRepository: SettingsRepository,
    private val errorLogRecorder: ErrorLogRecorder,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val folder = settingsRepository.getString(SYNC_FOLDER_URI_KEY)
            ?.ifBlank { null }
            ?.toUri()
            ?: return Result.success()

        val resolver = applicationContext.contentResolver

        return try {
            val children = childDocuments(folder)

            children.filter { (_, name) -> isIngestCandidate(name) }
                .forEach { (document, _) ->
                    resolver.openInputStream(document)?.use { csvService.importTransactions(it) }
                    DocumentsContract.deleteDocument(resolver, document)
                }

            val target = children.firstOrNull { (_, name) -> name == EXPORT_FILE_NAME }?.first
                ?: DocumentsContract.createDocument(
                    resolver,
                    DocumentsContract.buildDocumentUriUsingTree(
                        folder,
                        DocumentsContract.getTreeDocumentId(folder)
                    ),
                    MIME_TYPE_CSV,
                    EXPORT_FILE_NAME
                )
                ?: error("Could not create $EXPORT_FILE_NAME in the sync folder")

            resolver.openOutputStream(target, "wt")?.use { output ->
                csvService.exportAllTransactions(output)
            } ?: error("Could not open $EXPORT_FILE_NAME for writing")

            Result.success()
        } catch (e: SecurityException) {
            settingsRepository.setString(SYNC_FOLDER_URI_KEY, "")
            errorLogRecorder.record("CsvSyncWorker.doWork", e)
            Result.failure()
        } catch (e: Exception) {
            errorLogRecorder.record("CsvSyncWorker.doWork", e)
            Result.retry()
        }
    }

    private fun childDocuments(folder: Uri): List<Pair<Uri, String>> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            folder,
            DocumentsContract.getTreeDocumentId(folder)
        )

        return applicationContext.contentResolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(0) ?: continue
                    val displayName = cursor.getString(1) ?: continue
                    add(
                        DocumentsContract.buildDocumentUriUsingTree(folder, documentId)
                                to displayName
                    )
                }
            }
        }.orEmpty()
    }

    companion object {
        const val SYNC_FOLDER_URI_KEY = "sync_folder_uri"
        const val EXPORT_FILE_NAME = "minus_latest.csv"
        const val UNIQUE_WORK_NAME = "csv_folder_sync"
        const val IMMEDIATE_WORK_NAME = "csv_folder_sync_now"
        const val ACTION_SYNC_NOW = "com.serranoie.app.minus.action.SYNC_NOW"
        const val SYNC_INTERVAL_HOURS = 2L

        private const val MIME_TYPE_CSV = "text/csv"

        fun isIngestCandidate(displayName: String): Boolean {
            return !displayName.equals(EXPORT_FILE_NAME, ignoreCase = true) &&
                    displayName.endsWith(".csv", ignoreCase = true)
        }

        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<CsvSyncWorker>(
                    SYNC_INTERVAL_HOURS,
                    TimeUnit.HOURS
                ).build()
            )
        }

        fun syncNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<CsvSyncWorker>().build()
            )
        }
    }
}
