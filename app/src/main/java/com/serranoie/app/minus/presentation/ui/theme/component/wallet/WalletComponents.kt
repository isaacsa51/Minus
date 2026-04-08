package com.serranoie.app.minus.presentation.ui.theme.component.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.LocalWindowInsets
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.onboarding.FinishDateSelector
import com.serranoie.app.minus.presentation.onboarding.availablePeriodsFor
import com.serranoie.app.minus.presentation.onboarding.budgetForPeriod
import com.serranoie.app.minus.presentation.onboarding.label
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorEditor
import com.serranoie.app.minus.presentation.ui.theme.colorOnEditor
import com.serranoie.app.minus.presentation.ui.theme.colorPrimary
import com.serranoie.app.minus.presentation.ui.theme.component.LocalBottomSheetScrollState
import com.serranoie.app.minus.presentation.util.CurrencyAmountInputVisualTransformation
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletContent(
	forceChange: Boolean = false,
	currentBudget: BigDecimal = BigDecimal.ZERO,
	currentSpent: BigDecimal = BigDecimal.ZERO,
	startDate: LocalDate = LocalDate.now(),
	endDate: LocalDate? = null,
	currencyCode: String = "USD",
	period: BudgetPeriod = BudgetPeriod.MONTHLY,
	hasBudgetSettings: Boolean = false,
	onSave: (budget: BigDecimal, startDate: LocalDate, endDate: LocalDate?, period: BudgetPeriod) -> Unit = { _, _, _, _ -> },
	onFinishEarly: () -> Unit = {},
	onClose: () -> Unit = {},
) {
	val haptic = LocalHapticFeedback.current
	val localBottomSheetScrollState = LocalBottomSheetScrollState.current
	val navigationBarHeight =
		LocalWindowInsets.current.calculateBottomPadding().coerceAtLeast(16.dp)
	val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

	var budgetCache by remember(currentBudget) { mutableStateOf(currentBudget) }
	var startCache by remember(startDate) { mutableStateOf(startDate) }
	var endCache by remember(endDate) { mutableStateOf(endDate) }
	var periodCache by remember(period) { mutableStateOf(period) }

	// Calculate previous period days for suggestion
	val previousPeriodDays = remember(startDate, endDate) {
		if (endDate != null) {
			ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
		} else 0
	}

	val restBudget = budgetCache - currentSpent
	val totalDays = endCache?.let { ChronoUnit.DAYS.between(startCache, it).toInt() + 1 } ?: 1

	val isChanged =
		budgetCache != currentBudget || startCache != startDate || endCache != endDate || periodCache != period

	var isEditMode by remember(forceChange, hasBudgetSettings) {
		mutableStateOf(forceChange || !hasBudgetSettings)
	}

	val openConfirmFinishDialog = remember { mutableStateOf(false) }

	var showDateSelector by remember { mutableStateOf(false) }

	Column(modifier = Modifier.fillMaxSize()) {
		WalletStatusBarStub()
		Surface(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = localBottomSheetScrollState.topPadding)
		) {
			Column(modifier = Modifier.fillMaxSize()) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(vertical = 8.dp, horizontal = 16.dp),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.SpaceBetween,
				) {
					if (!forceChange && isEditMode) {
						IconButton(onClick = { isEditMode = false }) {
							Icon(Icons.Default.ArrowBack, contentDescription = "Back")
						}
					} else {
						Spacer(Modifier.size(48.dp))
					}

					Text(
						text = if (isChanged || isEditMode) "Editar presupuesto" else "Presupuesto",
						style = MaterialTheme.typography.titleLarge,
					)

					if (!isEditMode && hasBudgetSettings) {
						IconButton(onClick = { isEditMode = true }) {
							Icon(Icons.Filled.Edit, contentDescription = "Edit budget")
						}
					} else {
						Spacer(Modifier.size(48.dp))
					}
				}

				Column(
					modifier = Modifier
						.verticalScroll(rememberScrollState())
						.padding(bottom = navigationBarHeight),
				) {
					if (isEditMode) {
						BudgetEditor(
							initialBudget = budgetCache,
							startDate = startCache,
							endDate = endCache,
							selectedPeriod = periodCache,
							totalDays = totalDays,
							currencyCode = currencyCode,
							previousPeriodDays = previousPeriodDays,
							onBudgetChange = { budgetCache = it },
							onPickDates = { showDateSelector = true },
							onPeriodChange = { periodCache = it },
							onApplyPreviousPeriod = { days ->
								startCache = LocalDate.now()
								endCache = LocalDate.now().plusDays(days.toLong() - 1)
							},
						)
					} else {
						BudgetSummary(
							budget = currentBudget,
							spent = currentSpent,
							restBudget = restBudget,
							startDate = startDate,
							endDate = endDate,
							period = period,
							totalDays = totalDays,
							currencyCode = currencyCode,
							dateFormatter = dateFormatter,
						)
					}

					Spacer(modifier = Modifier.height(16.dp))

					if (isEditMode || isChanged) {
						Button(
							onClick = {
								onSave(budgetCache, startCache, endCache, periodCache)
								haptic.performHapticFeedback(HapticFeedbackType.LongPress)
							},
							modifier = Modifier
								.fillMaxWidth()
								.heightIn(min = 56.dp)
								.padding(horizontal = 16.dp),
							enabled = budgetCache > BigDecimal.ZERO && endCache != null,
						) {
							Text(
								text = if (hasBudgetSettings) "Actualizar" else "Aplicar",
								style = MaterialTheme.typography.bodyLarge,
							)
						}
					} else if (hasBudgetSettings && currentSpent > BigDecimal.ZERO) {
						OutlinedButton(
							onClick = { openConfirmFinishDialog.value = true },
							modifier = Modifier
								.fillMaxWidth()
								.padding(horizontal = 16.dp),
							colors = ButtonDefaults.outlinedButtonColors(
								contentColor = MaterialTheme.colorScheme.error,
							),
						) { Text("Finalizar presupuesto temprano") }
					}
				}
			}
		}

		if (showDateSelector) {
			FinishDateSelector(
				totalBudget = budgetCache,
				currencyCode = currencyCode,
				onBackPressed = { showDateSelector = false },
				onApply = { newStart, newEnd, newPeriod ->
					startCache = newStart
					endCache = newEnd
					periodCache = newPeriod
					showDateSelector = false
				},
			)
		}

		if (openConfirmFinishDialog.value) {
			AlertDialog(
				onDismissRequest = { openConfirmFinishDialog.value = false },
				title = { Text("¿Finalizar presupuesto?") },
				text = { Text("Esto cerrará el período actual y comenzará uno nuevo.") },
				confirmButton = {
					TextButton(
						onClick = {
							openConfirmFinishDialog.value = false
							haptic.performHapticFeedback(HapticFeedbackType.LongPress)
							onFinishEarly()
						},
					) { Text("Finalizar", color = MaterialTheme.colorScheme.error) }
				},
				dismissButton = {
					TextButton(onClick = { openConfirmFinishDialog.value = false }) {
						Text("Cancelar")
					}
				},
			)
		}
	}
}

