package com.serranoie.app.minus.presentation.history


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.budget.mvi.BudgetUiIntent
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetDisplay
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.SwipeActions
import com.serranoie.app.minus.presentation.ui.theme.component.SwipeActionsConfig
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.WavyDivider
import com.serranoie.app.minus.presentation.ui.theme.component.date.HistoryDateDivider
import com.serranoie.app.minus.presentation.util.prettyDate
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private const val SWIPE_ACTION_THRESHOLD = 0.35f

@Composable
fun History(
	modifier: Modifier = Modifier,
	viewModel: BudgetViewModel = hiltViewModel(),
	readOnly: Boolean = false,
	onClose: () -> Unit = {},
	onQueueDeleteWithUndo: (transaction: Transaction, message: String, onUndo: () -> Unit) -> Unit = { _, _, _ -> },
	onCancelPendingDelete: () -> Unit = {},
	onShowInfoSnackbar: (message: String) -> Unit = {},
) {
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val scrollState = rememberLazyListState()
	val coroutineScope = rememberCoroutineScope()

	var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
	var deletingTransactionIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
	
	// State for recurrent item confirmation dialogs
	var recurrentToDelete by remember { mutableStateOf<Transaction?>(null) }
	var recurrentToEdit by remember { mutableStateOf<Transaction?>(null) }
	var showDeleteRecurrentDialog by remember { mutableStateOf(false) }

	val budgetSettings = uiState.budgetSettings
	val budgetStartDate = budgetSettings?.startDate ?: LocalDate.now().minusDays(30)
	val budgetEndDate = budgetSettings?.getPeriodEndDate() ?: LocalDate.now()
	val currencyCode = budgetSettings?.currencyCode ?: "USD"

	val currencyFormat = remember(currencyCode) {
		symbolOnlyCurrencyFormat(currencyCode)
	}

	// Track expanded state for each date group
	var expandedDates by remember { mutableStateOf<Set<LocalDate>>(emptySet()) }
	
	// Track expanded state for recurrent sections
	var expandedUpcomingRecurrent by remember { mutableStateOf(true) }
	var expandedFutureRecurrent by remember { mutableStateOf(false) }

	// Separate transactions into current period and past period
	val (currentPeriodTransactions, pastPeriodTransactions) = remember(
		uiState.transactions,
		budgetStartDate,
		budgetEndDate,
		uiState.currentPeriodStartedAtMillis,
		uiState.currentPeriodId
	) {
		val sorted = uiState.transactions.sortedByDescending { it.date }
		val current = sorted.filter { transaction ->
			if (uiState.currentPeriodId > 0L && transaction.periodId > 0L) {
				return@filter transaction.periodId == uiState.currentPeriodId
			}
			val txDate = transaction.date?.toLocalDate() ?: return@filter false
			if (txDate.isBefore(budgetStartDate) || txDate.isAfter(budgetEndDate)) {
				return@filter false
			}
			if (txDate.isEqual(budgetStartDate) && uiState.currentPeriodStartedAtMillis > 0L) {
				return@filter transaction.createdAt >= uiState.currentPeriodStartedAtMillis
			}
			true
		}
		val past = sorted.filter { transaction ->
			if (uiState.currentPeriodId > 0L && transaction.periodId > 0L) {
				return@filter transaction.periodId != uiState.currentPeriodId
			}
			val txDate = transaction.date?.toLocalDate() ?: return@filter false
			txDate.isBefore(budgetStartDate) ||
				(txDate.isEqual(budgetStartDate) && uiState.currentPeriodStartedAtMillis > 0L && transaction.createdAt < uiState.currentPeriodStartedAtMillis)
		}
		Pair(current, past)
	}
	
	// Get all recurrent transactions and categorize them
	val (upcomingRecurrentInPeriod, futureRecurrentOutOfPeriod) = remember(
		uiState.transactions,
		budgetStartDate,
		budgetEndDate
	) {
		val today = LocalDate.now()
		val recurrentTransactions = uiState.transactions.filter { it.isRecurrent }
		
		val upcomingInPeriod = recurrentTransactions.mapNotNull { transaction ->
			val nextDate = calculateNextChargeDate(transaction, today)
			nextDate?.let { date ->
				if (!date.isBefore(budgetStartDate) && !date.isAfter(budgetEndDate)) {
					UpcomingRecurrentItem(
						transaction = transaction,
						nextChargeDate = date,
						isInCurrentPeriod = true
					)
				} else {
					null
				}
			}
		}.sortedBy { it.nextChargeDate }
		
		val futureOutOfPeriod = recurrentTransactions.mapNotNull { transaction ->
			calculateNextChargeDate(transaction, today)?.let { nextDate ->
				if (nextDate.isAfter(budgetEndDate)) {
					UpcomingRecurrentItem(
						transaction = transaction,
						nextChargeDate = nextDate,
						isInCurrentPeriod = false
					)
				} else null
			}
		}.sortedBy { it.nextChargeDate }
		
		Pair(upcomingInPeriod, futureOutOfPeriod)
	}
	
	val groupedCurrentTransactions = remember(
		currentPeriodTransactions,
		budgetStartDate,
		budgetEndDate
	) {
		val today = LocalDate.now()
		
		// Add "virtual" transactions for recurring expenses due on each date
		val withVirtualRecurrent = currentPeriodTransactions.flatMap { transaction ->
			if (transaction.isRecurrent && !transaction.isDeleted) {
				val charges = getRecurringChargesInPeriod(transaction, budgetStartDate, budgetEndDate, today)
				if (charges.isEmpty()) listOf(transaction) else charges
			} else {
				listOf(transaction)
			}
		}

		withVirtualRecurrent.groupBy { it.date?.toLocalDate() }
			.toSortedMap(compareByDescending { it })
	}

	val groupedPastTransactions = remember(pastPeriodTransactions) {
		pastPeriodTransactions.groupBy { it.date?.toLocalDate() }
			.toSortedMap(compareByDescending { it })
	}

	// Auto-expand date groups with a lightweight default in read-only sheet mode
	LaunchedEffect(groupedCurrentTransactions.keys, readOnly) {
		val sortedDates = groupedCurrentTransactions.keys.filterNotNull().sortedDescending()
		val defaultExpanded = if (readOnly) {
			sortedDates.take(3).toSet()
		} else {
			sortedDates.toSet()
		}
		expandedDates = expandedDates + defaultExpanded
	}

	// Clean up deletingTransactionIds when transactions are actually removed
	val allTransactionIds = remember(uiState.transactions) {
		uiState.transactions.map { it.id }.toSet()
	}
	LaunchedEffect(allTransactionIds) {
		// Remove IDs that are no longer in the transactions list
		deletingTransactionIds = deletingTransactionIds.filter { it in allTransactionIds }.toSet()
	}

	fun queueDeleteWithUndo(transaction: Transaction) {
		deletingTransactionIds = deletingTransactionIds - transaction.id
		onQueueDeleteWithUndo(
			transaction,
			"${transaction.comment.ifEmpty { "Gasto" }} eliminado",
			{ onCancelPendingDelete() }
		)
	}

	Box(modifier = modifier.fillMaxSize()) {
			LazyColumn(
				state = scrollState,
				modifier = Modifier
					.fillMaxSize()
					.statusBarsPadding()
					.padding(horizontal = 16.dp)
			) {
				item("budget-display") {
					val budgetState = uiState.budgetState

					val startDate = budgetSettings?.startDate?.let {
						Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
					} ?: Date()

					val finishDate = budgetSettings?.getPeriodEndDate()?.let {
						Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
					}

					val budget = budgetState?.totalBudget ?: BigDecimal.ZERO

					BudgetDisplay(
						budget = budget,
						budgetState = budgetState,
						budgetSettings = budgetSettings,
						currencyCode = currencyCode,
						bigVariant = true,
						modifier = Modifier.fillMaxWidth(),
						startDate = startDate,
						finishDate = finishDate
					)
				}

				item("spacer-after-budget") {
					Spacer(modifier = Modifier.height(16.dp))
				}
				
				// === UPCOMING RECURRENT PAYMENTS SECTION ===
				if (upcomingRecurrentInPeriod.isNotEmpty()) {
					item("upcoming-recurrent-header") {
						RecurrentPaymentsDivider(
							title = "Siguientes pagos recurrentes",
							isExpanded = expandedUpcomingRecurrent,
							onToggleClick = { expandedUpcomingRecurrent = !expandedUpcomingRecurrent },
							itemCount = upcomingRecurrentInPeriod.size
						)
					}
					
					item("upcoming-recurrent-content") {
						AnimatedVisibility(
							visible = expandedUpcomingRecurrent,
							enter = expandVertically(
								animationSpec = tween(300),
								expandFrom = Alignment.Top
							) + fadeIn(animationSpec = tween(300)),
							exit = shrinkVertically(
								animationSpec = tween(300),
								shrinkTowards = Alignment.Top
							) + fadeOut(animationSpec = tween(300))
						) {
							Column {
								upcomingRecurrentInPeriod.forEachIndexed { index, item ->
									val position = when {
										upcomingRecurrentInPeriod.size == 1 -> PaddedListItemPosition.Single
										index == 0 -> PaddedListItemPosition.First
										index == upcomingRecurrentInPeriod.size - 1 -> PaddedListItemPosition.Last
										else -> PaddedListItemPosition.Middle
									}
									
									UpcomingRecurrentSwipeItem(
										item = item,
										currencyFormat = currencyFormat,
										position = position,
										onDelete = {
											recurrentToDelete = item.transaction
											showDeleteRecurrentDialog = true
										},
										onEdit = {
											recurrentToEdit = item.transaction
										}
									)
									
									if (index < upcomingRecurrentInPeriod.size - 1) {
										Spacer(modifier = Modifier.height(2.dp))
									}
								}
							}
						}
					}
					
					item("spacer-after-upcoming") {
						Spacer(modifier = Modifier.height(16.dp))
					}
				}

				// Current period transactions
				groupedCurrentTransactions.forEach { (date, transactions) ->
					val isExpanded = date?.let { it in expandedDates } ?: false
					val dayTotal = transactions.sumOf { it.amount }

					item("date-$date") {
						HistoryDateDivider(
							date = date,
							isExpanded = isExpanded,
							onToggleClick = {
								date?.let { dateKey ->
									expandedDates = if (isExpanded) {
										expandedDates - dateKey
									} else {
										expandedDates + dateKey
									}
								}
							},
							totalAmount = dayTotal.toPlainString(),
							currencyCode = currencyFormat.currency?.symbol ?: "$"
						)
					}

					// Always include the content in composition, animate visibility
					item("date-content-$date") {
						AnimatedVisibility(
							visible = isExpanded,
							enter = expandVertically(
								animationSpec = tween(300),
								expandFrom = Alignment.Top
							) + fadeIn(animationSpec = tween(300)),
							exit = shrinkVertically(
								animationSpec = tween(300),
								shrinkTowards = Alignment.Top
							) + fadeOut(animationSpec = tween(300))
						) {
							Column {
								transactions.forEachIndexed { index, transaction ->
									key(transaction.id) {
										val position = when {
											transactions.size == 1 -> PaddedListItemPosition.Single
											index == 0 -> PaddedListItemPosition.First
											index == transactions.size - 1 -> PaddedListItemPosition.Last
											else -> PaddedListItemPosition.Middle
										}

										// Individual delete animation
										AnimatedVisibility(
											visible = transaction.id !in deletingTransactionIds,
											enter = fadeIn(animationSpec = tween(300)),
											exit = shrinkVertically(
												animationSpec = tween(durationMillis = 300),
												shrinkTowards = Alignment.Top
											) + fadeOut(animationSpec = tween(durationMillis = 300))
										) {
											val isBeingDeleted = transaction.id in deletingTransactionIds
											TransactionSwipeItem(
												transaction = transaction,
												currencyFormat = currencyFormat,
												position = position,
												isBeingDeleted = isBeingDeleted,
												onDelete = {
																																queueDeleteWithUndo(transaction)
												},
												onEdit = { editingTransaction = transaction },
												readOnly = readOnly
											)
										}

										if (index < transactions.size - 1) {
											Spacer(modifier = Modifier.height(2.dp))
										}
									}
								}

								val totalText = currencyFormat.format(dayTotal)
								Row(
									modifier = Modifier
										.fillMaxWidth()
										.padding(horizontal = 16.dp, vertical = 8.dp),
									horizontalArrangement = Arrangement.End,
									verticalAlignment = Alignment.CenterVertically
								) {
									Text(
										text = "Total del día: ",
										style = MaterialTheme.typography.labelMedium,
										color = MaterialTheme.colorScheme.onSurfaceVariant
									)

									Text(
										text = totalText,
										style = MaterialTheme.typography.labelLarge,
										color = MaterialTheme.colorScheme.primary,
										fontWeight = FontWeight.Bold
									)
								}
							}
						}
					}
				}

				// === FUTURE RECURRENT PAYMENTS (OUTSIDE CURRENT PERIOD) ===
				if (futureRecurrentOutOfPeriod.isNotEmpty()) {
					item("future-recurrent-divider") {
						WavyDivider(
							text = "Pagos recurrentes fuera del período actual",
							amplitude = 4f,
							wavelength = 45f
						)
					}
					
					item("future-recurrent-header") {
						RecurrentPaymentsDivider(
							title = "Próximos pagos (fuera de período)",
							isExpanded = expandedFutureRecurrent,
							onToggleClick = { expandedFutureRecurrent = !expandedFutureRecurrent },
							itemCount = futureRecurrentOutOfPeriod.size,
							isSecondary = true
						)
					}
					
					item("future-recurrent-content") {
						AnimatedVisibility(
							visible = expandedFutureRecurrent,
							enter = expandVertically(
								animationSpec = tween(300),
								expandFrom = Alignment.Top
							) + fadeIn(animationSpec = tween(300)),
							exit = shrinkVertically(
								animationSpec = tween(300),
								shrinkTowards = Alignment.Top
							) + fadeOut(animationSpec = tween(300))
						) {
							Column {
								futureRecurrentOutOfPeriod.forEachIndexed { index, item ->
									val position = when {
										futureRecurrentOutOfPeriod.size == 1 -> PaddedListItemPosition.Single
										index == 0 -> PaddedListItemPosition.First
										index == futureRecurrentOutOfPeriod.size - 1 -> PaddedListItemPosition.Last
										else -> PaddedListItemPosition.Middle
									}
									
									UpcomingRecurrentSwipeItem(
										item = item,
										currencyFormat = currencyFormat,
										position = position,
										isOutOfPeriod = true,
										onDelete = {
											recurrentToDelete = item.transaction
											showDeleteRecurrentDialog = true
										},
										onEdit = {
											recurrentToEdit = item.transaction
										}
									)
									
									if (index < futureRecurrentOutOfPeriod.size - 1) {
										Spacer(modifier = Modifier.height(2.dp))
									}
								}
							}
						}
					}
				}

				// Wavy divider between current and past periods
				if (groupedPastTransactions.isNotEmpty()) {
					item("wavy-divider") {
						WavyDivider(
							text = "Gastos en el periodo pasado",
							amplitude = 4f,
							wavelength = 45f
						)
					}
				}

				// Past period transactions (collapsed by default)
				groupedPastTransactions.forEach { (date, transactions) ->
					val isExpanded = date?.let { it in expandedDates } ?: false
					val dayTotal = transactions.sumOf { it.amount }

					item("past-date-$date") {
						HistoryDateDivider(
							date = date,
							isExpanded = isExpanded,
							onToggleClick = {
								date?.let { dateKey ->
									expandedDates = if (isExpanded) {
										expandedDates - dateKey
									} else {
										expandedDates + dateKey
									}
								}
							},
							totalAmount = dayTotal.toPlainString(),
							currencyCode = currencyFormat.currency?.symbol ?: "$"
						)
					}

					// Always include the content in composition, animate visibility
					item("past-date-content-$date") {
						AnimatedVisibility(
							visible = isExpanded,
							enter = expandVertically(
								animationSpec = tween(300),
								expandFrom = Alignment.Top
							) + fadeIn(animationSpec = tween(300)),
							exit = shrinkVertically(
								animationSpec = tween(300),
								shrinkTowards = Alignment.Top
							) + fadeOut(animationSpec = tween(300))
						) {
							Column {
								transactions.forEachIndexed { index, transaction ->
									key(transaction.id) {
										val position = when {
											transactions.size == 1 -> PaddedListItemPosition.Single
											index == 0 -> PaddedListItemPosition.First
											index == transactions.size - 1 -> PaddedListItemPosition.Last
											else -> PaddedListItemPosition.Middle
										}

										// Individual delete animation
										AnimatedVisibility(
											visible = transaction.id !in deletingTransactionIds,
											enter = fadeIn(animationSpec = tween(300)),
											exit = shrinkVertically(
												animationSpec = tween(durationMillis = 300),
												shrinkTowards = Alignment.Top
											) + fadeOut(animationSpec = tween(durationMillis = 300))
										) {
											val isBeingDeleted = transaction.id in deletingTransactionIds
											TransactionSwipeItem(
												transaction = transaction,
												currencyFormat = currencyFormat,
												position = position,
												isBeingDeleted = isBeingDeleted,
												onDelete = {
																																queueDeleteWithUndo(transaction)
												},
												onEdit = { editingTransaction = transaction },
												readOnly = readOnly
											)
										}

										if (index < transactions.size - 1) {
											Spacer(modifier = Modifier.height(2.dp))
										}
									}
								}

								val totalText = currencyFormat.format(dayTotal)
								Row(
									modifier = Modifier
										.fillMaxWidth()
										.padding(horizontal = 16.dp, vertical = 8.dp),
									horizontalArrangement = Arrangement.End,
									verticalAlignment = Alignment.CenterVertically
								) {
									Text(
										text = "Total del día: ",
										style = MaterialTheme.typography.labelMedium,
										color = MaterialTheme.colorScheme.onSurfaceVariant
									)

									Text(
										text = totalText,
										style = MaterialTheme.typography.labelLarge,
										color = MaterialTheme.colorScheme.primary,
										fontWeight = FontWeight.Bold
									)
								}
							}
						}
					}
				}

				// Bottom spacer for better scrolling
				item("spacer-bottom") {
					Spacer(modifier = Modifier.height(32.dp))
				}
			}

		// Empty state
		if (uiState.transactions.isEmpty()) {
			NoTransactionsView(
				modifier = Modifier
					.align(Alignment.Center)
					.padding(32.dp)
			)
		}
	}

	// Edit Transaction Dialog
	if (editingTransaction != null) {
		val transaction = editingTransaction!!
		Dialog(
			onDismissRequest = { editingTransaction = null },
			properties = DialogProperties(
				usePlatformDefaultWidth = false,
				dismissOnBackPress = true,
				dismissOnClickOutside = false
			)
		) {
			Surface(
				modifier = Modifier.fillMaxSize(),
				color = MaterialTheme.colorScheme.background
			) {
				TransactionEditScreen(
					transaction = transaction,
					budgetStartDate = budgetStartDate,
					budgetEndDate = budgetEndDate,
					currencyCode = currencyCode,
					onCancel = { editingTransaction = null },
					onSave = { newAmount, newComment, newDateTime, newIsRecurrent, newFrequency, newEndDate, newSubscriptionDay ->
						val updatedTransaction = transaction.copy(
							amount = newAmount,
							comment = newComment,
							date = newDateTime,
							isRecurrent = newIsRecurrent,
							recurrentFrequency = newFrequency,
							recurrentEndDate = newEndDate?.atStartOfDay(),
							subscriptionDay = newSubscriptionDay
						)
						viewModel.processIntent(BudgetUiIntent.EditTransactionTapped(updatedTransaction))
						onShowInfoSnackbar("${updatedTransaction.comment.ifEmpty { "Gasto" }} ha sido modificado")
						editingTransaction = null
					}
				)
			}
		}
	}
	
	// Delete Recurrent Confirmation Dialog
	if (showDeleteRecurrentDialog && recurrentToDelete != null) {
		val transaction = recurrentToDelete!!
		AlertDialog(
			onDismissRequest = { 
				showDeleteRecurrentDialog = false
				recurrentToDelete = null
			},
			title = { Text("Eliminar gasto recurrente") },
			text = {
				Column {
					Text(
						"¿Estás seguro de que deseas eliminar \"${transaction.comment.ifEmpty { "este gasto recurrente" }}\"?"
					)
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = "Esta acción eliminará toda la configuración de este gasto recurrente y no recibirás más notificaciones.",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			},
			confirmButton = {
				Button(
					onClick = {
						viewModel.processIntent(BudgetUiIntent.DeleteTransactionTapped(transaction))
						showDeleteRecurrentDialog = false
						recurrentToDelete = null
					},
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.error
					)
				) {
					Text("Eliminar")
				}
			},
			dismissButton = {
				TextButton(
					onClick = { 
						showDeleteRecurrentDialog = false
						recurrentToDelete = null
					}
				) {
					Text("Cancelar")
				}
			}
		)
	}
	
	// Edit Recurrent Dialog - Opens the edit screen for recurrent transactions
	if (recurrentToEdit != null) {
		val transaction = recurrentToEdit!!
		Dialog(
			onDismissRequest = { recurrentToEdit = null },
			properties = DialogProperties(
				usePlatformDefaultWidth = false,
				dismissOnBackPress = true,
				dismissOnClickOutside = false
			)
		) {
			Surface(
				modifier = Modifier.fillMaxSize(),
				color = MaterialTheme.colorScheme.background
			) {
				TransactionEditScreen(
					transaction = transaction,
					budgetStartDate = budgetStartDate,
					budgetEndDate = budgetEndDate,
					currencyCode = currencyCode,
					onCancel = { recurrentToEdit = null },
					onSave = { newAmount, newComment, newDateTime, newIsRecurrent, newFrequency, newEndDate, newSubscriptionDay ->
						val updatedTransaction = transaction.copy(
							amount = newAmount,
							comment = newComment,
							date = newDateTime,
							isRecurrent = newIsRecurrent,
							recurrentFrequency = newFrequency,
							recurrentEndDate = newEndDate?.atStartOfDay(),
							subscriptionDay = newSubscriptionDay
						)
						viewModel.processIntent(BudgetUiIntent.EditTransactionTapped(updatedTransaction))
						onShowInfoSnackbar("${updatedTransaction.comment.ifEmpty { "Gasto" }} ha sido modificado")
						recurrentToEdit = null
					}
				)
			}
		}
	}
}

