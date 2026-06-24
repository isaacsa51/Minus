package com.serranoie.app.minus.presentation.ui.history.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.ticket.RecurrentTicketCard
import com.serranoie.app.minus.presentation.util.prettyDate
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate

@Composable
internal fun RecurrentItemsContent(
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
                    index,
                    item,
                    paddedListItemPosition(index, items.lastIndex, items.size)
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

@PreviewLightDark
@Composable
private fun RecurrentItemsContentPreview() {
    val today = LocalDate.now()
    val sampleItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("15.00"),
            comment = "Netflix",
            date = today.atStartOfDay(),
            isDeleted = false,
        ),
        nextChargeDate = today.plusDays(5),
        isInCurrentPeriod = true,
    )
    MinusTheme {
        RecurrentItemsContent(
            items = listOf(
                sampleItem,
                sampleItem.copy(transaction = sampleItem.transaction.copy(id = 2L))
            ),
            recurrentPaymentsViewMode = RecurrentPaymentsViewMode.HORIZONTAL_LIST,
            currencyFormat = NumberFormat.getCurrencyInstance(),
            verticalItem = { _, _, _ -> },
            horizontalKeyPrefix = "preview",
            onClick = {},
        )
    }
}
