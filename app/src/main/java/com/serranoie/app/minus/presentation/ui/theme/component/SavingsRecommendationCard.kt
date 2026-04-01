package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Composable
fun SavingsRecommendationCard(
	modifier: Modifier = Modifier,
	budget: BigDecimal,
	spends: List<Transaction>,
	currency: String = "MXN",
) {
	// Separate recurrent from non-recurrent expenses
	val recurrentExpenses = spends.filter { it.isRecurrent }
	val nonRecurrentExpenses = spends.filter { !it.isRecurrent }

	// Calculate totals
	val totalSpent = spends.sumOf { it.amount }
	val totalRecurrent = recurrentExpenses.sumOf { it.amount }
	val totalNonRecurrent = nonRecurrentExpenses.sumOf { it.amount }

	// Calculate savings (what's left from budget)
	val savings = budget.subtract(totalSpent).max(BigDecimal.ZERO)

	// Calculate savings percentage
	val savingsPercentage = if (budget.compareTo(BigDecimal.ZERO) > 0) {
		savings.divide(budget, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100)).toFloat()
	} else {
		0f
	}

	// Calculate "adjustable" savings (excluding recurrent expenses)
	// This shows what savings would be possible if you only control non-recurrent spending
	val remainingAfterRecurrent = budget.subtract(totalRecurrent).max(BigDecimal.ZERO)
	val adjustableSavings = remainingAfterRecurrent.subtract(totalNonRecurrent).max(BigDecimal.ZERO)
	val adjustableSavingsPercentage = if (remainingAfterRecurrent.compareTo(BigDecimal.ZERO) > 0) {
		adjustableSavings.divide(remainingAfterRecurrent, 4, RoundingMode.HALF_UP)
			.multiply(BigDecimal(100)).toFloat()
	} else {
		0f
	}

	// Find categories breakdown
	val categorySpending = spends.groupBy { it.comment.ifEmpty { "Sin categoría" } }
		.mapValues { it.value.sumOf { tx -> tx.amount } }

	val topCategory = categorySpending.maxByOrNull { it.value }
	val topCategoryIsRecurrent = topCategory?.let { categoryEntry ->
		spends.any { it.comment == categoryEntry.key && it.isRecurrent }
	} ?: false

	val topCategoryPercentage =
		if (totalSpent.compareTo(BigDecimal.ZERO) > 0 && topCategory != null) {
			topCategory.value.divide(totalSpent, 4, RoundingMode.HALF_UP).multiply(BigDecimal(100))
				.toFloat()
		} else {
			0f
		}

	// Standard recommended savings percentage (50/30/20 rule)
	val recommendedSavingsPercentage = 20

	Box(
		modifier = modifier.fillMaxWidth(),
	) {
		Column(
			modifier = Modifier.padding(16.dp)
		) {
			// Título con icono
			Row(verticalAlignment = Alignment.CenterVertically) {
				Icon(
					imageVector = Icons.Outlined.Info,
					tint = MaterialTheme.colorScheme.onSurfaceVariant,
					contentDescription = null,
					modifier = Modifier
						.padding(end = 8.dp)
						.size(30.dp)
				)

				Text(
					text = "Recomendación de ahorro",
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			Spacer(modifier = Modifier.height(12.dp))

			Text(
				text = buildAnnotatedString {
					append("El porcentaje estándar recomendado a nivel mundial para el ahorro y la inversión a largo plazo es el ")
					withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
						append("20%")
					}
					append(" de tus ingresos netos, basado en la regla 50/30/20.")
				},
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)

			Spacer(modifier = Modifier.height(16.dp))

			// Valores de la regla 50/30/20
			val needs50 = budget.multiply(BigDecimal("0.50"))
			val wants30 = budget.multiply(BigDecimal("0.30"))
			val savings20 = budget.multiply(BigDecimal("0.20"))

			Column(
				modifier = Modifier
					.fillMaxWidth()
			) {
				Text(
					text = buildAnnotatedString {
						append("- ")
						withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
							append("Necesidades (50%)")
						}
						append(" = $needs50 $currency")
					},
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = buildAnnotatedString {
						append("- ")
						withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
							append("Deseos (30%)")
						}
						append(" = $wants30 $currency")
					},
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = buildAnnotatedString {
						append("- ")
						withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
							append("Ahorro (20%)")
						}
						append(" = $savings20 $currency")
					},
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.primary
				)
			}

			Spacer(modifier = Modifier.height(12.dp))

			// 2. Fórmula - interactive with variables in italic monospace
			var showValues by remember { mutableStateOf(false) }

			Box(
				modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
			) {
				Text(
					text = buildAnnotatedString {
						append("(")
						if (showValues) {
							withStyle(
								SpanStyle(
									fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
								)
							) {
								append("$totalSpent")
							}
						} else {
							withStyle(
								SpanStyle(
									fontFamily = FontFamily.Monospace,
									fontWeight = FontWeight.Bold,
									fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
								)
							) {
								append("gastos")
							}
						}
						append(" + ")
						if (showValues) {
							withStyle(
								SpanStyle(
									fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
								)
							) {
								append("$savings")
							}
						} else {
							withStyle(
								SpanStyle(
									fontFamily = FontFamily.Monospace,
									fontWeight = FontWeight.Bold,
									fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
								)
							) {
								append("restante")
							}
						}
						append(") ÷ ")
						if (showValues) {
							withStyle(
								SpanStyle(
									fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
								)
							) {
								append("$budget")
							}
						} else {
							withStyle(
								SpanStyle(
									fontFamily = FontFamily.Monospace,
									fontWeight = FontWeight.Bold,
									fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
								)
							) {
								append("total")
							}
						}
						append(" × 100 = ")
						withStyle(
							SpanStyle(
								fontFamily = FontFamily.Monospace,
								fontWeight = FontWeight.Bold,
								color = MaterialTheme.colorScheme.primary
							)
						) {
							append("${savingsPercentage.toInt()}%")
						}
					},
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurface,
					modifier = Modifier
						.padding(8.dp)
						.clickable { showValues = !showValues })
			}

			// Variable values explanation - 8sp in Column
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp)
			) {
				Text(
					text = "gastos = $totalSpent $currency",
					style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Text(
					text = "restante = $savings $currency",
					style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
				Text(
					text = "total = $budget $currency",
					style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			Spacer(modifier = Modifier.height(8.dp))

			// Recurrent expenses breakdown
			if (totalRecurrent.compareTo(BigDecimal.ZERO) > 0) {
				val recurrentPercentage = if (totalSpent.compareTo(BigDecimal.ZERO) > 0) {
					totalRecurrent.divide(totalSpent, 4, RoundingMode.HALF_UP)
						.multiply(BigDecimal(100)).toFloat()
				} else 0f

				Text(
					text = buildAnnotatedString {
						append("Gastos recurrentes (fijos): ")
						withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
							append("$totalRecurrent $currency")
						}
						append(" (${recurrentPercentage.toInt()}% del total)")
						append("\nGastos no recurrentes (variables): ")
						withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
							append("$totalNonRecurrent $currency")
						}
					},
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)

				Spacer(modifier = Modifier.height(8.dp))

				// Adjustable savings calculation
				Text(
					text = buildAnnotatedString {
						append("Ahorro ajustable (sin fijos): ")
						withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
							append("${adjustableSavingsPercentage.toInt()}%")
						}
						append(" de ")
						withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
							append("$remainingAfterRecurrent $currency")
						}
					},
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.primary
				)
			}

			Spacer(modifier = Modifier.height(8.dp))

			// Category breakdown
			if (topCategory != null && topCategoryPercentage > 0) {
				Text(
					text = buildAnnotatedString {
						append("Categoría con más gastos: ")
						withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
							append(topCategory.key)
						}
						append(" (${topCategory.value} $currency)")
						append("\nRepresenta el ")
						withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
							append("${topCategoryPercentage.toInt()}%")
						}
						append(" de tus gastos totales.")

						if (topCategoryIsRecurrent) {
							append(" Este es un gasto recurrente (fijo).")
						}
					},
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			Spacer(modifier = Modifier.height(12.dp))

			// 4. Texto dinámico dependiendo del período
			Text(
				text = buildAnnotatedString {
					// Dynamic part based on user's actual savings
					if (savingsPercentage >= recommendedSavingsPercentage) {
						append("¡Excelente! En este período has ahorrado el ")
						withStyle(
							SpanStyle(
								fontWeight = FontWeight.Bold,
								color = MaterialTheme.colorScheme.primary
							)
						) {
							append("${savingsPercentage.toInt()}%")
						}
						append(" de tu presupuesto, superando la recomendación del 20%.")
					} else if (savingsPercentage > 0) {
						append("En este período ahorraste el ")
						withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
							append("${savingsPercentage.toInt()}%")
						}
						append(" de tu presupuesto. La meta recomendada es el 20%.")

						// Add insight about recurrent expenses if relevant
						if (totalRecurrent.compareTo(BigDecimal.ZERO) > 0) {
							append("\n\n")
							append(
								"Nota: Tus gastos recurrentes representan ${
									(totalRecurrent.divide(
										budget, 2, RoundingMode.HALF_UP
									).multiply(BigDecimal(100)).toInt())
								}% de tu presupuesto."
							)
							if (adjustableSavingsPercentage >= recommendedSavingsPercentage) {
								append(" Si solo redujeras gastos variables, podrías alcanzar el 20% de ahorro.")
							}
						}
						append(" ¡Aún puedes mejorar!")
					} else {
						append("En este período no lograste ahorrar. La regla 50/30/20 recomienda ahorrar al menos el 20% de tus ingresos.")

						// Add insight about recurrent expenses
						if (totalRecurrent.compareTo(BigDecimal.ZERO) > 0) {
							append("\n\n")
							append(
								"Análisis: Tus gastos fijos (recurrentes) son $totalRecurrent $currency (${
									(totalRecurrent.divide(
										budget, 2, RoundingMode.HALF_UP
									).multiply(BigDecimal(100)).toInt())
								}% del presupuesto). "
							)
							append("Los gastos variables son $totalNonRecurrent $currency. ")
							if (adjustableSavingsPercentage >= recommendedSavingsPercentage) {
								append("Si optimizaras solo los gastos variables, podrías ahorrar hasta ${adjustableSavingsPercentage.toInt()}%.")
							} else {
								append("¡Intenta reducir gastos no esenciales para el siguiente período!")
							}
						} else {
							append(" ¡Intenta reducir gastos no esenciales para el siguiente período!")
						}
					}
				},
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Preview(
	name = "SavingsRecommendationCard - Con ahorro", showSystemUi = false, showBackground = true
)
@Composable
private fun SavingsRecommendationCardWithSavingsPreview() {
	MinusTheme {
		SavingsRecommendationCard(
			budget = BigDecimal("39000"), spends = listOf(
				Transaction(
					amount = BigDecimal("15000"),
					date = LocalDateTime.now(),
					comment = "Renta",
					isRecurrent = true,
				),
				Transaction(
					amount = BigDecimal("4332"),
					date = LocalDateTime.now().minusDays(1),
					comment = "Comida",
					isRecurrent = false,
				),
				Transaction(
					amount = BigDecimal("2500"),
					date = LocalDateTime.now().minusDays(2),
					comment = "Transporte",
					isRecurrent = false,
				),
				Transaction(
					amount = BigDecimal("1200"),
					date = LocalDateTime.now().minusDays(3),
					comment = "Servicios",
					isRecurrent = true,
				),
			), currency = "MXN"
		)
	}
}

