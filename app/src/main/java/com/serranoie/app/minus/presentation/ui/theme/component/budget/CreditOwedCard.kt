package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleSmallCondensed
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreditOwedCard(
    owed: BigDecimal,
    currency: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currencyFormat = symbolOnlyCurrencyFormat(currency)
    val currencySymbol = SupportedCurrency.findByCode(currency)?.symbol ?: "$"
    val formattedValue = currencyFormat.format(owed)
    val formattedAmount = formattedValue.removePrefix(currencySymbol)

    val valueFontSize = MaterialTheme.typography.titleLargeEmphasized.fontSize
    val useAnnotatedValue = currencySymbol.length > 2

    Card(
        modifier = modifier,
        shape = MaterialShapes.SoftBurst.toShape(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                if (useAnnotatedValue) {
                    Text(
                        text = currencySymbol,
                        style = MaterialTheme.typography.titleSmallCondensed,
                        fontSize = valueFontSize * 0.5f,
                        color = LocalContentColor.current.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Text(
                    text = if (useAnnotatedValue) formattedAmount else formattedValue,
                    style = MaterialTheme.typography.displaySmallEmphasized.copy(fontWeight = FontWeight.Light),
                    fontSize = valueFontSize,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    modifier = Modifier.censor(),
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = stringResource(R.string.credit_owed_label),
                style = MaterialTheme.typography.labelMediumCondensed,
                color = LocalContentColor.current.copy(alpha = 0.6f),
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=500px,height=500px,dpi=500")
@Composable
private fun CreditOwedCardPreview() {
    MinusTheme {
        CreditOwedCard(
            owed = BigDecimal("121"),
            currency = "MAD",
            onClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
