package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorEditor
import com.serranoie.app.minus.presentation.ui.theme.colorOnEditor
import com.serranoie.app.minus.presentation.ui.theme.colorPrimary
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Dialog shown at midnight when a budget period ends while the user is in the app.
 * Displays period summary and allows navigation to Analytics or dismissal.
 */
@Composable
fun MidnightTransitionDialog(
	periodStartDate: LocalDate,
	periodEndDate: LocalDate,
	totalBudget: BigDecimal,
	remainingAmount: BigDecimal,
	totalSpent: BigDecimal,
	currencyCode: String,
	onViewAnalytics: () -> Unit,
	onDismiss: () -> Unit
) {
	val currencyFormat =
		com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat(currencyCode)
	val formattedRemaining = currencyFormat.format(remainingAmount)
	val formattedSpent = currencyFormat.format(totalSpent)
	currencyFormat.format(totalBudget)

	Dialog(onDismissRequest = onDismiss) {
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp), colors = CardDefaults.cardColors(
				containerColor = colorEditor
			), shape = RoundedCornerShape(24.dp)
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(24.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				// Icon
				Icon(
					imageVector = Icons.Outlined.FactCheck,
					contentDescription = null,
					modifier = Modifier.size(56.dp),
					tint = colorPrimary
				)

				Spacer(modifier = Modifier.height(16.dp))

				// Title
				Text(
					text = "Periodo finalizado",
					style = MaterialTheme.typography.headlineSmall.copy(
						fontWeight = FontWeight.Bold
					),
					color = colorOnEditor
				)

				Spacer(modifier = Modifier.height(8.dp))

				// Period dates
				Text(
					text = "${periodStartDate.dayOfMonth} ${getMonthName(periodStartDate.monthValue)} - ${periodEndDate.dayOfMonth} ${
						getMonthName(
							periodEndDate.monthValue
						)
					}",
					style = MaterialTheme.typography.bodyMedium,
					color = colorOnEditor.copy(alpha = 0.7f)
				)

				Spacer(modifier = Modifier.height(24.dp))

				// Summary cards
				SummaryCard(
					label = "Gastado", value = formattedSpent, modifier = Modifier.fillMaxWidth()
				)

				Spacer(modifier = Modifier.height(8.dp))

				SummaryCard(
					label = "Restante",
					value = formattedRemaining,
					valueColor = if (remainingAmount >= BigDecimal.ZERO) colorPrimary else MaterialTheme.colorScheme.error,
					modifier = Modifier.fillMaxWidth()
				)

				Spacer(modifier = Modifier.height(24.dp))

				Button(
					onClick = onViewAnalytics,
					modifier = Modifier.fillMaxWidth(),
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.tertiary,
						contentColor = MaterialTheme.colorScheme.onTertiary
					),
				) {
					Text(
						text = "Ver analisis de gastos",
						modifier = Modifier.padding(vertical = 8.dp)
					)
				}

				Spacer(modifier = Modifier.height(8.dp))

				OutlinedButton(
					onClick = onDismiss,
					modifier = Modifier.fillMaxWidth(),
					colors = ButtonDefaults.outlinedButtonColors(
						contentColor = MaterialTheme.colorScheme.outline,
					)
				) {
					Text(
						text = "Mas tarde", modifier = Modifier.padding(vertical = 8.dp)
					)
				}
			}
		}
	}
}

@Composable
private fun SummaryCard(
	label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = colorOnEditor
) {
	Card(
		modifier = modifier, colors = CardDefaults.cardColors(
			containerColor = colorPrimary.copy(alpha = 0.1f)
		), shape = RoundedCornerShape(12.dp)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			Text(
				text = label,
				style = MaterialTheme.typography.bodySmall,
				color = colorOnEditor.copy(alpha = 0.6f)
			)
			Text(
				text = value, style = MaterialTheme.typography.titleLarge.copy(
					fontWeight = FontWeight.Bold
				), color = valueColor
			)
		}
	}
}

private fun getMonthName(month: Int): String {
	return when (month) {
		1 -> "ene"
		2 -> "feb"
		3 -> "mar"
		4 -> "abr"
		5 -> "may"
		6 -> "jun"
		7 -> "jul"
		8 -> "ago"
		9 -> "sep"
		10 -> "oct"
		11 -> "nov"
		12 -> "dic"
		else -> ""
	}
}

@Preview
@Composable
private fun MidnightTransitionDialogPreview() {
	MinusTheme {
		MidnightTransitionDialog(
			periodStartDate = LocalDate.of(2026, 4, 1),
			periodEndDate = LocalDate.of(2026, 4, 15),
			totalBudget = BigDecimal("5000.00"),
			remainingAmount = BigDecimal("1250.00"),
			totalSpent = BigDecimal("3750.00"),
			currencyCode = "MXN",
			onViewAnalytics = {},
			onDismiss = {})
	}
}