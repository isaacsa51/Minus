package com.serranoie.app.minus.data.currency

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.serranoie.app.minus.domain.currency.ConversionPreferences
import com.serranoie.app.minus.domain.currency.ExchangeRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.net.URL
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@Serializable
internal data class RateRecord(val base: String, val quote: String, val rate: String, val date: String, val manual: Boolean) {
    fun domain() = ExchangeRate(base, quote, rate.toBigDecimal(), date, manual)
}
@Serializable
internal data class ConversionRecord(val displayCurrency: String? = null, val manual: List<RateRecord> = emptyList(), val reference: List<RateRecord> = emptyList()) {
    fun domain() = ConversionPreferences(displayCurrency, manual.map { it.domain() }, reference.map { it.domain() })
}
@Serializable
internal data class FrankfurterRate(val date: String, val base: String, val quote: String, val rate: kotlinx.serialization.json.JsonPrimitive)

// Parse decimal literals without a lossy Double intermediate.
internal fun parseReferenceRate(body: String, base: String, quote: String, today: LocalDate = LocalDate.now()): ExchangeRate {
    val rows = Json { ignoreUnknownKeys = true }.decodeFromString<List<FrankfurterRate>>(body)
    val row = rows.single { it.base == base && it.quote == quote }
    require(!row.rate.isString)
    return ExchangeRate(row.base, row.quote, row.rate.content.toBigDecimal(), row.date, false).also {
        require(!LocalDate.parse(it.date).isAfter(today))
    }
}

@Singleton
class CurrencyConversionRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
    private val key = stringPreferencesKey("currency_conversion_v1")
    private val json = Json { ignoreUnknownKeys = true }
    private fun read(raw: String?): ConversionRecord = runCatching {
        json.decodeFromString<ConversionRecord>(raw ?: "{}").also { it.domain() }
    }.getOrDefault(ConversionRecord())
    val preferences = dataStore.data.map { read(it[key]).domain() }

    suspend fun setDisplayCurrency(code: String?) {
        require(code == null || code.matches(Regex("[A-Z]{3}")))
        dataStore.edit { it[key] = json.encodeToString(read(it[key]).copy(displayCurrency = code)) }
    }
    suspend fun setManualRate(base: String, quote: String, rate: BigDecimal) {
        require(rate.precision() <= 30 && rate.scale() in -18..24)
        val checked = ExchangeRate(base, quote, rate, LocalDate.now().toString(), true)
        val record = RateRecord(checked.base, checked.quote, checked.rate.toPlainString(), checked.date, true)
        dataStore.edit {
            val old = read(it[key])
            // Keep only one manual override for the pair, including its inverse.
            val remaining = old.manual.filterNot { r -> setOf(r.base, r.quote) == setOf(base, quote) }
            it[key] = json.encodeToString(old.copy(manual = remaining + record))
        }
    }
    suspend fun removeManualRate(base: String, quote: String) {
        dataStore.edit {
            val old = read(it[key])
            it[key] = json.encodeToString(old.copy(manual = old.manual.filterNot { r -> r.base == base && r.quote == quote }))
        }
    }
    suspend fun fetchReferenceRate(base: String, quote: String) {
        require(base.matches(Regex("[A-Z]{3}")) && quote.matches(Regex("[A-Z]{3}")) && base != quote)
        val rate = withContext(Dispatchers.IO) {
            val connection = URL("https://api.frankfurter.dev/v2/rates?base=$base&quotes=$quote").openConnection() as HttpsURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/json")
            try {
                check(connection.responseCode == 200) { "Reference rate request failed (${connection.responseCode})" }
                val body = connection.inputStream.bufferedReader().use { reader ->
                    val chars = CharArray(65537)
                    var count = 0
                    while (count < chars.size) {
                        val n = reader.read(chars, count, chars.size - count)
                        if (n < 0) break
                        count += n
                    }
                    check(count <= 65536) { "Reference rate response too large" }
                    String(chars, 0, count)
                }
                parseReferenceRate(body, base, quote)
            } finally { connection.disconnect() }
        }
        val record = RateRecord(rate.base, rate.quote, rate.rate.toPlainString(), rate.date, false)
        dataStore.edit {
            val old = read(it[key])
            it[key] = json.encodeToString(old.copy(reference = old.reference.filterNot { r -> setOf(r.base, r.quote) == setOf(base, quote) } + record))
        }
    }
}
