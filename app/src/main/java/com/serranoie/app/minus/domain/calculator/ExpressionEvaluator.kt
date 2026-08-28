package com.serranoie.app.minus.domain.calculator

import java.math.BigDecimal
import java.math.RoundingMode

private const val DIVISION_SCALE = 2
private const val OPERATOR_CHARS = "+-*/"

internal fun evaluateExpression(input: String): String? {
    if (input.isBlank()) return null

    return try {
        val normalized = input.trim()
            .replace("×", "*")
            .replace("÷", "/")

        val leadingOperator = if (normalized.startsWith("+") || normalized.startsWith("-")) {
            normalized[0]
        } else null

        val expression = if (leadingOperator != null) {
            normalized.substring(1).trim()
        } else {
            normalized
        }

        expression.lastOrNull()?.let { if (it in OPERATOR_CHARS) return null }

        val hasOperator = expression.any { it in OPERATOR_CHARS }
        if (!hasOperator) {
            val number = expression.toBigDecimalOrNull() ?: return null
            return formatResult(if (leadingOperator == '-') number.negate() else number)
        }

        val tokenPattern = Regex("([+\\-*/])")
        val parts = tokenPattern.split(expression).filter { it.isNotEmpty() }
        val operators = tokenPattern.findAll(expression).map { it.value.first() }.toList()

        if (parts.isEmpty() || parts[0].isEmpty()) return null
        if (operators.size > parts.size - 1) return null

        val numbers = parts.mapIndexed { index, part ->
            val value = part.toBigDecimalOrNull() ?: return null
            if (index == 0 && leadingOperator == '-') value.negate() else value
        }

        val additiveOperands = mutableListOf<BigDecimal>()
        val additiveOperators = mutableListOf<Char>()
        var acc = numbers[0]
        for (i in operators.indices) {
            if (i + 1 >= numbers.size) break
            val next = numbers[i + 1]
            when (operators[i]) {
                '*' -> acc = acc.multiply(next)
                '/' -> {
                    if (next.signum() == 0) return null
                    acc = acc.divide(next, DIVISION_SCALE, RoundingMode.HALF_UP)
                }

                else -> {
                    additiveOperands.add(acc)
                    additiveOperators.add(operators[i])
                    acc = next
                }
            }
        }
        additiveOperands.add(acc)

        var result = additiveOperands[0]
        for (i in additiveOperators.indices) {
            val next = additiveOperands[i + 1]
            result = when (additiveOperators[i]) {
                '+' -> result.add(next)
                '-' -> result.subtract(next)
                else -> return null
            }
        }

        formatResult(result)
    } catch (_: Exception) {
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
