@file:OptIn(ExperimentalMaterial3Api::class)

package com.serranoie.app.minus.presentation.ui.analytics.dialogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.budget.AverageSpendCard
import com.serranoie.app.minus.presentation.ui.theme.component.charts.DetailedChart
import com.serranoie.app.minus.presentation.ui.theme.component.date.HistoryDateDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.ExpenseItem
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Date

data class CategoryAnalyticsState(
    val periodFinished: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val spends: List<Transaction> = emptyList(),
    val wholeBudget: BigDecimal = BigDecimal.ZERO,
    val finishPeriodActualDate: Date? = null,
    val startPeriodDate: Date = Date(),
    val finishPeriodDate: Date? = null,
    val isLoading: Boolean = false,
    val categoryName: String = "",
    val categorySpends: List<Transaction> = emptyList(),
    val currencyCode: String = "USD"
)

@Composable
fun CategoryAnalytics(
    modifier: Modifier = Modifier,
    state: CategoryAnalyticsState = CategoryAnalyticsState(),
) {
    val scrollState = rememberScrollState()

    val navigationBarHeight =
        LocalWindowInsets.current.calculateBottomPadding().coerceAtLeast(16.dp)

    val groupedCategoryTransactions = remember(state.categorySpends) {
        state.categorySpends.sortedByDescending { it.date }.groupBy { it.date?.toLocalDate() }
            .toSortedMap(compareByDescending { it })
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(bottom = navigationBarHeight)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.categoryName,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.testTag("CategoryAnalyticsTitle"),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.categorySpends.size >= 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
            ) {
                DetailedChart(
                    spends = state.categorySpends,
                    currencyCode = state.currencyCode,
                    modifier = Modifier.fillMaxSize(),
                    chartPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 32.dp,
                    ),
                    graphColor = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        } else if (state.categorySpends.size == 1) {
            val transaction = state.categorySpends.first()
            val currencyFormat =
                symbolOnlyCurrencyFormat(state.currencyCode)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.single_expense),
                        style = MaterialTheme.typography.labelMediumCondensed,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currencyFormat.format(transaction.amount),
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (state.categorySpends.size > 1) {
            AverageSpendCard(
                spends = state.categorySpends,
                startDate = state.startPeriodDate,
                finishDate = state.finishPeriodDate,
                currency = state.currencyCode,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (state.categorySpends.isNotEmpty()) {
            val currencyFormat =
                symbolOnlyCurrencyFormat(state.currencyCode)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                groupedCategoryTransactions.forEach { (date, transactions) ->
                    HistoryDateDivider(date = date)

                    transactions.forEachIndexed { index, transaction ->
                        val position = when {
                            transactions.size == 1 -> PaddedListItemPosition.Single
                            index == 0 -> PaddedListItemPosition.First
                            index == transactions.size - 1 -> PaddedListItemPosition.Last
                            else -> PaddedListItemPosition.Middle
                        }

                        ExpenseItem(
                            modifier = Modifier.fillMaxWidth(),
                            transaction = transaction,
                            currencyFormat = currencyFormat,
                            position = position
                        )
                    }

                    val dayTotal = transactions.sumOf { it.amount }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "Total: ${currencyFormat.format(dayTotal)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (state.categorySpends.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay gastos en esta categoria",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Preview(name = "CategoryAnalytics - Multiple transactions")
@Composable
private fun PreviewCategoryAnalytics() {
    MinusTheme {
        CategoryAnalytics(
            state = CategoryAnalyticsState(
                categoryName = "Comida",
                categorySpends = listOf(
                    Transaction(
                        amount = BigDecimal("150.00"),
                        comment = "Comida",
                        date = LocalDateTime.now().minusDays(2)
                    ),
                    Transaction(
                        amount = BigDecimal("85.50"),
                        comment = "Comida",
                        date = LocalDateTime.now().minusDays(1)
                    ),
                    Transaction(
                        amount = BigDecimal("120.00"),
                        comment = "Comida",
                        date = LocalDateTime.now()
                    )
                )
            )
        )
    }
}

@Preview(name = "CategoryAnalytics - Single transaction")
@Composable
private fun PreviewCategoryAnalyticsSingle() {
    MinusTheme {
        CategoryAnalytics(
            state = CategoryAnalyticsState(
                categoryName = "Comida",
                categorySpends = listOf(
                    Transaction(
                        amount = BigDecimal("4.00"),
                        comment = "comida",
                        date = LocalDateTime.now()
                    )
                )
            )
        )
    }
}
