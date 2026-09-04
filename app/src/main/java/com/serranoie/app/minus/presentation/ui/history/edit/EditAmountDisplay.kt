package com.serranoie.app.minus.presentation.ui.history.edit

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.domain.model.SymbolPosition
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorGood
import com.serranoie.app.minus.presentation.ui.theme.component.AutoResizeBasicTextField
import com.serranoie.app.minus.presentation.ui.theme.displayLargeCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleSmallCondensed
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal

private val AmountFontSize = 96.sp
private const val AmountSymbolScale = 0.45f
private val AmountMinFontSize = 24.sp

@Composable
internal fun EditAmountDisplay(
    rawAmount: String,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val currencyFormat = symbolOnlyCurrencyFormat(currencyCode)
    val amountValue = rawAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val isIncome = amountValue.signum() < 0

    val supportedCurrency = SupportedCurrency.findByCode(currencyCode)
    val currencySymbol = supportedCurrency?.symbol ?: "$"
    val isSymbolAtEnd = supportedCurrency?.symbolPosition == SymbolPosition.END

    val symbolStyle = MaterialTheme.typography.titleSmallCondensed.toSpanStyle()
    val annotatedAmount = remember(amountValue, isIncome, currencyCode, symbolStyle) {
        val formatted = currencyFormat.format(amountValue.abs())
        val amount = formatted.replace(currencySymbol, "").trim()
        val sign = if (isIncome) "+" else ""

        AnnotatedString.Builder().apply {
            val currencySpan = symbolStyle.copy(
                fontSize = AmountFontSize * AmountSymbolScale,
                fontWeight = FontWeight.Bold,
                baselineShift = BaselineShift(0f)
            )
            if (isSymbolAtEnd) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Light))
                append(sign)
                append(amount)
                pop()
                pushStyle(currencySpan)
                append(currencySymbol)
                pop()
            } else {
                pushStyle(SpanStyle(fontWeight = FontWeight.Light))
                append(sign)
                pop()
                pushStyle(currencySpan)
                append(currencySymbol)
                pop()
                pushStyle(SpanStyle(fontWeight = FontWeight.Light))
                append(amount)
                pop()
            }
        }.toAnnotatedString()
    }

    val baseTextStyle = MaterialTheme.typography.displayLargeCondensed.copy(
        fontWeight = FontWeight.W500,
        fontSize = AmountFontSize,
        color = if (isIncome) colorGood else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
    )

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd,
    ) {
        val density = LocalDensity.current
        val containerSizePx = remember(maxWidth, maxHeight, density) {
            with(density) {
                IntSize(width = maxWidth.toPx().toInt(), height = maxHeight.toPx().toInt())
            }
        }

        AutoResizeBasicTextField(
            value = rawAmount,
            annotatedValue = annotatedAmount,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.wrapContentWidth(Alignment.End),
            textStyle = baseTextStyle,
            singleLine = true,
            minFontSize = AmountMinFontSize,
            maxFontSize = AmountFontSize,
            containerSize = containerSizePx,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditAmountDisplayPreview() {
    MinusTheme {
        EditAmountDisplay(
            rawAmount = "1234.56",
            currencyCode = "USD",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditAmountDisplayIncomePreview() {
    MinusTheme {
        EditAmountDisplay(
            rawAmount = "-1234.56",
            currencyCode = "USD",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
        )
    }
}
