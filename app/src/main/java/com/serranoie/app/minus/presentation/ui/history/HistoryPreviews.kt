package com.serranoie.app.minus.presentation.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
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
import com.serranoie.app.minus.presentation.ui.theme.component.expense.RecurrentPaymentsDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableExpenseItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.ticket.RecurrentTicketCard
import com.serranoie.app.minus.presentation.util.prettyDate
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@Preview(showBackground = true)
@Composable
private fun TransactionDetailTicketCardPreview() {
	MinusTheme {
		TransactionDetailTicketCard(
			transaction = Transaction(
				id = 2048L,
				amount = BigDecimal("129.99"),
				comment = "Internet hogar",
				date = LocalDateTime.now(),
				isRecurrent = true,
				recurrentFrequency = RecurrentFrequency.MONTHLY,
				subscriptionDay = 15,
				recurrentEndDate = LocalDateTime.now().plusMonths(6),
			),
			totalAmountText = "$129.99",
			details = listOf(
				"Descripción" to "Internet hogar",
				"Fecha" to "Hoy, 8:30 PM",
				"Frecuencia" to "Mensual",
				"Día de cobro" to "Día 15",
				"Fin recurrencia" to "En 6 meses",
			),
			onMarkAsPaid = {},
			onEdit = {},
			onDelete = {},
			readOnly = false,
			modifier = Modifier.padding(24.dp),
		)
	}
}

