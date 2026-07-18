package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.titleLargeCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed
import com.serranoie.app.minus.presentation.util.prettyDate
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.time.LocalDateTime

@Composable
fun CreditTransactionsBottomSheet(
    transactions: List<Transaction>,
    totalOwed: BigDecimal,
    currency: String,
    onPayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currencyFormat = symbolOnlyCurrencyFormat(currency)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.credit_owed_label),
            style = MaterialTheme.typography.titleLargeCondensed.copy(fontWeight = FontWeight.Light)
        )
        Text(
            text = currencyFormat.format(totalOwed),
            style = MaterialTheme.typography.headlineLargeEmphasized,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                text = stringResource(R.string.mark_as_paid),
                style = MaterialTheme.typography.labelMediumEmphasized
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
            ), totalOwed = BigDecimal("177.50"), currency = "USD", onPayClick = {})
    }
}
