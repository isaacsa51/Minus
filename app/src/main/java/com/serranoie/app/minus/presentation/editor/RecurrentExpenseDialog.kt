package com.serranoie.app.minus.presentation.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecurrentExpenseDialog(
	budgetSettings: BudgetSettings?,
	onDismiss: () -> Unit,
	onConfirm: (frequency: RecurrentFrequency, endDate: LocalDate, subscriptionDay: Int?) -> Unit,
	modifier: Modifier = Modifier,
) {
	val today = LocalDate.now()
	val budgetEndDate = budgetSettings?.getPeriodEndDate() ?: today.plusDays(30)

	var selectedFrequency by remember { mutableStateOf(RecurrentFrequency.MONTHLY) }
	var selectedDay by remember { mutableIntStateOf(today.dayOfMonth.coerceIn(1, 28)) }

	var showDatePicker by remember { mutableStateOf(false) }
	val defaultEndDate = remember(budgetEndDate) { budgetEndDate.plusMonths(3) }
	var selectedEndDate by remember { mutableStateOf(defaultEndDate) }

	if (showDatePicker) {
		val maxSelectableDate = today.plusMonths(12)
		val datePickerState = rememberDatePickerState(
			initialSelectedDateMillis = selectedEndDate.atStartOfDay(ZoneId.systemDefault())
				.toInstant().toEpochMilli(), selectableDates = object : SelectableDates {
				override fun isSelectableDate(utcTimeMillis: Long): Boolean {
					val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneId.systemDefault())
						.toLocalDate()
					return date.isAfter(today) && !date.isAfter(maxSelectableDate)
				}
			})

		DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
			TextButton(
				onClick = {
					datePickerState.selectedDateMillis?.let { millis ->
						selectedEndDate =
							Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
								.toLocalDate()
					}
					showDatePicker = false
				}) {
				Text(stringResource(android.R.string.ok))
			}
		}, dismissButton = {
			TextButton(onClick = { showDatePicker = false }) {
				Text(stringResource(android.R.string.cancel))
			}
		}) {
			DatePicker(state = datePickerState)
		}
	}

	val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()) }

	AlertDialog(modifier = modifier, onDismissRequest = onDismiss, title = {
		Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
			Text(
				text = "Gasto recurrente", style = MaterialTheme.typography.titleLargeEmphasized
			)
			Text(
				text = "Configura en qué frecuencia se repetirá este gasto.",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}, text = {
		Column(
			modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			Text("Frecuencia del pago", style = MaterialTheme.typography.labelMediumCondensed)

			val options = listOf("Semanal", "Quincenal", "Mensual")
			val frequencies = listOf(
				RecurrentFrequency.WEEKLY,
				RecurrentFrequency.BIWEEKLY,
				RecurrentFrequency.MONTHLY,
			)
			val selectedIndex = frequencies.indexOf(selectedFrequency).coerceAtLeast(0)

			Row(
				Modifier.padding(horizontal = 8.dp),
				horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
			) {
				val modifiers =
					listOf(Modifier.weight(1f), Modifier.weight(1.5f), Modifier.weight(1f))

				options.forEachIndexed { index, label ->
					ToggleButton(
						checked = selectedIndex == index,
						onCheckedChange = { selectedFrequency = frequencies[index] },
						modifier = modifiers[index].semantics { role = Role.RadioButton },
						shapes = when (index) {
							0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
							options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
							else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
						},
					) {
						Text(label, modifier = Modifier.basicMarquee())
					}
				}
			}

			if (selectedFrequency == RecurrentFrequency.MONTHLY) {
				Surface(
					shape = MaterialTheme.shapes.medium,
					color = MaterialTheme.colorScheme.surfaceContainer
				) {
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(horizontal = 8.dp, vertical = 4.dp),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceBetween
					) {
						IconButton(onClick = {
							selectedDay = (selectedDay - 1).coerceAtLeast(1)
						}) {
							Icon(
								Icons.AutoMirrored.Filled.KeyboardArrowLeft,
								contentDescription = "Día anterior",
								modifier = Modifier.size(18.dp)
							)
						}

						Text(
							text = "Día $selectedDay de cada mes",
							style = MaterialTheme.typography.titleMedium
						)

						IconButton(onClick = {
							selectedDay = (selectedDay + 1).coerceAtMost(31)
						}) {
							Icon(
								Icons.AutoMirrored.Filled.KeyboardArrowRight,
								contentDescription = "Día siguiente",
								modifier = Modifier.size(18.dp)
							)
						}
					}
				}
			}

			Text(
				text = "Fecha límite",
				style = MaterialTheme.typography.labelMediumCondensed,
			)

			OutlinedTextField(
				value = selectedEndDate.format(dateFormatter),
				onValueChange = {},
				readOnly = true,
				placeholder = { Text("Fecha") },
				trailingIcon = {
					Icon(
						imageVector = Icons.Default.CalendarToday,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.outlineVariant,
					)
				},
				shape = RoundedCornerShape(14.dp),
				colors = OutlinedTextFieldDefaults.colors(
					focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
					unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
					focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
					unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
				),
				modifier = Modifier.fillMaxWidth(),
				singleLine = true,
			)

			TextButton(
				onClick = { showDatePicker = true }, modifier = Modifier.align(Alignment.End)
			) {
				Text("Cambiar fecha")
			}

			OutlinedCard(
				modifier = Modifier.fillMaxWidth(),
				shape = MaterialTheme.shapes.large,
				border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
				colors = CardDefaults.outlinedCardColors(
					containerColor = Color.Transparent
				),
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 12.dp, vertical = 14.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(10.dp),
				) {
					Icon(
						imageVector = Icons.Default.Info,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.outline,
					)
					Text(
						text = buildRecurrentSummary(
							frequency = selectedFrequency,
							selectedDay = selectedDay,
							selectedEndDate = selectedEndDate,
							formatter = dateFormatter,
						),
						style = MaterialTheme.typography.bodySmallCondensed,
						color = MaterialTheme.colorScheme.onSurface,
					)
				}
			}
		}
	}, confirmButton = {
		Button(
			onClick = {
				onConfirm(
					selectedFrequency,
					selectedEndDate,
					if (selectedFrequency == RecurrentFrequency.MONTHLY) selectedDay else null
				)
			}) {
			Text("Guardar")
		}
	}, dismissButton = {
		TextButton(onClick = onDismiss) {
			Text("Cancelar")
		}
	})
}

private fun buildRecurrentSummary(
	frequency: RecurrentFrequency,
	selectedDay: Int,
	selectedEndDate: LocalDate,
	formatter: DateTimeFormatter
): String {
	return when (frequency) {
		RecurrentFrequency.WEEKLY -> "Se cobrará cada semana hasta ${
			selectedEndDate.format(
				formatter
			)
		}."

		RecurrentFrequency.BIWEEKLY -> "Se cobrará cada quincena hasta ${
			selectedEndDate.format(
				formatter
			)
		}."

		RecurrentFrequency.MONTHLY -> "Se cobrará cada mes el día $selectedDay hasta ${
			selectedEndDate.format(
				formatter
			)
		}."
	}
}

@Preview(showBackground = true)
@Composable
private fun RecurrentExpenseDialogPreview() {
	MinusTheme {
		RecurrentExpenseDialog(
			budgetSettings = BudgetSettings(
			totalBudget = BigDecimal("500.00"),
			period = BudgetPeriod.MONTHLY,
			startDate = LocalDate.now(),
			currencyCode = "USD"
		), onDismiss = {}, onConfirm = { _, _, _ -> })
	}
}
