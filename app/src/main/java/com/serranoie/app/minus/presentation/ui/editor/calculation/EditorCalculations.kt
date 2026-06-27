package com.serranoie.app.minus.presentation.ui.editor.calculation

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Evaluate a free-form arithmetic expression entered by the user in the
 * editor's amount slot.
 *
 * Supports:
 * - decimal numbers (BigDecimal-backed; "." is the decimal separator)
 * - operators: `+`, `-`, `*` (also `×`), `/` (also `÷`)
 * - left-to-right evaluation (no precedence)
 * - division by zero -> `null`
 * - trailing operators -> `null`
 *
 * Returns a normalized string suitable for display:
 * - integers are returned without a decimal point (e.g. `5` not `5.00`)
 * - decimals are returned at scale=2 with `RoundingMode.HALF_UP`
 *
 * Returns `null` for any input that can't be evaluated cleanly (so the UI can
 * fall back to showing the raw expression).
 */
internal fun evaluateCalculation(input: String): String? {
    if (input.isBlank()) return null

    return try {
        val normalized = input.trim().replace("×", "*").replace("÷", "/")

        // Reject expressions ending in an operator (incomplete expression)
        normalized.lastOrNull()?.let { if (it in "+-*/") return null }

        val hasOperator = normalized.any { it in "+-*/" }

        if (!hasOperator) {
            val num = normalized.toBigDecimalOrNull() ?: return null
            return if (num.scale() <= 0 || num.stripTrailingZeros().scale() <= 0) {
                num.toBigInteger().toString()
            } else {
                num.setScale(2, RoundingMode.HALF_UP).toPlainString()
            }
        }

        val tokenPattern = Regex("([+\\-*/])")
        val parts = tokenPattern.split(normalized).filter { it.isNotEmpty() }
        val operators = tokenPattern.findAll(normalized).map { it.value }.toList()

        if (parts.isEmpty() || parts[0].isEmpty()) return null

        if (operators.size > parts.size - 1) return null

        var result = parts[0].toBigDecimalOrNull() ?: return null

        for (i in operators.indices) {
            if (i + 1 >= parts.size) break
            val operator = operators[i]
            val nextNum = parts[i + 1].toBigDecimalOrNull() ?: return null

            result = when (operator) {
                "+" -> result + nextNum
                "-" -> result - nextNum
                "*" -> result * nextNum
                "/" -> {
                    if (nextNum.compareTo(BigDecimal.ZERO) == 0) return null
                    result.divide(nextNum, 2, RoundingMode.HALF_UP)
                }

                else -> return null
            }
        }

        if (result.scale() <= 0 || result.stripTrailingZeros().scale() <= 0) {
            result.toBigInteger().toString()
        } else {
            result.setScale(2, RoundingMode.HALF_UP).toPlainString()
        }
    } catch (_: Exception) {
        null
    }
}
