package com.serranoie.app.minus.data.currency

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ReferenceRateTest {
    private val today = LocalDate.parse("2026-10-08")
    private fun parse(body: String) = parseReferenceRate(body, "USD", "BRL", today)
    @Test fun decimalAndDate() {
        val rate = parse("""[{"base":"USD","quote":"BRL","rate":4.99621234567890123456,"date":"2026-10-08","providers":[]}]""")
        assertEquals(BigDecimal("4.99621234567890123456"), rate.rate)
        assertEquals("2026-10-08", rate.date)
        assertFalse(rate.manual)
    }
    @Test(expected = NoSuchElementException::class) fun wrongPair() { parse("""[{"base":"EUR","quote":"BRL","rate":5,"date":"2026-10-08"}]""") }
    @Test(expected = IllegalArgumentException::class) fun futureDate() { parse("""[{"base":"USD","quote":"BRL","rate":5,"date":"2026-10-09"}]""") }
    @Test(expected = IllegalArgumentException::class) fun negativeRate() { parse("""[{"base":"USD","quote":"BRL","rate":-1,"date":"2026-10-08"}]""") }
    @Test(expected = IllegalArgumentException::class) fun quotedRate() { parse("""[{"base":"USD","quote":"BRL","rate":"5","date":"2026-10-08"}]""") }
    @Test(expected = IllegalArgumentException::class) fun duplicatePair() { parse("""[{"base":"USD","quote":"BRL","rate":5,"date":"2026-10-08"},{"base":"USD","quote":"BRL","rate":6,"date":"2026-10-08"}]""") }
}
