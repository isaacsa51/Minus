package com.serranoie.app.minus.data.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvSyncWorkerTest {

    @Test
    fun `own export is never ingested`() {
        assertFalse(CsvSyncWorker.isIngestCandidate(CsvSyncWorker.EXPORT_FILE_NAME))
        assertFalse(CsvSyncWorker.isIngestCandidate("MINUS_SYNCED.CSV"))
    }

    @Test
    fun `only prefixed csv files are ingested`() {
        assertTrue(CsvSyncWorker.isIngestCandidate("minus_import_2026-10-06.csv"))
        assertTrue(CsvSyncWorker.isIngestCandidate("MINUS_IMPORT_pluggy.CSV"))
    }

    @Test
    fun `unrelated files in a shared folder are left alone`() {
        assertFalse(CsvSyncWorker.isIngestCandidate("bank_statement.csv"))
        assertFalse(CsvSyncWorker.isIngestCandidate("pluggy_2026-10-06.csv"))
        assertFalse(CsvSyncWorker.isIngestCandidate("notes.txt"))
        assertFalse(CsvSyncWorker.isIngestCandidate("minus_synced.csv.bak"))
    }

    @Test
    fun `rotated backups are never re-ingested`() {
        val backup = "minus_backup-20261009-143052.csv"
        assertTrue(CsvSyncWorker.isRotatedBackup(backup))
        assertFalse(CsvSyncWorker.isIngestCandidate(backup))
        assertFalse(CsvSyncWorker.isRotatedBackup(CsvSyncWorker.EXPORT_FILE_NAME))
        assertFalse(CsvSyncWorker.isRotatedBackup(CsvSyncWorker.STAGING_FILE_NAME))
    }

    @Test
    fun `the legacy mirror name is not ingested during migration`() {
        assertFalse(CsvSyncWorker.isIngestCandidate(CsvSyncWorker.LEGACY_EXPORT_FILE_NAME))
        assertFalse(CsvSyncWorker.isRotatedBackup(CsvSyncWorker.LEGACY_EXPORT_FILE_NAME))
        assertFalse(CsvSyncWorker.LEGACY_EXPORT_FILE_NAME == CsvSyncWorker.EXPORT_FILE_NAME)
    }

    @Test
    fun `staging file is not ingested or mistaken for the mirror`() {
        assertFalse(CsvSyncWorker.isIngestCandidate(CsvSyncWorker.STAGING_FILE_NAME))
        assertFalse(CsvSyncWorker.STAGING_FILE_NAME == CsvSyncWorker.EXPORT_FILE_NAME)
    }

    @Test
    fun `backup names sort newest first`() {
        val names = listOf(
            "minus_backup-20261009-090000.csv",
            "minus_backup-20261009-143052.csv",
            "minus_backup-20261008-235959.csv",
        ).sortedByDescending { it }

        assertEquals("minus_backup-20261009-143052.csv", names.first())
        assertEquals("minus_backup-20261008-235959.csv", names.last())
    }

    @Test
    fun `settled files are never ingested again`() {
        val dropped = "minus_import_pluggy.csv"
        assertTrue(CsvSyncWorker.isIngestCandidate(dropped))
        assertFalse(
            CsvSyncWorker.isIngestCandidate(dropped + CsvSyncWorker.SUFFIX_IN_PROGRESS)
        )
        assertFalse(CsvSyncWorker.isIngestCandidate(dropped + CsvSyncWorker.SUFFIX_IMPORTED))
        assertFalse(CsvSyncWorker.isIngestCandidate(dropped + CsvSyncWorker.SUFFIX_REJECTED))
    }

    @Test
    fun `status detail survives a separator in the payload`() {
        val detail = "a.csv, b.csv"
        val raw = listOf(CsvSyncWorker.STATUS_REJECTED, "123", detail)
            .joinToString(CsvSyncWorker.STATUS_SEPARATOR)
        val parts = raw.split(CsvSyncWorker.STATUS_SEPARATOR)

        assertEquals(CsvSyncWorker.STATUS_REJECTED, parts[0])
        assertEquals(123L, parts[1].toLong())
        assertEquals(detail, parts.drop(2).joinToString(CsvSyncWorker.STATUS_SEPARATOR))
    }
}
