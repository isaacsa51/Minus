package com.serranoie.app.minus.presentation.analytics

import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.serranoie.app.minus.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetDisplay
import com.serranoie.app.minus.presentation.history.History
import com.serranoie.app.minus.presentation.ui.theme.component.budget.AverageSpendCard
import com.serranoie.app.minus.presentation.ui.theme.component.date.CalendarHeatmap
import com.serranoie.app.minus.presentation.ui.theme.component.charts.CategoriesChartCard
import com.serranoie.app.minus.presentation.ui.theme.component.FinishedPeriodHeader
import com.serranoie.app.minus.presentation.ui.theme.component.MiddlePeriodHeader
import com.serranoie.app.minus.presentation.ui.theme.component.budget.MinMaxSpentCard
import com.serranoie.app.minus.presentation.ui.theme.component.budget.SpendBudgetCard
import com.serranoie.app.minus.presentation.ui.theme.component.charts.SpendsChart
import com.serranoie.app.minus.presentation.ui.theme.component.budget.SpendsCountCard
import java.math.BigDecimal
import java.util.Date

const val ANALYTICS_SHEET = "finishPeriod"

data class AnalyticsState(
	val periodFinished: Boolean = false,
	val transactions: List<Transaction> = emptyList(),
	val spends: List<Transaction> = emptyList(),
	val wholeBudget: BigDecimal = BigDecimal.ZERO,
	val currencyCode: String = "USD",
	val finishPeriodActualDate: Date? = null,
	val startPeriodDate: Date = Date(),
	val finishPeriodDate: Date? = null,
	val extraAffordableDaysFromRemaining: Int = 0,
	val isLoading: Boolean = false,
)

data class AnalyticsActions(
	val onCreateNewPeriod: () -> Unit = {},
	val onClose: () -> Unit = {},
	val onExportCSV: () -> Unit = {},
)

data class Size(val width: Dp, val height: Dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Analytics(
	state: AnalyticsState = AnalyticsState(),
	actions: AnalyticsActions = AnalyticsActions(),
	activityResultRegistryOwner: ActivityResultRegistryOwner? = null,
) {
	val scrollState = rememberScrollState()
	var showHistorySheet by remember { mutableStateOf(false) }
	val sheetState = rememberModalBottomSheetState()

	var selectedCategory by remember { mutableStateOf<CategoryAnalyticsState?>(null) }

	val navigationBarHeight =
		LocalWindowInsets.current.calculateBottomPadding().coerceAtLeast(16.dp)
	val statusBarHeight = LocalWindowInsets.current.calculateTopPadding()

	Surface(modifier = Modifier.fillMaxSize()) {
		Box(modifier = Modifier.fillMaxSize()) {
			Column(
				Modifier
					.fillMaxSize()
					.verticalScroll(scrollState)
					.padding(top = statusBarHeight)
			) {
				if (!state.periodFinished) {
					MiddlePeriodHeader(
						onClose = actions.onClose,
					)
				} else {
					FinishedPeriodHeader(
						scrollState = scrollState,
						hasSpends = state.spends.isNotEmpty(),
					)
				}
				Spacer(modifier = Modifier.height(16.dp))
				BudgetDisplay(
						budget = state.wholeBudget,
						currencyCode = state.currencyCode,
						startDate = state.startPeriodDate,
						finishDate = state.finishPeriodDate,
						actualFinishDate = state.finishPeriodActualDate,
						extraDaysFromRemaining = state.extraAffordableDaysFromRemaining,
						modifier = Modifier.padding(horizontal = 16.dp),
						budgetState = null,
						budgetSettings = null,
					)
Spacer(modifier = Modifier.height(16.dp))
					Row(
						Modifier
							.fillMaxWidth()
							.padding(horizontal = 16.dp)
					) {
						if (state.finishPeriodDate != null && state.transactions.isNotEmpty()) {
							CalendarHeatmap(
								transactions = state.transactions,
								budget = state.wholeBudget,
								startDate = state.startPeriodDate,
								finishDate = state.finishPeriodDate,
								modifier = Modifier
.weight(1f)
									.wrapContentHeight(),
							)
						}
					}
					Spacer(modifier = Modifier.height(16.dp))
					SpendsChart(
						spends = state.spends,
						modifier = Modifier
							.fillMaxWidth()
							.heightIn(0.dp, 400.dp)
							.padding(horizontal = 16.dp),
					)
					Spacer(modifier = Modifier.height(16.dp))
					Row(
						Modifier
							.fillMaxWidth()
							.height(IntrinsicSize.Min)
							.padding(horizontal = 16.dp)
					) {
						MinMaxSpentCard(
							isMin = true,
							spends = state.spends,
							currency = "MXN",
							modifier = Modifier
								.weight(1f)
								.fillMaxHeight(),
						)
						Spacer(modifier = Modifier.width(16.dp))
						MinMaxSpentCard(
							isMin = false,
							spends = state.spends,
							currency = "MXN",
							modifier = Modifier
								.weight(1f)
								.fillMaxHeight(),
						)
					}
					Spacer(modifier = Modifier.height(16.dp))
					Row(
						Modifier
							.fillMaxWidth()
							.height(IntrinsicSize.Min)
							.padding(horizontal = 16.dp)
					) {
						SpendsCountCard(
							count = state.spends.size,
							onClick = { showHistorySheet = true },
							modifier = Modifier
								.weight(1f)
								.fillMaxHeight(),
						)
						Spacer(modifier = Modifier.width(16.dp))
						AverageSpendCard(
							spends = state.spends,
							currency = "MXN",
							modifier = Modifier
								.weight(1f)
								.fillMaxHeight(),
						)
					}
					Spacer(modifier = Modifier.height(16.dp))
					SpendBudgetCard(
						budget = state.wholeBudget,
						spend = state.spends.sumOf { it.amount },
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 16.dp),
					)
					Spacer(modifier = Modifier.height(16.dp))
					CategoriesChartCard(
					spends = state.spends,
					currency = "MXN",
					modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
					onCategoryClick = { categoryName, categorySpends ->
						selectedCategory = CategoryAnalyticsState(
							periodFinished = state.periodFinished,
							transactions = state.transactions,
							spends = state.spends,
							wholeBudget = state.wholeBudget,
							finishPeriodActualDate = state.finishPeriodActualDate,
							startPeriodDate = state.startPeriodDate,
							finishPeriodDate = state.finishPeriodDate,
							isLoading = state.isLoading,
							categoryName = categoryName,
							categorySpends = categorySpends
						)
					}
				)
				// Spacer to push content up, making room for the fixed button at bottom
				Spacer(modifier = Modifier.height(80.dp + navigationBarHeight))
			}

			// Button fixed at the bottom
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.align(androidx.compose.ui.Alignment.BottomCenter)
					.zIndex(1f)
					.padding(bottom = navigationBarHeight, start = 16.dp, end = 16.dp)
			) {
				Button(
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(60.dp),
					onClick = {
						actions.onCreateNewPeriod()
					},
				) {
					Text(
						text = "Nuevo periodo",
						style = MaterialTheme.typography.bodyLarge,
					)
				}
			}
		}
	}

	// Category Analytics Bottom Sheet
	if (selectedCategory != null) {
		ModalBottomSheet(
			onDismissRequest = { selectedCategory = null },
			sheetState = sheetState,
		) {
			CategoryAnalytics(
				state = selectedCategory!!,
				actions = CategoryAnalyticsActions(
					onClose = { selectedCategory = null },
					onCreateNewPeriod = actions.onCreateNewPeriod
				)
			)
		}
	}

	// History Bottom Sheet
	if (showHistorySheet) {
		ModalBottomSheet(
			onDismissRequest = { showHistorySheet = false },
			sheetState = sheetState,
		) {
			History(
				readOnly = true,
				onClose = { showHistorySheet = false }
			)
		}
	}
}