data class UpcomingRecurrentItem(
	val transaction: Transaction,
	val nextChargeDate: LocalDate,
	val isInCurrentPeriod: Boolean
)

private fun calculateNextChargeDate(transaction: Transaction, today: LocalDate): LocalDate? {
	if (!transaction.isRecurrent) {
		return null
	}
	
	val frequency = transaction.recurrentFrequency
	if (frequency == null) {
		return null
	}
	
	val startDate = transaction.date?.toLocalDate()
	if (startDate == null) {
		return null
	}
	
	val endDate = transaction.recurrentEndDate?.toLocalDate()
	
	if (endDate != null && today.isAfter(endDate)) {
		return null
	}
	
	val result = when (frequency) {
		RecurrentFrequency.WEEKLY -> {
			var nextDate: LocalDate = startDate
			while (!nextDate.isAfter(today)) {
				nextDate = nextDate.plusWeeks(1)
			}
			if (endDate == null || !nextDate.isAfter(endDate)) nextDate else null
		}
		RecurrentFrequency.BIWEEKLY -> {
			var nextDate: LocalDate = startDate
			while (!nextDate.isAfter(today)) {
				nextDate = nextDate.plusWeeks(2)
			}
			if (endDate == null || !nextDate.isAfter(endDate)) nextDate else null
		}
		RecurrentFrequency.MONTHLY -> {
			val billingDay = transaction.subscriptionDay ?: startDate.dayOfMonth
			
			var nextDate: LocalDate = today.withDayOfMonth(billingDay.coerceAtMost(today.lengthOfMonth()))
			
			if (today.dayOfMonth >= billingDay) {
				nextDate = nextDate.plusMonths(1)
				val maxDay = nextDate.lengthOfMonth()
				if (billingDay > maxDay) {
					nextDate = nextDate.withDayOfMonth(maxDay)
				}
			}
			
			if (endDate != null && nextDate.isAfter(endDate)) null else nextDate
		}
	}
	
	return result
}

