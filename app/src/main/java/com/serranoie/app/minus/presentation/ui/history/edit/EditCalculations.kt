package com.serranoie.app.minus.presentation.ui.history.edit

import java.math.BigDecimal
import java.math.RoundingMode

internal fun evaluateCalculation(input: String): String? {
    if (input.isBlank()) return null

    return try {
        val normalized = input.trim().replace("×", "*").replace("÷", "/")

        normalized.lastOrNull()?.let { if (it in "+-*/") return null }

        val hasOperator = normalized.any { it in "+-*/" }

        if (!hasOperator) {
            val num = normalized.toBigDecimalOrNull() ?: return null
            return formatResult(num)
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
                    if (nextNum.compareTo(BigDecimal.ZERO) == 0) return null // Division by zero
                    result.divide(nextNum, 2, RoundingMode.HALF_UP)
                }

                else -> return null
            }
        }

        formatResult(result)
    } catch (e: Exception) {
        null
    }
}

private fun formatResult(value: BigDecimal): String {
    val stripped = value.stripTrailingZeros()
    return if (stripped.scale() <= 0) {
        stripped.toPlainString()
    } else {
        value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }
}
