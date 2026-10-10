package com.serranoie.app.minus.data.csv

import android.content.ContentResolver
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@HiltWorker
class CsvSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val csvService: MinusCsvService,
    private val settingsRepository: SettingsRepository,
    private val errorLogRecorder: ErrorLogRecorder,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = syncMutex.withLock { runSync() }

    private suspend fun runSync(): Result {
        val folder = settingsRepository.getString(SYNC_FOLDER_URI_KEY)
            ?.ifBlank { null }
            ?.toUri()
            ?: return Result.success()

        val resolver = applicationContext.contentResolver

        return try {
            val listing = childDocuments(folder)
                ?: error("Could not list the sync folder")
            val children = migrateLegacyMirror(resolver, folder, listing)

            val restored = restoreFromMirror(resolver, children)
            if (restored != null && restored.imported > 0) {
                recordStatus(STATUS_RESTORED)
                return Result.success()
            }
            if (restored != null && restored.errors.isNotEmpty()) {
                recordStatus(STATUS_REJECTED, EXPORT_FILE_NAME)
                return Result.success()
            }

            val outcome = ingest(resolver, children)
            writeMirror(resolver, folder, children)

            when {
                outcome.rejectedFiles.isNotEmpty() -> recordStatus(
                    STATUS_REJECTED,
                    outcome.rejectedFiles.joinToString(", ")
                )

                else -> recordStatus(STATUS_OK, outcome.imported.toString())
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            settingsRepository.setString(SYNC_FOLDER_URI_KEY, "")
            recordStatus(STATUS_NO_ACCESS)
            errorLogRecorder.record("CsvSyncWorker.doWork", e)
            Result.failure()
        } catch (e: Exception) {
            recordStatus(STATUS_ERROR, e.message.orEmpty())
            errorLogRecorder.record("CsvSyncWorker.doWork", e)
            Result.retry()
        }
    }

    private fun migrateLegacyMirror(
        resolver: ContentResolver,
        folder: Uri,
        children: List<Pair<Uri, String>>,
    ): List<Pair<Uri, String>> {
        if (children.any { (_, name) -> name == EXPORT_FILE_NAME }) return children

        val legacy = children.firstOrNull { (_, name) -> name == LEGACY_EXPORT_FILE_NAME }?.first
            ?: return children

        DocumentsContract.renameDocument(resolver, legacy, EXPORT_FILE_NAME)
            ?: error("Could not rename $LEGACY_EXPORT_FILE_NAME to $EXPORT_FILE_NAME")

        return childDocuments(folder) ?: error("Could not list the sync folder")
    }

    private suspend fun restoreFromMirror(
        resolver: ContentResolver,
        children: List<Pair<Uri, String>>,
    ): CsvImportResult? {
        val mirror = children.firstOrNull { (_, name) -> name == EXPORT_FILE_NAME }?.first
            ?: return null
        if (!csvService.hasNoLocalData()) return null

        return resolver.openInputStream(mirror)?.use { csvService.importTransactions(it) }
            ?: error("Could not open $EXPORT_FILE_NAME for reading")
    }

    private suspend fun ingest(
        resolver: ContentResolver,
        children: List<Pair<Uri, String>>,
    ): IngestOutcome {
        var imported = 0
        val rejected = mutableListOf<String>()

        children.filter { (_, name) -> isIngestCandidate(name) }
            .forEach { (document, name) ->
                val claimed = DocumentsContract.renameDocument(
                    resolver,
                    document,
                    name + SUFFIX_IN_PROGRESS
                ) ?: error("Could not claim $name for import")

                val result = try {
                    resolver.openInputStream(claimed)?.use { csvService.importTransactions(it) }
                        ?: error("Could not open $name for reading")
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (failure: Exception) {
                    DocumentsContract.renameDocument(resolver, claimed, name + SUFFIX_REJECTED)
                    throw failure
                }

                imported += result.imported
                val settledName = if (result.errors.isEmpty()) {
                    name + SUFFIX_IMPORTED
                } else {
                    rejected += name
                    name + SUFFIX_REJECTED
                }

                DocumentsContract.renameDocument(resolver, claimed, settledName)
                    ?: error("Imported $name but could not mark it as processed")
            }

        return IngestOutcome(imported, rejected)
    }

    private suspend fun writeMirror(
        resolver: ContentResolver,
        folder: Uri,
        children: List<Pair<Uri, String>>,
    ) {
        val treeDocument = DocumentsContract.buildDocumentUriUsingTree(
            folder,
            DocumentsContract.getTreeDocumentId(folder)
        )

        val staging = children.firstOrNull { (_, name) -> name == STAGING_FILE_NAME }?.first
            ?: DocumentsContract.createDocument(
                resolver,
                treeDocument,
                MIME_TYPE_CSV,
                STAGING_FILE_NAME
            )
            ?: error("Could not create $STAGING_FILE_NAME in the sync folder")

        resolver.openOutputStream(staging, "wt")?.use { output ->
            csvService.exportAllTransactions(output)
        } ?: error("Could not open $STAGING_FILE_NAME for writing")

        children.firstOrNull { (_, name) -> name == EXPORT_FILE_NAME }?.first?.let { previous ->
            val stamp = LocalDateTime.now().format(BACKUP_STAMP)
            DocumentsContract.renameDocument(resolver, previous, "$BACKUP_PREFIX$stamp.csv")
                ?: error("Could not rotate the previous $EXPORT_FILE_NAME")
        }

        DocumentsContract.renameDocument(resolver, staging, EXPORT_FILE_NAME)
            ?: error("Exported data but could not publish it as $EXPORT_FILE_NAME")

        pruneBackups(resolver, folder)
    }

    private fun pruneBackups(resolver: ContentResolver, folder: Uri) {
        val backups = childDocuments(folder)
            ?.filter { (_, name) -> isRotatedBackup(name) }
            ?.sortedByDescending { (_, name) -> name }
            ?: return

        backups.drop(BACKUPS_KEPT).forEach { (document, _) ->
            runCatching { DocumentsContract.deleteDocument(resolver, document) }
        }
    }

    private suspend fun recordStatus(code: String, detail: String = "") {
        settingsRepository.setString(
            SYNC_STATUS_KEY,
            listOf(code, System.currentTimeMillis().toString(), detail)
                .joinToString(STATUS_SEPARATOR)
        )
    }

    private fun childDocuments(folder: Uri): List<Pair<Uri, String>>? {
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
        }
    }

    private data class IngestOutcome(
        val imported: Int,
        val rejectedFiles: List<String>,
    )

    companion object {
        const val SYNC_FOLDER_URI_KEY = "sync_folder_uri"
        const val SYNC_STATUS_KEY = "sync_last_status"
        const val EXPORT_FILE_NAME = "minus_synced.csv"
        const val STAGING_FILE_NAME = "minus_synced.csv.writing"
        const val LEGACY_EXPORT_FILE_NAME = "minus_latest.csv"
        const val UNIQUE_WORK_NAME = "csv_folder_sync"
        const val IMMEDIATE_WORK_NAME = "csv_folder_sync_now"
        const val ACTION_SYNC_NOW = "com.serranoie.app.minus.action.SYNC_NOW"
        const val SYNC_INTERVAL_HOURS = 2L

        const val STATUS_SEPARATOR = "|"
        const val STATUS_OK = "ok"
        const val STATUS_RESTORED = "restored"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_ERROR = "error"
        const val STATUS_NO_ACCESS = "no_access"

        const val IMPORT_PREFIX = "minus_import_"
        const val BACKUP_PREFIX = "minus_backup-"
        const val BACKUPS_KEPT = 5
        const val SUFFIX_IN_PROGRESS = ".importing"
        const val SUFFIX_IMPORTED = ".imported"
        const val SUFFIX_REJECTED = ".rejected"

        private const val MIME_TYPE_CSV = "text/csv"
        private val BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

        private val syncMutex = Mutex()

        fun isIngestCandidate(displayName: String): Boolean {
            return displayName.startsWith(IMPORT_PREFIX, ignoreCase = true) &&
                    displayName.endsWith(".csv", ignoreCase = true)
        }

        fun isRotatedBackup(displayName: String): Boolean {
            return displayName.startsWith(BACKUP_PREFIX, ignoreCase = true) &&
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
