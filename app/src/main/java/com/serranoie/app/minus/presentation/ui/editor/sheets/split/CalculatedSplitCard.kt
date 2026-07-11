package com.serranoie.app.minus.presentation.ui.editor.sheets.split

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.censor
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

const val BUDGET_PERIOD_CALCULATED_CARD_TAG = "BudgetPeriodSheet.CalculatedCard"

private fun BudgetPeriod.periodBlockDays(): Int = when (this) {
    BudgetPeriod.DAILY -> 1
    BudgetPeriod.WEEKLY -> 7
    BudgetPeriod.BIWEEKLY -> 14
    BudgetPeriod.MONTHLY -> 30
}

@Composable
fun CalculatedSplitCard(
    periodCache: BudgetPeriod,
    allocation: BigDecimal,
    splitMode: BudgetSplitMode,
    currencyFormat: NumberFormat,
    totalBudget: BigDecimal = BigDecimal.ZERO,
    remaining: BigDecimal = BigDecimal.ZERO,
    totalDays: Int = 0,
    daysRemaining: Int = 0,
    modifier: Modifier = Modifier,
) {
    val formattedAmount = currencyFormat.format(allocation)
    val periodBlockDays = periodCache.periodBlockDays()
    val periodWord = stringResource(
        when (periodCache) {
            BudgetPeriod.DAILY -> R.string.budget_split_period_daily
            BudgetPeriod.WEEKLY -> R.string.budget_split_period_weekly
            BudgetPeriod.BIWEEKLY -> R.string.budget_split_period_biweekly
            BudgetPeriod.MONTHLY -> R.string.budget_split_period_monthly
        }
    )

    val briefText = when (splitMode) {
        BudgetSplitMode.STATIC -> {
            val blockCount = if (periodBlockDays > 0) {
                (totalDays / periodBlockDays).coerceAtLeast(1)
            } else 1
            stringResource(
                R.string.budget_split_helper_static_brief,
                currencyFormat.format(totalBudget),
                blockCount,
                periodWord,
            )
        }

        BudgetSplitMode.DYNAMIC -> {
            val blockCount = blocksRemaining(daysRemaining, periodBlockDays)
            stringResource(
                R.string.budget_split_helper_dynamic_brief,
                currencyFormat.format(remaining),
                blockCount,
                periodWord,
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(BUDGET_PERIOD_CALCULATED_CARD_TAG),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.budget_split_calculated_amount),
                style = MaterialTheme.typography.labelMediumCondensed,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.censor(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = briefText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalculatedSplitCardPreview() {
    MinusTheme {
        CalculatedSplitCard(
            periodCache = BudgetPeriod.MONTHLY,
            allocation = BigDecimal("22000.00"),
            splitMode = BudgetSplitMode.STATIC,
            currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
            totalBudget = BigDecimal("22000.00"),
            totalDays = 30,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CalculatedSplitCardBiweeklyDynamicPreview() {
    MinusTheme {
        CalculatedSplitCard(
            periodCache = BudgetPeriod.BIWEEKLY,
            allocation = BigDecimal("5000.00"),
            splitMode = BudgetSplitMode.DYNAMIC,
            currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
            remaining = BigDecimal("15000.00"),
            daysRemaining = 30,
            modifier = Modifier.padding(16.dp)
        )
    }
}