@Composable
fun BudgetEditor(
	initialBudget: BigDecimal,
	startDate: LocalDate,
	endDate: LocalDate?,
	selectedPeriod: BudgetPeriod,
	totalDays: Int,
	currencyCode: String,
	previousPeriodDays: Int,
	onBudgetChange: (BigDecimal) -> Unit,
	onPickDates: () -> Unit,
	onPeriodChange: (BudgetPeriod) -> Unit,
	onApplyPreviousPeriod: (Int) -> Unit,
) {
	val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

	// Plain number format for input display (no automatic decimals added)
	val plainNumberFormat = remember(currencyCode) {
		val symbol = SupportedCurrency.findByCode(currencyCode)?.symbol ?: "$"
		(DecimalFormat.getNumberInstance(java.util.Locale.getDefault()) as DecimalFormat).apply {
			maximumFractionDigits = 0
			minimumFractionDigits = 0
			positivePrefix = symbol
		}
	}

	// Format initial budget with currency (plain format)
	// Store as raw digits for currency input transformation
	var budgetText by remember(initialBudget) {
		mutableStateOf(
			if (initialBudget > BigDecimal.ZERO) {
				// Convert to cents (multiply by 100 and round)
				(initialBudget.multiply(BigDecimal(100)).toBigInteger()).toString()
			} else ""
		)
	}

	// All possible periods
	val allPeriods = BudgetPeriod.entries.toList()

	// Available periods based on total days
	val available =
		if (totalDays > 0) availablePeriodsFor(totalDays) else listOf(BudgetPeriod.DAILY)

	LaunchedEffect(available) {
		if (selectedPeriod !in available) onPeriodChange(BudgetPeriod.DAILY)
	}

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp),
	) {
		Text(
			text = "Monto del presupuesto",
			style = MaterialTheme.typography.bodyMedium,
			color = MaterialTheme.colorScheme.onSurface,
			modifier = Modifier.padding(bottom = 8.dp),
		)

		OutlinedTextField(
			value = budgetText,
			onValueChange = { newValue ->
				// Only allow digits
				val filtered = newValue.filter { it.isDigit() }
				budgetText = filtered
				// Convert from cents to actual amount
				val amount = filtered.toBigDecimalOrNull()?.divide(BigDecimal(100)) ?: BigDecimal.ZERO
				onBudgetChange(amount)
			},
			visualTransformation = CurrencyAmountInputVisualTransformation(),
			keyboardOptions = KeyboardOptions(
				keyboardType = KeyboardType.NumberPassword,
			),
			modifier = Modifier.fillMaxWidth(),
			singleLine = true,
			shape = MaterialTheme.shapes.medium,
			colors = OutlinedTextFieldDefaults.colors(
				focusedContainerColor = MaterialTheme.colorScheme.surface,
				unfocusedContainerColor = MaterialTheme.colorScheme.surface,
			),
		)

		Spacer(Modifier.height(24.dp))

		Text(
			text = "Período de tiempo",
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.Medium,
			modifier = Modifier.padding(bottom = 8.dp),
		)

		OutlinedCard(
			modifier = Modifier.fillMaxWidth(),
			onClick = onPickDates,
			shape = MaterialTheme.shapes.medium,
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 16.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Surface(
					shape = MaterialTheme.shapes.small,
					color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
					modifier = Modifier.size(48.dp),
				) {
					Box(contentAlignment = Alignment.Center) {
						Icon(
							imageVector = Icons.Outlined.DateRange,
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary,
							modifier = Modifier.size(24.dp),
						)
					}
				}

				Spacer(Modifier.width(16.dp))

				Column(modifier = Modifier.weight(1f)) {
					if (endDate != null) {
						Text(
							text = "${startDate.format(dateFormatter)} - ${
								endDate.format(
									dateFormatter
								)
							}",
							style = MaterialTheme.typography.bodyMedium,
							fontWeight = FontWeight.Medium,
						)
						Text(
							text = "$totalDays días seleccionados",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
						)
					} else {
						Text(
							text = "Selecciona las fechas",
							style = MaterialTheme.typography.bodyMedium,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
						)
					}
				}

				Icon(
					imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
					contentDescription = "Seleccionar",
					tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
				)
			}
		}

		// AssistChip for previous period suggestion
		if (previousPeriodDays > 0) {
			Spacer(Modifier.height(8.dp))
			Row(
				modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start
			) {
				AssistChip(
					onClick = { onApplyPreviousPeriod(previousPeriodDays) },
					label = { Text("Usar $previousPeriodDays días (período anterior)") },
					leadingIcon = {
						Icon(
							Icons.Filled.Check,
							contentDescription = null,
							modifier = Modifier.size(18.dp)
						)
					},
					colors = AssistChipDefaults.assistChipColors(
						containerColor = MaterialTheme.colorScheme.secondaryContainer,
						labelColor = MaterialTheme.colorScheme.onSecondaryContainer
					)
				)
			}
		}

		if (endDate != null && totalDays > 0) {
			Spacer(Modifier.height(24.dp))

			Text(
				text = "¿Cómo repartir el presupuesto?",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Medium,
				modifier = Modifier.padding(bottom = 12.dp),
			)

			val budget =
				budgetText.replace(Regex("[^0-9.]"), "").toBigDecimalOrNull() ?: BigDecimal.ZERO

			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				allPeriods.chunked(2).forEach { rowPeriods ->
					Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
						rowPeriods.forEach { period ->
							val isAvailable = period in available
							val isSelected = selectedPeriod == period
							val preview =
								if (budget > BigDecimal.ZERO && totalDays > 0 && isAvailable) {
									budgetForPeriod(budget, totalDays, period)
								} else BigDecimal.ZERO

							PeriodGridCard(
								period = period,
								budgetAmount = preview,
								currencyFormatter = plainNumberFormat,
								isSelected = isSelected,
								enabled = isAvailable,
								onClick = { if (isAvailable) onPeriodChange(period) },
								modifier = Modifier.weight(1f),
							)
						}
						if (rowPeriods.size == 1) Spacer(Modifier.weight(1f))
					}
				}
			}
		}

		Spacer(Modifier.height(24.dp))
	}
}

