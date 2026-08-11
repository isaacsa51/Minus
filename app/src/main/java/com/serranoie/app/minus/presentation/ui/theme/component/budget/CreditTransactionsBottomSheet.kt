package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.editor.dialogs.CreditCutoffDayDialog
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelLargeCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.font.format.prettyDate
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun CreditTransactionsBottomSheet(
    transactions: List<Transaction>,
    totalOwed: BigDecimal,
    currency: String,
    onPayClick: () -> Unit,
    onPayTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    creditCardCutoffDay: Int? = null,
    onCutoffDayChanged: (Int) -> Unit = {},
) {
    val currencyFormat = symbolOnlyCurrencyFormat(currency)
    var showCutoffDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CreditCardVisualization(
            totalOwed = totalOwed,
            currency = currency,
            cutoffDay = creditCardCutoffDay,
            onEditClick = { showCutoffDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

        if (showCutoffDialog) {
            CreditCutoffDayDialog(
                initialDay = creditCardCutoffDay ?: 15,
                onDismiss = { showCutoffDialog = false },
                onConfirm = { day ->
                    onCutoffDayChanged(day)
                    showCutoffDialog = false
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions) { tx ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tx.comment.ifEmpty { stringResource(R.string.no_name) },
                            style = MaterialTheme.typography.titleMediumCondensed,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = prettyDate(tx.date, showTime = true, human = true),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = currencyFormat.format(tx.amount),
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { onPayTransactionClick(tx.id) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.mark_as_paid),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onPayClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text(
                text = stringResource(R.string.mark_all_as_paid),
                style = MaterialTheme.typography.labelMediumEmphasized
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CreditCardVisualization(
    totalOwed: BigDecimal,
    currency: String,
    cutoffDay: Int?,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currencyFormat = symbolOnlyCurrencyFormat(currency)
    val today = LocalDate.now()
    val cutoffDate = runCatching { today.withDayOfMonth(cutoffDay ?: 1) }.getOrElse {
        today.withDayOfMonth(today.lengthOfMonth())
    }
    val nextCutoff = if (today.isAfter(cutoffDate)) {
        val nextMonth = today.plusMonths(1)
        runCatching { nextMonth.withDayOfMonth(cutoffDay ?: 1) }.getOrElse {
            nextMonth.withDayOfMonth(nextMonth.lengthOfMonth())
        }
    } else cutoffDate

    val cutoffDateText = nextCutoff.format(DateTimeFormatter.ofPattern("dd MMMM"))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2.5f)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(18.dp)
    ) {
        Text(
            text = "Credit Card",
            style = MaterialTheme.typography.displayLargeEmphasized,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier
                .align(Alignment.Center)
                .alpha(0.05f),
            textAlign = TextAlign.Center,
            softWrap = true,
            lineHeight = 64.sp
        )

        Surface(
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.TopStart),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.credit_owed_label),
                style = MaterialTheme.typography.labelLargeCondensed,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = currencyFormat.format(totalOwed),
                style = MaterialTheme.typography.headlineLargeEmphasized,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.1f),
                shape = CircleShape
            ) {
                Text(
                    text = "Cutoff: $cutoffDateText",
                    style = MaterialTheme.typography.bodySmallEmphasized,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        IconButton(
            onClick = onEditClick,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreditTransactionsBottomSheetPreview() {
    MinusTheme {
        CreditTransactionsBottomSheet(
            transactions = listOf(
                Transaction(
                    id = 1,
                    amount = BigDecimal("45.00"),
                    comment = "Gas",
                    date = LocalDateTime.now(),
                    isCredit = true
                ), Transaction(
                    id = 2,
                    amount = BigDecimal("12.50"),
                    comment = "Coffee",
                    date = LocalDateTime.now().minusHours(2),
                    isCredit = true
                ), Transaction(
                    id = 3,
                    amount = BigDecimal("120.00"),
                    comment = "Groceries",
                    date = LocalDateTime.now().minusDays(1),
                    isCredit = true
                )
            ), totalOwed = BigDecimal("177.50"), currency = "USD", onPayClick = {}, onPayTransactionClick = {}, creditCardCutoffDay = 15)
    }
}