@Composable
fun RecurrentPaymentsDivider(
	title: String,
	isExpanded: Boolean,
	onToggleClick: () -> Unit,
	itemCount: Int,
	isSecondary: Boolean = false
) {
	val interactionSource = remember { MutableInteractionSource() }
	val color = if (isSecondary) 
		MaterialTheme.colorScheme.onSurfaceVariant 
	else 
		MaterialTheme.colorScheme.primary

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(
				onClick = onToggleClick,
				interactionSource = interactionSource,
				indication = null
			)
			.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
		verticalAlignment = Alignment.CenterVertically,
		horizontalArrangement = Arrangement.SpaceBetween
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Icon(
				imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
				contentDescription = if (isExpanded) "Collapse" else "Expand",
				tint = color,
				modifier = Modifier
			)
			
			Icon(
				imageVector = Icons.Rounded.Repeat,
				contentDescription = null,
				tint = color,
				modifier = Modifier.size(18.dp)
			)

			Text(
				text = title,
				style = MaterialTheme.typography.labelMedium,
				color = color
			)
		}

		Text(
			text = "$itemCount",
			style = MaterialTheme.typography.labelMedium,
			color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
		)
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpcomingRecurrentItemRow(
	item: UpcomingRecurrentItem,
	currencyFormat: NumberFormat,
	position: PaddedListItemPosition,
	isOutOfPeriod: Boolean = false
) {
	val transaction = item.transaction
	val nextChargeDate = item.nextChargeDate
	
	val shape = when (position) {
		PaddedListItemPosition.First -> RoundedCornerShape(
			topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp
		)
		PaddedListItemPosition.Last -> RoundedCornerShape(
			bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp
		)
		PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)
		PaddedListItemPosition.Middle -> RoundedCornerShape(4.dp)
	}
	
	val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), nextChargeDate)
	val daysText = when {
		daysUntil == 0L -> "Hoy"
		daysUntil == 1L -> "Mañana"
		daysUntil < 7 -> "En $daysUntil días"
		else -> "En ${daysUntil / 7} semanas"
	}
	
	val alpha = if (isOutOfPeriod) 0.6f else 1f

	Surface(
		shape = shape,
		color = if (isOutOfPeriod) 
			MaterialTheme.colorScheme.surfaceVariant 
		else 
			MaterialTheme.colorScheme.surfaceContainer,
		modifier = Modifier.fillMaxWidth()
	) {
		CustomPaddedListItem(
			onClick = { },
			position = position,
			background = MaterialTheme.colorScheme.surface,
			contentColor = MaterialTheme.colorScheme.onSurface
		) {
			Column(modifier = Modifier.weight(1f)) {
				Text(
					text = transaction.comment.ifEmpty { "Gasto recurrente sin nombre" },
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
					fontWeight = FontWeight.Medium
				)
				
				val dateText = prettyDate(
					date = nextChargeDate.atStartOfDay(),
					showTime = false,
					forceHideDate = false,
					human = true
				)
				
				val frequencyLabel = when (transaction.recurrentFrequency) {
					RecurrentFrequency.WEEKLY -> "Semanal"
					RecurrentFrequency.BIWEEKLY -> "Quincenal"
					RecurrentFrequency.MONTHLY -> {
						if (transaction.subscriptionDay != null) {
							"Mensual (día ${transaction.subscriptionDay})"
						} else "Mensual"
					}
					else -> "Recurrente"
				}
				
				Text(
					text = "$frequencyLabel - $dateText | $daysText",
					style = MaterialTheme.typography.bodySmall,
					color = if (daysUntil <= 3 && !isOutOfPeriod)
						MaterialTheme.colorScheme.primary 
					else 
						MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * alpha)
				)
			}

			Text(
				text = currencyFormat.format(transaction.amount),
				style = MaterialTheme.typography.titleSmallEmphasized,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
				fontWeight = FontWeight.SemiBold
			)
		}
	}
}

