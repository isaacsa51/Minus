package com.serranoie.app.minus.data.csv

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvSyncWorkerTest {

    @Test
    fun `own export is never ingested`() {
        assertFalse(CsvSyncWorker.isIngestCandidate(CsvSyncWorker.EXPORT_FILE_NAME))
        assertFalse(CsvSyncWorker.isIngestCandidate("MINUS_LATEST.CSV"))
    }

    @Test
    fun `dropped csv files are ingested`() {
        assertTrue(CsvSyncWorker.isIngestCandidate("pluggy_2026-10-06.csv"))
        assertTrue(CsvSyncWorker.isIngestCandidate("minus_export.CSV"))
    }

    @Test
    fun `non csv files are ignored`() {
        assertFalse(CsvSyncWorker.isIngestCandidate("notes.txt"))
        assertFalse(CsvSyncWorker.isIngestCandidate("minus_latest.csv.bak"))
    }
}
