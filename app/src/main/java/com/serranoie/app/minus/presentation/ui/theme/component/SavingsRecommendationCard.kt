package com.serranoie.app.minus.presentation.ui.theme.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.font.format.formatCurrencySymbolOnly
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * Recommendation card based on the 50/30/20 rule (or any custom needs/wants/
 * savings split the user configured in Settings).
 *
 * The split percentages, the bar's "safe zone" threshold, and the per-period
 * savings target all come from [preferences]. Defaults to the classic
 * 50/30/20 rule.
 *
 * [recurringInPeriod] should contain every recurring charge that falls inside
 * the current budget period — both already-paid ones and upcoming ones still
 * scheduled. [oneTimeSpends] are the discretionary (non-recurring) transactions
 * already paid in the period.
 */
@Composable
fun SavingsRecommendationCard(
    modifier: Modifier = Modifier,
    budget: BigDecimal,
    recurringInPeriod: List<Transaction> = emptyList(),
    oneTimeSpends: List<Transaction> = emptyList(),
    currency: String = "MXN",
    preferences: SavingsPreferences = SavingsPreferences.DEFAULT,
) {
    val recurrentSpent = recurringInPeriod.sumOf { it.amount }
    val variableSpent = oneTimeSpends.sumOf { it.amount }
    val totalSpent = recurrentSpent.add(variableSpent)
    val savings = budget.subtract(totalSpent).max(BigDecimal.ZERO)

    val safeBudget = if (budget <= BigDecimal.ZERO) BigDecimal.ONE else budget

    val savingsPct =
        savings.divide(safeBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
    val recurrentPct =
        recurrentSpent.divide(safeBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
    val variablePct =
        variableSpent.divide(safeBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
    val projectedPerPeriod =
        preferences.projectedPerPeriod() ?: budget.multiply(BigDecimal(preferences.savingsPct))
            .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)

    val idealSavingsPerPeriod = budget.multiply(BigDecimal(preferences.savingsPct))
        .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    val projectedSavingsSixMonths = idealSavingsPerPeriod.multiply(BigDecimal(6))

    val spendingCeiling = preferences.spendingCeilingPct

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.savings_recommendation_title),
                    style = MaterialTheme.typography.bodySmallCondensed,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = stringResource(
                    R.string.savings_recommendation_method_brief,
                    preferences.needsPct,
                    preferences.wantsPct,
                    preferences.savingsPct,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)
            )

            if (projectedPerPeriod != null && preferences.savingsGoalAmount != null && preferences.savingsGoalMonths != null) {
                Text(
                    text = stringResource(
                        R.string.savings_recommendation_goal_format,
                        formatCurrencySymbolOnly(preferences.savingsGoalAmount, currency),
                        preferences.savingsGoalMonths,
                        formatCurrencySymbolOnly(projectedPerPeriod, currency),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                )
            } else {
                Text(
                    text = stringResource(R.string.savings_recommendation_six_month_example_intro),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                )
                Text(
                    text = stringResource(
                        R.string.savings_recommendation_six_month_example_equation,
                        formatCurrencySymbolOnly(idealSavingsPerPeriod, currency),
                        formatCurrencySymbolOnly(projectedSavingsSixMonths, currency)
                    ),
                    style = MaterialTheme.typography.labelSmallCondensed,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.savings_recommendation_current_savings_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$savingsPct%",
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (savingsPct >= preferences.savingsPct) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }

            LinearSavingsBar(
                recurrentPct = recurrentPct,
                variablePct = variablePct,
                spendingCeilingPct = spendingCeiling,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = stringResource(
                        R.string.savings_recommendation_available_format,
                        formatCurrencySymbolOnly(savings, currency)
                    ),
                    style = MaterialTheme.typography.labelSmallCondensed.copy(fontWeight = FontWeight.Light),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(horizontalAlignment = Alignment.End) {
                    val idealLabel = projectedPerPeriod ?: idealSavingsPerPeriod
                    Text(
                        text = stringResource(
                            R.string.savings_recommendation_ideal_savings_format,
                            formatCurrencySymbolOnly(idealLabel, currency)
                        ), style = MaterialTheme.typography.labelSmallCondensed.copy(
                            fontWeight = FontWeight.Light,
                            textDecoration = if (savingsPct < preferences.savingsPct) TextDecoration.LineThrough
                            else TextDecoration.None
                        ), color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (savingsPct < preferences.savingsPct) {
                        Text(
                            text = stringResource(
                                R.string.savings_recommendation_current_savings_format,
                                formatCurrencySymbolOnly(savings, currency)
                            ),
                            style = MaterialTheme.typography.labelSmallCondensed.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RecommendationItem(
                label = stringResource(R.string.savings_recommendation_recurrent_expenses_label),
                value = formatCurrencySymbolOnly(recurrentSpent, currency),
                percentage = recurrentPct,
                target = preferences.needsPct,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            RecommendationItem(
                label = stringResource(R.string.savings_recommendation_one_time_expenses_label),
                value = formatCurrencySymbolOnly(variableSpent, currency),
                percentage = variablePct,
                target = preferences.wantsPct,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun LinearSavingsBar(
    recurrentPct: Int,
    variablePct: Int,
    spendingCeilingPct: Int,
    modifier: Modifier = Modifier,
) {
    val totalSpentPct = recurrentPct + variablePct
    val ceiling = spendingCeilingPct.coerceIn(0, 100)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val recSafe = recurrentPct.coerceAtMost(ceiling).toFloat()
            val varSafe = if (recurrentPct < ceiling) {
                variablePct.coerceAtMost(ceiling - recurrentPct).toFloat()
            } else 0f
            val gapSafe = (ceiling - (recSafe + varSafe)).coerceAtLeast(0f)

            val invasionBudget = (100 - ceiling).coerceAtLeast(0)
            val totalInvasion = (totalSpentPct - ceiling).coerceIn(0, invasionBudget).toFloat()
            val safeSavings = (invasionBudget - totalInvasion).coerceAtLeast(0f)

            if (recSafe > 0) {
                Box(
                    Modifier
                        .weight(recSafe)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            if (varSafe > 0) {
                Box(
                    Modifier
                        .weight(varSafe)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            if (gapSafe > 0) {
                Spacer(Modifier.weight(gapSafe))
            }

            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
            )

            if (totalInvasion > 0) {
                Box(
                    Modifier
                        .weight(totalInvasion)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.error)
                )
            }
            if (safeSavings > 0) {
                Box(
                    Modifier
                        .weight(safeSavings)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                )
            }
        }
    }
}

@Composable
private fun RecommendationItem(
    label: String, value: String, percentage: Int, target: Int, color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "$value ($percentage%)",
            style = MaterialTheme.typography.bodyMediumCondensed,
            fontWeight = FontWeight.Bold,
            color = if (percentage > target) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SavingsRecommendationPreview() {
    MinusTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SavingsRecommendationCard(
                budget = BigDecimal("20000"),
                recurringInPeriod = listOf(
                    Transaction(
                        amount = BigDecimal("6000"),
                        isRecurrent = true,
                        comment = "Renta",
                        date = LocalDateTime.now()
                    ),
                ),
                oneTimeSpends = listOf(
                    Transaction(
                        amount = BigDecimal("4000"),
                        isRecurrent = false,
                        comment = "Súper",
                        date = LocalDateTime.now()
                    ),
                ),
            )
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun SavingsRecommendationNoRecurrentPreview() {
    MinusTheme {
        Surface {

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                SavingsRecommendationCard(
                    budget = BigDecimal("12000"),
                    recurringInPeriod = emptyList(),
                    oneTimeSpends = listOf(
                        Transaction(
                            amount = BigDecimal("6000"),
                            isRecurrent = false,
                            comment = "Salidas",
                            date = LocalDateTime.now()
                        ),
                        Transaction(
                            amount = BigDecimal("4000"),
                            isRecurrent = false,
                            comment = "Súper",
                            date = LocalDateTime.now()
                        ),
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Custom split + goal")
@Composable
private fun SavingsRecommendationCustomSplitPreview() {
    MinusTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SavingsRecommendationCard(
                budget = BigDecimal("20000"),
                recurringInPeriod = listOf(
                    Transaction(
                        amount = BigDecimal("8000"),
                        isRecurrent = true,
                        comment = "Renta",
                        date = LocalDateTime.now()
                    ),
                ),
                oneTimeSpends = listOf(
                    Transaction(
                        amount = BigDecimal("4000"),
                        isRecurrent = false,
                        comment = "Súper",
                        date = LocalDateTime.now()
                    ),
                ),
                preferences = SavingsPreferences(
                    needsPct = 40,
                    wantsPct = 30,
                    savingsPct = 30,
                    savingsGoalAmount = BigDecimal("60000"),
                    savingsGoalMonths = 12,
                ),
            )
        }
    }
}
