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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.StatCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.AverageSpendCard
import com.serranoie.app.minus.presentation.ui.theme.component.charts.DetailedChart
import com.serranoie.app.minus.presentation.ui.theme.component.date.DayTotalItem
import com.serranoie.app.minus.presentation.ui.theme.component.date.HistoryDateDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.ExpenseItem
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Date

data class CategoryAnalyticsState(
    val startPeriodDate: Date = Date(),
    val finishPeriodDate: Date? = null,
    val isLoading: Boolean = false,
    val categoryName: String = "",
    val categorySpends: List<Transaction> = emptyList(),
    val currencyCode: String = "USD",
    val creditCardCutoffDay: Int? = null,
    val isDayView: Boolean = false,
)

@Composable
fun CategoryAnalytics(
    modifier: Modifier = Modifier,
    state: CategoryAnalyticsState = CategoryAnalyticsState(),
) {
    val scrollState = rememberScrollState()

    val groupedCategoryTransactions = remember(state.categorySpends) {
        state.categorySpends.sortedByDescending { it.date }.groupBy { it.date?.toLocalDate() }
            .toSortedMap(compareByDescending { it })
    }

    val statCardColors = CardDefaults.cardColors(
        containerColor = combineColors(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant,
            t = 0.3f,
        ),
    )

    Column(
        modifier = modifier.verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("CategoryAnalyticsTitle"),
                text = state.categoryName,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (state.isLoading) {
            val loadingLabel = stringResource(R.string.loading)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .semantics { contentDescription = loadingLabel },
                contentAlignment = Alignment.Center,
            ) {
                ContainedLoadingIndicator()
            }

            Spacer(modifier = Modifier.height(32.dp))
            return@Column
        }

        val currencyFormat = symbolOnlyCurrencyFormat(state.currencyCode)

        if (state.categorySpends.size >= 2 && !state.isDayView) {
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
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        } else if (state.categorySpends.size >= 2) {
            val spentTotal = remember(state.categorySpends) {
                state.categorySpends.filter { it.amount > BigDecimal.ZERO }.sumOf { it.amount }
            }

            StatCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = currencyFormat.format(spentTotal),
                label = stringResource(R.string.total_spent),
                valueFontStyle = MaterialTheme.typography.headlineSmallEmphasized,
                valueFontSize = MaterialTheme.typography.headlineSmallEmphasized.fontSize,
                colors = statCardColors,
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            )

            Spacer(modifier = Modifier.height(12.dp))
        } else if (state.categorySpends.size == 1) {
            val transaction = state.categorySpends.first()

            StatCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                value = currencyFormat.format(transaction.amount),
                label = stringResource(R.string.single_expense),
                valueFontStyle = MaterialTheme.typography.headlineSmallEmphasized,
                valueFontSize = MaterialTheme.typography.headlineSmallEmphasized.fontSize,
                colors = statCardColors,
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.categorySpends.size > 1 && !state.isDayView) {
            AverageSpendCard(
                spends = state.categorySpends,
                startDate = state.startPeriodDate,
                finishDate = state.finishPeriodDate,
                currency = state.currencyCode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.categorySpends.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                groupedCategoryTransactions.forEach { (date, transactions) ->
                    if (!state.isDayView) {
                        HistoryDateDivider(date = date)
                    }

                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
                                position = position,
                                sharedTransitionScope = null,
                                animatedVisibilityScope = null,
                                creditCardCutoffDay = state.creditCardCutoffDay,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            )

                            if (index < transactions.size - 1) {
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }

                        if (!state.isDayView) {
                            val dayTotal =
                                transactions.filter { it.amount > BigDecimal.ZERO }
                                    .sumOf { it.amount }
                            DayTotalItem(
                                total = dayTotal,
                                currencyFormat = currencyFormat,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (state.categorySpends.isEmpty()) {
            val emptyMessage = if (state.isDayView) {
                stringResource(R.string.day_no_expenses)
            } else {
                stringResource(R.string.category_no_expenses)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                PaddedListItem(
                    title = emptyMessage,
                    icon = Icons.Default.Info,
                    onClick = {},
                    position = PaddedListItemPosition.Single
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
                creditCardCutoffDay = 15,
                categorySpends = listOf(
                    Transaction(
                        amount = BigDecimal("150.00"),
                        comment = "Comida",
                        date = LocalDateTime.now().minusDays(2),
                        isCredit = true
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

@Preview(name = "CategoryAnalytics - Day view")
@Composable
private fun PreviewCategoryAnalyticsDayView() {
    MinusTheme {
        CategoryAnalytics(
            state = CategoryAnalyticsState(
                categoryName = "25 September",
                isDayView = true,
                categorySpends = listOf(
                    Transaction(
                        amount = BigDecimal("40.00"),
                        comment = "Cafe",
                        date = LocalDateTime.now()
                    ),
                    Transaction(
                        amount = BigDecimal("12.50"),
                        comment = "Bus",
                        date = LocalDateTime.now()
                    )
                )
            )
        )
    }
}

@Preview(name = "CategoryAnalytics - Loading")
@Composable
private fun PreviewCategoryAnalyticsLoading() {
    MinusTheme {
        CategoryAnalytics(
            state = CategoryAnalyticsState(
                categoryName = "Comida",
                isLoading = true,
            )
        )
    }
}
