package com.serranoie.app.minus.presentation.util

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.min

/**
 * Simplified currency format that only supports USD/MXN with $ symbol.
 */
fun formatCurrency(amount: BigDecimal): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    format.currency = Currency.getInstance("USD")
    return format.format(amount)
}

/**
 * Gets the currency symbol ($ for USD/MXN).
 */
fun getCurrencySymbol(): String = "$"


fun getAnnotatedString(
    value: String,
    hintParts: List<Pair<Int, Int>>,
    styles: List<SpanStyle>,
): AnnotatedString {
    val builder = AnnotatedString.Builder(value)
    hintParts.forEachIndexed { index, part ->
        builder.addStyle(styles[index], part.first, part.second)
    }
    return builder.toAnnotatedString()
}

fun getAnnotatedString(
    value: String,
    hintParts: List<Pair<Int, Int>>,
    hintColor: Color,
): AnnotatedString {
    return getAnnotatedString(value, hintParts, hintParts.map { SpanStyle(color = hintColor) })
}

fun getAnnotatedString(
    value: String,
    hintPart: Pair<Int, Int>,
    hintColor: Color,
): AnnotatedString {
    return getAnnotatedString(value, listOf(hintPart), hintColor)
}

private fun calcShift(before: String, after: String, position: Int): Int {
    var shift = 0

    for (i in 0 until position) {
        while (i < before.length && i + shift < after.length && (before[i] != after[i + shift])) {
            shift += 1
        }
    }

    return shift
}

/**
 * Visual transformation that formats input as USD/MXN currency with $ symbol.
 */
private fun visualTransformationAsCurrency(
    context: Context,
    input: AnnotatedString,
    hintColor: Color,
): TransformedText {
    val floatDivider = getFloatDivider()
    val fixed = tryConvertStringToNumber(input.text)

    // Format as currency
    val amount = input.text.ifEmpty { "0" }.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val formatted = formatCurrency(amount)

    // Remove $ symbol for the raw number display
    val currSymbol = getCurrencySymbol()
    var output = formatted.replace(currSymbol, "").trim()

    val forceShowAfterDot = input.text.contains(".0")
    val before = output.substringBefore("${floatDivider}0")
    val after = if (forceShowAfterDot) {
        output.substringAfter(floatDivider, "")
    } else {
        output.substringAfter("${floatDivider}0", "")
    }

    val divider = if (fixed.third.isNotEmpty() || forceShowAfterDot) {
        "$floatDivider${fixed.third}"
    } else {
        ""
    }

    output = if (input.text.isEmpty()) "" else before + divider + after

    val offsetTranslator = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            val shift = calcShift(input.text.replace(".", floatDivider), output, offset)
            return (offset + shift).coerceIn(0, output.length)
        }

        override fun transformedToOriginal(offset: Int): Int {
            val shift = calcShift(input.text.replace(".", floatDivider), output, offset)
            return (offset - shift).coerceIn(0, input.length)
        }
    }

    return if (input.text.isEmpty()) {
        TransformedText(
            getAnnotatedString(
                output,
                listOf(),
                listOf(),
            ),
            offsetTranslator,
        )
    } else {
        TransformedText(
            getAnnotatedString(
                output,
                listOf(
                    Pair(
                        before.length + (if (fixed.third.isNotEmpty()) 1 else 0),
                        before.length + (if (fixed.third.isNotEmpty()) 2 else 0),
                    ),
                ),
                listOf(
                    SpanStyle(color = hintColor),
                ),
            ),
            offsetTranslator,
        )
    }
}

/**
 * Creates a visual transformation function for currency input.
 * Only supports USD/MXN with $ symbol.
 */
fun visualTransformationAsCurrency(
    context: Context,
    hintColor: Color,
): ((input: AnnotatedString) -> TransformedText) {
    return {
        visualTransformationAsCurrency(context, it, hintColor)
    }
}

/**
 * Currency amount input visual transformation following the blog post approach.
 * Takes raw digits as input and formats them as currency with two decimal places.
 *
 * Original input => Integer part + Decimal part => Output
 * 123 => 1 + 23 => 1.23
 * 542010 => 5420 + 10 => 5420.10
 * 1 => 0 + 01 => 0.01
 */
class CurrencyAmountInputVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val symbols = DecimalFormat().decimalFormatSymbols
        val thousandsSeparator = symbols.groupingSeparator
        val decimalSeparator = symbols.decimalSeparator

        val inputText = text.text

        // Handle empty input - show "0.00" as placeholder
        if (inputText.isEmpty()) {
            val newText = AnnotatedString(
                "0${decimalSeparator}00",
                text.spanStyles,
                text.paragraphStyles
            )
            // For empty input, use simple identity mapping
            val offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset.coerceIn(0, 4)
                override fun transformedToOriginal(offset: Int) = offset.coerceIn(0, 0)
            }
            return TransformedText(newText, offsetMapping)
        }

        // Get integer part (all digits except last 2)
        val intPart = if (inputText.length > 2) {
            inputText.substring(0, inputText.length - 2)
        } else {
            "0"
        }

        // Get decimal part (last 2 digits)
        var fractionPart = if (inputText.length >= 2) {
            inputText.substring(inputText.length - 2, inputText.length)
        } else {
            inputText
        }

        // Add zeros if the fraction part length is not 2
        if (fractionPart.length < 2) {
            fractionPart = fractionPart.padStart(2, '0')
        }

        // Add thousands separators to integer part
        val thousandsReplacementPattern = Regex("\\B(?=(?:\\d{3})+(?!\\d))")
        val formattedIntWithThousandsSeparator = intPart.replace(
            thousandsReplacementPattern,
            thousandsSeparator.toString()
        )

        // Build the final formatted string
        val newText = AnnotatedString(
            formattedIntWithThousandsSeparator + decimalSeparator + fractionPart,
            text.spanStyles,
            text.paragraphStyles
        )

        // Create offset mapping from the actual transformed text so cursor positions stay valid
        // around decimal and thousands separators.
        val offsetMapping = CurrencyAmountOffsetMapping(
            originalLength = inputText.length,
            transformedText = newText.text,
        )

        return TransformedText(newText, offsetMapping)
    }
}

