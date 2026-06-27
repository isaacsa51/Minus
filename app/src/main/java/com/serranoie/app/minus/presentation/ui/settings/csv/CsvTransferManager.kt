package com.serranoie.app.minus.presentation.ui.settings.csv

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.serranoie.app.minus.data.csv.CsvImportWorker
import com.serranoie.app.minus.data.csv.MinusCsvService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.asLog
import logcat.logcat
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvTransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val csvService: MinusCsvService
) {

    suspend fun exportAndShareCsv() = withContext(Dispatchers.IO) {
        val fileName = csvService.getExportFileName()
        val uri = saveCsvToDownloads(fileName)

        if (uri != null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export saved to Downloads", Toast.LENGTH_LONG).show()
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(shareIntent, "Export CSV")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            // Fallback to cache if MediaStore failed or API < 29
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val csvFile = File(exportDir, fileName)

            try {
                csvFile.outputStream().use { output ->
                    csvService.exportAllTransactions(output)
                }

                val cacheUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, cacheUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(
                    Intent.createChooser(shareIntent, "Export CSV")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
                logcat { "Failed to export CSV: ${e.asLog()}" }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to export CSV", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun saveCsvToDownloads(fileName: String): Uri? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        try {
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { output ->
                    csvService.exportAllTransactions(output)
                }
                return@withContext it
            }
        } catch (e: Exception) {
            logcat { "Failed to save CSV to Downloads: ${e.asLog()}" }
        }
        null
    }

    fun enqueueImport(uriString: String) {
        val request = OneTimeWorkRequestBuilder<CsvImportWorker>()
            .setInputData(
                Data.Builder()
                    .putString(CsvImportWorker.KEY_INPUT_URI, uriString)
                    .build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
