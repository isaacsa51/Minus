package com.serranoie.app.minus.presentation.ui.theme.component.expense

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.SwipeActions
import com.serranoie.app.minus.presentation.ui.theme.component.SwipeActionsConfig
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

private const val SWIPE_ACTION_THRESHOLD = 0.25f

@Composable
fun SwipeableExpenseItem(
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
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 4.dp
        )

        PaddedListItemPosition.Last -> RoundedCornerShape(
            bottomStart = 16.dp,
            bottomEnd = 16.dp,
            topStart = 4.dp,
            topEnd = 4.dp
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
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 4.dp,
            bottomEnd = 4.dp
        )

        PaddedListItemPosition.Last -> RoundedCornerShape(
            bottomStart = 16.dp,
            bottomEnd = 16.dp,
            topStart = 4.dp,
            topEnd = 4.dp
        )

        PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)
        PaddedListItemPosition.Middle -> RoundedCornerShape(4.dp)
    }

    Surface(
        shape = shape,
        color = if (isOutOfPeriod) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
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
                isOutOfPeriod = isOutOfPeriod,
                onClick = onClick
            )
        }
    }
}

@Preview
@Composable
private fun SwipeableExpenseItemPreview() {
    MinusTheme {
        SwipeableExpenseItem(
            transaction = Transaction(
                id = 1L,
                amount = java.math.BigDecimal("150.50"),
                comment = "Compra en supermercado",
                date = LocalDateTime.now(),
                isDeleted = false,
                isRecurrent = false
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
            position = PaddedListItemPosition.Single,
            onDelete = {},
            onEdit = {},
            readOnly = false,
            isBeingDeleted = false,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun SwipeableUpcomingRecurrentItemPreview() {
    MinusTheme {
        SwipeableUpcomingRecurrentItem(
            item = UpcomingRecurrentItem(
                transaction = Transaction(
                    id = 1L,
                    amount = java.math.BigDecimal("200.00"),
                    comment = "Netflix Subscription",
                    date = LocalDateTime.now(),
                    isDeleted = false,
                    isRecurrent = true
                ),
                nextChargeDate = LocalDate.now().plusDays(3),
                isInCurrentPeriod = true
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
            position = PaddedListItemPosition.Single,
            isOutOfPeriod = false,
            onDelete = {},
            onEdit = {},
            onClick = {}
        )
    }
}
