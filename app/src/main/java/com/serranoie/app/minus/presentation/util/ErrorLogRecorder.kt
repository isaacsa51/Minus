package com.serranoie.app.minus.presentation.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorLogRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val logFile: File get() = File(context.filesDir, LOG_FILE_NAME)
    private val mutex = Mutex()

    suspend fun record(source: String, throwable: Throwable?) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER)
            val entry = buildString {
                append("[$timestamp] $source")
                if (throwable != null) {
                    append('\n')
                    append(throwable.stackTraceToString().trim())
                }
            }

            val existingEntries = if (logFile.exists()) {
                logFile.readText().split(ENTRY_SEPARATOR)
            } else {
                emptyList()
            }
            val trimmedEntries = (existingEntries + entry).takeLast(MAX_ENTRIES)
            logFile.writeText(trimmedEntries.joinToString(separator = ENTRY_SEPARATOR))
        }
    }

    fun hasEntries(): Boolean = logFile.exists() && logFile.length() > 0

    fun readAll(): String = if (hasEntries()) logFile.readText() else ""

    companion object {
        private const val LOG_FILE_NAME = "error_log.txt"
        private const val MAX_ENTRIES = 20
        private const val ENTRY_SEPARATOR = "\n\n---\n\n"
        private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
