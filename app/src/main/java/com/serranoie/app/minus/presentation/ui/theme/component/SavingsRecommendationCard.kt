package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.formatCurrencySymbolOnly
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * A simplified and helpful recommendation card based on the 50/30/20 rule.
 * 50% Needs (Recurrent), 30% Wants (Variable), 20% Savings.
 */
@Composable
fun SavingsRecommendationCard(
	modifier: Modifier = Modifier,
	budget: BigDecimal,
	spends: List<Transaction>,
	currency: String = "MXN",
) {
	val totalSpent = spends.sumOf { it.amount }
	val recurrentSpent = spends.filter { it.isRecurrent }.sumOf { it.amount }
	val variableSpent = totalSpent.subtract(recurrentSpent)
	val savings = budget.subtract(totalSpent).max(BigDecimal.ZERO)

	val safeBudget = if (budget <= BigDecimal.ZERO) BigDecimal.ONE else budget
	
	val savingsPct = savings.divide(safeBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
	val recurrentPct = recurrentSpent.divide(safeBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
	val variablePct = variableSpent.divide(safeBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()

	Column(
		modifier = modifier
			.fillMaxWidth()
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Icon(
				imageVector = Icons.Outlined.Analytics,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.primary,
				modifier = Modifier.size(20.dp)
			)
			Spacer(modifier = Modifier.width(8.dp))
			Text(
				text = "Regla 50/30/20",
				style = MaterialTheme.typography.titleSmall,
				fontWeight = FontWeight.Bold,
				color = MaterialTheme.colorScheme.primary
			)
		}

		Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Text(
					text = "Ahorro actual",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface
				)
				Text(
					text = "$savingsPct%",
					style = MaterialTheme.typography.titleLarge,
					fontWeight = FontWeight.Bold,
					color = if (savingsPct >= 20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
				)
			}
			LinearProgressIndicator(
				progress = { (savingsPct / 100f).coerceIn(0f, 1f) },
				modifier = Modifier.fillMaxWidth().height(8.dp),
				strokeCap = StrokeCap.Round,
				trackColor = MaterialTheme.colorScheme.surfaceVariant,
				color = if (savingsPct >= 20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
			)
			Text(
				text = "Meta: 20% (${formatCurrencySymbolOnly(budget.multiply(BigDecimal("0.2")), currency)})",
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}

		HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

		Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
			RecommendationItem(
				label = "Gastos Fijos (Necesidades)",
				value = formatCurrencySymbolOnly(recurrentSpent, currency),
				percentage = recurrentPct,
				target = 50,
				description = if (recurrentPct > 50) "Están elevados. Busca reducir servicios o suscripciones." else "Bajo el límite del 50%."
			)

			RecommendationItem(
				label = "Gastos Variables (Deseos)",
				value = formatCurrencySymbolOnly(variableSpent, currency),
				percentage = variablePct,
				target = 30,
				description = if (variablePct > 30) "Considera limitar gastos no esenciales este periodo." else "Buen control de tus gustos."
			)
		}
	}
}

@Composable
private fun RecommendationItem(
	label: String,
	value: String,
	percentage: Int,
	target: Int,
	description: String
) {
	Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = label,
				style = MaterialTheme.typography.labelMedium,
				fontWeight = FontWeight.SemiBold
			)
			Text(
				text = "$value ($percentage%)",
				style = MaterialTheme.typography.bodySmall,
				fontWeight = FontWeight.Medium,
				color = if (percentage > target) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
			)
		}
		Text(
			text = description,
			style = MaterialTheme.typography.labelSmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun SavingsRecommendationPreview() {
	MinusTheme {
		SavingsRecommendationCard(
			budget = BigDecimal("20000"),
			spends = listOf(
				Transaction(amount = BigDecimal("11000"), isRecurrent = true, comment = "Renta + Luz", date = LocalDateTime.now()),
				Transaction(amount = BigDecimal("4000"), isRecurrent = false, comment = "Cenas", date = LocalDateTime.now()),
			)
		)
	}
}