@Composable
private fun PeriodGridCard(
	period: BudgetPeriod,
	budgetAmount: BigDecimal,
	currencyFormatter: java.text.NumberFormat,
	isSelected: Boolean,
	enabled: Boolean = true,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val borderColor = when {
		!enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
		isSelected -> MaterialTheme.colorScheme.primary
		else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
	}
	val backgroundColor = when {
		!enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
		isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
		else -> MaterialTheme.colorScheme.surface
	}
	val textColor = when {
		!enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
		isSelected -> MaterialTheme.colorScheme.primary
		else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
	}
	val amountColor = when {
		!enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
		isSelected -> MaterialTheme.colorScheme.primary
		else -> MaterialTheme.colorScheme.onSurface
	}

	OutlinedCard(
		modifier = modifier
			.heightIn(min = 100.dp)
			.clickable(enabled = enabled, onClick = onClick),
		onClick = onClick,
		shape = MaterialTheme.shapes.medium,
		border = BorderStroke(
			width = if (isSelected) 2.dp else 1.dp, color = borderColor
		),
		colors = CardDefaults.outlinedCardColors(
			containerColor = backgroundColor, disabledContainerColor = backgroundColor
		),
		enabled = enabled
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
		) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
			) {
				Icon(
					imageVector = Icons.Outlined.DateRange,
					contentDescription = null,
					tint = textColor,
					modifier = Modifier.size(20.dp),
				)
				if (isSelected && enabled) {
					Icon(
						imageVector = Icons.Default.Check,
						contentDescription = "Seleccionado",
						tint = MaterialTheme.colorScheme.primary,
						modifier = Modifier.size(20.dp),
					)
				}
			}

			Spacer(Modifier.height(8.dp))

			Text(
				text = when (period) {
					BudgetPeriod.DAILY -> "Diario"
					BudgetPeriod.WEEKLY -> "Semanal"
					BudgetPeriod.BIWEEKLY -> "Quincenal"
					BudgetPeriod.MONTHLY -> "Mensual"
				},
				style = MaterialTheme.typography.bodyMedium,
				fontWeight = FontWeight.Medium,
				color = textColor
			)

			Spacer(Modifier.height(4.dp))

			Text(
				text = if (enabled && budgetAmount > BigDecimal.ZERO) {
					currencyFormatter.format(budgetAmount)
				} else "-",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.Bold,
				color = amountColor,
			)
		}
	}
}

