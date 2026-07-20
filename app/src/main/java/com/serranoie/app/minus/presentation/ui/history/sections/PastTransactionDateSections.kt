package com.serranoie.app.minus.presentation.ui.history.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.date.HistoryDateDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableExpenseItem
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate

@OptIn(ExperimentalSharedTransitionApi::class)
internal fun LazyListScope.pastTransactionDateSections(
    showPastPeriod: Boolean,
    groupedPastTransactions: Map<LocalDate?, List<Transaction>>,
    expandedDates: Set<LocalDate>,
    expandedTransactionId: Long?,
    deletingTransactionIds: Set<Long>,
    currencyCode: String,
    currencyFormat: NumberFormat,
    readOnly: Boolean,
    onToggleDate: (LocalDate) -> Unit,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onMarkAsPaid: (Transaction) -> Unit = {},
    onClick: (Transaction) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    creditCardCutoffDay: Int? = null,
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
                                index,
                                transactions.lastIndex,
                                transactions.size
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
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    position = position,
                                    isBeingDeleted = isBeingDeleted,
                                    isExpanded = expandedTransactionId == transaction.id,
                                    onDelete = { onDelete(transaction) },
                                    onEdit = { onEdit(transaction) },
                                    onMarkAsPaid = { onMarkAsPaid(transaction) },
                                    readOnly = readOnly,
                                    onClick = { onClick(transaction) },
                                    creditCardCutoffDay = creditCardCutoffDay,
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


@PreviewLightDark
@Composable
private fun PastTransactionDateSectionsPreview() {
    val today = LocalDate.now()
    val pastDate = today.minusDays(20)
    val tx = Transaction(
        id = 1L,
        amount = BigDecimal("30.00"),
        comment = "Lunch",
        date = pastDate.atStartOfDay(),
        isDeleted = false,
    )
    MinusTheme {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Previews for shared transitions are complex, usually no-op or mock
            item("preview") {
                Text("Past Transaction Date Sections Preview")
            }

        }
    }
}
