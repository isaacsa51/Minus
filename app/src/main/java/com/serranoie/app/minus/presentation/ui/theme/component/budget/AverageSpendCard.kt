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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.component.StatCard
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.numberFormat
import java.math.RoundingMode
import java.time.LocalDateTime
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

    val totalAmount = remember(spends) {
        spends.sumOf { it.amount }
    }

    val days = remember(spends, startDate, finishDate) {
        if (startDate != null && finishDate != null) {
            val diff = finishDate.time - startDate.time
            (diff / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
        } else {
            val dates = spends.mapNotNull { it.date?.toLocalDate() }
            val minDate = dates.minOrNull()
            val maxDate = dates.maxOrNull()
            if (minDate != null && maxDate != null) {
                java.time.temporal.ChronoUnit.DAYS.between(minDate, maxDate).coerceAtLeast(1)
            } else 1
        }
    }

    val averagePerDay = remember(totalAmount, days, spends.isEmpty()) {
        if (spends.isEmpty()) return@remember null
        totalAmount.divide(days.toBigDecimal(), 2, RoundingMode.HALF_EVEN)
    }

    StatCard(
        modifier = modifier,
        value = if (averagePerDay != null) {
            numberFormat(
                context,
                averagePerDay,
                currency = currency,
            )
        } else {
            stringResource(R.string.empty)
        },
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
    name = "AverageSpendCard",
    device = "spec:width=500px,height=200px,dpi=440"
)
@Composable
private fun PreviewAverageSpendCard() {
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
