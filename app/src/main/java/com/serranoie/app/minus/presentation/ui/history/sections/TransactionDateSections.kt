package com.serranoie.app.minus.presentation.ui.history.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.component.date.DayTotalItem
import com.serranoie.app.minus.presentation.ui.theme.component.date.HistoryDateDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableExpenseItem
import java.text.NumberFormat
import java.time.LocalDate
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import java.math.BigDecimal

@OptIn(ExperimentalSharedTransitionApi::class)
internal fun LazyListScope.transactionDateSections(
    groupedTransactions: Map<LocalDate?, List<Transaction>>,
    expandedDates: Set<LocalDate>,
    expandedTransactionId: Long?,
    deletingTransactionIds: Set<Long>,
    currencyCode: String,
    currencyFormat: NumberFormat,
    readOnly: Boolean,
    keyPrefix: String,
    onToggleDate: (LocalDate) -> Unit,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onMarkAsPaid: (Transaction) -> Unit = {},
    onClick: (Transaction) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    disableAnimations: Boolean = false,
    creditCardCutoffDay: Int? = null,
) {
    groupedTransactions.forEach { (date, transactions) ->
        val isExpanded = date?.let { expandedDates.contains(it) } ?: false
        val dayTotal = transactions.filter { it.amount > BigDecimal.ZERO }.sumOf { it.amount }

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
                                index,
                                transactions.lastIndex,
                                transactions.size
                            )
                            val isBeingDeleted = transaction.id in deletingTransactionIds
                            AnimatedVisibility(
                                visible = !isBeingDeleted,
                                enter = EnterTransition.None,
                                exit = if (!disableAnimations) {
                                    slideOutHorizontally(
                                        animationSpec = tween(durationMillis = 280),
                                        targetOffsetX = { fullWidth -> fullWidth },
                                    ) + fadeOut(animationSpec = tween(durationMillis = 280))
                                } else ExitTransition.None,
                            ) {
                                SwipeableExpenseItem(
                                    transaction = transaction,
                                    currencyFormat = currencyFormat,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    position = position,
                                    isBeingDeleted = isBeingDeleted,
                                    isExpanded = expandedTransactionId == transaction.id,
                                    onDelete = { onDelete(transaction) },
                                    onEdit = { onEdit(transaction) },
                                    onMarkAsPaid = { onMarkAsPaid(transaction) },
                                    readOnly = readOnly,
                                    disableAnimations = disableAnimations,
                                    onClick = { onClick(transaction) },
                                    creditCardCutoffDay = creditCardCutoffDay,
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

@PreviewLightDark
@Composable
private fun TransactionDateSectionsPreview() {
    val today = LocalDate.now()
    val tx = Transaction(
        id = 1L,
        amount = BigDecimal("45.50"),
        comment = "Groceries",
        date = today.atStartOfDay(),
        isDeleted = false,
    )
    MinusTheme {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            transactionDateSections(
                groupedTransactions = mapOf(today to listOf(tx)),
                expandedDates = setOf(today),
                expandedTransactionId = null,
                deletingTransactionIds = emptySet(),
                currencyCode = "USD",

                currencyFormat = NumberFormat.getCurrencyInstance(),
                readOnly = false,
                keyPrefix = "preview",
                onToggleDate = {},
                onDelete = {},
                onEdit = {},
                onClick = {},
                sharedTransitionScope = null,
                animatedVisibilityScope = null,
            )
        }
    }
}

