package com.serranoie.app.minus.presentation.ui.theme.component.expense

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.labelLargeCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.prettyDate
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ExpenseItem(
    transaction: Transaction,
    currencyFormat: NumberFormat,
    position: PaddedListItemPosition = PaddedListItemPosition.Middle,
    isExpanded: Boolean = false,
    onClick: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onMarkAsPaid: () -> Unit = {},
    readOnly: Boolean = false,
    disableAnimations: Boolean = false,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (!disableAnimations) Modifier.animateContentSize(animationSpec = tween(durationMillis = 200)) else Modifier)
    ) {
        CustomPaddedListItem(
            onClick = onClick,
            position = position,
            background = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    fadeIn(tween(200, delayMillis = 100)) togetherWith fadeOut(tween(100))
                },
                label = "expense_item_header",
                modifier = Modifier.weight(1f)
            ) { expanded ->
                if (expanded) {
                    ExpenseItemExpandedContent(
                        transaction = transaction,
                        currencyFormat = currencyFormat,
                        onMarkAsPaid = onMarkAsPaid,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        readOnly = readOnly,
                        onClick = onClick,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = transaction.comment.ifEmpty { stringResource(R.string.expense_item_unnamed_expense) },
                                style = MaterialTheme.typography.titleMediumCondensed,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                    with(sharedTransitionScope) {
                                        Modifier.sharedElement(
                                            rememberSharedContentState(key = "comment_${transaction.id}"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }
                                } else Modifier
                            )
                            val timeText = prettyDate(
                                date = transaction.date, showTime = true, forceHideDate = true
                            )
                            val subtitle = if (transaction.isRecurrent) {
                                stringResource(R.string.expense_item_recurrent_subtitle_format, timeText)
                            } else {
                                timeText
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                    with(sharedTransitionScope) {
                                        Modifier.sharedElement(
                                            rememberSharedContentState(key = "time_${transaction.id}"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }
                                } else Modifier
                            )
                        }

                        Text(
                            text = currencyFormat.format(transaction.amount),
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .censor()
                                .then(
                                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                        with(sharedTransitionScope) {
                                            Modifier.sharedElement(
                                                rememberSharedContentState(key = "amount_${transaction.id}"),
                                                animatedVisibilityScope = animatedVisibilityScope
                                            )
                                        }
                                    } else Modifier
                                )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpenseItemExpandedPreview() {
    MinusTheme {
        ExpenseItem(
            modifier = Modifier,
            transaction = Transaction(
                id = 1L,
                amount = java.math.BigDecimal("150.50"),
                comment = "Compra en supermercado",
                date = LocalDateTime.now(),
                isDeleted = false,
                isRecurrent = false,
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
            position = PaddedListItemPosition.Single,
            isExpanded = true,
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpenseItemPreview() {
	MinusTheme {
		ExpenseItem(
            modifier = Modifier,
            transaction = Transaction(
                id = 1L,
                amount = java.math.BigDecimal("150.50"),
                comment = "Compra en supermercado",
                date = LocalDateTime.now(),
                isDeleted = false,
                isRecurrent = false,
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
            position = PaddedListItemPosition.Single,
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
        )
	}
}