@Composable
fun BudgetSummary(
	budget: BigDecimal,
	spent: BigDecimal,
	restBudget: BigDecimal,
	startDate: LocalDate,
	endDate: LocalDate?,
	period: BudgetPeriod,
	totalDays: Int,
	currencyCode: String,
	dateFormatter: DateTimeFormatter,
) {
	val currencyFormat = remember(currencyCode) {
		symbolOnlyCurrencyFormat(currencyCode)
	}
	val periodBudget = if (totalDays > 0) budgetForPeriod(budget, totalDays, period) else budget

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(16.dp),
	) {
		Card(
			modifier = Modifier.fillMaxWidth(),
			colors = CardDefaults.cardColors(containerColor = colorEditor),
		) {
			Column(
				modifier = Modifier.padding(16.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				Text(
					text = "Presupuesto Total",
					style = MaterialTheme.typography.bodyMedium,
					color = colorOnEditor.copy(alpha = 0.7f),
				)
				Text(
					text = currencyFormat.format(budget),
					style = MaterialTheme.typography.headlineLarge,
					color = colorOnEditor,
					fontWeight = FontWeight.Bold,
				)
			}
		}

		Spacer(Modifier.height(12.dp))

		Card(
			modifier = Modifier.fillMaxWidth(),
			colors = CardDefaults.cardColors(
				containerColor = MaterialTheme.colorScheme.secondaryContainer,
			),
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = 16.dp, vertical = 10.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
			) {
				Column {
					Text(
						text = period.label(),
						style = MaterialTheme.typography.labelSmall,
						color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
					)
					Text(
						text = currencyFormat.format(periodBudget),
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSecondaryContainer,
						fontWeight = FontWeight.Bold,
					)
				}
				Text(
					text = "por ${period.label().lowercase()}",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
				)
			}
		}

		Spacer(Modifier.height(16.dp))

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
		) {
			Column {
				Text(
					text = "Inicio",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
				)
				Text(
					text = startDate.format(dateFormatter),
					style = MaterialTheme.typography.bodyMedium,
				)
			}
			Column(horizontalAlignment = Alignment.End) {
				Text(
					text = "Fin",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
				)
				Text(
					text = endDate?.format(dateFormatter) ?: "No definido",
					style = MaterialTheme.typography.bodyMedium,
				)
			}
		}

		Spacer(Modifier.height(16.dp))
		HorizontalDivider()
		Spacer(Modifier.height(16.dp))

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceEvenly,
		) {
			BudgetStat(
				label = "Gastado",
				value = currencyFormat.format(spent),
				color = MaterialTheme.colorScheme.error,
			)
			BudgetStat(
				label = "Restante",
				value = currencyFormat.format(restBudget),
				color = if (restBudget >= BigDecimal.ZERO) colorPrimary else MaterialTheme.colorScheme.error,
			)
		}
	}
}