@Preview(
	name = "SavingsRecommendationCard - Sin ahorro", showSystemUi = false, showBackground = true
)
@Composable
private fun SavingsRecommendationCardNoSavingsPreview() {
	MinusTheme {
		SavingsRecommendationCard(
			budget = BigDecimal("5000"), spends = listOf(
				Transaction(
					amount = BigDecimal("2000"),
					date = LocalDateTime.now(),
					comment = "Renta",
					isRecurrent = true,
				),
				Transaction(
					amount = BigDecimal("1500"),
					date = LocalDateTime.now().minusDays(1),
					comment = "Comida",
					isRecurrent = false,
				),
				Transaction(
					amount = BigDecimal("1500"),
					date = LocalDateTime.now().minusDays(2),
					comment = "Entretenimiento",
					isRecurrent = false,
				),
			), currency = "MXN"
		)
	}
}

@Preview(name = "SavingsRecommendationCard - Vacío", showSystemUi = false, showBackground = true)
@Composable
private fun SavingsRecommendationCardEmptyPreview() {
	MinusTheme {
		SavingsRecommendationCard(
			budget = BigDecimal("5000"), spends = emptyList(), currency = "MXN"
		)
	}
}

@Preview(
	name = "SavingsRecommendationCard - Solo recurrentes",
	showSystemUi = false,
	showBackground = true
)
@Composable
private fun SavingsRecommendationCardOnlyRecurrentPreview() {
	MinusTheme {
		SavingsRecommendationCard(
			budget = BigDecimal("10000"), spends = listOf(
				Transaction(
					amount = BigDecimal("8000"),
					date = LocalDateTime.now(),
					comment = "Renta",
					isRecurrent = true,
				),
				Transaction(
					amount = BigDecimal("1000"),
					date = LocalDateTime.now().minusDays(1),
					comment = "Servicios",
					isRecurrent = true,
				),
				Transaction(
					amount = BigDecimal("500"),
					date = LocalDateTime.now().minusDays(2),
					comment = "Internet",
					isRecurrent = true,
				),
			), currency = "MXN"
		)
	}
}