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
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetDisplay
import com.serranoie.app.minus.presentation.ui.theme.component.date.DayTotalItem
import com.serranoie.app.minus.presentation.ui.theme.component.date.HistoryDateDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.ExpenseItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.NoTransactionsView
import com.serranoie.app.minus.presentation.ui.theme.component.expense.RecurrentPaymentsDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.ticket.RecurrentTicketCard
import com.serranoie.app.minus.presentation.util.prettyDate
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
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

	@Test
	fun historyRecurringPaymentsSections() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				Surface(modifier = Modifier.fillMaxSize()) {
					HistoryRecurringContent()
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
				BudgetDisplay(
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

	@Composable
	private fun HistoryRecurringContent() {
		val today = LocalDate.of(2026, 1, 15)
		val currencyFormat = symbolOnlyCurrencyFormat("USD")
		val currentRecurring = listOf(
			UpcomingRecurrentItem(
				transaction = Transaction(
					id = 301L,
					amount = BigDecimal("16.99"),
					comment = "Video streaming",
					date = today.minusMonths(1).atTime(9, 0),
					isRecurrent = true,
					recurrentFrequency = RecurrentFrequency.MONTHLY,
					subscriptionDay = 18,
				),
				nextChargeDate = today.plusDays(3),
				isInCurrentPeriod = true,
			),
			UpcomingRecurrentItem(
				transaction = Transaction(
					id = 302L,
					amount = BigDecimal("9.99"),
					comment = "Weekly app",
					date = today.minusWeeks(1).atTime(8, 30),
					isRecurrent = true,
					recurrentFrequency = RecurrentFrequency.WEEKLY,
				),
				nextChargeDate = today.plusDays(5),
				isInCurrentPeriod = true,
			),
		)
		val nextPeriodRecurring = listOf(
			UpcomingRecurrentItem(
				transaction = Transaction(
					id = 401L,
					amount = BigDecimal("49.99"),
					comment = "Gym membership",
					date = today.minusMonths(1).atTime(8, 0),
					isRecurrent = true,
					recurrentFrequency = RecurrentFrequency.MONTHLY,
					subscriptionDay = 2,
				),
				nextChargeDate = today.plusDays(20),
				isInCurrentPeriod = false,
			),
			UpcomingRecurrentItem(
				transaction = Transaction(
					id = 402L,
					amount = BigDecimal("7.99"),
					comment = "Cloud storage",
					date = today.minusWeeks(2).atTime(8, 0),
					isRecurrent = true,
					recurrentFrequency = RecurrentFrequency.BIWEEKLY,
				),
				nextChargeDate = today.plusDays(23),
				isInCurrentPeriod = false,
			),
		)

		LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(bottom = 24.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp),
		) {
			item {
				RecurrentPaymentsDivider(
					title = "Recurrent payments this period",
					isExpanded = true,
					onToggleClick = {},
					itemCount = currentRecurring.size,
					modifier = Modifier.fillMaxWidth(),
				)
			}
			item {
				RecurringTicketsRow(currentRecurring, currencyFormat)
			}
			item {
				WavyDivider(
					text = "Recurrent payments next period",
					horizontalPadding = 0.dp,
					amplitude = 4f,
					wavelength = 45f,
				)
			}
			item {
				RecurringTicketsRow(nextPeriodRecurring, currencyFormat)
			}
		}
	}

	@Composable
	private fun RecurringTicketsRow(
		items: List<UpcomingRecurrentItem>,
		currencyFormat: java.text.NumberFormat,
	) {
		LazyRow(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			contentPadding = PaddingValues(horizontal = 16.dp),
		) {
			itemsIndexed(items, key = { _, item -> item.transaction.id }) { _, item ->
				RecurrentTicketCard(
					title = item.transaction.comment,
					amountFormatted = currencyFormat.format(item.transaction.amount),
					nextChargeDate = prettyDate(
						item.nextChargeDate.atStartOfDay(),
						showTime = false,
						forceShowDate = false,
					),
					frequencyLabel = item.transaction.recurrentFrequency?.name?.lowercase()
						?.replaceFirstChar { it.uppercase() },
					modifier = Modifier.fillParentMaxWidth(0.45f),
				)
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
