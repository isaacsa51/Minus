package com.serranoie.app.minus.presentation.ui.theme.component.expense

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SwipeableExpenseItem(
    transaction: Transaction,
    currencyFormat: NumberFormat,
    position: PaddedListItemPosition,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    readOnly: Boolean,
    disableAnimations: Boolean = false,
    isBeingDeleted: Boolean = false,
    isExpanded: Boolean = false,
    onClick: () -> Unit = {},
    onMarkAsPaid: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
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
                modifier = Modifier,
                transaction = transaction,
                currencyFormat = currencyFormat,
                position = position,
                isExpanded = isExpanded,
                onClick = onClick,
                onEdit = onEdit,
                onDelete = onDelete,
                onMarkAsPaid = onMarkAsPaid,
                readOnly = readOnly,
                disableAnimations = disableAnimations,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
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
        ) { _ ->
            ExpenseItem(
                modifier = Modifier,
                transaction = transaction,
                currencyFormat = currencyFormat,
                position = position,
                isExpanded = isExpanded,
                onClick = onClick,
                onEdit = onEdit,
                onDelete = onDelete,
                onMarkAsPaid = onMarkAsPaid,
                readOnly = readOnly,
                disableAnimations = disableAnimations,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }

    }

}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SwipeableUpcomingRecurrentItem(
    item: UpcomingRecurrentItem,
    currencyFormat: NumberFormat,
    position: PaddedListItemPosition,
    isOutOfPeriod: Boolean = false,
    isExpanded: Boolean = false,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onMarkAsPaid: () -> Unit = {},
    onClick: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
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

    SwipeActions(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        background = if (isOutOfPeriod) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
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
    ) { _ ->
        UpcomingRecurrentItemRow(
            item = item,
            currencyFormat = currencyFormat,
            position = position,
            isOutOfPeriod = isOutOfPeriod,
            isExpanded = isExpanded,
            onClick = onClick,
            onEdit = onEdit,
            onDelete = onDelete,
            onMarkAsPaid = onMarkAsPaid,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
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
            onClick = {},
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
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
            onClick = {},
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
        )
    }
}