@Composable
fun UpcomingRecurrentSwipeItem(
	item: UpcomingRecurrentItem,
	currencyFormat: NumberFormat,
	position: PaddedListItemPosition,
	isOutOfPeriod: Boolean = false,
	onDelete: () -> Unit,
	onEdit: () -> Unit
) {
	val shape = when (position) {
		PaddedListItemPosition.First -> RoundedCornerShape(
			topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp
		)
		PaddedListItemPosition.Last -> RoundedCornerShape(
			bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp
		)
		PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)
		PaddedListItemPosition.Middle -> RoundedCornerShape(4.dp)
	}

	Surface(
		shape = shape,
		color = if (isOutOfPeriod)
			MaterialTheme.colorScheme.surfaceVariant
		else
			MaterialTheme.colorScheme.surfaceContainer,
		modifier = Modifier.fillMaxWidth()
	) {
		SwipeActions(
			modifier = Modifier.fillMaxWidth(),
			shape = shape,
			startActionsConfig = SwipeActionsConfig(
				threshold = SWIPE_ACTION_THRESHOLD,
				icon = Icons.Default.Edit,
				iconTint = MaterialTheme.colorScheme.onPrimary,
				background = MaterialTheme.colorScheme.primary,
				backgroundActive = MaterialTheme.colorScheme.primary,
				stayDismissed = false,
				onDismiss = onEdit
			),
			endActionsConfig = SwipeActionsConfig(
				threshold = SWIPE_ACTION_THRESHOLD,
				icon = Icons.Default.Delete,
				iconTint = MaterialTheme.colorScheme.onError,
				background = MaterialTheme.colorScheme.error,
				backgroundActive = MaterialTheme.colorScheme.error,
				stayDismissed = true,
				onDismiss = onDelete
			)
		) {
			UpcomingRecurrentItemRow(
				item = item,
				currencyFormat = currencyFormat,
				position = position,
				isOutOfPeriod = isOutOfPeriod
			)
		}
	}
}

