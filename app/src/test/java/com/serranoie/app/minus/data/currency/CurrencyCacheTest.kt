package com.serranoie.app.minus.data.currency

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.nio.file.Files

class CurrencyCacheTest {
    @Test fun manualAndDisplaySurviveRepositoryRecreation() = runBlocking {
        val file = Files.createTempDirectory("currency-cache-test").resolve("settings.preferences_pb").toFile()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            val repo = CurrencyConversionRepository(store)
            repo.setDisplayCurrency("BRL")
            repo.setManualRate("USD", "BRL", BigDecimal("5.12"))
            val recreated = CurrencyConversionRepository(store)
            val settings = recreated.preferences.first()
            assertEquals("BRL", settings.displayCurrency)
            assertEquals(BigDecimal("51.20"), settings.convert(BigDecimal.TEN, "USD"))
            repo.setManualRate("BRL", "USD", BigDecimal("0.2"))
            assertEquals(1, repo.preferences.first().manualRates.size)
            repo.removeManualRate("BRL", "USD")
            assertNull(repo.preferences.first().convert(BigDecimal.TEN, "USD"))
            repo.setDisplayCurrency(null)
            assertNull(repo.preferences.first().displayCurrency)
        } finally { scope.cancel(); file.parentFile?.deleteRecursively() }
    }
    @Test fun referenceCacheSurvivesDiskReopen() = runBlocking {
        val file = Files.createTempDirectory("currency-offline-test").resolve("settings.preferences_pb").toFile()
        val job = SupervisorJob()
        val scope = CoroutineScope(job + Dispatchers.IO)
        val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        store.edit {
            it[stringPreferencesKey("currency_conversion_v1")] = """{"displayCurrency":"BRL","reference":[{"base":"USD","quote":"BRL","rate":"4.9962","date":"2026-10-08","manual":false}]}"""
        }
        job.cancelAndJoin()
        val secondJob = SupervisorJob()
        try {
            val reopened = PreferenceDataStoreFactory.create(scope = CoroutineScope(secondJob + Dispatchers.IO), produceFile = { file })
            val settings = CurrencyConversionRepository(reopened).preferences.first()
            assertEquals(BigDecimal("49.9620"), settings.convert(BigDecimal.TEN, "USD"))
            assertEquals("2026-10-08", settings.referenceRates.single().date)
        } finally { secondJob.cancelAndJoin(); file.parentFile?.deleteRecursively() }
    }

}
