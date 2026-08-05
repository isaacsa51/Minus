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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.ui.theme.component.WavyDivider
import com.serranoie.app.minus.presentation.ui.theme.component.date.DayTotalItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableUpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import java.time.LocalDate
import java.text.NumberFormat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import java.math.BigDecimal

@OptIn(ExperimentalSharedTransitionApi::class)
internal fun LazyListScope.futureRecurrentSection(
    futureRecurrentOutOfPeriod: List<UpcomingRecurrentItem>,
    showOutOfPeriodSubscriptions: Boolean,
    expandedTransactionId: Long?,
    onToggleShowOutOfPeriodSubscriptions: () -> Unit,
    recurrentPaymentsViewMode: RecurrentPaymentsViewMode,
    currencyFormat: NumberFormat,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onMarkAsPaid: (Transaction) -> Unit = {},
    onClick: (Transaction) -> Unit,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    creditCardCutoffDay: Int? = null,
) {
    if (futureRecurrentOutOfPeriod.isEmpty()) return

    val futureTotal = futureRecurrentOutOfPeriod.sumOf { it.transaction.amount }

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
                    stringResource(R.string.hide_subscriptions_outside_period)
                } else {
                    stringResource(R.string.show_subscriptions_outside_period)
                },
                horizontalPadding = 0.dp,
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
            Column {
                RecurrentItemsContent(
                    items = futureRecurrentOutOfPeriod,
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
                    horizontalKeyPrefix = "future",
                    onClick = onClick,
                )

                if (recurrentPaymentsViewMode == RecurrentPaymentsViewMode.VERTICAL_LIST) {
                    Spacer(modifier = Modifier.height(8.dp))
                    DayTotalItem(
                        total = futureTotal,
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
private fun FutureRecurrentSectionPreview() {
    val today = LocalDate.now()
    val sampleItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("99.00"),
            comment = "Spotify",
            date = today.atStartOfDay(),
            isDeleted = false,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today.plusMonths(2),
        isInCurrentPeriod = false,
    )
    MinusTheme {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Previews for shared transitions are complex, usually no-op or mock
            item("preview") {
                Text("Future Recurrent Section Preview")
            }

        }
    }
}