/**
 * Offset mapping for currency amount input.
 * Provides bidirectional offset mapping between original cursor offsets and the digit positions
 * in the transformed text.
 */
class CurrencyAmountOffsetMapping(
    private val originalLength: Int,
    transformedText: String,
) : OffsetMapping {

    private val transformedLength = transformedText.length
    private val originalDigitOffsets = transformedText
        .mapIndexedNotNull { index, char -> if (char.isDigit()) index else null }
        .takeLast(originalLength)

    override fun originalToTransformed(offset: Int): Int {
        val safeOffset = offset.coerceIn(0, originalLength)
        return if (safeOffset == originalLength) {
            transformedLength
        } else {
            originalDigitOffsets.getOrElse(safeOffset) { transformedLength }
        }
    }

    override fun transformedToOriginal(offset: Int): Int {
        val safeOffset = offset.coerceIn(0, transformedLength)
        return originalDigitOffsets.count { digitOffset -> digitOffset < safeOffset }
            .coerceIn(0, originalLength)
    }
}

fun isNumber(char: Char): Boolean {
    return try {
        char.toString().toInt(); true
    } catch (e: Exception) {
        false
    }
}

fun Triple<String, String, String>.join(third: Boolean = true): String =
    this.first + this.second + if (third) this.third else ""

fun fixedNumberString(input: String): String {
    val dotExist = input.contains(".")
    val before = input.substringBefore(".")
    val after = input.substringAfter(".", "")

    var addZero = false
    var beforeFiltered = before
        .replace("\\D".toRegex(), "")
        .trimStart { addZero = addZero || it == '0'; it == '0'}

    if (addZero) beforeFiltered = "0$beforeFiltered"
    addZero = false

    var afterFiltered = after
        .replace("\\D".toRegex(), "")
        .trimEnd { addZero = addZero || it == '0'; it == '0' }

    if (addZero && afterFiltered.isEmpty()) afterFiltered = "${afterFiltered}0"

    if (afterFiltered.length > 2) afterFiltered = afterFiltered.dropLast(afterFiltered.length - 2)

    if (beforeFiltered.isEmpty() && afterFiltered.isEmpty()) return ""

    if (afterFiltered.isEmpty() && !dotExist) return beforeFiltered

    return "$beforeFiltered.$afterFiltered"
}

fun tryConvertStringToNumber(input: String): Triple<String, String, String> {
    val afterDot = input.dropWhile { it != '.' }
    val beforeDot = input.substring(0, input.length - afterDot.length)

    val start = beforeDot.filter { isNumber(it) }.dropWhile { it == '0' }
    val hintStart = if (start.isEmpty()) "0" else ""
    val end = afterDot.filter { isNumber(it) }
    var hintEnd = ""
    if (end.isEmpty() && input.lastOrNull() == '.') {
        hintEnd = "0"
    }
    val middle = if (end.isNotEmpty() || (input.lastOrNull() == '.')) {
        "."
    } else {
        ""
    }

    return Triple(
        hintStart,
        "$start$middle${end.substring(0, min(2, end.length))}",
        hintEnd,
    )
}

@Preview(name = "Currency visual transformations", showBackground = true)
@Composable
private fun VisualTransformationAsCurrencyPreview() {
    val context = LocalContext.current
    val legacyTransformation = visualTransformationAsCurrency(
        context = context,
        hintColor = Color.Green,
    )
    val amountInputTransformation = CurrencyAmountInputVisualTransformation()
    val decimalInputs = listOf("", "0", "12", "1000", "12233.45")
    val digitInputs = listOf("", "1", "12", "123", "542010", "123456789")

    Column {
        Text(text = "visualTransformationAsCurrency(context, hintColor)")
        decimalInputs.forEach { input ->
            Text(text = "${input.ifEmpty { "empty" }} → ${legacyTransformation(AnnotatedString(input)).text}")
        }

        Text(text = "CurrencyAmountInputVisualTransformation")
        digitInputs.forEach { input ->
            val transformed = remember(input) {
                amountInputTransformation.filter(AnnotatedString(input))
            }
            val start = remember(transformed) {
                transformed.offsetMapping.originalToTransformed(0)
            }
            val end = remember(transformed, input) {
                transformed.offsetMapping.originalToTransformed(input.length)
            }
            Text(
                text = "${input.ifEmpty { "empty" }} → ${transformed.text} | caret $start..$end"
            )
        }

        Text(text = "formatCurrency(BigDecimal)")
        listOf(BigDecimal.ZERO, BigDecimal("12.34"), BigDecimal("1234567.89")).forEach { amount ->
            Text(text = "$amount → ${formatCurrency(amount)}")
        }

        Text(text = "getCurrencySymbol() → ${getCurrencySymbol()}")
    }
}
