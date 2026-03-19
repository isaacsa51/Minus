package com.serranoie.app.minus.presentation.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.serranoie.app.minus.data.csv.CsvImportWorker
import com.serranoie.app.minus.data.csv.MinusCsvContract
import com.serranoie.app.minus.data.csv.MinusCsvService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvTransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val csvService: MinusCsvService
) {

    suspend fun exportAndShareCsv() = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(exportDir, MinusCsvContract.FILE_NAME)

        csvFile.outputStream().use { output ->
            csvService.exportAllTransactions(output)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            csvFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Export CSV")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
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
