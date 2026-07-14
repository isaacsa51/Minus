package com.serranoie.app.minus.presentation.ui.history.dialogs

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleSmallCondensed
import java.math.BigDecimal
import java.time.LocalDateTime

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TransactionDetailTicketCard(
    modifier: Modifier = Modifier,
    transaction: Transaction,
    totalAmountText: String,
    details: List<Pair<String, String>>,
    onMarkAsPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    readOnly: Boolean,
    onClick: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        details.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmallCondensed.copy(fontWeight = FontWeight.W200),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMediumCondensed.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1.8f)
                        .then(
                            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    val key = when (label) {
                                        stringResource(R.string.description) -> "comment_${transaction.id}"
                                        stringResource(R.string.date) -> "time_${transaction.id}"
                                        else -> null
                                    }
                                    if (key != null) {
                                        Modifier.sharedElement(
                                            rememberSharedContentState(key = key),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    } else Modifier
                                }
                            } else Modifier
                        ),
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        HorizontalDivider()

        Text(
            text = stringResource(R.string.ticket_total_amount),
            style = MaterialTheme.typography.bodyMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Text(
            text = totalAmountText,
            style = MaterialTheme.typography.headlineMediumEmphasized,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.then(
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

        HorizontalDivider()

        Spacer(modifier = Modifier.height(4.dp))

        if (transaction.isRecurrent) {
            Button(
                onClick = onMarkAsPaid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.mark_as_paid),
                    style = MaterialTheme.typography.labelSmallEmphasized,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onEdit,
            ) {
                Text(
                    stringResource(R.string.edit),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!readOnly) {
                IconButton(
                    onClick = onDelete,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionDetailTicketCardPreview() {
    MinusTheme {
        TransactionDetailTicketCard(
            transaction = Transaction(
                id = 1L,
                amount = BigDecimal("42.50"),
                comment = "Lunch with team",
                date = LocalDateTime.of(2026, 1, 15, 12, 30),
                periodId = 7L,
                isRecurrent = false
            ),
            totalAmountText = "$42.50",
            details = listOf(
                "Comment" to "Lunch with team",
                "Date" to "Jan 15, 2026",
                "Time" to "12:30 PM"
            ),
            onMarkAsPaid = {},
            onEdit = {},
            onDelete = {},
            readOnly = false
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionDetailTicketCardRecurrentPreview() {
    MinusTheme {
        TransactionDetailTicketCard(
            transaction = Transaction(
                id = 2L,
                amount = BigDecimal("15.00"),
                comment = "Netflix",
                date = LocalDateTime.of(2026, 1, 15, 10, 0),
                periodId = 7L,
                isRecurrent = true
            ),
            totalAmountText = "$15.00",
            details = listOf(
                "Comment" to "Netflix",
                "Frequency" to "Monthly",
                "Next payment" to "Feb 15, 2026"
            ),
            onMarkAsPaid = {},
            onEdit = {},
            onDelete = {},
            readOnly = false
        )
    }
}
