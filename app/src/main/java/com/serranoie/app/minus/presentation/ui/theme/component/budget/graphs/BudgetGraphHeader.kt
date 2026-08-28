package com.serranoie.app.minus.presentation.ui.theme.component.budget.graphs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.text.Format

@Composable
internal fun BudgetGraphHeader(
    totalSpent: BigDecimal,
    currencyFormat: Format,
    currencyCode: String,
) {
    val symbol = remember(currencyCode) { SupportedCurrency.findByCode(currencyCode)?.symbol ?: "$" }
    val formatted = currencyFormat.format(totalSpent)
    val symbolAtEnd = formatted.endsWith(symbol) && !formatted.startsWith(symbol)
    val amountOnly = if (symbolAtEnd) formatted.removeSuffix(symbol) else formatted.removePrefix(symbol)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.total_spent),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.Bottom) {
            val symbolText = @Composable {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.displaySmallEmphasized,
                    fontSize = 28.sp,
                )
            }
            if (!symbolAtEnd) symbolText()
            Text(
                text = amountOnly,
                style = MaterialTheme.typography.displaySmallEmphasized,
                fontSize = 28.sp,
                maxLines = 2,
                softWrap = true,
            )
            if (symbolAtEnd) symbolText()
        }
    }
}

@PreviewLightDark
@Composable
private fun BudgetGraphHeaderPreview() {
    MinusTheme {
        BudgetGraphHeader(
            totalSpent = BigDecimal("1234.56"),
            currencyFormat = symbolOnlyCurrencyFormat("USD"),
            currencyCode = "USD"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BudgetGraphHeaderEndSymbolPreview() {
    MinusTheme {
        BudgetGraphHeader(
            totalSpent = BigDecimal("123456"),
            currencyFormat = symbolOnlyCurrencyFormat("VND"),
            currencyCode = "VND"
        )
    }
}
