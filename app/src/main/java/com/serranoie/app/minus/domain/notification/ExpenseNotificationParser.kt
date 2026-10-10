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

    fun parse(
        text: String,
        currencyCode: String,
        denyWords: List<String>,
    ): BigDecimal? {
        if (text.isBlank()) return null
        val haystack = text.lowercase()

        if (denyWords.any { it.isNotBlank() && haystack.contains(it.trim().lowercase()) }) return null

        val evidence = buildList {
            currencyCode.trim().lowercase().takeIf { it.isNotEmpty() }?.let { add(it) }
            SupportedCurrency.findByCode(currencyCode)
                ?.symbol
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() }
                ?.let { add(it) }
        }
        if (evidence.isEmpty() || evidence.none { haystack.contains(it) }) return null

        for (match in AMOUNT.findAll(haystack)) {
            if (haystack.getOrNull(match.range.last + 1) == '%') continue
            val leading = haystack.substring(
                (match.range.first - SIGN_WINDOW).coerceAtLeast(0),
                match.range.first,
            )
            if (match.value.startsWith("+") || leading.contains('+')) continue

            val from = (match.range.first - EVIDENCE_WINDOW).coerceAtLeast(0)
            val to = (match.range.last + 1 + EVIDENCE_WINDOW).coerceAtMost(haystack.length)
            if (evidence.none { haystack.substring(from, to).contains(it) }) continue

            val amount = normalize(match.value) ?: continue
            if (amount.compareTo(BigDecimal.ZERO) == 0) continue
            return amount
        }
        return null
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
