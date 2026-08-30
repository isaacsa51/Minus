package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.StatCard
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.numberFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Date

@Composable
fun AverageSpendCard(
    modifier: Modifier = Modifier,
    spends: List<Transaction>,
    startDate: Date? = null,
    finishDate: Date? = null,
    currency: String = "MXN",
) {
    val context = LocalContext.current

    val filteredSpends = remember(spends) {
        spends.filter { it.amount > BigDecimal.ZERO }
    }

    val totalAmount = remember(filteredSpends) {
        filteredSpends.sumOf { it.amount }
    }

    val days = remember(filteredSpends, startDate, finishDate) {
        if (startDate != null && finishDate != null) {
            val diff = finishDate.time - startDate.time
            (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        } else {
            val dates = filteredSpends.mapNotNull { it.date?.toLocalDate() }
            val minDate = dates.minOrNull()
            val maxDate = dates.maxOrNull()
            if (minDate != null && maxDate != null) {
                ChronoUnit.DAYS.between(minDate, maxDate).coerceAtLeast(1)
            } else 1
        }
    }

    val averagePerDay = remember(totalAmount, days, filteredSpends.isEmpty()) {
        if (filteredSpends.isEmpty()) return@remember null
        totalAmount.divide(days.toBigDecimal(), 2, RoundingMode.HALF_EVEN)
    }

    val formattedAverage = if (averagePerDay != null) {
        numberFormat(context, averagePerDay, currency = currency)
    } else {
        null
    }
    val emptyLabel = stringResource(R.string.empty)
    val annotatedAverage = remember(formattedAverage, currency, emptyLabel) {
        if (formattedAverage != null) {
            val currencySymbol = SupportedCurrency.findByCode(currency)?.symbol ?: ""
            if (currencySymbol.length > 2 && formattedAverage.startsWith(currencySymbol)) {
                AnnotatedString.Builder().apply {
                    pushStyle(
                        SpanStyle(
                            fontSize = TextUnit(1f, TextUnitType.Em) * 0.5f,
                            baselineShift = BaselineShift(0f)
                        )
                    )
                    append(currencySymbol)
                    pop()
                    append(formattedAverage.removePrefix(currencySymbol))
                }.toAnnotatedString()
            } else {
                AnnotatedString(formattedAverage)
            }
        } else {
            AnnotatedString(emptyLabel)
        }
    }

    StatCard(
        modifier = modifier,
        value = formattedAverage ?: emptyLabel,
        valueFontStyle = MaterialTheme.typography.labelSmallEmphasized,
        annotatedValue = annotatedAverage,
        colors = CardDefaults.cardColors(
            containerColor = combineColors(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant,
                t = 0.3f,
            ),
        ),
        label = stringResource(R.string.daily_average),
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    )
}


@Preview(
    device = "spec:width=500px,height=200px,dpi=440"
)
@Composable
private fun PreviewAverageSpendCard() {
    MinusTheme {
        AverageSpendCard(
            spends = listOf(
                Transaction(
                    amount = 100.toBigDecimal(),
                    date = LocalDateTime.now(),
                    comment = "Category",
                ),
            )
        )
    }
}