@Composable
fun BudgetStat(label: String, value: String, color: Color) {
	Column(horizontalAlignment = Alignment.CenterHorizontally) {
		Text(
			text = label,
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
		)
		Text(
			text = value,
			style = MaterialTheme.typography.titleLarge,
			color = color,
			fontWeight = FontWeight.Bold,
		)
	}
}

@Preview(showBackground = true, name = "Period Grid Card - Selected")
@Composable
private fun PeriodGridCardSelectedPreview() {
	MinusTheme {
		Box(modifier = Modifier.padding(16.dp)) {
			PeriodGridCard(
				period = BudgetPeriod.MONTHLY,
				budgetAmount = BigDecimal("1200.00"),
				currencyFormatter = java.text.NumberFormat.getCurrencyInstance(),
				isSelected = true,
				onClick = {}
			)
		}
	}
}

@Preview(showBackground = true, name = "Period Grid Card - Unselected")
@Composable
private fun PeriodGridCardUnselectedPreview() {
	MinusTheme {
		Box(modifier = Modifier.padding(16.dp)) {
			PeriodGridCard(
				period = BudgetPeriod.WEEKLY,
				budgetAmount = BigDecimal("300.00"),
				currencyFormatter = java.text.NumberFormat.getCurrencyInstance(),
				isSelected = false,
				onClick = {}
			)
		}
	}
}

@Preview(showBackground = true, name = "Period Grid Card - Disabled")
@Composable
private fun PeriodGridCardDisabledPreview() {
	MinusTheme {
		Box(modifier = Modifier.padding(16.dp)) {
			PeriodGridCard(
				period = BudgetPeriod.DAILY,
				budgetAmount = BigDecimal("42.85"),
				currencyFormatter = java.text.NumberFormat.getCurrencyInstance(),
				isSelected = false,
				enabled = false,
				onClick = {}
			)
		}
	}
}

// ============== BudgetEditor Previews ==============

