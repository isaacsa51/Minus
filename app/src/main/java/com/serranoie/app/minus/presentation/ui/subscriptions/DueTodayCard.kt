package com.serranoie.app.minus.presentation.ui.subscriptions

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate

@Composable
internal fun DueTodayCard(
    item: UpcomingRecurrentItem,
    currencyFormat: NumberFormat,
    onConfirmPaid: () -> Unit,
    onSkip: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubscriptionItem(
        item = item,
        currencyFormat = currencyFormat,
        onMarkAsPaid = onConfirmPaid,
        onSkip = onSkip,
        onEdit = onEdit,
        onDelete = onDelete,
        modifier = modifier,
        containerColor = MinusTheme.budgetStatus.notGood.copy(alpha = 0.16f),
        borderStroke = BorderStroke(1.dp, MinusTheme.budgetStatus.notGood.copy(alpha = 0.5f)),
        accentColor = MinusTheme.budgetStatus.notGood,
    )
}

@Preview(showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(showBackground = true)
@Composable
private fun DueTodayCardPreview() {
    val today = LocalDate.now()
    val sampleItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("16.99"),
            comment = "Netflix",
            date = today.minusMonths(1).atStartOfDay(),
            isDeleted = false,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today,
        isInCurrentPeriod = true,
    )

    MinusTheme {
        DueTodayCard(
            item = sampleItem,
            currencyFormat = NumberFormat.getCurrencyInstance(),
            onConfirmPaid = {},
            onSkip = {},
            onEdit = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
