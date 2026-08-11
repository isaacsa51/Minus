package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.colorMax
import com.serranoie.app.minus.presentation.ui.theme.colorMin
import com.serranoie.app.minus.presentation.ui.theme.component.StatCard
import com.serranoie.app.minus.presentation.ui.theme.component.charts.SpendsChart
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.harmonize
import com.serranoie.app.minus.presentation.util.isZero
import com.serranoie.app.minus.presentation.util.numberFormat
import com.serranoie.app.minus.presentation.util.prettyDate
import com.serranoie.app.minus.presentation.util.toDate
import com.serranoie.app.minus.presentation.util.toLocalDateTime
import com.serranoie.app.minus.presentation.util.toPalette
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Date

@Composable
fun MinMaxSpentCard(
    modifier: Modifier = Modifier,
    isMin: Boolean,
    spends: List<Transaction>,
    currency: String = "MXN",
) {
    val context = LocalContext.current

    val minSpent = spends.minByOrNull { it.amount }
    val maxSpent = spends.maxByOrNull { it.amount }

    val spent = if (isMin) minSpent else maxSpent

    val minValue = minSpent?.amount ?: BigDecimal.ZERO
    val maxValue = maxSpent?.amount ?: BigDecimal.ZERO
    val currValue = spent?.amount ?: BigDecimal.ZERO

    val harmonizedColor = toPalette(
        harmonize(
            combineColors(
                colorMin,
                colorMax,
                if ((maxValue - minValue).isZero()) {
                    if (isMin) 0f else 1f
                } else if (maxValue != BigDecimal.ZERO) {
                    ((currValue - minValue) / (maxValue - minValue)).toFloat()
                } else {
                    0f
                },
            )
        )
    )

    val formattedSpent = if (spent != null) {
        numberFormat(context, spent.amount, currency = currency)
    } else {
        null
    }
    val annotatedSpent = remember(formattedSpent, currency) {
        if (formattedSpent != null) {
            val currencySymbol = SupportedCurrency.findByCode(currency)?.symbol ?: ""
            if (currencySymbol.length > 2 && formattedSpent.startsWith(currencySymbol)) {
                AnnotatedString.Builder().apply {
                    pushStyle(SpanStyle(fontSize = TextUnit(1f, TextUnitType.Em) * 0.5f, baselineShift = BaselineShift(0f)))
                    append(currencySymbol)
                    pop()
                    append(formattedSpent.removePrefix(currencySymbol))
                }.toAnnotatedString()
            } else {
                AnnotatedString(formattedSpent)
            }
        } else {
            AnnotatedString("-")
        }
    }
    StatCard(
        modifier = modifier.heightIn(min = 140.dp),
        value = formattedSpent ?: "-",
        annotatedValue = annotatedSpent,
        label = if (isMin) stringResource(R.string.minimum_spent) else stringResource(R.string.maximum_spent),
        colors = CardDefaults.cardColors(
            containerColor = harmonizedColor.container,
            contentColor = harmonizedColor.onContainer,
        ),
        content = {
            Spacer(modifier = Modifier.height(6.dp))

            if (spent != null) {
                Text(
                    text = prettyDate(
                        spent.date,
                        showTime = true,
                        forceShowDate = true,
                        shortMonth = true,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (spent.comment.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(16.dp),
                            imageVector = Icons.Rounded.Label,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = spent.comment,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(),
                            style = MaterialTheme.typography.bodyMediumCondensed,
                        )
                    }
                }
            }
        },
        backdropContent = {
            if (spends.isNotEmpty()) {
                SpendsChart(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(),
                    spends = spends,
                    markedTransaction = spent,
                    chartPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                    showBeforeMarked = 4,
                    showAfterMarked = 1,
                )
            }
        }
    )
}

@Preview(name = "MinMaxSpentCard")
@Composable
private fun PreviewMinMaxSpentCard() {
    MinusTheme {
        Surface {
            Column {
                MinMaxSpentCard(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    isMin = false,
                    currency = "USD",
                    spends = listOf(
                        Transaction(
                            amount = BigDecimal(42),
                            date = LocalDate.now().minusDays(1).toDate().toLocalDateTime()
                        ),
                        Transaction(
                            amount = BigDecimal(42),
                            date = Date().toLocalDateTime()
                        ),
                    ),
                )

                MinMaxSpentCard(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    isMin = false,
                    currency = "EUR",
                    spends = listOf(),
                )
            }
        }
    }
}

@Preview(name = "MinMaxSpentCard Mobile", widthDp = 380, heightDp = 300)
@Composable
private fun PreviewMinMaxSpentCardMobile() {
    MinusTheme {
        Surface {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MinMaxSpentCard(
                    modifier = Modifier.weight(1f),
                    isMin = true,
                    currency = "MXN",
                    spends = listOf(
                        Transaction(
                            amount = BigDecimal(52),
                            date = LocalDate.now().minusDays(2).toDate().toLocalDateTime()
                        ),
                        Transaction(
                            amount = BigDecimal(42),
                            date = LocalDate.now().minusDays(1).toDate().toLocalDateTime()
                        ),
                        Transaction(
                            amount = BigDecimal(15),
                            date = Date().toLocalDateTime(),
                            comment = "Some weird payment to test if this edge case passes"
                        ),
                    ),
                )

                MinMaxSpentCard(
                    modifier = Modifier.weight(1f),
                    isMin = false,
                    currency = "MXN",
                    spends = listOf(
                        Transaction(
                            amount = BigDecimal(52),
                            date = LocalDate.now().minusDays(2).toDate().toLocalDateTime()
                        ),
                        Transaction(
                            amount = BigDecimal(72),
                            date = LocalDate.now().minusDays(1).toDate().toLocalDateTime()
                        ),
                        Transaction(
                            amount = BigDecimal(15),
                            date = Date().toLocalDateTime(),
                            comment = "Spotify"
                        ),
                    ),
                )
            }
        }
    }
}
