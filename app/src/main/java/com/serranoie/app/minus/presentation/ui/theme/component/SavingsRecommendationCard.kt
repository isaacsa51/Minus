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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleSmallCondensed
import com.serranoie.app.minus.presentation.util.formatCurrencySymbolOnly
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * Recommendation card based on the popular 50/30/20 rule.
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

	val savingsPct =
		savings.divide(safeBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
	val recurrentPct =
		recurrentSpent.divide(safeBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()
	val variablePct =
		variableSpent.divide(safeBudget, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toInt()

	Column(
		modifier = modifier
			.fillMaxWidth()
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp)
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			Icon(
				imageVector = Icons.Outlined.Info,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.outline,
				modifier = Modifier.size(20.dp)
			)
			Spacer(modifier = Modifier.width(8.dp))
			Text(
				text = "Recomendación de ahorro",
				style = MaterialTheme.typography.titleSmallCondensed,
				color = MaterialTheme.colorScheme.outline
			)
		}

		Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.Bottom
			) {
				Column {
					Text(
						text = "Ahorro actual",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Text(
						text = "$savingsPct%",
						style = MaterialTheme.typography.headlineSmallEmphasized,
						fontWeight = FontWeight.ExtraBold,
						color = if (savingsPct >= 20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
					)
				}
			}

			LinearSavingsBar(
				recurrentPct = recurrentPct,
				variablePct = variablePct,
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
					text = "Disponible: ${formatCurrencySymbolOnly(savings, currency)}",
					style = MaterialTheme.typography.labelSmallCondensed.copy(fontWeight = FontWeight.Light),
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Column(horizontalAlignment = Alignment.End) {
					Text(
						text = "Ahorro ideal: ${
							formatCurrencySymbolOnly(
								budget.multiply(BigDecimal("0.2")), currency
							)
						}",
						style = MaterialTheme.typography.labelSmallCondensed.copy(
							fontWeight = FontWeight.Light,
							textDecoration = if (savingsPct < 20) TextDecoration.LineThrough else TextDecoration.None
						),
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					if (savingsPct < 20) {
						Text(
							text = "Ahorro actual: ${formatCurrencySymbolOnly(savings, currency)}",
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
				label = "Gastos Recurrentes",
				value = formatCurrencySymbolOnly(recurrentSpent, currency),
				percentage = recurrentPct,
				target = 50,
				color = MaterialTheme.colorScheme.outlineVariant
			)

			RecommendationItem(
				label = "Gastos Únicos",
				value = formatCurrencySymbolOnly(variableSpent, currency),
				percentage = variablePct,
				target = 30,
				color = MaterialTheme.colorScheme.primary
			)
		}
	}
}

@Composable
fun LinearSavingsBar(
	recurrentPct: Int, variablePct: Int, modifier: Modifier = Modifier
) {
	val totalSpentPct = recurrentPct + variablePct
	val roundedCorner = RoundedCornerShape(12.dp)

	Box(
		modifier = modifier
			.clip(CircleShape)
			.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
	) {
		Row(modifier = Modifier.fillMaxSize()) {
			val recSafe = recurrentPct.coerceAtMost(80).toFloat()
			val varSafe = if (recurrentPct < 80) {
				variablePct.coerceAtMost(80 - recurrentPct).toFloat()
			} else 0f
			val gapSafe = (80 - (recSafe + varSafe)).coerceAtLeast(0f)

			val totalInvasion = (totalSpentPct - 80).coerceIn(0, 20).toFloat()
			val safeSavings = (20 - totalInvasion).coerceAtLeast(0f)

			if (recSafe > 0) {
				Box(
					Modifier
						.weight(recSafe)
						.fillMaxHeight()
						.clip(roundedCorner)
						.background(MaterialTheme.colorScheme.outlineVariant)
				)
			}
			if (varSafe > 0) {
				Box(
					Modifier
						.weight(varSafe)
						.fillMaxHeight()
						.clip(roundedCorner)
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
						.clip(roundedCorner)
						.background(MaterialTheme.colorScheme.error)
				)
			}
			if (safeSavings > 0) {
				Box(
					Modifier
						.weight(safeSavings)
						.fillMaxHeight()
						.clip(roundedCorner)
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
				budget = BigDecimal("20000"), spends = listOf(
					Transaction(
						amount = BigDecimal("6000"),
						isRecurrent = true,
						comment = "Renta",
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
					budget = BigDecimal("12000"), spends = listOf(
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
