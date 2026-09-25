package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.censor
import java.math.BigDecimal
import java.text.NumberFormat

@Composable
internal fun SubscriptionsHeroCard(
    monthlyTotal: BigDecimal,
    activeCount: Int,
    daysUntilNextCharge: Long?,
    currencyFormatted: String,
    periodBudgetTotal: BigDecimal,
    periodCommittedTotal: BigDecimal,
    budgetFormatted: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.subscriptions_monthly_commitment_label),
                style = MaterialTheme.typography.labelMediumCondensed,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    modifier = Modifier.censor(),
                    text = currencyFormatted,
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
                Text(
                    text = stringResource(R.string.subscriptions_per_month_suffix),
                    style = MaterialTheme.typography.bodyMediumCondensed.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))

            if (periodBudgetTotal > BigDecimal.ZERO) {
                val ratio =
                    (periodCommittedTotal.toDouble() / periodBudgetTotal.toDouble()).coerceIn(
                        0.0,
                        1.0
                    )
                val percentFormat = remember {
                    NumberFormat.getPercentInstance().apply { maximumFractionDigits = 0 }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.subscriptions_budget_committed,
                        percentFormat.format(ratio),
                        budgetFormatted,
                    ),
                    style = MaterialTheme.typography.labelMediumCondensed,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Comfortably under budget")
@Composable
private fun SubscriptionsHeroCardPreview() {
    MinusTheme {
        SubscriptionsHeroCard(
            monthlyTotal = BigDecimal("76.97"),
            activeCount = 3,
            daysUntilNextCharge = 2L,
            currencyFormatted = "$76.97",
            periodBudgetTotal = BigDecimal("200.00"),
            periodCommittedTotal = BigDecimal("76.97"),
            budgetFormatted = "$200.00",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Over budget")
@Composable
private fun SubscriptionsHeroCardOverBudgetPreview() {
    MinusTheme {
        SubscriptionsHeroCard(
            monthlyTotal = BigDecimal("249.99"),
            activeCount = 9,
            daysUntilNextCharge = 0L,
            currencyFormatted = "$249.99",
            periodBudgetTotal = BigDecimal("200.00"),
            periodCommittedTotal = BigDecimal("238.50"),
            budgetFormatted = "$200.00",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "No budget configured")
@Composable
private fun SubscriptionsHeroCardNoBudgetPreview() {
    MinusTheme {
        SubscriptionsHeroCard(
            monthlyTotal = BigDecimal("12.99"),
            activeCount = 1,
            daysUntilNextCharge = null,
            currencyFormatted = "$12.99",
            periodBudgetTotal = BigDecimal.ZERO,
            periodCommittedTotal = BigDecimal.ZERO,
            budgetFormatted = "$0.00",
            modifier = Modifier.padding(16.dp),
        )
    }
}