@Preview(name = "Analytics - Period not finished")
@Composable
private fun PreviewAnalyticsNotFinished() {
	MinusTheme {
		Surface {
			Analytics(
				state = AnalyticsState(
					periodFinished = false,
					spends = listOf(
						Transaction(
							amount = BigDecimal(150),
							date = java.time.LocalDateTime.now().minusDays(2),
							comment = "Comida",
						),
						Transaction(
							amount = BigDecimal(80),
							date = java.time.LocalDateTime.now().minusDays(1),
							comment = "Transporte",
						),
						Transaction(
							amount = BigDecimal(200),
							date = java.time.LocalDateTime.now(),
							comment = "Comida",
						),
						Transaction(
							amount = BigDecimal(50),
							date = java.time.LocalDateTime.now(),
							comment = "Entretenimiento",
						),
					),
					wholeBudget = BigDecimal(500),
					startPeriodDate = Date(),
					finishPeriodDate = java.util.Calendar.getInstance().apply {
						add(java.util.Calendar.DAY_OF_MONTH, 15)
					}.time,
				),
			)
		}
	}
}

@Preview(name = "Analytics - Period finished")
@Composable
private fun PreviewAnalyticsFinished() {
	MinusTheme {
		Surface {
			Analytics(
				state = AnalyticsState(
					periodFinished = true,
					spends = listOf(
						Transaction(
							amount = BigDecimal(150),
							date = java.time.LocalDateTime.now().minusDays(14),
							comment = "Comida",
						),
						Transaction(
							amount = BigDecimal(80),
							date = java.time.LocalDateTime.now().minusDays(10),
							comment = "Transporte",
						),
						Transaction(
							amount = BigDecimal(200),
							date = java.time.LocalDateTime.now().minusDays(7),
							comment = "Comida",
						),
						Transaction(
							amount = BigDecimal(50),
							date = java.time.LocalDateTime.now().minusDays(5),
							comment = "Entretenimiento",
						),
						Transaction(
							amount = BigDecimal(120),
							date = java.time.LocalDateTime.now().minusDays(2),
							comment = "Salud",
						),
						Transaction(
							amount = BigDecimal(300),
							date = java.time.LocalDateTime.now().minusDays(1),
							comment = "",
						),
					),
					wholeBudget = BigDecimal(500),
					startPeriodDate = java.util.Calendar.getInstance().apply {
						add(java.util.Calendar.DAY_OF_MONTH, -15)
					}.time,
					finishPeriodDate = Date(),
					finishPeriodActualDate = Date(),
				),
			)
		}
	}
}
