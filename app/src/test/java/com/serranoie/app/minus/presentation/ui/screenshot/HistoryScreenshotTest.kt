package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.WavyDivider
import com.serranoie.app.minus.presentation.ui.theme.component.budget.TotalBudgetCard
import com.serranoie.app.minus.presentation.ui.theme.component.date.DayTotalItem
import com.serranoie.app.minus.presentation.ui.theme.component.date.HistoryDateDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.ExpenseItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.NoTransactionsView
import com.serranoie.app.minus.presentation.ui.theme.component.expense.RecurrentPaymentsDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.util.font.format.prettyDate
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class HistoryScreenshotTest {
	@get:Rule
	val paparazzi = Paparazzi(
		deviceConfig = DeviceConfig.PIXEL_5,
		renderingMode = SessionParams.RenderingMode.NORMAL,
        maxPercentDifference = 10.0,
	)

	@Test
	fun historyEmptyState() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				Surface(modifier = Modifier.fillMaxSize()) {
					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(32.dp),
						contentAlignment = Alignment.Center,
					) {
						NoTransactionsView()
					}
				}
			}
		}
	}

	@Test
	fun historyCurrentAndPastExpenses() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				Surface(modifier = Modifier.fillMaxSize()) {
					HistoryMixedExpensesContent()
				}
			}
		}
	}

	@Composable
	private fun HistoryMixedExpensesContent() {
		val today = LocalDate.of(2026, 1, 15)
		val settings = sampleBudgetSettings(today)
		val state = sampleBudgetState()
		val currencyFormat = symbolOnlyCurrencyFormat("USD")
		val todaysTransactions = listOf(
			Transaction(
				id = 101L,
				amount = BigDecimal("18.75"),
				comment = "Lunch",
				date = today.atTime(12, 30),
				periodId = 7L,
			),
			Transaction(
				id = 102L,
				amount = BigDecimal("6.25"),
				comment = "Coffee",
				date = today.atTime(16, 10),
				periodId = 7L,
			),
		)
		val olderTransactions = listOf(
			Transaction(
				id = 201L,
				amount = BigDecimal("42.30"),
				comment = "Groceries",
				date = today.minusDays(2).atTime(18, 20),
				periodId = 7L,
			),
			Transaction(
				id = 202L,
				amount = BigDecimal("11.50"),
				comment = "Bus fare",
				date = today.minusDays(4).atTime(8, 45),
				periodId = 7L,
			),
		)

		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(bottom = 24.dp),
			verticalArrangement = Arrangement.spacedBy(2.dp),
		) {
			item {
				TotalBudgetCard(
					budget = state.totalBudget,
					budgetState = state,
					budgetSettings = settings,
					currencyCode = "USD",
					bigVariant = true,
					modifier = Modifier.fillMaxWidth(),
					startDate = fixedDate(2026, 1, 1),
					finishDate = fixedDate(2026, 1, 30),
				)
			}
			item {
				HistoryDateDivider(
					date = today,
					isExpanded = true,
					onToggleClick = {},
					totalAmount = todaysTransactions.sumOf { it.amount },
					currencyCode = "$",
				)
			}
			itemsIndexed(todaysTransactions, key = { _, tx -> tx.id }) { index, tx ->
                ExpenseItem(
                    transaction = tx,
                    currencyFormat = currencyFormat,
                    position = paddedPosition(index, todaysTransactions.lastIndex, todaysTransactions.size),
                )
			}
			item {
				DayTotalItem(
					total = todaysTransactions.sumOf { it.amount },
					currencyFormat = currencyFormat,
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp, vertical = 8.dp),
				)
			}
			olderTransactions.groupBy { it.date?.toLocalDate() }
				.toSortedMap(compareByDescending { it })
				.forEach { (date, transactions) ->
					item("history-date-$date") {
						HistoryDateDivider(
							date = date,
							isExpanded = true,
							onToggleClick = {},
							totalAmount = transactions.sumOf { it.amount },
							currencyCode = "$",
						)
					}
					itemsIndexed(transactions, key = { _, tx -> tx.id }) { index, tx ->
                        ExpenseItem(
                            transaction = tx,
                            currencyFormat = currencyFormat,
                            position = paddedPosition(index, transactions.lastIndex, transactions.size),
                        )
					}
				}
		}
	}

	private fun sampleBudgetSettings(today: LocalDate): BudgetSettings = BudgetSettings(
		totalBudget = BigDecimal("900.00"),
		period = BudgetPeriod.MONTHLY,
		startDate = today.withDayOfMonth(1),
		endDate = today.withDayOfMonth(30),
		currencyCode = "USD",
		daysInPeriod = 30,
	)

	private fun sampleBudgetState(): BudgetState = BudgetState(
		remainingToday = BigDecimal("31.25"),
		totalSpentToday = BigDecimal("25.00"),
		dailyBudget = BigDecimal("50.00"),
		daysRemaining = 12,
		progress = 0.58f,
		isOverBudget = false,
		totalBudget = BigDecimal("900.00"),
		totalSpentInPeriod = BigDecimal("522.45"),
	)

	private fun fixedDate(year: Int, month: Int, day: Int): Date = Date.from(
		LocalDate.of(year, month, day)
			.atStartOfDay()
			.toInstant(ZoneOffset.UTC)
	)

	private fun paddedPosition(
		index: Int,
		lastIndex: Int,
		size: Int,
	): PaddedListItemPosition = when {
		size == 1 -> PaddedListItemPosition.Single
		index == 0 -> PaddedListItemPosition.First
		index == lastIndex -> PaddedListItemPosition.Last
		else -> PaddedListItemPosition.Middle
	}
}
