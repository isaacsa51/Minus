package com.serranoie.app.minus.presentation.ui.history.edit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorGood
import com.serranoie.app.minus.presentation.ui.theme.displayLargeCondensed
import java.math.BigDecimal
import java.text.NumberFormat

@Composable
internal fun EditAmountDisplay(
    rawAmount: String,
    currencyFormat: NumberFormat,
    style: TextStyle,
    modifier: Modifier = Modifier,
    isIncome: Boolean = false,
) {
    val formattedAmount = remember(rawAmount, isIncome) {
        try {
            val value = rawAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
            val formatted = currencyFormat.format(value.abs())
            if (isIncome) "+$formatted" else formatted
        } catch (e: Exception) {
            rawAmount
        }
    }

    Text(
        text = formattedAmount,
        style = style,
        color = if (isIncome) colorGood else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.End,
        modifier = modifier.fillMaxWidth()
    )
}


@Preview(showBackground = true)
@Composable
private fun EditAmountDisplayPreview() {
    MinusTheme {
        EditAmountDisplay(
            rawAmount = "1234.56",
            currencyFormat = NumberFormat.getCurrencyInstance(),
            style = MaterialTheme.typography.displayLargeCondensed,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
