package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.BarSegment
import com.serranoie.app.minus.presentation.ui.theme.component.LinearSavingBar
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.numberFormat
import java.math.BigDecimal
import java.math.RoundingMode

private data class IncomeCategoryData(
    val name: String,
    val amount: BigDecimal,
    val percentage: Float,
    val color: Color
)

private val IncomeColors = listOf(
    Color(0xFFF86BAE),
    Color(0xFFF36FFF),
    Color(0xFFAB96FF),
    Color(0xFF5FC7E7),
    Color(0xFF75E584),
    Color(0xFFFFD386),
    Color(0xFFEF7564),
    Color(0xFF64B5F6),
    Color(0xFFAED581),
    Color(0xFFFFB74D),
    Color(0xFFBA68C8),
    Color(0xFF4DB6AC),
    Color(0xFF9575CD),
    Color(0xFFF06292),
)

@Composable
fun IncomeAddedCard(
    modifier: Modifier = Modifier,
    incomes: List<Transaction>,
    categories: List<Category> = emptyList(),
    currency: String = "MXN",
) {
    val context = LocalContext.current
    val totalIncome = remember(incomes) {
        incomes.sumOf { it.amount }.abs()
    }

    val uncategorizedLabel = stringResource(R.string.categories_chart_uncategorized)

    val groupedData = remember(incomes, categories, totalIncome, uncategorizedLabel) {
        if (totalIncome == BigDecimal.ZERO) return@remember emptyList<IncomeCategoryData>()

        incomes.groupBy { it.categoryId }
            .map { (catId, txs) ->
                val category = categories.find { it.id == catId }
                val name = category?.name
                    ?: txs.firstOrNull()?.comment?.takeIf { it.isNotBlank() }
                    ?: uncategorizedLabel
                val amount = txs.sumOf { it.amount }.abs()
                val percentage = amount.divide(totalIncome, 4, RoundingMode.HALF_UP).toFloat()
                Triple(name, amount, percentage)
            }
            .sortedByDescending { it.second }
            .mapIndexed { index, triple ->
                IncomeCategoryData(
                    name = triple.first,
                    amount = triple.second,
                    percentage = triple.third,
                    color = IncomeColors[index % IncomeColors.size]
                )
            }
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = combineColors(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant,
                t = 0.3f,
            ),
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = numberFormat(context, totalIncome, currency = currency),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                modifier = Modifier.censor()
            )

            Text(
                text = stringResource(R.string.income_added),
                style = MaterialTheme.typography.labelMediumCondensed,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearSavingBar(
                segments = groupedData.map { BarSegment(weight = it.percentage, color = it.color) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                gap = 1
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                groupedData.forEach { data ->
                    IncomeCategoryItem(data, currency)
                }
            }
        }
    }
}

@Composable
private fun IncomeCategoryItem(
    data: IncomeCategoryData,
    currency: String
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(data.color)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Text(
                text = "${(data.percentage * 100).toInt()}%",
                style = MaterialTheme.typography.labelMediumCondensed,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }

        Text(
            text = numberFormat(context, data.amount, currency = currency),
            style = MaterialTheme.typography.bodyLargeEmphasized,
            modifier = Modifier.censor()
        )
    }
}

@Preview
@Composable
private fun PreviewIncomeAddedCard() {
    MinusTheme {
        IncomeAddedCard(
            incomes = listOf(
                Transaction(amount = BigDecimal("-2730"), categoryId = 1, date = null),
                Transaction(amount = BigDecimal("-1050"), categoryId = 2, date = null),
                Transaction(amount = BigDecimal("-420"), categoryId = 3, date = null),
            ),
            categories = listOf(
                Category(id = 1, name = "Salary"),
                Category(id = 2, name = "Freelance"),
                Category(id = 3, name = "Dividends"),
            )
        )
    }
}
