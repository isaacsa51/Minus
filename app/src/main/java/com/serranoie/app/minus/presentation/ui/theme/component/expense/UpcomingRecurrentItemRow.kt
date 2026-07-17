package com.serranoie.app.minus.presentation.ui.theme.component.expense

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.calculateDaysToCutoff
import com.serranoie.app.minus.presentation.util.prettyDate
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale

data class UpcomingRecurrentItem(
    val transaction: Transaction,
    val nextChargeDate: LocalDate,
    val isInCurrentPeriod: Boolean,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun UpcomingRecurrentItemRow(
    item: UpcomingRecurrentItem,
    currencyFormat: NumberFormat,
    position: PaddedListItemPosition,
    isOutOfPeriod: Boolean = false,
    isExpanded: Boolean = false,
    onClick: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onMarkAsPaid: () -> Unit = {},
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
    customShape: androidx.compose.ui.graphics.Shape? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    creditCardCutoffDay: Int? = null,
) {
    val transaction = item.transaction
    val nextChargeDate = item.nextChargeDate

    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), nextChargeDate)
    val relativeChargeDateText = when {
        daysUntil == 0L -> stringResource(R.string.upcoming_recurrent_today)
        daysUntil == 1L -> stringResource(R.string.upcoming_recurrent_tomorrow)
        daysUntil < 7 -> stringResource(R.string.upcoming_recurrent_in_days, daysUntil)
        else -> stringResource(R.string.upcoming_recurrent_in_weeks, daysUntil / 7)
    }

    val daysText = when {
        transaction.isCredit && creditCardCutoffDay != null -> {
            val daysToCutoff = calculateDaysToCutoff(creditCardCutoffDay)
            if (daysToCutoff == 0) {
                stringResource(R.string.credit_cutoff_subtitle_today, relativeChargeDateText)
            } else {
                stringResource(R.string.credit_cutoff_subtitle, relativeChargeDateText, daysToCutoff)
            }
        }

        else -> relativeChargeDateText
    }

    val alpha = if (isOutOfPeriod) 0.6f else 1f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 200))
    ) {
        CustomPaddedListItem(
            onClick = onClick,
            position = position,
            background = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            customShape = customShape
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(150, delayMillis = 50)) togetherWith
                            fadeOut(animationSpec = tween(50)))
                },
                label = "header_content",
                modifier = Modifier.weight(1f)
            ) { expanded ->
                if (expanded) {
                    ExpenseItemExpandedContent(
                        transaction = transaction,
                        currencyFormat = currencyFormat,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onMarkAsPaid = onMarkAsPaid,
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
                                style = MaterialTheme.typography.titleMediumCondensed.copy(
                                    fontStyle = if (transaction.isCredit) FontStyle.Italic else FontStyle.Normal
                                ),
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
                            Text(
                                text = daysText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f * alpha)
                            )
                        }

                        Text(
                            text = currencyFormat.format(transaction.amount),
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = if (isOutOfPeriod) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
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

@Preview
@Composable
private fun UpcomingRecurrentItemRowPreview() {
    MinusTheme {
        UpcomingRecurrentItemRow(
            item = UpcomingRecurrentItem(
                transaction = Transaction(
                    id = 1L,
                    amount = java.math.BigDecimal("200.00"),
                    comment = "Netflix Subscription",
                    date = LocalDateTime.now(),
                    isDeleted = false,
                    isRecurrent = true,
                ),
                nextChargeDate = LocalDate.now().plusDays(3),
                isInCurrentPeriod = true
            ),
            currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
            position = PaddedListItemPosition.Single,
            isOutOfPeriod = false,
            onClick = {},
            sharedTransitionScope = null,
            animatedVisibilityScope = null,
        )
    }
}
