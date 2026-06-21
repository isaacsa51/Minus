package com.serranoie.app.minus.presentation.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.WavyDivider
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetDisplay
import com.serranoie.app.minus.presentation.ui.theme.component.date.DayTotalItem
import com.serranoie.app.minus.presentation.ui.theme.component.date.HistoryDateDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.RecurrentPaymentsDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableExpenseItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableUpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.ticket.RecurrentTicketCard
import com.serranoie.app.minus.presentation.util.prettyDate
import logcat.logcat
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

internal fun LazyListScope.budgetDisplaySection(
    budgetState: BudgetState?,
    budgetSettings: BudgetSettings?,
    currencyCode: String,
) {
    item("budget-display") {
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
            finishDate = finishDate,
        )
    }
}

internal fun LazyListScope.currentPeriodRecurrentSection(
    upcomingRecurrentInPeriod: List<UpcomingRecurrentItem>,
    showUpcomingRecurrentInPeriod: Boolean,
    onToggleShowUpcomingRecurrentInPeriod: () -> Unit,
    recurrentPaymentsViewMode: RecurrentPaymentsViewMode,
    currencyFormat: NumberFormat,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onClick: (Transaction) -> Unit,
) {
    if (upcomingRecurrentInPeriod.isEmpty()) return

    item("upcoming-recurrent-toggle") {
        RecurrentPaymentsDivider(
            title = stringResource(R.string.recurrent_payments_divider_title_current_period),
            isExpanded = showUpcomingRecurrentInPeriod,
            onToggleClick = onToggleShowUpcomingRecurrentInPeriod,
            itemCount = upcomingRecurrentInPeriod.size,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    item("upcoming-recurrent-content") {
        AnimatedVisibility(
            visible = showUpcomingRecurrentInPeriod,
            enter = expandVertically(
                animationSpec = tween(300),
                expandFrom = Alignment.Top,
            ) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(
                animationSpec = tween(300),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(animationSpec = tween(300)),
        ) {
            RecurrentItemsContent(
                items = upcomingRecurrentInPeriod,
                recurrentPaymentsViewMode = recurrentPaymentsViewMode,
                currencyFormat = currencyFormat,
                verticalItem = { _, item, position ->
                    SwipeableUpcomingRecurrentItem(
                        item = item,
                        currencyFormat = currencyFormat,
                        position = position,
                        onDelete = { onDelete(item.transaction) },
                        onEdit = { onEdit(item.transaction) },
                        onClick = { onClick(item.transaction) },
                    )
                },
                horizontalKeyPrefix = "upcoming",
                onClick = onClick,
            )
        }
    }
}

internal fun LazyListScope.transactionDateSections(
    groupedTransactions: Map<LocalDate?, List<Transaction>>,
    expandedDates: Set<LocalDate>,
    deletingTransactionIds: Set<Long>,
    currencyCode: String,
    currencyFormat: NumberFormat,
    readOnly: Boolean,
    keyPrefix: String,
    onToggleDate: (LocalDate) -> Unit,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onClick: (Transaction) -> Unit,
) {
    groupedTransactions.forEach { (date, transactions) ->
        val isExpanded = date?.let { expandedDates.contains(it) } ?: false
        val dayTotal = transactions.sumOf { it.amount }

        item("$keyPrefix-date-$date") {
            HistoryDateDivider(
                date = date,
                isExpanded = isExpanded,
                onToggleClick = {
                    date?.let(onToggleDate)
                },
                totalAmount = dayTotal,
                currencyCode = currencyCode,
            )
        }

        item("$keyPrefix-date-content-$date") {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300),
                    expandFrom = Alignment.Top,
                ) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(
                    animationSpec = tween(300),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(animationSpec = tween(300)),
            ) {
                Column {
                    transactions.forEachIndexed { index, transaction ->
                        key(transaction.id) {
                            val position = paddedListItemPosition(
                                index, transactions.lastIndex, transactions.size
                            )
                            val isBeingDeleted = transaction.id in deletingTransactionIds
                            AnimatedVisibility(
                                visible = !isBeingDeleted,
                                enter = EnterTransition.None,
                                exit = slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 280),
                                    targetOffsetX = { fullWidth -> fullWidth },
                                ) + fadeOut(animationSpec = tween(durationMillis = 280)),
                            ) {
                                SwipeableExpenseItem(
                                    transaction = transaction,
                                    currencyFormat = currencyFormat,
                                    position = position,
                                    isBeingDeleted = isBeingDeleted,
                                    onDelete = { onDelete(transaction) },
                                    onEdit = { onEdit(transaction) },
                                    readOnly = readOnly,
                                    onClick = { onClick(transaction) },
                                )
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
							.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

internal fun LazyListScope.futureRecurrentSection(
    futureRecurrentOutOfPeriod: List<UpcomingRecurrentItem>,
    showOutOfPeriodSubscriptions: Boolean,
    onToggleShowOutOfPeriodSubscriptions: () -> Unit,
    recurrentPaymentsViewMode: RecurrentPaymentsViewMode,
    currencyFormat: NumberFormat,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onClick: (Transaction) -> Unit,
) {
    if (futureRecurrentOutOfPeriod.isEmpty()) return

    item("future-recurrent-toggle") {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
	            .fillMaxWidth()
	            .clickable(
					interactionSource = interactionSource,
					indication = null,
				) {
					onToggleShowOutOfPeriodSubscriptions()
				},
        ) {
            WavyDivider(
                text = if (showOutOfPeriodSubscriptions) {
                    "Ocultar subscripciones fuera del periodo"
                } else {
                    "Mostrar subscripciones fuera del periodo"
                },
                horizontalPadding = 0.dp,
                amplitude = 4f,
                wavelength = 45f,
            )
        }
    }

    item("future-recurrent-content") {
        AnimatedVisibility(
            visible = showOutOfPeriodSubscriptions,
            enter = expandVertically(
                animationSpec = tween(300),
                expandFrom = Alignment.Top,
            ) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(
                animationSpec = tween(300),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(animationSpec = tween(300)),
        ) {
            RecurrentItemsContent(
                items = futureRecurrentOutOfPeriod,
                recurrentPaymentsViewMode = recurrentPaymentsViewMode,
                currencyFormat = currencyFormat,
                verticalItem = { _, item, position ->
                    SwipeableUpcomingRecurrentItem(
                        item = item,
                        currencyFormat = currencyFormat,
                        position = position,
                        onDelete = { onDelete(item.transaction) },
                        onEdit = { onEdit(item.transaction) },
                        onClick = { onClick(item.transaction) },
                    )
                },
                horizontalKeyPrefix = "future",
                onClick = onClick,
            )
        }
    }
}

internal fun LazyListScope.pastPeriodToggleSection(
    groupedPastTransactions: Map<LocalDate?, List<Transaction>>,
    showPastPeriod: Boolean,
    onToggleShowPastPeriod: () -> Unit,
) {
    if (groupedPastTransactions.isEmpty()) return

    item("wavy-divider") {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
	            .fillMaxWidth()
	            .clickable(
					interactionSource = interactionSource,
					indication = null,
				) {
					onToggleShowPastPeriod()
				},
        ) {
            WavyDivider(
                text = if (showPastPeriod) "Ocultar gastos del periodo pasado" else "Mostrar gastos del periodo pasado",
                horizontalPadding = 0.dp,
                amplitude = 4f,
                wavelength = 45f,
            )
        }
    }
}

internal fun LazyListScope.pastTransactionDateSections(
    showPastPeriod: Boolean,
    groupedPastTransactions: Map<LocalDate?, List<Transaction>>,
    expandedDates: Set<LocalDate>,
    deletingTransactionIds: Set<Long>,
    currencyCode: String,
    currencyFormat: NumberFormat,
    readOnly: Boolean,
    onToggleDate: (LocalDate) -> Unit,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onClick: (Transaction) -> Unit,
) {
    if (!showPastPeriod) return

    groupedPastTransactions.forEach { (date, transactions) ->
        val isExpanded = date?.let { expandedDates.contains(it) } ?: false
        val dayTotal = transactions.sumOf { it.amount }

        item("past-date-$date") {
            HistoryDateDivider(
                date = date,
                isExpanded = isExpanded,
                onToggleClick = {
                    date?.let(onToggleDate)
                },
                totalAmount = dayTotal,
                currencyCode = currencyCode,
            )
        }

        item("past-date-content-$date") {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300),
                    expandFrom = Alignment.Top,
                ) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(
                    animationSpec = tween(300),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(animationSpec = tween(300)),
            ) {
                Column {
                    transactions.forEachIndexed { index, transaction ->
                        key(transaction.id) {
                            val position = paddedListItemPosition(
                                index, transactions.lastIndex, transactions.size
                            )
                            val isBeingDeleted = transaction.id in deletingTransactionIds
                            AnimatedVisibility(
                                visible = !isBeingDeleted,
                                enter = EnterTransition.None,
                                exit = slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 280),
                                    targetOffsetX = { fullWidth -> fullWidth },
                                ) + fadeOut(animationSpec = tween(durationMillis = 280)),
                            ) {
                                SwipeableExpenseItem(
                                    transaction = transaction,
                                    currencyFormat = currencyFormat,
                                    position = position,
                                    isBeingDeleted = isBeingDeleted,
                                    onDelete = { onDelete(transaction) },
                                    onEdit = { onEdit(transaction) },
                                    readOnly = readOnly,
                                    onClick = { onClick(transaction) },
                                )
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
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Total del día: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Text(
                            text = totalText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurrentItemsContent(
    items: List<UpcomingRecurrentItem>,
    recurrentPaymentsViewMode: RecurrentPaymentsViewMode,
    currencyFormat: NumberFormat,
    verticalItem: @Composable (index: Int, item: UpcomingRecurrentItem, position: PaddedListItemPosition) -> Unit,
    horizontalKeyPrefix: String,
    onClick: (Transaction) -> Unit,
) {
    if (recurrentPaymentsViewMode == RecurrentPaymentsViewMode.VERTICAL_LIST) {
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                verticalItem(
                    index, item, paddedListItemPosition(index, items.lastIndex, items.size)
                )
                if (index < items.lastIndex) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> "$horizontalKeyPrefix-${item.transaction.id}" },
            ) { _, item ->
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
                    onClick = { onClick(item.transaction) },
                    modifier = Modifier.fillParentMaxWidth(0.45f),
                )
            }
        }
    }
}

private fun paddedListItemPosition(
    index: Int,
    lastIndex: Int,
    size: Int,
): PaddedListItemPosition = when {
    size == 1 -> PaddedListItemPosition.Single
    index == 0 -> PaddedListItemPosition.First
    index == lastIndex -> PaddedListItemPosition.Last
    else -> PaddedListItemPosition.Middle
}
