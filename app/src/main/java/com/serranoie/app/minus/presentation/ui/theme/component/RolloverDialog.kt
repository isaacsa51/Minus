package com.serranoie.app.minus.presentation.ui.theme.component

import android.content.res.Configuration
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.NextPlan
import androidx.compose.material.icons.outlined.Splitscreen
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyLargeCondensed
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.colorEditor
import com.serranoie.app.minus.presentation.ui.theme.colorOnEditor
import com.serranoie.app.minus.presentation.ui.theme.colorPrimary
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal

/**
 * Roll-over options dialog shown when there's remaining budget and a new period starts.
 */
@Composable
fun RolloverDialog(
	remainingAmount: BigDecimal,
	currencyCode: String,
	periodLabel: String,
	spentAmount: BigDecimal,
	onSplitEqually: () -> Unit,
	onCarryToNextDay: () -> Unit,
	onDismiss: () -> Unit,
	onViewAnalytics: (() -> Unit)? = null,
) {
	val currencyFormat = symbolOnlyCurrencyFormat(currencyCode)

	val formattedRemaining = currencyFormat.format(remainingAmount)
	val formattedSpent = currencyFormat.format(spentAmount)

	val scrollState = rememberScrollState()

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
					.verticalScroll(scrollState)
					.padding(24.dp),
				horizontalAlignment = Alignment.CenterHorizontally
			) {
				Icon(
					imageVector = Icons.Outlined.FactCheck,
					contentDescription = null,
					modifier = Modifier.size(48.dp),
					tint = colorPrimary
				)

				Spacer(modifier = Modifier.height(16.dp))

				Text(
					text = stringResource(R.string.rollover_dialog_period_finished_title),
					textAlign = TextAlign.Center,
					style = MaterialTheme.typography.titleMediumEmphasized,
					color = colorOnEditor
				)

				Spacer(modifier = Modifier.height(8.dp))

				Text(
					text = formattedRemaining,
					textAlign = TextAlign.Center,
					style = MaterialTheme.typography.displaySmallEmphasized,
					color = MaterialTheme.colorScheme.tertiary
				)

				Spacer(modifier = Modifier.height(8.dp))
				Text(
					text = periodLabel,
					textAlign = TextAlign.Center,
					style = MaterialTheme.typography.bodyLargeCondensed,
					color = colorOnEditor.copy(alpha = 0.7f)
				)

				Spacer(modifier = Modifier.height(8.dp))

				Text(
					text = stringResource(R.string.rollover_dialog_question),
					style = MaterialTheme.typography.bodyMedium,
					color = colorOnEditor.copy(alpha = 0.7f),
					textAlign = TextAlign.Center
				)

				Spacer(modifier = Modifier.height(16.dp))

				RollOverOptionCard(
					icon = Icons.Outlined.Splitscreen,
					title = stringResource(R.string.rollover_dialog_split_equally_title),
					description = stringResource(
						R.string.rollover_dialog_split_equally_desc,
						formattedRemaining
					),
					onClick = onSplitEqually
				)

				Spacer(modifier = Modifier.height(12.dp))

				RollOverOptionCard(
					icon = Icons.Outlined.NextPlan,
					title = stringResource(R.string.rollover_dialog_carry_to_tomorrow_title),
					description = stringResource(
						R.string.rollover_dialog_carry_to_tomorrow_desc,
						formattedRemaining
					),
					onClick = onCarryToNextDay
				)

				if (onViewAnalytics != null) {
					Spacer(modifier = Modifier.height(12.dp))

					RollOverOptionCard(
						icon = Icons.Outlined.FactCheck,
						title = stringResource(R.string.rollover_dialog_view_analytics_title),
						description = stringResource(R.string.rollover_dialog_view_analytics_desc),
						onClick = onViewAnalytics
					)
				}

				Spacer(modifier = Modifier.height(16.dp))

				TextButton(onClick = onDismiss) {
					Text(
						text = stringResource(R.string.cancel), color = colorOnEditor.copy(alpha = 0.6f)
					)
				}
			}
		}
	}
}

@Composable
private fun RollOverOptionCard(
	icon: ImageVector, title: String, description: String, onClick: () -> Unit
) {
	Card(
		modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
			containerColor = colorPrimary.copy(alpha = 0.1f)
		), shape = RoundedCornerShape(16.dp), onClick = onClick
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
					text = title, style = MaterialTheme.typography.titleMedium.copy(
						fontWeight = FontWeight.Medium
					), color = colorPrimary
				)
				Text(
					text = description,
					style = MaterialTheme.typography.bodySmallCondensed,
					color = colorOnEditor.copy(alpha = 0.7f)
				)
			}
		}
	}
}

@Preview(showBackground = true, name = "Estado Básico")
@Composable
private fun RolloverDialogBasicPreview() {
	MinusTheme {
		RolloverDialog(
			remainingAmount = BigDecimal("150.50"),
			currencyCode = "MXN",
			periodLabel = "1 abr - 30 abr",
			spentAmount = BigDecimal("850.00"),
			onSplitEqually = {},
			onCarryToNextDay = {},
			onDismiss = {})
	}
}

@Preview(showBackground = true, name = "Con Info de Periodo")
@Composable
private fun RolloverDialogInfoPreview() {
	MinusTheme {
		RolloverDialog(
			remainingAmount = BigDecimal("2450.00"),
			currencyCode = "USD",
			periodLabel = "Periodo: 1 - 15 de Octubre",
			spentAmount = BigDecimal("8500.00"),
			onSplitEqually = {},
			onCarryToNextDay = {},
			onDismiss = {})
	}
}

@Preview(showBackground = true, name = "Con Análisis")
@Composable
private fun RolloverDialogAnalyticsPreview() {
	MinusTheme {
		RolloverDialog(
			remainingAmount = BigDecimal("320.00"),
			currencyCode = "EUR",
			periodLabel = "1 may - 31 may",
			spentAmount = BigDecimal("1680.00"),
			onSplitEqually = {},
			onCarryToNextDay = {},
			onDismiss = {},
			onViewAnalytics = {})
	}
}

@Preview(showBackground = true, name = "Estado Completo")
@Composable
private fun RolloverDialogFullPreview() {
	MinusTheme {
		RolloverDialog(
			remainingAmount = BigDecimal("1250.75"),
			currencyCode = "MXN",
			periodLabel = "Septiembre 2023",
			spentAmount = BigDecimal("15400.00"),
			onSplitEqually = {},
			onCarryToNextDay = {},
			onDismiss = {},
			onViewAnalytics = {})
	}
}

@Preview(
	showBackground = true, name = "Modo Noche", uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun RolloverDialogDarkModePreview() {
	MinusTheme {
		RolloverDialog(
			remainingAmount = BigDecimal("500.00"),
			currencyCode = "MXN",
			periodLabel = "Ayer",
			spentAmount = BigDecimal("1200.00"),
			onSplitEqually = {},
			onCarryToNextDay = {},
			onDismiss = {},
			onViewAnalytics = {})
	}
}
