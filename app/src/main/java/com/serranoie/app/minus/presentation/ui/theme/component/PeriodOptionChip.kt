@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.presentation.onboarding.availablePeriodsFor
import com.serranoie.app.minus.presentation.onboarding.budgetForPeriod
import com.serranoie.app.minus.presentation.onboarding.label
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import java.math.BigDecimal

/**
 * Single period option chip using Card for selection.
 */
@Composable
fun PeriodOptionChip(
	period: BudgetPeriod,
	budgetPreview: BigDecimal?,
	currencyCode: String = "USD",
	isSelected: Boolean,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val currencyFormat = remember(currencyCode) {
		com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat(currencyCode)
	}

	val cardColors = if (isSelected) {
		CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.primaryContainer,
			contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
		)
	} else {
		CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceVariant,
			contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
		)
	}

	Card(
		onClick = onClick,
		modifier = modifier,
		colors = cardColors,
	) {
		Column(
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.Center,
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 12.dp, vertical = 8.dp),
		) {
			Text(
				text = period.label(),
				style = MaterialTheme.typography.labelMedium,
				fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
			)
			if (budgetPreview != null) {
				Text(
					text = currencyFormat.format(budgetPreview),
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					style = MaterialTheme.typography.bodySmall,
				)
			}
		}
	}
}

@Composable
fun PeriodOptionGroup(
	totalBudget: BigDecimal,
	totalDays: Int,
	currencyCode: String = "USD",
	selectedPeriod: BudgetPeriod,
	onPeriodSelected: (BudgetPeriod) -> Unit,
	modifier: Modifier = Modifier,
) {
	val availablePeriods = availablePeriodsFor(totalDays)

	Row(
		modifier = modifier.fillMaxWidth(),
		horizontalArrangement = Arrangement.spacedBy(8.dp),
	) {
		availablePeriods.forEach { period ->
			val budgetPreview = budgetForPeriod(totalBudget, totalDays, period)
			val isSelected = period == selectedPeriod

			PeriodOptionChip(
				period = period,
				budgetPreview = budgetPreview,
				currencyCode = currencyCode,
				isSelected = isSelected,
				onClick = { onPeriodSelected(period) },
				modifier = Modifier.weight(1f),
			)
		}
	}
}

@Composable
private fun PeriodButtonContent(
	isSelected: Boolean,
	period: BudgetPeriod,
	budgetPreview: java.math.BigDecimal,
	currencyFormat: java.text.NumberFormat,
) {
	Column(
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
	) {
		if (isSelected) {
			Icon(
				imageVector = Icons.Default.Check,
				contentDescription = null,
				modifier = Modifier.size(16.dp),
			)
			Spacer(Modifier.size(4.dp))
		}
		Text(
			text = period.label(),
			style = MaterialTheme.typography.labelMedium,
			fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
		)
		Text(
			text = currencyFormat.format(budgetPreview),
			style = MaterialTheme.typography.bodySmall,
		)
	}
}

@Preview(name = "Period Group — 7 days", showBackground = true)
@Composable
private fun PreviewPeriodGroup7Days() {
	MinusTheme {
		CompositionLocalProvider(
			LocalBottomSheetScrollState provides BottomSheetScrollState(0.dp)
		) {
			PeriodOptionGroup(
				totalBudget = BigDecimal("500"),
				totalDays = 7,
				selectedPeriod = BudgetPeriod.DAILY,
				onPeriodSelected = {},
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
			)
		}
	}
}

@Preview(name = "Period Group — 30 days", showBackground = true)
@Composable
private fun PreviewPeriodGroup30Days() {
	MinusTheme {
		CompositionLocalProvider(
			LocalBottomSheetScrollState provides BottomSheetScrollState(0.dp)
		) {
			PeriodOptionGroup(
				totalBudget = BigDecimal("15000"),
				totalDays = 30,
				selectedPeriod = BudgetPeriod.BIWEEKLY,
				onPeriodSelected = {},
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
			)
		}
	}
}
