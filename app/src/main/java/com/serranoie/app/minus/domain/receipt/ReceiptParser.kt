package com.serranoie.app.minus.domain.receipt

import com.serranoie.app.minus.domain.notification.ExpenseNotificationParser
import java.math.BigDecimal

data class ReceiptScan(
    val merchant: String?,
    val total: BigDecimal,
)

object ReceiptParser {

    private const val MERCHANT_LOOKAHEAD = 6
    private const val MERCHANT_MAX_LENGTH = 40
    private const val MERCHANT_MIN_LETTER_WEIGHT = 3

    private val AMOUNT = Regex("""(?<![\d.,])(?:\d{1,3}(?:[.,]\d{3})+|\d+)[.,]\d{1,2}(?![.,]?\d)""")
    private val TOTAL_AMOUNT = Regex("""(?<![\d.,])(?:\d{1,3}(?:[.,]\d{3})+|\d+)(?:[.,]\d{1,2})?(?![.,]?\d)""")
    private val SPACED_DECIMAL = Regex("""(\d)\s*([.,])\s*(\d{1,2})(?!\d)""")
    private val ONE_OR_ZERO_NEAR_DIGIT = Regex("""(?<=[\d.,])[OoIl|]|[OoIl|](?=\d)""")
    private val FIVE_OR_EIGHT_AFTER_DIGIT = Regex("""(?<=\d|\d[.,])[SB]""")

    fun parse(text: String): ReceiptScan? {
        val lines = ReceiptVocabulary.asciiDigits(text).lines().map { it.trim() }.filter { it.isNotEmpty() }
        val total = totalFrom(lines) ?: return null
        return ReceiptScan(merchant = merchantFrom(lines), total = total)
    }

    private fun amountsIn(line: String, pattern: Regex = AMOUNT): List<BigDecimal> =
        pattern.findAll(repairOcrDigits(line)).mapNotNull { ExpenseNotificationParser.normalize(it.value) }.toList()

    private fun totalAmountsIn(line: String) = amountsIn(line, TOTAL_AMOUNT)

    private fun repairOcrDigits(line: String): String {
        val digits = ONE_OR_ZERO_NEAR_DIGIT.replace(line) { if (it.value in "Oo") "0" else "1" }
        val fivesAndEights = FIVE_OR_EIGHT_AFTER_DIGIT.replace(digits) { if (it.value == "S") "5" else "8" }
        return SPACED_DECIMAL.replace(fivesAndEights, "$1$2$3")
    }

    private fun isTotalLine(line: String) = ReceiptVocabulary.isTotal(line) && !isSubtotalOrPayment(line)

    private fun isSubtotalOrPayment(line: String) =
        ReceiptVocabulary.isSubtotal(line) || ReceiptVocabulary.isPayment(line)

    private fun isFallbackLine(line: String) = !isSubtotalOrPayment(line) && !ReceiptVocabulary.isTaxOrCount(line)

    private fun totalFrom(lines: List<String>): BigDecimal? {
        val keyed = lines.indices
            .filter { isTotalLine(lines[it]) }
            .flatMap { i ->
                totalAmountsIn(lines[i]).ifEmpty {
                    lines.getOrNull(i + 1)?.takeIf(::isFallbackLine)?.let(::totalAmountsIn).orEmpty()
                }
            }
        val candidates = keyed.ifEmpty { fallbackAmounts(lines) }
        return candidates.filter { it.signum() > 0 }.maxOrNull()
    }

    private fun fallbackAmounts(lines: List<String>): List<BigDecimal> {
        val eligible = lines.filter(::isFallbackLine)
        val withCurrency = eligible.filter(ReceiptVocabulary::hasCurrency).flatMap(::amountsIn)
        return withCurrency.ifEmpty { eligible.flatMap(::amountsIn) }
    }

    private fun merchantFrom(lines: List<String>): String? =
        lines.take(MERCHANT_LOOKAHEAD).firstOrNull { line ->
            val letterLike = line.count { it.isLetterOrMark() }
            val weight = line.sumOf { c -> if (Character.isIdeographic(c.code)) 2 else if (c.isLetterOrMark()) 1 else 0 }
            weight >= MERCHANT_MIN_LETTER_WEIGHT &&
                letterLike * 2 > line.length &&
                !ReceiptVocabulary.isNoise(line) &&
                amountsIn(line).isEmpty()
        }?.take(MERCHANT_MAX_LENGTH)
}
