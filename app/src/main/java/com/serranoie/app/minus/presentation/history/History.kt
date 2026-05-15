package com.serranoie.app.minus.presentation.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.budget.mvi.intent.BudgetSystemIntent
import com.serranoie.app.minus.presentation.budget.mvi.intent.BudgetTransactionIntent
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.WavyDivider
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetDisplay
import com.serranoie.app.minus.presentation.ui.theme.component.date.DayTotalItem
import com.serranoie.app.minus.presentation.ui.theme.component.date.HistoryDateDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.ExpenseItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.NoTransactionsView
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableExpenseItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.ticket.RecurrentTicketCard
import com.serranoie.app.minus.presentation.ui.theme.component.ticket.TicketView
import com.serranoie.app.minus.presentation.util.prettyDate
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import kotlinx.coroutines.delay
import logcat.logcat
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
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

	val isAtEndOfList: Boolean =
		remember(scrollState.canScrollForward, scrollState.layoutInfo.visibleItemsInfo) {
			!scrollState.canScrollForward && scrollState.layoutInfo.visibleItemsInfo.lastOrNull() != null
		}

	LaunchedEffect(isAtEndOfList) {
		viewModel.processIntent(BudgetSystemIntent.SetLockSwipeable(!isAtEndOfList))
	}

	LaunchedEffect(isAtEndOfList) {
		if (isAtEndOfList) {
			// When at end, we could add additional handling here
			// The lockSwipeable=false already enables swipe gestures
			// The actual overscroll-to-dismiss is handled by the swipeable modifier
		}
	}

	var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
	var pendingRemovedTransactions by remember { mutableStateOf<Map<Long, Transaction>>(emptyMap()) }

	var recurrentToDelete by remember { mutableStateOf<Transaction?>(null) }
	var recurrentToEdit by remember { mutableStateOf<Transaction?>(null) }
	var showDeleteRecurrentDialog by remember { mutableStateOf(false) }

	var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
	var isDismissingTransactionDialog by remember { mutableStateOf(false) }

	val budgetSettings = uiState.budgetSettings
	val budgetStartDate = budgetSettings?.startDate ?: LocalDate.now().minusDays(30)
	val budgetEndDate = budgetSettings?.getPeriodEndDate() ?: LocalDate.now()
	val currencyCode = budgetSettings?.currencyCode ?: "USD"

	val currencyFormat = remember(currencyCode) {
		symbolOnlyCurrencyFormat(currencyCode)
	}

	var expandedDates by rememberSaveable { mutableStateOf<Set<LocalDate>>(emptySet()) }
	var showPastPeriod by rememberSaveable { mutableStateOf(false) }
	val deletingTransactionIds = remember(pendingRemovedTransactions) {
		pendingRemovedTransactions.keys.toSet()
	}

	var showOutOfPeriodSubscriptions by rememberSaveable { mutableStateOf(false) }

	val displayTransactions = remember(uiState.transactions, pendingRemovedTransactions) {
		uiState.transactions + pendingRemovedTransactions.values.filterNot { pending ->
			uiState.transactions.any { it.id == pending.id }
		}
	}

	val previousPeriodId = uiState.currentPeriodId - 1
	val (currentPeriodTransactions, pastPeriodTransactions) = remember(
		displayTransactions,
		budgetStartDate,
		budgetEndDate,
		uiState.currentPeriodStartedAtMillis,
		uiState.currentPeriodId,
		previousPeriodId
	) {
		val sorted = displayTransactions.sortedByDescending { it.date }
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
				return@filter transaction.periodId == previousPeriodId
			}
			val txDate = transaction.date?.toLocalDate() ?: return@filter false
			txDate.isBefore(budgetStartDate) || (txDate.isEqual(budgetStartDate) && uiState.currentPeriodStartedAtMillis > 0L && transaction.createdAt < uiState.currentPeriodStartedAtMillis)
		}
		Pair(current, past)
	}

	val (upcomingRecurrentInPeriod, futureRecurrentOutOfPeriod) = remember(
		displayTransactions, budgetStartDate, budgetEndDate
	) {
		val today = LocalDate.now()
		val recurrentTransactions = displayTransactions.filter { it.isRecurrent }

		val upcomingInPeriod = recurrentTransactions.mapNotNull { transaction ->
			val nextDate = calculateNextChargeDate(transaction, today)
			nextDate?.let { date ->
				if (!date.isBefore(budgetStartDate) && !date.isAfter(budgetEndDate)) {
					UpcomingRecurrentItem(
						transaction = transaction, nextChargeDate = date, isInCurrentPeriod = true
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
		currentPeriodTransactions, displayTransactions, budgetStartDate, budgetEndDate
	) {
		val today = LocalDate.now()
		val regularTransactions = currentPeriodTransactions.filterNot { it.isRecurrent }
		val recurrentCharges = displayTransactions
			.filter { it.isRecurrent && !it.isDeleted }
			.flatMap { transaction ->
				getRecurringChargesInPeriod(transaction, budgetStartDate, budgetEndDate, today)
			}

		(regularTransactions + recurrentCharges)
			.groupBy { it.date?.toLocalDate() }
			.toSortedMap(compareByDescending { it })
	}

	val groupedPastTransactions = remember(pastPeriodTransactions) {
		pastPeriodTransactions.groupBy { it.date?.toLocalDate() }
			.toSortedMap(compareByDescending { it })
	}

	LaunchedEffect(groupedCurrentTransactions.keys, readOnly) {
		val sortedDates = groupedCurrentTransactions.keys.filterNotNull().sortedDescending()
		val defaultExpanded = sortedDates.take(1).toSet()
		expandedDates = expandedDates + defaultExpanded
	}

	LaunchedEffect(pendingRemovedTransactions.keys) {
		if (pendingRemovedTransactions.isNotEmpty()) {
			delay(330)
			pendingRemovedTransactions = emptyMap()
		}
	}

	fun queueDeleteWithUndo(transaction: Transaction) {
		pendingRemovedTransactions = pendingRemovedTransactions + (transaction.id to transaction)
		onQueueDeleteWithUndo(
			transaction,
			"${transaction.comment.ifEmpty { "Gasto" }} eliminado",
			{
				pendingRemovedTransactions = pendingRemovedTransactions - transaction.id
				onCancelPendingDelete()
			})
	}

	Box(modifier = modifier.fillMaxSize()) {
		LazyColumn(
			state = scrollState,
			modifier = Modifier
				.fillMaxSize()
				.animateContentSize()
				.then(if (selectedTransaction != null) Modifier.blur(10.dp) else Modifier)
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
				logcat("History") {
					"BudgetDisplay input budget=$budget budgetStateTotal=${budgetState?.totalBudget} budgetSettingsTotal=${budgetSettings?.totalBudget} rollOverLimit=${budgetSettings?.rollOverLimit} rollOverCarry=${budgetSettings?.rollOverCarryForward}"
				}

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


			groupedCurrentTransactions.forEach { (date, transactions) ->
				val isExpanded = date?.let { expandedDates.contains(it) } ?: false
				val dayTotal = transactions.sumOf { it.amount }

				item("date-$date") {
					HistoryDateDivider(
						date = date,
						isExpanded = isExpanded,
						onToggleClick = {
							date?.let { dateKey ->
								expandedDates = if (isExpanded) {
									expandedDates.minus(dateKey)
								} else {
									expandedDates.plus(dateKey)
								}
							}
						},
						totalAmount = dayTotal,
						currencyCode = currencyCode ?: "$"
					)
				}

				item("date-content-$date") {
					AnimatedVisibility(
						visible = isExpanded, enter = expandVertically(
							animationSpec = tween(300), expandFrom = Alignment.Top
						) + fadeIn(animationSpec = tween(300)), exit = shrinkVertically(
							animationSpec = tween(300), shrinkTowards = Alignment.Top
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

									val isBeingDeleted = transaction.id in deletingTransactionIds
									AnimatedVisibility(
										visible = !isBeingDeleted,
										enter = EnterTransition.None,
										exit = slideOutHorizontally(
											animationSpec = tween(durationMillis = 280),
											targetOffsetX = { fullWidth -> fullWidth }
										) + fadeOut(animationSpec = tween(durationMillis = 280))
									) {
										SwipeableExpenseItem(
											transaction = transaction,
											currencyFormat = currencyFormat,
											position = position,
											isBeingDeleted = isBeingDeleted,
											onDelete = {
												queueDeleteWithUndo(transaction)
											},
											onEdit = { editingTransaction = transaction },
											readOnly = readOnly,
											onClick = { selectedTransaction = transaction })
									}

									if (index < transactions.size - 1 && transaction.id !in deletingTransactionIds) {
										Spacer(modifier = Modifier.height(2.dp))
									}
								}
							}

							DayTotalItem(
								total = dayTotal,
								currencyFormat = currencyFormat,
								modifier = Modifier
									.fillMaxWidth()
									.padding(horizontal = 16.dp, vertical = 8.dp)
							)
						}
					}
				}
			}

			if (futureRecurrentOutOfPeriod.isNotEmpty()) {
				item("future-recurrent-toggle") {
					val interactionSource = remember { MutableInteractionSource() }
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.clickable(
								interactionSource = interactionSource,
								indication = null
							) {
								showOutOfPeriodSubscriptions = !showOutOfPeriodSubscriptions
							}
					) {
						WavyDivider(
							text = if (showOutOfPeriodSubscriptions) {
								"Ocultar subscripciones fuera del periodo"
							} else {
								"Mostrar subscripciones fuera del periodo"
							},
							amplitude = 4f,
							wavelength = 45f
						)
					}
				}

				item("future-recurrent-content") {
					AnimatedVisibility(
						visible = showOutOfPeriodSubscriptions,
						enter = expandVertically(
							animationSpec = tween(300),
							expandFrom = Alignment.Top
						) + fadeIn(animationSpec = tween(300)),
						exit = shrinkVertically(
							animationSpec = tween(300),
							shrinkTowards = Alignment.Top
						) + fadeOut(animationSpec = tween(300))
					) {
						LazyRow(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(8.dp),
							contentPadding = androidx.compose.foundation.layout.PaddingValues(
								horizontal = 16.dp
							)
						) {
							itemsIndexed(
								items = futureRecurrentOutOfPeriod,
								key = { _, item -> "future-${item.transaction.id}" }
							) { _, item ->
								RecurrentTicketCard(
									title = item.transaction.comment,
									amountFormatted = currencyFormat.format(item.transaction.amount),
									nextChargeDate = prettyDate(
										item.nextChargeDate.atStartOfDay(),
										showTime = false,
										forceShowDate = false
									),
									frequencyLabel = item.transaction.recurrentFrequency?.name?.lowercase()
										?.replaceFirstChar { it.uppercase() },
									onClick = { selectedTransaction = item.transaction },
									modifier = Modifier.fillParentMaxWidth(0.45f)
								)
							}
						}
					}
				}
			}

			if (groupedPastTransactions.isNotEmpty()) {
				item("wavy-divider") {
					val interactionSource = remember { MutableInteractionSource() }
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.clickable(
								interactionSource = interactionSource,
								indication = null
							) {
								showPastPeriod = !showPastPeriod
							}
					) {
						WavyDivider(
							text = if (showPastPeriod) "Ocultar gastos del periodo pasado" else "Mostrar gastos del periodo pasado",
							amplitude = 4f,
							wavelength = 45f
						)
					}
				}
			}

			if (showPastPeriod) {
				groupedPastTransactions.forEach { (date, transactions) ->
					val isExpanded = date?.let { expandedDates.contains(it) } ?: false
					val dayTotal = transactions.sumOf { it.amount }

					item("past-date-$date") {
						HistoryDateDivider(
							date = date,
							isExpanded = isExpanded,
							onToggleClick = {
								date?.let { dateKey ->
									expandedDates = if (isExpanded) {
										expandedDates.minus(dateKey)
									} else {
										expandedDates.plus(dateKey)
									}
								}
							},
							totalAmount = dayTotal,
							currencyCode = currencyCode ?: "$"
						)
					}

					item("past-date-content-$date") {
						AnimatedVisibility(
							visible = isExpanded, enter = expandVertically(
								animationSpec = tween(300), expandFrom = Alignment.Top
							) + fadeIn(animationSpec = tween(300)), exit = shrinkVertically(
								animationSpec = tween(300), shrinkTowards = Alignment.Top
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

										val isBeingDeleted =
											transaction.id in deletingTransactionIds
										AnimatedVisibility(
											visible = !isBeingDeleted,
											enter = EnterTransition.None,
											exit = slideOutHorizontally(
												animationSpec = tween(durationMillis = 280),
												targetOffsetX = { fullWidth -> fullWidth }
											) + fadeOut(animationSpec = tween(durationMillis = 280))
										) {
											SwipeableExpenseItem(
												transaction = transaction,
												currencyFormat = currencyFormat,
												position = position,
												isBeingDeleted = isBeingDeleted,
												onDelete = {
													queueDeleteWithUndo(transaction)
												},
												onEdit = { editingTransaction = transaction },
												readOnly = readOnly,
												onClick = { selectedTransaction = transaction })
										}

										if (index < transactions.size - 1 && transaction.id !in deletingTransactionIds) {
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
			}

			item("spacer-bottom") {
				Spacer(modifier = Modifier.height(32.dp))
			}
		}

		if (uiState.transactions.isEmpty()) {
			NoTransactionsView(
				modifier = Modifier
					.align(Alignment.Center)
					.padding(32.dp)
			)
		}

		if (selectedTransaction != null) {
			isDismissingTransactionDialog = false
			val transaction = selectedTransaction!!
			val transactionDateText = transaction.date?.let { date ->
				prettyDate(date, showTime = true, forceHideDate = false, human = true)
			} ?: "Sin fecha"
			val recurrenceLabel = when (transaction.recurrentFrequency) {
				RecurrentFrequency.WEEKLY -> "Semanal"
				RecurrentFrequency.BIWEEKLY -> "Quincenal"
				RecurrentFrequency.MONTHLY -> "Mensual"
				null -> ""
			}

			val details = buildList {
				add("Descripción" to transaction.comment.ifEmpty { "Sin nombre" })
				add("Fecha" to transactionDateText)
				if (transaction.isRecurrent && recurrenceLabel.isNotEmpty()) {
					add("Frecuencia" to recurrenceLabel)
				}
				transaction.subscriptionDay?.let { day ->
					if (transaction.isRecurrent) {
						add("Día de cobro" to "Día $day")
					}
				}
				transaction.recurrentEndDate?.let { endDate ->
					if (transaction.isRecurrent) {
						add(
							"Fin recurrencia" to prettyDate(
								endDate, showTime = false, forceHideDate = false, human = true
							)
						)
					}
				}
			}

			AnimatedVisibility(
				visible = selectedTransaction != null,
				enter = EnterTransition.None,
				exit = ExitTransition.None
			) {
				var showDialogInnerContent by remember(transaction.id) { mutableStateOf(false) }
				LaunchedEffect(transaction.id) {
					showDialogInnerContent = false
					delay(90)
					showDialogInnerContent = true
				}

				Box(
					modifier = Modifier.fillMaxSize()
				) {
					Box(
						modifier = Modifier
							.fillMaxSize()
							.background(Color.Black.copy(alpha = 0.5f))
							.clickable(
								interactionSource = remember { MutableInteractionSource() },
								indication = null
							) {
								if (!isDismissingTransactionDialog && selectedTransaction != null) {
									isDismissingTransactionDialog = true
									selectedTransaction = null
								}
							})
					TicketView(
						backgroundColor = MaterialTheme.colorScheme.background,
						teethWidthDp = 20f,
						teethHeightDp = 4f,
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 24.dp)
							.align(Alignment.Center)
					) {
						TransactionDetailContent(
							transaction = transaction,
							isRecurrentExpense = transaction.isRecurrent,
							operationNumber = "#${transaction.id}",
							operationTime = transactionDateText,
							totalAmountText = currencyFormat.format(transaction.amount),
							details = details,
							onMarkAsPaid = if (transaction.isRecurrent) {
								{ selectedTransaction = null }
							} else null,
							onEdit = {
								selectedTransaction = null
								editingTransaction = transaction
							},
							onDelete = {
								selectedTransaction = null
								if (transaction.isRecurrent) {
									recurrentToDelete = transaction
									showDeleteRecurrentDialog = true
								} else {
									queueDeleteWithUndo(transaction)
								}
							},
							readOnly = readOnly
						)
					}
				}
			}
		}
	}

	if (editingTransaction != null) {
		val transaction = editingTransaction!!
		Dialog(
			onDismissRequest = { editingTransaction = null }, properties = DialogProperties(
				usePlatformDefaultWidth = false,
				dismissOnBackPress = true,
				dismissOnClickOutside = false
			)
		) {
			Surface(
				modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
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
						viewModel.processIntent(
							BudgetTransactionIntent.EditTransactionTapped(
								updatedTransaction
							)
						)
						onShowInfoSnackbar("${updatedTransaction.comment.ifEmpty { "Gasto" }} ha sido modificado")
						editingTransaction = null
					})
			}
		}
	}

	if (showDeleteRecurrentDialog && recurrentToDelete != null) {
		val transaction = recurrentToDelete!!
		AlertDialog(onDismissRequest = {
			showDeleteRecurrentDialog = false
			recurrentToDelete = null
		}, title = { Text("Eliminar gasto recurrente") }, text = {
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
		}, confirmButton = {
			Button(
				onClick = {
					viewModel.processIntent(BudgetTransactionIntent.DeleteTransactionTapped(transaction))
					showDeleteRecurrentDialog = false
					recurrentToDelete = null
				}, colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.error
				)
			) {
				Text("Eliminar")
			}
		}, dismissButton = {
			TextButton(
				onClick = {
					showDeleteRecurrentDialog = false
					recurrentToDelete = null
				}) {
				Text("Cancelar")
			}
		})
	}

	if (recurrentToEdit != null) {
		val transaction = recurrentToEdit!!
		Dialog(
			onDismissRequest = { recurrentToEdit = null }, properties = DialogProperties(
				usePlatformDefaultWidth = false,
				dismissOnBackPress = true,
				dismissOnClickOutside = false
			)
		) {
			Surface(
				modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
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
						viewModel.processIntent(
							BudgetTransactionIntent.EditTransactionTapped(
								updatedTransaction
							)
						)
						onShowInfoSnackbar("${updatedTransaction.comment.ifEmpty { "Gasto" }} ha sido modificado")
						recurrentToEdit = null
					})
			}
		}
	}
}

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

			var nextDate: LocalDate =
				today.withDayOfMonth(billingDay.coerceAtMost(today.lengthOfMonth()))

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

