package com.serranoie.app.minus.presentation.ui.history.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.ui.theme.component.date.DayTotalItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.RecurrentPaymentsDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableUpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import java.text.NumberFormat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import java.math.BigDecimal
import java.time.LocalDate


@OptIn(ExperimentalSharedTransitionApi::class)
internal fun LazyListScope.currentPeriodRecurrentSection(
    upcomingRecurrentInPeriod: List<UpcomingRecurrentItem>,
    showUpcomingRecurrentInPeriod: Boolean,
    expandedTransactionId: Long?,
    onToggleShowUpcomingRecurrentInPeriod: () -> Unit,
    recurrentPaymentsViewMode: RecurrentPaymentsViewMode,
    currencyCode: String,
    currencyFormat: NumberFormat,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onMarkAsPaid: (Transaction) -> Unit = {},
    onClick: (Transaction) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    creditCardCutoffDay: Int? = null,
) {
    if (upcomingRecurrentInPeriod.isEmpty()) return

    val recurrentTotal = upcomingRecurrentInPeriod.sumOf { it.transaction.amount }

    item("upcoming-recurrent-toggle") {
        RecurrentPaymentsDivider(
            title = stringResource(R.string.recurrent_payments_divider_title_current_period),
            isExpanded = showUpcomingRecurrentInPeriod,
            onToggleClick = onToggleShowUpcomingRecurrentInPeriod,
            itemCount = upcomingRecurrentInPeriod.size,
            totalAmount = recurrentTotal,
            currencyCode = currencyCode,
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
            Column {
                RecurrentItemsContent(
                    items = upcomingRecurrentInPeriod,
                    recurrentPaymentsViewMode = recurrentPaymentsViewMode,
                    currencyFormat = currencyFormat,
                    verticalItem = { _, item, position ->
                        SwipeableUpcomingRecurrentItem(
                            item = item,
                            currencyFormat = currencyFormat,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            position = position,
                            isExpanded = expandedTransactionId == item.transaction.id,
                            onDelete = { onDelete(item.transaction) },
                            onEdit = { onEdit(item.transaction) },
                            onMarkAsPaid = { onMarkAsPaid(item.transaction) },
                            onClick = { onClick(item.transaction) },
                            creditCardCutoffDay = creditCardCutoffDay,
                        )
                    },
                    horizontalKeyPrefix = "upcoming",
                    onClick = onClick,
                )

                if (recurrentPaymentsViewMode == RecurrentPaymentsViewMode.VERTICAL_LIST) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DayTotalItem(
                        total = recurrentTotal,
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
private fun CurrentPeriodRecurrentSectionPreview() {
    val today = LocalDate.now()
    val sampleItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("15.00"),
            comment = "Netflix",
            date = today.atStartOfDay(),
            isDeleted = false,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today.plusDays(5),
        isInCurrentPeriod = true,
    )
    MinusTheme {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            currentPeriodRecurrentSection(
                upcomingRecurrentInPeriod = listOf(sampleItem),
                showUpcomingRecurrentInPeriod = true,
                expandedTransactionId = null,
                onToggleShowUpcomingRecurrentInPeriod = {},
                recurrentPaymentsViewMode = RecurrentPaymentsViewMode.HORIZONTAL_LIST,
                currencyCode = "USD",
                currencyFormat = NumberFormat.getCurrencyInstance(),
                onDelete = {},
                onEdit = {},
                onClick = {},
                sharedTransitionScope = null,
                animatedVisibilityScope = null,
            )
        }
    }
}