@Preview(showBackground = true, name = "Budget Editor - Empty")
@Composable
fun BudgetEditorEmptyPreview() {
	MinusTheme {
		BudgetEditor(
			initialBudget = BigDecimal.ZERO,
			startDate = LocalDate.now(),
			endDate = LocalDate.now().plusDays(30),
			selectedPeriod = BudgetPeriod.MONTHLY,
			totalDays = 31,
			currencyCode = "USD",
			previousPeriodDays = 0,
			onBudgetChange = {},
			onPickDates = {},
			onPeriodChange = {},
			onApplyPreviousPeriod = {},
		)
	}
}

@Preview(showBackground = true, name = "Budget Editor - With Budget")
@Composable
fun BudgetEditorWithBudgetPreview() {
	MinusTheme {
		BudgetEditor(
			initialBudget = BigDecimal("1500"),
			startDate = LocalDate.now(),
			endDate = LocalDate.now().plusDays(30),
			selectedPeriod = BudgetPeriod.MONTHLY,
			totalDays = 31,
			currencyCode = "USD",
			previousPeriodDays = 0,
			onBudgetChange = {},
			onPickDates = {},
			onPeriodChange = {},
			onApplyPreviousPeriod = {},
		)
	}
}

@Preview(showBackground = true, name = "Budget Editor - With Previous Period")
@Composable
fun BudgetEditorWithPreviousPeriodPreview() {
	MinusTheme {
		BudgetEditor(
			initialBudget = BigDecimal("1200"),
			startDate = LocalDate.now().minusDays(30),
			endDate = LocalDate.now().minusDays(1),
			selectedPeriod = BudgetPeriod.MONTHLY,
			totalDays = 30,
			currencyCode = "USD",
			previousPeriodDays = 30,
			onBudgetChange = {},
			onPickDates = {},
			onPeriodChange = {},
			onApplyPreviousPeriod = {},
		)
	}
}

// ============== BudgetSummary Previews ==============

@Preview(showBackground = true, name = "Budget Summary - Normal")
@Composable
fun BudgetSummaryNormalPreview() {
	MinusTheme {
		BudgetSummary(
			budget = BigDecimal("1500"),
		spent = BigDecimal("800"),
			restBudget = BigDecimal("700"),
			startDate = LocalDate.of(2026, 4, 1),
			endDate = LocalDate.of(2026, 4, 30),
			period = BudgetPeriod.MONTHLY,
			totalDays = 30,
			currencyCode = "USD",
			dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy"),
		)
	}
}

@Preview(showBackground = true, name = "Budget Summary - Overspent")
@Composable
fun BudgetSummaryOverspentPreview() {
	MinusTheme {
		BudgetSummary(
			budget = BigDecimal("1000"),
			spent = BigDecimal("1500"),
			restBudget = BigDecimal("-500"),
			startDate = LocalDate.of(2026, 4, 1),
			endDate = LocalDate.of(2026, 4, 30),
			period = BudgetPeriod.MONTHLY,
			totalDays = 30,
			currencyCode = "USD",
			dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy"),
		)
	}
}

@Preview(showBackground = true, name = "Budget Summary - No End Date")
@Composable
fun BudgetSummaryNoEndDatePreview() {
	MinusTheme {
		BudgetSummary(
			budget = BigDecimal("2000"),
			spent = BigDecimal("500"),
			restBudget = BigDecimal("1500"),
			startDate = LocalDate.of(2026, 4, 1),
			endDate = null,
			period = BudgetPeriod.MONTHLY,
			totalDays = 0,
			currencyCode = "USD",
			dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy"),
		)
	}
}

// ============== BudgetStat Previews ==============

@Preview(showBackground = true, name = "Budget Stat - Positive")
@Composable
fun BudgetStatPositivePreview() {
	MinusTheme {
		BudgetStat(
			label = "Restante",
			value = "$700.00",
			color = colorPrimary,
		)
	}
}

@Preview(showBackground = true, name = "Budget Stat - Negative")
@Composable
fun BudgetStatNegativePreview() {
	MinusTheme {
		BudgetStat(
			label = "Gastado",
			value = "$1,500.00",
			color = MaterialTheme.colorScheme.error,
		)
	}
}

// ============== WalletContent Previews ==============
// Note: WalletContent requires LocalWindowInsets and LocalBottomSheetScrollState 
// which are provided at runtime but not in preview mode. 
// Preview functionality is available through the app's preview system.