@Preview
@Composable
fun HistoryPreview() {
	MinusTheme {
		Column {

			HistoryDateDivider(
				date = LocalDate.now(),
				isExpanded = true,
				onToggleClick = { },
				totalAmount = BigDecimal("100.00"),
				currencyCode = "$"
			)

			SwipeableExpenseItem(
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
			ExpenseItem(
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
			ExpenseItem(
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
				currencyCode = "EUR"
			)
			val budgetState = BudgetState(
				remainingToday = BigDecimal("250.00"),
				totalSpentToday = BigDecimal("45.00"),
				dailyBudget = BigDecimal("26.66"),
				daysRemaining = 18,
				progress = 0.68f,
				isOverBudget = false,
				totalBudget = BigDecimal("800.00"),
				totalSpentInPeriod = BigDecimal("550.00")
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
						subscriptionDay = 12
					),
					nextChargeDate = LocalDate.now().plusDays(2),
					isInCurrentPeriod = true
				),
				UpcomingRecurrentItem(
					transaction = Transaction(
						id = 9002L,
						amount = BigDecimal("5.00"),
						comment = "Subscripción semanal sin nombre",
						date = LocalDateTime.now().minusDays(6),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.WEEKLY
					),
					nextChargeDate = LocalDate.now().plusDays(4),
					isInCurrentPeriod = true
				)
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
						subscriptionDay = 2
					),
					nextChargeDate = LocalDate.now().plusDays(20),
					isInCurrentPeriod = false
				),
				UpcomingRecurrentItem(
					transaction = Transaction(
						id = 9011L,
						amount = BigDecimal("14.99"),
						comment = "Music Pro",
						date = LocalDateTime.now().minusDays(25),
						isRecurrent = true,
						recurrentFrequency = RecurrentFrequency.BIWEEKLY
					),
					nextChargeDate = LocalDate.now().plusDays(28),
					isInCurrentPeriod = false
				)
			)

			val currentTx = listOf(
				Transaction(
					id = 1001L,
					amount = BigDecimal("23.40"),
					comment = "Supermercado",
					date = LocalDateTime.now().minusHours(2)
				),
				Transaction(
					id = 1002L,
					amount = BigDecimal("12.00"),
					comment = "Café",
					date = LocalDateTime.now().minusHours(5)
				)
			)

			val pastTx = listOf(
				Transaction(
					id = 2001L,
					amount = BigDecimal("70.00"),
					comment = "Combustible",
					date = LocalDateTime.now().minusDays(35)
				),
				Transaction(
					id = 2002L,
					amount = BigDecimal("31.50"),
					comment = "Comida",
					date = LocalDateTime.now().minusDays(36)
				)
			)

			LazyColumn(
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(bottom = 24.dp)
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
							budgetSettings.startDate.atStartOfDay(ZoneId.systemDefault())
								.toInstant()
						),
						finishDate = Date.from(
							budgetSettings.getPeriodEndDate().atStartOfDay(ZoneId.systemDefault())
								.toInstant()
						)
					)
				}

				item {
					LazyRow(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
					) {
						itemsIndexed(
							inPeriodRecurring,
							key = { _, item -> item.transaction.id }) { _, item ->
							RecurrentTicketCard(
								title = item.transaction.comment,
								amountFormatted = currencyFormat.format(item.transaction.amount),
								nextChargeDate = prettyDate(
									item.nextChargeDate.atStartOfDay(),
									showTime = false,
									forceShowDate = false
								),
								frequencyLabel = item.transaction.recurrentFrequency?.name?.lowercase()
									?.replaceFirstChar { it.uppercase() },
								modifier = Modifier.fillParentMaxWidth(0.45f)
							)
						}
					}
				}

				item {
					WavyDivider(
						text = "Mostrar subscripciones fuera del periodo",
						amplitude = 4f,
						wavelength = 45f
					)
				}
				item {
					LazyRow(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
					) {
						itemsIndexed(
							outOfPeriodRecurring,
							key = { _, item -> item.transaction.id }) { _, item ->
							RecurrentTicketCard(
								title = item.transaction.comment,
								amountFormatted = currencyFormat.format(item.transaction.amount),
								nextChargeDate = prettyDate(
									item.nextChargeDate.atStartOfDay(),
									showTime = false,
									forceShowDate = false
								),
								frequencyLabel = item.transaction.recurrentFrequency?.name?.lowercase()
									?.replaceFirstChar { it.uppercase() },
								modifier = Modifier.fillParentMaxWidth(0.45f)
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
						currencyCode = "€"
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
						}
					)
				}

				item {
					WavyDivider(
						text = "Mostrar gastos del periodo pasado",
						amplitude = 4f,
						wavelength = 45f
					)
				}
				item {
					HistoryDateDivider(
						date = LocalDate.now().minusDays(35),
						isExpanded = true,
						onToggleClick = {},
						totalAmount = pastTx.sumOf { it.amount }.toPlainString() as BigDecimal?,
						currencyCode = "€"
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
						}
					)
				}
			}
		}
	}
}

