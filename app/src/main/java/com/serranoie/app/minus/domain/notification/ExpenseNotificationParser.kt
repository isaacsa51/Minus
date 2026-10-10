package com.serranoie.app.minus.domain.notification

import com.serranoie.app.minus.domain.model.SupportedCurrency
import java.math.BigDecimal

const val PENDING_QUICK_ADD_AMOUNT_KEY_NAME = "pending_quick_add_amount"

object ExpenseNotificationParser {

    private const val EVIDENCE_WINDOW = 8
    private const val SIGN_WINDOW = 4

    private val AMOUNT = Regex(
        """[+\-]?\d{1,3}(?:[.,\u00A0 ]\d{3})+(?:[.,]\d{1,2})?|[+\-]?\d+(?:[.,]\d{1,2})?"""
    )

    /**
     * Symbols that more than one supported currency uses, such as "$" for USD, MXN, ARS, COP and
     * CLP. Seeing one of these says nothing about which currency the amount is in, so they are not
     * accepted as evidence on their own.
     */
    private val SHARED_SYMBOLS: Set<String> by lazy {
        SupportedCurrency.ALL
            .groupingBy { it.symbol.trim().lowercase() }
            .eachCount()
            .filterValues { it > 1 }
            .keys
    }

    fun parse(
        text: String,
        currencyCode: String,
        denyWords: List<String>,
    ): BigDecimal? {
        if (text.isBlank()) return null
        val haystack = text.lowercase()

        if (denyWords.any { it.isNotBlank() && haystack.contains(it.trim().lowercase()) }) return null

        val evidence = evidenceRanges(haystack, currencyCode)
        if (evidence.isEmpty()) return null

        for (match in AMOUNT.findAll(haystack)) {
            if (haystack.getOrNull(match.range.last + 1) == '%') continue
            val leading = haystack.substring(
                (match.range.first - SIGN_WINDOW).coerceAtLeast(0),
                match.range.first,
            )
            if (match.value.startsWith("+") || leading.contains('+')) continue

            val from = (match.range.first - EVIDENCE_WINDOW).coerceAtLeast(0)
            val to = (match.range.last + EVIDENCE_WINDOW).coerceAtMost(haystack.length - 1)
            if (evidence.none { it.first >= from && it.last <= to }) continue

            val amount = normalize(match.value) ?: continue
            if (amount.compareTo(BigDecimal.ZERO) == 0) continue
            return amount
        }
        return null
    }

    /**
     * Where in [haystack] the configured currency is spelled out. The ISO code always counts, but
     * only as a whole code, so "usdx" or "busd" is not evidence of USD. The symbol counts only when
     * a single supported currency uses it.
     */
    private fun evidenceRanges(haystack: String, currencyCode: String): List<IntRange> {
        val code = currencyCode.trim().lowercase()
        val ranges = mutableListOf<IntRange>()
        if (code.isNotEmpty()) ranges += occurrencesOf(haystack, code)

        val symbol = SupportedCurrency.findByCode(currencyCode)?.symbol?.trim()?.lowercase()
        if (!symbol.isNullOrEmpty() && symbol != code && symbol !in SHARED_SYMBOLS) {
            ranges += occurrencesOf(haystack, symbol)
        }
        return ranges
    }

    /**
     * Every occurrence of [token] in [haystack] that is not glued to a letter on either end, so an
     * alphabetic token is matched as a whole word while digits around it are still allowed
     * ("12.00usd" counts, "usdx" does not).
     */
    private fun occurrencesOf(haystack: String, token: String): List<IntRange> {
        if (token.isEmpty()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        var index = haystack.indexOf(token)
        while (index >= 0) {
            val endExclusive = index + token.length
            val startIsBounded = !token.first().isLetter() ||
                haystack.getOrNull(index - 1)?.isLetter() != true
            val endIsBounded = !token.last().isLetter() ||
                haystack.getOrNull(endExclusive)?.isLetter() != true
            if (startIsBounded && endIsBounded) ranges += index until endExclusive
            index = haystack.indexOf(token, index + 1)
        }
        return ranges
    }

    private fun normalize(token: String): BigDecimal? {
        val cleaned = token.trimStart('+', '-').filterNot { it.isWhitespace() || it == '\u00A0' }
        val lastSeparator = cleaned.indexOfLast { it == '.' || it == ',' }
        return runCatching {
            if (lastSeparator < 0) {
                BigDecimal(cleaned)
            } else {
                val fraction = cleaned.substring(lastSeparator + 1)
                if (fraction.length in 1..2) {
                    val whole = cleaned.take(lastSeparator).filter { it.isDigit() }
                    BigDecimal("${whole.ifEmpty { "0" }}.$fraction")
                } else {
                    BigDecimal(cleaned.filter { it.isDigit() })
                }
            }
        }.getOrNull()
    }
}
