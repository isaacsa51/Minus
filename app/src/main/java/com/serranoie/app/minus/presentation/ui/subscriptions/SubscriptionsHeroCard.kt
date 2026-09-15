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
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
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
                    text = currencyFormatted,
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.subscriptions_per_month_suffix),
                    style = MaterialTheme.typography.bodyMediumCondensed,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))

            if (periodBudgetTotal > BigDecimal.ZERO) {
                val ratio = (periodCommittedTotal.toDouble() / periodBudgetTotal.toDouble()).coerceIn(0.0, 1.0)
                val percentFormat = remember { NumberFormat.getPercentInstance().apply { maximumFractionDigits = 0 } }

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
