package com.serranoie.app.minus.presentation.history


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoneyOffCsred
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
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
import com.serranoie.app.minus.presentation.ui.theme.component.expense.ExpenseDetailContent
import com.serranoie.app.minus.presentation.ui.theme.component.expense.ExpenseItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.NoTransactionsView
import com.serranoie.app.minus.presentation.ui.theme.component.expense.RecurrentPaymentsDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableExpenseItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableUpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItemRow
import com.serranoie.app.minus.presentation.ui.theme.component.WavyDivider
import com.serranoie.app.minus.presentation.ui.theme.component.date.HistoryDateDivider
import com.serranoie.app.minus.presentation.ui.theme.component.ticket.RecurrentTicketCard
import com.serranoie.app.minus.presentation.ui.theme.component.ticket.TicketView
import com.serranoie.app.minus.presentation.ui.theme.component.ticket.TransactionTicketPopup
import com.serranoie.app.minus.presentation.util.prettyDate
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlinx.coroutines.delay


private const val SWIPE_ACTION_THRESHOLD = 0.5f

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

	val isAtEndOfList: Boolean = remember(scrollState.canScrollForward, scrollState.layoutInfo.visibleItemsInfo) {
		!scrollState.canScrollForward && scrollState.layoutInfo.visibleItemsInfo.lastOrNull() != null
	}

	LaunchedEffect(isAtEndOfList) {
		viewModel.processIntent(BudgetUiIntent.SetLockSwipeable(!isAtEndOfList))
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

	var expandedUpcomingRecurrent by remember { mutableStateOf(true) }
	var expandedFutureRecurrent by remember { mutableStateOf(false) }

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
		currentPeriodTransactions, budgetStartDate, budgetEndDate
	) {
		val today = LocalDate.now()

		val withVirtualRecurrent = currentPeriodTransactions.flatMap { transaction ->
			if (transaction.isRecurrent && !transaction.isDeleted) {
				val charges =
					getRecurringChargesInPeriod(transaction, budgetStartDate, budgetEndDate, today)
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

	LaunchedEffect(groupedCurrentTransactions.keys, readOnly) {
		val sortedDates = groupedCurrentTransactions.keys.filterNotNull().sortedDescending()
		val defaultExpanded = if (readOnly) {
			sortedDates.take(3).toSet()
		} else {
			sortedDates.toSet()
		}
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
						visible = expandedUpcomingRecurrent, enter = expandVertically(
							animationSpec = tween(300), expandFrom = Alignment.Top
						) + fadeIn(animationSpec = tween(300)), exit = shrinkVertically(
							animationSpec = tween(300), shrinkTowards = Alignment.Top
						) + fadeOut(animationSpec = tween(300))
					) {
						LazyRow(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(8.dp),
							contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
						) {
							itemsIndexed(
								items = upcomingRecurrentInPeriod,
								key = { _, item -> "upcoming-${item.transaction.id}" }
							) { _, item ->
								RecurrentTicketCard(
									title = item.transaction.comment.ifEmpty { "Pago recurrente" },
									amountFormatted = currencyFormat.format(item.transaction.amount),
									nextChargeDate = prettyDate(item.nextChargeDate.atStartOfDay(), showTime = false, forceShowDate = false),
									frequencyLabel = item.transaction.recurrentFrequency?.name?.lowercase()?.replaceFirstChar { it.uppercase() },
									onClick = { selectedTransaction = item.transaction },
									modifier = Modifier.fillParentMaxWidth(0.45f)
								)
							}
						}
					}
				}

				item("spacer-after-upcoming") {
					Spacer(modifier = Modifier.height(16.dp))
				}
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
						totalAmount = dayTotal.toPlainString(),
						currencyCode = currencyFormat.currency?.symbol ?: "$"
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
						visible = expandedFutureRecurrent, enter = expandVertically(
							animationSpec = tween(300), expandFrom = Alignment.Top
						) + fadeIn(animationSpec = tween(300)), exit = shrinkVertically(
							animationSpec = tween(300), shrinkTowards = Alignment.Top
						) + fadeOut(animationSpec = tween(300))
					) {
						LazyRow(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(8.dp),
							contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
						) {
							itemsIndexed(
								items = futureRecurrentOutOfPeriod,
								key = { _, item -> "future-${item.transaction.id}" }
							) { _, item ->
								RecurrentTicketCard(
									title = item.transaction.comment.ifEmpty { "Pago recurrente" },
									amountFormatted = currencyFormat.format(item.transaction.amount),
									nextChargeDate = prettyDate(item.nextChargeDate.atStartOfDay(), showTime = false, forceShowDate = false),
									frequencyLabel = item.transaction.recurrentFrequency?.name?.lowercase()?.replaceFirstChar { it.uppercase() },
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
						totalAmount = dayTotal.toPlainString(),
						currencyCode = currencyFormat.currency?.symbol ?: "$"
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

		// Transaction Detail Popup
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
							BudgetUiIntent.EditTransactionTapped(
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
					viewModel.processIntent(BudgetUiIntent.DeleteTransactionTapped(transaction))
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
							BudgetUiIntent.EditTransactionTapped(
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

data class UpcomingRecurrentItem(
	val transaction: Transaction, val nextChargeDate: LocalDate, val isInCurrentPeriod: Boolean
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

@Composable
fun RecurrentPaymentsDivider(
	title: String,
	isExpanded: Boolean,
	onToggleClick: () -> Unit,
	itemCount: Int,
	isSecondary: Boolean = false
) {
	val interactionSource = remember { MutableInteractionSource() }
	val color = if (isSecondary) MaterialTheme.colorScheme.onSurfaceVariant
	else MaterialTheme.colorScheme.primary

	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(
				onClick = onToggleClick, interactionSource = interactionSource, indication = null
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
				text = title, style = MaterialTheme.typography.labelMedium, color = color
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
	isOutOfPeriod: Boolean = false,
	onClick: () -> Unit = {}
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
		shape = shape, color = if (isOutOfPeriod) MaterialTheme.colorScheme.surfaceVariant
		else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()
	) {
		CustomPaddedListItem(
			onClick = onClick,
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
					color = if (daysUntil <= 3 && !isOutOfPeriod) MaterialTheme.colorScheme.primary
					else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * alpha)
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
fun SwipeableUpcomingRecurrentItem(
	item: UpcomingRecurrentItem,
	currencyFormat: NumberFormat,
	position: PaddedListItemPosition,
	isOutOfPeriod: Boolean = false,
	onDelete: () -> Unit,
	onEdit: () -> Unit,
	onClick: () -> Unit = {}
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
		shape = shape, color = if (isOutOfPeriod) MaterialTheme.colorScheme.surfaceVariant
		else MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()
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
				isOutOfPeriod = isOutOfPeriod,
				onClick = onClick
			)
		}
	}
}

@Composable
private fun SwipeableExpenseItem(
	transaction: Transaction,
	currencyFormat: NumberFormat,
	position: PaddedListItemPosition,
	onDelete: () -> Unit,
	onEdit: () -> Unit,
	readOnly: Boolean,
	isBeingDeleted: Boolean = false,
	onClick: () -> Unit = {}
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
		Surface(
			shape = shape,
			color = MaterialTheme.colorScheme.surfaceContainer,
			modifier = Modifier.fillMaxWidth()
		) {
			ExpenseItem(
				transaction = transaction,
				currencyFormat = currencyFormat,
				position = position,
				onClick = onClick
			)
		}
	} else {
		Surface(
			shape = shape,
			color = MaterialTheme.colorScheme.surfaceContainer,
			modifier = Modifier.fillMaxWidth()
		) {
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
				ExpenseItem(
					transaction = transaction,
					currencyFormat = currencyFormat,
					position = position,
					onClick = onClick
				)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpenseItem(
	transaction: Transaction,
	currencyFormat: NumberFormat,
	position: PaddedListItemPosition = PaddedListItemPosition.Middle,
	onClick: () -> Unit = {}
) {
	CustomPaddedListItem(
		onClick = onClick,
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
				date = transaction.date, showTime = true, forceHideDate = true
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
			imageVector = Icons.Rounded.MoneyOffCsred,
			contentDescription = null,
			modifier = Modifier.size(64.dp),
			tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
		)
		Spacer(modifier = Modifier.height(16.dp))
		Text(
			text = "Sin gastos registrados",
			style = MaterialTheme.typography.titleMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Spacer(modifier = Modifier.height(8.dp))
		Text(
			text = "Agrega un gasto para empezar",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
		)
	}
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
				totalAmount = "100.00",
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
					), nextChargeDate = LocalDate.now().plusDays(2), isInCurrentPeriod = true
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
					), nextChargeDate = LocalDate.now().plusDays(15), isInCurrentPeriod = false
				),
				currencyFormat = NumberFormat.getCurrencyInstance(),
				position = PaddedListItemPosition.Single,
				isOutOfPeriod = true
			)
		}
	}
}

/**
 * Content for the "fake" dialog that displays transaction details.
 */
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

/**
 * Generate virtual transactions for a recurring expense that charges within the given period.
 * This creates "virtual" transaction copies for each billing date so they appear in history.
 */
private fun getRecurringChargesInPeriod(
	transaction: Transaction, periodStart: LocalDate, periodEnd: LocalDate, today: LocalDate
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
		if (!chargeDate.isBefore(periodStart) && !chargeDate.isAfter(periodEnd) && !chargeDate.isAfter(
				today
			)
		) {
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
