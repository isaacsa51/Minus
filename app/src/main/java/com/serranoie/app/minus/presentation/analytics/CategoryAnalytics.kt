@file:OptIn(ExperimentalMaterial3Api::class)

package com.serranoie.app.minus.presentation.analytics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.LocalWindowInsets
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.history.TransactionItem
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.charts.SpendsChart
import com.serranoie.app.minus.presentation.ui.theme.component.date.HistoryDateDivider
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

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
	val categorySpends: List<Transaction> = emptyList()
)

data class CategoryAnalyticsActions(
	val onCreateNewPeriod: () -> Unit = {},
	val onClose: () -> Unit = {},
)

@Composable
fun CategoryAnalytics(
	modifier: Modifier = Modifier,
	state: CategoryAnalyticsState = CategoryAnalyticsState(),
	actions: CategoryAnalyticsActions = CategoryAnalyticsActions()
) {
	val scrollState = rememberScrollState()

	val navigationBarHeight =
		LocalWindowInsets.current.calculateBottomPadding().coerceAtLeast(16.dp)

	// Group category transactions by date for the history list
	val groupedCategoryTransactions = remember(state.categorySpends) {
		state.categorySpends
			.sortedByDescending { it.date }
			.groupBy { it.date?.toLocalDate() }
			.toSortedMap(compareByDescending { it })
	}

	Surface(
		modifier = modifier
	) {
		Column(
			Modifier
				.verticalScroll(scrollState)
				.padding(bottom = navigationBarHeight)
				.padding(horizontal = 16.dp)
		) {
			// Title and Category Name
			Column(
				modifier = Modifier.fillMaxWidth(),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Text(
					text = "Analisis",
					style = MaterialTheme.typography.headlineMedium,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface
				)
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = state.categoryName,
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
				)
			}

			Spacer(modifier = Modifier.height(24.dp))

			// Spends Chart for this category - requires at least 2 transactions
			if (state.categorySpends.size >= 2) {
				Surface(
					modifier = Modifier
						.fillMaxWidth()
						.height(200.dp),
					color = MaterialTheme.colorScheme.surface
				) {
					SpendsChart(
						spends = state.categorySpends,
						modifier = Modifier.fillMaxWidth()
					)
				}

				Spacer(modifier = Modifier.height(24.dp))
			} else if (state.categorySpends.size == 1) {
				// Show simple single transaction visualization
				val transaction = state.categorySpends.first()
				val currencyFormat = com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat("USD")

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
							text = "Gasto unico",
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
						Spacer(modifier = Modifier.height(8.dp))
						Text(
							text = currencyFormat.format(transaction.amount),
							style = MaterialTheme.typography.headlineMedium,
							fontWeight = FontWeight.Bold,
							color = MaterialTheme.colorScheme.onSurface
						)
					}
				}

				Spacer(modifier = Modifier.height(24.dp))
			}

			if (state.categorySpends.isNotEmpty()) {
				Text(
					text = "Historial de gastos",
					style = MaterialTheme.typography.titleMedium,
					fontWeight = FontWeight.Medium,
					modifier = Modifier.padding(vertical = 8.dp)
				)

				Spacer(modifier = Modifier.height(8.dp))

				// History of expenses for this category only
				val currencyFormat = com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat("USD")

				groupedCategoryTransactions.forEach { (date, transactions) ->
					HistoryDateDivider(date = date)

					transactions.forEach { transaction ->
						TransactionItem(
							transaction = transaction,
							currencyFormat = currencyFormat
						)
					}

					// Day total for this category
					val dayTotal = transactions.sumOf { it.amount }
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 16.dp, vertical = 4.dp)
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

				Spacer(modifier = Modifier.height(32.dp))
			}

			if (state.categorySpends.isEmpty()) {
				Box(
					modifier = Modifier
						.fillMaxWidth()
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
						date = java.time.LocalDateTime.now().minusDays(2)
					),
					Transaction(
						amount = BigDecimal("85.50"),
						comment = "Comida",
						date = java.time.LocalDateTime.now().minusDays(1)
					),
					Transaction(
						amount = BigDecimal("120.00"),
						comment = "Comida",
						date = java.time.LocalDateTime.now()
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
						date = java.time.LocalDateTime.now()
					)
				)
			)
		)
	}
}