@Composable
private fun TransactionSwipeItem(
	transaction: Transaction,
	currencyFormat: NumberFormat,
	position: PaddedListItemPosition,
	onDelete: () -> Unit,
	onEdit: () -> Unit,
	readOnly: Boolean,
	isBeingDeleted: Boolean = false
) {
	val shape = when (position) {
		PaddedListItemPosition.First -> RoundedCornerShape(
			topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp
		)
		PaddedListItemPosition.Last -> RoundedCornerShape(
			bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp
		)
		PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)
		PaddedListItemPosition.Middle -> RoundedCornerShape(4.dp)
	}

	if (readOnly) {
		Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainer) {
			TransactionItem(
				transaction = transaction,
				currencyFormat = currencyFormat,
				position = position
			)
		}
	} else {
		Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainer) {
			SwipeActions(
				modifier = Modifier.fillMaxWidth(),
				shape = shape,
				enabled = !isBeingDeleted,
				startActionsConfig = SwipeActionsConfig(
					threshold = SWIPE_ACTION_THRESHOLD,
					icon = Icons.Default.Edit,
					iconTint = MaterialTheme.colorScheme.onPrimary,
					background = MaterialTheme.colorScheme.primary,
					backgroundActive = MaterialTheme.colorScheme.primary,
					stayDismissed = false,
					onDismiss = onEdit
				),
				endActionsConfig = SwipeActionsConfig(
					threshold = SWIPE_ACTION_THRESHOLD,
					icon = Icons.Default.Delete,
					iconTint = MaterialTheme.colorScheme.onError,
					background = MaterialTheme.colorScheme.error,
					backgroundActive = MaterialTheme.colorScheme.error,
					stayDismissed = true,
					onDismiss = onDelete
				)
			) {
				TransactionItem(
					transaction = transaction,
					currencyFormat = currencyFormat,
					position = position
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TransactionItem(
	transaction: Transaction,
	currencyFormat: NumberFormat,
	position: PaddedListItemPosition = PaddedListItemPosition.Middle
) {
	CustomPaddedListItem(
		onClick = { },
		position = position,
		background = MaterialTheme.colorScheme.surface,
		contentColor = MaterialTheme.colorScheme.onSurface
	) {
		Column(modifier = Modifier.weight(1f)) {
			Text(
				text = transaction.comment.ifEmpty { "Gasto sin nombre" },
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurface,
				fontWeight = FontWeight.Medium
			)
			val timeText = prettyDate(
				date = transaction.date,
				showTime = true,
				forceHideDate = true
			)
			val subtitle = if (transaction.isRecurrent) "Gasto recurrente - $timeText" else timeText
			Text(
				text = subtitle,
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
			)
		}

		Text(
			text = currencyFormat.format(transaction.amount),
			style = MaterialTheme.typography.titleSmallEmphasized,
			color = MaterialTheme.colorScheme.onSurface,
			fontWeight = FontWeight.SemiBold
		)
	}
}

@Composable
fun NoTransactionsView(modifier: Modifier = Modifier) {
	Column(
		modifier = modifier,
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center
	) {
		Icon(
			imageVector = Icons.Rounded.Check,
			contentDescription = null,
			modifier = Modifier.size(64.dp),
			tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
		)
		Spacer(modifier = Modifier.height(16.dp))
		Text(
			text = "No expenses yet",
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Spacer(modifier = Modifier.height(8.dp))
		Text(
			text = "Tap the numpad to add your first expense",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
		)
	}
}

@Preview
@Composable
fun HistoryPreview() {
	MinusTheme {
		Surface {
			Column {

				HistoryDateDivider(
					date = LocalDate.now(),
					isExpanded = true,
					onToggleClick = { },
					totalAmount = "100.00",
					currencyCode = "$"
				)

				TransactionSwipeItem(
					transaction = Transaction(
						id = 1L,
						amount = BigDecimal("10.00"),
						comment = "Category",
						date = LocalDateTime.now(),
						isDeleted = false
					),
					currencyFormat = NumberFormat.getCurrencyInstance(),
					position = PaddedListItemPosition.First,
					onDelete = {},
					onEdit = {},
					readOnly = false
				)
				TransactionItem(
					transaction = Transaction(
						id = 2L,
						amount = BigDecimal("25.00"),
						comment = "Category 2",
						date = LocalDateTime.now(),
						isDeleted = false
					),
					currencyFormat = NumberFormat.getCurrencyInstance(),
					position = PaddedListItemPosition.Middle
				)
				TransactionItem(
					transaction = Transaction(
						id = 3L,
						amount = BigDecimal("199.00"),
						comment = "Streaming",
						date = LocalDateTime.now().minusHours(6),
						isDeleted = false,
						isRecurrent = true
					),
					currencyFormat = NumberFormat.getCurrencyInstance(),
					position = PaddedListItemPosition.Last
				)
			}
		}
	}
}

@Preview
@Composable
private fun HistoryPreviewEmpty() {
	MinusTheme {
		Surface {
			NoTransactionsView()
		}
	}
}

@Preview
@Composable
private fun RecurrentPaymentsDividerPreview() {
	MinusTheme {
		Column {
			RecurrentPaymentsDivider(
				title = "Siguientes pagos recurrentes",
				isExpanded = true,
				onToggleClick = {},
				itemCount = 3
			)
			
			RecurrentPaymentsDivider(
				title = "Próximos pagos (fuera de período)",
				isExpanded = false,
				onToggleClick = {},
				itemCount = 2,
				isSecondary = true
			)
		}
	}
}

@Preview
@Composable
private fun UpcomingRecurrentItemPreview() {
	MinusTheme {
		Column {
			UpcomingRecurrentItemRow(
				item = UpcomingRecurrentItem(
					transaction = Transaction(
						id = 1L,
						amount = BigDecimal("199.00"),
						comment = "Netflix",
						date = LocalDateTime.now(),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.MONTHLY,
						subscriptionDay = 15
					),
					nextChargeDate = LocalDate.now().plusDays(2),
					isInCurrentPeriod = true
				),
				currencyFormat = NumberFormat.getCurrencyInstance(),
				position = PaddedListItemPosition.Single
			)
			
			Spacer(modifier = Modifier.height(8.dp))
			
			UpcomingRecurrentItemRow(
				item = UpcomingRecurrentItem(
					transaction = Transaction(
						id = 2L,
						amount = BigDecimal("99.00"),
						comment = "Spotify",
						date = LocalDateTime.now(),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.MONTHLY,
						subscriptionDay = 3
					),
					nextChargeDate = LocalDate.now().plusDays(15),
					isInCurrentPeriod = false
				),
				currencyFormat = NumberFormat.getCurrencyInstance(),
				position = PaddedListItemPosition.Single,
				isOutOfPeriod = true
			)
		}
	}
}

/**
 * Generate virtual transactions for a recurring expense that charges within the given period.
 * This creates "virtual" transaction copies for each billing date so they appear in history.
 */
private fun getRecurringChargesInPeriod(
	transaction: Transaction,
	periodStart: LocalDate,
	periodEnd: LocalDate,
	today: LocalDate
): List<Transaction> {
	val frequency = transaction.recurrentFrequency ?: return emptyList()
	val startDate = transaction.date?.toLocalDate() ?: return emptyList()
	val subscriptionEnd = transaction.recurrentEndDate?.toLocalDate() ?: periodEnd.plusMonths(1)
	
	val virtualTransactions = mutableListOf<Transaction>()
	var chargeDate = startDate
	
	// Generate charge dates
	while (!chargeDate.isAfter(subscriptionEnd)) {
		// Only include charges within the budget period AND up to today
		// (don't show future charges in history, those go in "upcoming" section)
		if (!chargeDate.isBefore(periodStart) && !chargeDate.isAfter(periodEnd) && !chargeDate.isAfter(today)) {
			virtualTransactions.add(
				transaction.copy(
					date = chargeDate.atStartOfDay(),
					// Mark as virtual by using a special ID pattern (based on date)
					id = transaction.id * 1000000 + chargeDate.toEpochDay()
				)
			)
		}
		
		// Calculate next charge date
		chargeDate = when (frequency) {
			RecurrentFrequency.WEEKLY -> chargeDate.plusWeeks(1)
			RecurrentFrequency.BIWEEKLY -> chargeDate.plusWeeks(2)
			RecurrentFrequency.MONTHLY -> {
				val billingDay = transaction.subscriptionDay ?: startDate.dayOfMonth
				val nextMonth = chargeDate.plusMonths(1)
				val maxDay = nextMonth.lengthOfMonth()
				nextMonth.withDayOfMonth(billingDay.coerceAtMost(maxDay))
			}
		}
	}
	
	return virtualTransactions
}