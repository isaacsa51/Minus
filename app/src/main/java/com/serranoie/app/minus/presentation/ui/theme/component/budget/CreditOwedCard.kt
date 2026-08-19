package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleSmallCondensed
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
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

    val valueFontSize = MaterialTheme.typography.titleMedium.fontSize
    val useAnnotatedValue = currencySymbol.length > 2

    val infiniteTransition = rememberInfiniteTransition(label = "PulseEffect")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = 1.1f
            scaleY = 1.1f
        },
        contentAlignment = Alignment.Center
    ) {
        // Pulse Layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = pulseAlpha
                }
                .background(
                    color = MaterialTheme.colorScheme.tertiary,
                    shape = MaterialShapes.Clover4Leaf.toShape()
                )
        )

        Card(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialShapes.Clover4Leaf.toShape(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
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
                            style = MaterialTheme.typography.titleSmallCondensed.copy(fontWeight = FontWeight.Bold),
                            fontSize = valueFontSize * 0.65f,
                            color = LocalContentColor.current.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    Text(
                        text = if (useAnnotatedValue) formattedAmount else formattedValue,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        fontSize = valueFontSize,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        modifier = Modifier.censor(),
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = stringResource(R.string.credit_owed_label),
                    style = MaterialTheme.typography.labelSmallCondensed,
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
}

@Preview(showBackground = true, device = "spec:width=500px,height=500px,dpi=500")
@Composable
private fun CreditOwedCardPreview() {
    MinusTheme {
        CreditOwedCard(
            owed = BigDecimal("12871"),
            currency = "MAD",
            onClick = {},
            modifier = Modifier
                .padding(16.dp)
                .size(120.dp)
        )
    }
}
