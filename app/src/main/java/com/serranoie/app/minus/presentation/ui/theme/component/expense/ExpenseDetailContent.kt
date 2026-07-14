package com.serranoie.app.minus.presentation.ui.theme.component.expense

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.headlineSmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelLargeCondensed
import java.time.LocalDateTime

@Composable
fun ExpenseDetailContent(
	transaction: Transaction,
	isRecurrentExpense: Boolean,
	operationNumber: String,
	operationTime: String,
	totalAmountText: String,
	details: List<Pair<String, String>>,
	onMarkAsPaid: (() -> Unit)?,
	onEdit: () -> Unit,
	onDelete: () -> Unit,
	readOnly: Boolean,
	modifier: Modifier = Modifier
) {
	Column(
		modifier = modifier.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp),
		horizontalAlignment = Alignment.CenterHorizontally
	) {
		Text(
			text = "MONTO TOTAL",
			style = MaterialTheme.typography.labelLargeCondensed,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			textAlign = TextAlign.Center
		)

		Text(
			text = totalAmountText,
			style = MaterialTheme.typography.headlineSmallEmphasized,
			color = MaterialTheme.colorScheme.error,
			fontWeight = FontWeight.Bold,
			textAlign = TextAlign.Center
		)

		HorizontalDivider()

		details.forEach { (label, value) ->
			Box(modifier = Modifier.fillMaxWidth()) {
				Text(
					text = label,
					style = MaterialTheme.typography.bodyMediumCondensed,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.align(Alignment.CenterStart)
				)
				Text(
					text = value,
					style = MaterialTheme.typography.bodyMediumCondensed,
					color = MaterialTheme.colorScheme.onSurface,
					modifier = Modifier.align(Alignment.CenterEnd)
				)
			}
		}

		if (isRecurrentExpense && onMarkAsPaid != null) {
			Button(
				onClick = onMarkAsPaid, modifier = Modifier.fillMaxWidth()
			) {
				Text(text = "Marcar como pagado")
			}
		}

		Row(
			modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
		) {
			OutlinedButton(
				onClick = onEdit, modifier = Modifier.weight(1f)
			) {
				Text("Editar")
			}

			if (!readOnly) {
				Button(
					onClick = onDelete,
					modifier = Modifier.weight(1f),
					colors = ButtonDefaults.buttonColors(
						containerColor = MaterialTheme.colorScheme.error,
						contentColor = MaterialTheme.colorScheme.onError
					)
				) {
					Text("Eliminar")
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Preview(
	uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Composable
private fun ExpenseDetailContentPreview() {
	MinusTheme {
		ExpenseDetailContent(
			transaction = Transaction(
				id = 1L,
				amount = java.math.BigDecimal("150.50"),
				comment = "Compra en supermercado",
				date = LocalDateTime.now(),
				isDeleted = false,
				isRecurrent = false
			),
			isRecurrentExpense = false,
			operationNumber = "1",
			operationTime = "Hoy, 10:30 AM",
			totalAmountText = "$150.50",
			details = listOf(
				"Fecha" to "05/04/2026",
				"Categoría" to "General",
				"Creado" to "05/04/2026"
			),
			onMarkAsPaid = null,
			onEdit = {},
			onDelete = {},
			readOnly = false
		)
	}
}