@Preview
@Composable
private fun HistoryPreview() {
	MinusTheme {
		Column {
			HistoryDateDivider(
				date = LocalDate.now(),
				isExpanded = true,
				onToggleClick = {},
				totalAmount = BigDecimal("100.00"),
				currencyCode = "$",
			)

			SwipeableExpenseItem(
				transaction = Transaction(
					id = 1L,
					amount = BigDecimal("10.00"),
					comment = "Category",
					date = LocalDateTime.now(),
					isDeleted = false,
				),
				currencyFormat = NumberFormat.getCurrencyInstance(),
				position = PaddedListItemPosition.First,
				onDelete = {},
				onEdit = {},
				readOnly = false,
			)
			ExpenseItem(
				transaction = Transaction(
					id = 2L,
					amount = BigDecimal("25.00"),
					comment = "Category 2",
					date = LocalDateTime.now(),
					isDeleted = false,
				),
				currencyFormat = NumberFormat.getCurrencyInstance(),
				position = PaddedListItemPosition.Middle,
			)
			ExpenseItem(
				transaction = Transaction(
					id = 3L,
					amount = BigDecimal("199.00"),
					comment = "Streaming",
					date = LocalDateTime.now().minusHours(6),
					isDeleted = false,
					isRecurrent = true,
				),
				currencyFormat = NumberFormat.getCurrencyInstance(),
				position = PaddedListItemPosition.Last,
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun HistoryFullContentPreview() {
	MinusTheme {
		Surface(modifier = Modifier.padding(16.dp)) {
			val budgetSettings = BudgetSettings(
				totalBudget = BigDecimal("800.00"),
				period = BudgetPeriod.MONTHLY,
				startDate = LocalDate.now().minusDays(12),
				endDate = LocalDate.now().plusDays(18),
				currencyCode = "EUR",
			)
			val budgetState = BudgetState(
				remainingToday = BigDecimal("250.00"),
				totalSpentToday = BigDecimal("45.00"),
				dailyBudget = BigDecimal("26.66"),
				daysRemaining = 18,
				progress = 0.68f,
				isOverBudget = false,
				totalBudget = BigDecimal("800.00"),
				totalSpentInPeriod = BigDecimal("550.00"),
			)

			val currencyFormat = symbolOnlyCurrencyFormat("EUR")
			val inPeriodRecurring = listOf(
				UpcomingRecurrentItem(
					transaction = Transaction(
						id = 9001L,
						amount = BigDecimal("8.99"),
						comment = "Subscripción mensual sin nombre",
						date = LocalDateTime.now().minusDays(10),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.MONTHLY,
						subscriptionDay = 12,
					),
					nextChargeDate = LocalDate.now().plusDays(2),
					isInCurrentPeriod = true,
				),
				UpcomingRecurrentItem(
					transaction = Transaction(
						id = 9002L,
						amount = BigDecimal("5.00"),
						comment = "Subscripción semanal sin nombre",
						date = LocalDateTime.now().minusDays(6),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.WEEKLY,
					),
					nextChargeDate = LocalDate.now().plusDays(4),
					isInCurrentPeriod = true,
				),
			)

			val outOfPeriodRecurring = listOf(
				UpcomingRecurrentItem(
					transaction = Transaction(
						id = 9010L,
						amount = BigDecimal("49.99"),
						comment = "Gym Premium",
						date = LocalDateTime.now().minusDays(30),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.MONTHLY,
						subscriptionDay = 2,
					),
					nextChargeDate = LocalDate.now().plusDays(20),
					isInCurrentPeriod = false,
				),
				UpcomingRecurrentItem(
					transaction = Transaction(
						id = 9011L,
						amount = BigDecimal("14.99"),
						comment = "Music Pro",
						date = LocalDateTime.now().minusDays(25),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.BIWEEKLY,
					),
					nextChargeDate = LocalDate.now().plusDays(28),
					isInCurrentPeriod = false,
				),
			)

			val currentTx = listOf(
				Transaction(
					id = 1001L,
					amount = BigDecimal("23.40"),
					comment = "Supermercado",
					date = LocalDateTime.now().minusHours(2),
				),
				Transaction(
					id = 1002L,
					amount = BigDecimal("12.00"),
					comment = "Café",
					date = LocalDateTime.now().minusHours(5),
				),
			)

			val pastTx = listOf(
				Transaction(
					id = 2001L,
					amount = BigDecimal("70.00"),
					comment = "Combustible",
					date = LocalDateTime.now().minusDays(35),
				),
				Transaction(
					id = 2002L,
					amount = BigDecimal("31.50"),
					comment = "Comida",
					date = LocalDateTime.now().minusDays(36),
				),
			)

			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(bottom = 24.dp),
			) {
				item {
					BudgetDisplay(
						budget = budgetState.totalBudget,
						budgetState = budgetState,
						budgetSettings = budgetSettings,
						currencyCode = "EUR",
						bigVariant = true,
						modifier = Modifier.fillMaxWidth(),
						startDate = Date.from(
							budgetSettings.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
						),
						finishDate = Date.from(
							budgetSettings.getPeriodEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
						),
					)
				}

				item {
					LazyRow(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						contentPadding = PaddingValues(horizontal = 16.dp),
					) {
						itemsIndexed(inPeriodRecurring, key = { _, item -> item.transaction.id }) { _, item ->
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

				item {
					WavyDivider(
						text = "Mostrar subscripciones fuera del periodo",
						horizontalPadding = 0.dp,
						amplitude = 4f,
						wavelength = 45f,
					)
				}
				item {
					LazyRow(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						contentPadding = PaddingValues(horizontal = 16.dp),
					) {
						itemsIndexed(outOfPeriodRecurring, key = { _, item -> item.transaction.id }) { _, item ->
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

				item {
					HistoryDateDivider(
						date = LocalDate.now(),
						isExpanded = true,
						onToggleClick = {},
						totalAmount = currentTx.sumOf { it.amount }.toPlainString() as BigDecimal?,
						currencyCode = "€",
					)
				}
				itemsIndexed(currentTx, key = { _, tx -> tx.id }) { index, tx ->
					ExpenseItem(
						transaction = tx,
						currencyFormat = currencyFormat,
						position = when {
							currentTx.size == 1 -> PaddedListItemPosition.Single
							index == 0 -> PaddedListItemPosition.First
							index == currentTx.lastIndex -> PaddedListItemPosition.Last
							else -> PaddedListItemPosition.Middle
						},
					)
				}

				item {
					WavyDivider(
						text = "Mostrar gastos del periodo pasado",
						horizontalPadding = 0.dp,
						amplitude = 4f,
						wavelength = 45f,
					)
				}
				item {
					HistoryDateDivider(
						date = LocalDate.now().minusDays(35),
						isExpanded = true,
						onToggleClick = {},
						totalAmount = pastTx.sumOf { it.amount }.toPlainString() as BigDecimal?,
						currencyCode = "€",
					)
				}
				itemsIndexed(pastTx, key = { _, tx -> tx.id }) { index, tx ->
					ExpenseItem(
						transaction = tx,
						currencyFormat = currencyFormat,
						position = when {
							pastTx.size == 1 -> PaddedListItemPosition.Single
							index == 0 -> PaddedListItemPosition.First
							index == pastTx.lastIndex -> PaddedListItemPosition.Last
							else -> PaddedListItemPosition.Middle
						},
					)
				}
			}
		}
	}
}

@Preview(showBackground = true, name = "History mixed expense sections")
@Composable
private fun HistoryMixedExpenseSectionsPreview() {
	MinusTheme {
		Surface(modifier = Modifier.padding(16.dp)) {
			val today = LocalDate.now()
			val currencyFormat = symbolOnlyCurrencyFormat("EUR")
			val currentPeriodStart = today.minusDays(6)
			val currentPeriodEnd = today.plusDays(7)
			val nextPeriodStart = currentPeriodEnd.plusDays(1)

			val budgetSettings = BudgetSettings(
				totalBudget = BigDecimal("900.00"),
				period = BudgetPeriod.BIWEEKLY,
				startDate = currentPeriodStart,
				endDate = currentPeriodEnd,
				currencyCode = "EUR",
			)
			val budgetState = BudgetState(
				remainingToday = BigDecimal("48.25"),
				totalSpentToday = BigDecimal("34.99"),
				dailyBudget = BigDecimal("64.28"),
				daysRemaining = 7,
				progress = 0.42f,
				isOverBudget = false,
				totalBudget = BigDecimal("900.00"),
				totalSpentInPeriod = BigDecimal("378.15"),
			)

			val todaysTransactions = listOf(
				Transaction(
					id = 3001L,
					amount = BigDecimal("14.99"),
					comment = "Music recurrent",
					date = today.atTime(9, 0),
					isRecurrent = true,
					recurrentFrequency = RecurrentFrequency.MONTHLY,
					subscriptionDay = today.dayOfMonth,
					recurrentEndDate = today.plusMonths(6).atTime(9, 0),
				),
				Transaction(
					id = 3002L,
					amount = BigDecimal("20.00"),
					comment = "Lunch",
					date = today.atTime(13, 15),
				),
			)

			val activePeriodTransactions = listOf(
				Transaction(
					id = 3010L,
					amount = BigDecimal("42.30"),
					comment = "Groceries",
					date = today.minusDays(2).atTime(18, 20),
				),
				Transaction(
					id = 3011L,
					amount = BigDecimal("11.50"),
					comment = "Coffee and snack",
					date = today.minusDays(4).atTime(10, 45),
				),
			)

			val currentPeriodUpcomingRecurring = listOf(
				UpcomingRecurrentItem(
					transaction = Transaction(
						id = 3040L,
						amount = BigDecimal("16.99"),
						comment = "Video streaming",
						date = today.minusMonths(1).atTime(9, 0),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.MONTHLY,
						subscriptionDay = today.plusDays(3).dayOfMonth,
						recurrentEndDate = today.plusMonths(5).atTime(9, 0),
					),
					nextChargeDate = today.plusDays(3),
					isInCurrentPeriod = true,
				),
				UpcomingRecurrentItem(
					transaction = Transaction(
						id = 3041L,
						amount = BigDecimal("9.99"),
						comment = "Weekly app",
						date = today.minusWeeks(1).atTime(8, 30),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.WEEKLY,
						recurrentEndDate = today.plusMonths(2).atTime(8, 30),
					),
					nextChargeDate = today.plusDays(5),
					isInCurrentPeriod = true,
				),
			)

			val nextPeriodRecurring = listOf(
				UpcomingRecurrentItem(
					transaction = Transaction(
						id = 3020L,
						amount = BigDecimal("49.99"),
						comment = "Gym membership",
						date = today.minusMonths(1).atTime(8, 0),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.MONTHLY,
						subscriptionDay = nextPeriodStart.dayOfMonth,
					),
					nextChargeDate = nextPeriodStart,
					isInCurrentPeriod = false,
				),
				UpcomingRecurrentItem(
					transaction = Transaction(
						id = 3021L,
						amount = BigDecimal("7.99"),
						comment = "Cloud storage",
						date = today.minusWeeks(2).atTime(8, 0),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.BIWEEKLY,
					),
					nextChargeDate = nextPeriodStart.plusDays(3),
					isInCurrentPeriod = false,
				),
			)

			val pastTransactions = listOf(
				Transaction(
					id = 3030L,
					amount = BigDecimal("65.00"),
					comment = "Fuel",
					date = currentPeriodStart.minusDays(3).atTime(19, 10),
				),
				Transaction(
					id = 3031L,
					amount = BigDecimal("28.40"),
					comment = "Dinner",
					date = currentPeriodStart.minusDays(5).atTime(21, 0),
				),
			)

			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(bottom = 24.dp),
				verticalArrangement = Arrangement.spacedBy(2.dp),
			) {
				item {
					BudgetDisplay(
						budget = budgetState.totalBudget,
						budgetState = budgetState,
						budgetSettings = budgetSettings,
						currencyCode = "EUR",
						bigVariant = true,
						modifier = Modifier.fillMaxWidth(),
						startDate = Date.from(
							currentPeriodStart.atStartOfDay(ZoneId.systemDefault()).toInstant()
						),
						finishDate = Date.from(
							currentPeriodEnd.atStartOfDay(ZoneId.systemDefault()).toInstant()
						),
					)
				}

				item {
					RecurrentPaymentsDivider(
						title = stringResource(R.string.recurrent_payments_divider_title_current_period),
						isExpanded = true,
						onToggleClick = {},
						itemCount = currentPeriodUpcomingRecurring.size,
						modifier = Modifier.fillMaxWidth(),
					)
				}
				item {
					LazyRow(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						contentPadding = PaddingValues(horizontal = 16.dp),
					) {
						itemsIndexed(currentPeriodUpcomingRecurring, key = { _, item -> item.transaction.id }) { _, item ->
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

				item {
					HistoryDateDivider(
						date = today,
						isExpanded = true,
						onToggleClick = {},
						totalAmount = todaysTransactions.sumOf { it.amount },
						currencyCode = "€",
					)
				}
				itemsIndexed(todaysTransactions, key = { _, tx -> tx.id }) { index, tx ->
					ExpenseItem(
						transaction = tx,
						currencyFormat = currencyFormat,
						position = when {
							todaysTransactions.size == 1 -> PaddedListItemPosition.Single
							index == 0 -> PaddedListItemPosition.First
							index == todaysTransactions.lastIndex -> PaddedListItemPosition.Last
							else -> PaddedListItemPosition.Middle
						},
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

				activePeriodTransactions.groupBy { it.date?.toLocalDate() }
					.toSortedMap(compareByDescending { it }).forEach { (date, transactions) ->
						item("preview-active-date-$date") {
							HistoryDateDivider(
								date = date,
								isExpanded = true,
								onToggleClick = {},
								totalAmount = transactions.sumOf { it.amount },
								currencyCode = "€",
							)
						}
						itemsIndexed(transactions, key = { _, tx -> tx.id }) { index, tx ->
							ExpenseItem(
								transaction = tx,
								currencyFormat = currencyFormat,
								position = when {
									transactions.size == 1 -> PaddedListItemPosition.Single
									index == 0 -> PaddedListItemPosition.First
									index == transactions.lastIndex -> PaddedListItemPosition.Last
									else -> PaddedListItemPosition.Middle
								},
							)
						}
					}

				item {
					WavyDivider(
						text = "Pagos recurrentes del próximo periodo",
						horizontalPadding = 0.dp,
						amplitude = 4f,
						wavelength = 45f,
					)
				}
				item {
					LazyRow(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						contentPadding = PaddingValues(horizontal = 16.dp),
					) {
						itemsIndexed(nextPeriodRecurring, key = { _, item -> item.transaction.id }) { _, item ->
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

				item {
					WavyDivider(
						text = "Gastos del periodo pasado",
						horizontalPadding = 0.dp,
						amplitude = 4f,
						wavelength = 45f,
					)
				}
				item {
					HistoryDateDivider(
						date = currentPeriodStart.minusDays(3),
						isExpanded = true,
						onToggleClick = {},
						totalAmount = pastTransactions.sumOf { it.amount },
						currencyCode = "€",
					)
				}
				itemsIndexed(pastTransactions, key = { _, tx -> tx.id }) { index, tx ->
					ExpenseItem(
						transaction = tx,
						currencyFormat = currencyFormat,
						position = when {
							pastTransactions.size == 1 -> PaddedListItemPosition.Single
							index == 0 -> PaddedListItemPosition.First
							index == pastTransactions.lastIndex -> PaddedListItemPosition.Last
							else -> PaddedListItemPosition.Middle
						},
					)
				}
			}
		}
	}
}