@Composable
private fun TransactionDetailContent(
	transaction: Transaction,
	isRecurrentExpense: Boolean,
	operationNumber: String,
	operationTime: String,
	totalAmountText: String,
	details: List<Pair<String, String>>,
	onMarkAsPaid: (() -> Unit)?,
	onEdit: () -> Unit,
	onDelete: () -> Unit,
	readOnly: Boolean
) {
	Column(
		modifier = Modifier.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Box(
			modifier = Modifier
				.background(Color.Black)
				.padding(vertical = 8.dp, horizontal = 18.dp)
		) {
			Text(
				text = if (isRecurrentExpense) "GASTO RECURRENTE" else "GASTO",
				color = Color.White,
				style = MaterialTheme.typography.labelLarge,
				fontWeight = FontWeight.Bold,
				fontSize = TextUnit(26f, TextUnitType.Sp),
				fontFamily = FontFamily.Monospace
			)
		}

		Text(
			text = "Num. de Operación: $operationNumber",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurface,
			textAlign = TextAlign.Center
		)

		HorizontalDivider()

		Text(
			text = "MONTO TOTAL",
			style = MaterialTheme.typography.labelLarge,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
		)

		Text(
			text = totalAmountText,
			style = MaterialTheme.typography.headlineLarge,
			color = MaterialTheme.colorScheme.error,
			fontWeight = FontWeight.Bold,
			textAlign = TextAlign.Center
		)

		HorizontalDivider()

		details.forEach { (label, value) ->
			Box(modifier = Modifier.fillMaxWidth()) {
				Text(
					text = label,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.align(Alignment.CenterStart)
				)
				Text(
					text = value,
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface,
					modifier = Modifier.align(Alignment.CenterEnd)
				)
			}
		}

		if (isRecurrentExpense && onMarkAsPaid != null) {
			Button(
				onClick = onMarkAsPaid, modifier = Modifier.fillMaxWidth()
			) {
				Text(text = "Marcar como pagado")
			}
		}

		Row(
			modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Button(
				onClick = onEdit, modifier = Modifier.weight(1f)
			) {
				Text("Editar")
			}

			if (!readOnly) {
				Button(
					onClick = onDelete,
					modifier = Modifier.weight(1f),
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.error,
						contentColor = MaterialTheme.colorScheme.onError
					)
				) {
					Text("Eliminar")
				}
			}
		}
	}
}

private fun getRecurringChargesInPeriod(
	transaction: Transaction, periodStart: LocalDate, periodEnd: LocalDate, today: LocalDate
): List<Transaction> {
	val frequency = transaction.recurrentFrequency ?: return emptyList()
	val originalDateTime = transaction.date ?: return emptyList()
	val startDate = originalDateTime.toLocalDate()
	val originalTime = originalDateTime.toLocalTime()
	val subscriptionEnd = transaction.recurrentEndDate?.toLocalDate() ?: periodEnd.plusMonths(1)

	val virtualTransactions = mutableListOf<Transaction>()
	var chargeDate = startDate

	while (!chargeDate.isAfter(subscriptionEnd)) {
		if (!chargeDate.isBefore(periodStart) && !chargeDate.isAfter(periodEnd) && !chargeDate.isAfter(
				today
			)
		) {
			virtualTransactions.add(
				transaction.copy(
					date = chargeDate.atTime(originalTime),
					id = transaction.id * 1000000 + chargeDate.toEpochDay()
				)
			)
		}

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
