package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorEditor
import com.serranoie.app.minus.presentation.ui.theme.colorOnEditor
import com.serranoie.app.minus.presentation.ui.theme.colorPrimary
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Roll-over options dialog shown when there's remaining budget and a new period starts.
 */
@Composable
fun RolloverDialog(
	remainingAmount: BigDecimal,
	currencyCode: String,
	onSplitEqually: () -> Unit,
	onCarryToNextDay: () -> Unit,
	onDismiss: () -> Unit
) {
	val currencyFormat = com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat(currencyCode)

	val formattedRemaining = currencyFormat.format(remainingAmount)

	Dialog(onDismissRequest = onDismiss) {
		Card(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			colors = CardDefaults.cardColors(
				containerColor = colorEditor
			),
			shape = RoundedCornerShape(24.dp)
		) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(24.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				// Title
				Text(
					text = "Presupuesto restante",
					style = MaterialTheme.typography.titleLarge.copy(
						fontWeight = FontWeight.Bold
					),
					color = colorOnEditor
				)

				Spacer(modifier = Modifier.height(8.dp))

				// Remaining amount
				Text(
					text = formattedRemaining,
					style = MaterialTheme.typography.displaySmall.copy(
						fontWeight = FontWeight.Bold
					),
					color = colorPrimary
				)

				Spacer(modifier = Modifier.height(8.dp))

				Text(
					text = "¿Qué quieres hacer con el dinero restante?",
					style = MaterialTheme.typography.bodyMedium,
					color = colorOnEditor.copy(alpha = 0.7f),
					textAlign = TextAlign.Center
				)

				Spacer(modifier = Modifier.height(24.dp))

				// Option 1: Split equally
				RollOverOptionCard(
					icon = Icons.Outlined.DateRange,
					title = "Dividir igualmente",
					description = "Agregar $formattedRemaining al presupuesto de los próximos días",
					onClick = onSplitEqually
				)

				Spacer(modifier = Modifier.height(12.dp))

				// Option 2: Carry to next day
				RollOverOptionCard(
					icon = Icons.Outlined.Edit,
					title = "Pasar a mañana",
					description = "Agregar $formattedRemaining al presupuesto de mañana",
					onClick = onCarryToNextDay
				)

				Spacer(modifier = Modifier.height(16.dp))

				// Dismiss button
				TextButton(onClick = onDismiss) {
					Text(
						text = "Cancelar",
						color = colorOnEditor.copy(alpha = 0.6f)
					)
				}
			}
		}
	}
}

/**
 * Individual roll-over option card.
 */
@Composable
private fun RollOverOptionCard(
	icon: ImageVector,
	title: String,
	description: String,
	onClick: () -> Unit
) {
	Card(
		modifier = Modifier
			.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = colorPrimary.copy(alpha = 0.1f)
		),
		shape = RoundedCornerShape(16.dp),
		onClick = onClick
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Icon(
				imageVector = icon,
				contentDescription = null,
				modifier = Modifier.size(32.dp),
				tint = colorPrimary
			)

			Spacer(modifier = Modifier.width(16.dp))

			Column {
				Text(
					text = title,
					style = MaterialTheme.typography.titleMedium.copy(
						fontWeight = FontWeight.Medium
					),
					color = colorPrimary
				)
				Text(
					text = description,
					style = MaterialTheme.typography.bodySmall,
					color = colorOnEditor.copy(alpha = 0.7f)
				)
			}
		}
	}
}

@Preview
@Composable
private fun RolloverDialogPreview() {
	MinusTheme {
		RolloverDialog(
			remainingAmount = BigDecimal("125.00"),
			currencyCode = "MXN",
			onSplitEqually = {},
			onCarryToNextDay = {},
			onDismiss = {},
		)
	}
}
