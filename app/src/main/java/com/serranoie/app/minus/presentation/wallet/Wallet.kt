package com.serranoie.app.minus.presentation.wallet

import android.content.res.Configuration
import android.util.Log
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.LocalWindowInsets
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.presentation.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.editor.EditBudgetContent
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import com.serranoie.app.minus.presentation.onboarding.FinishDateSelector
import com.serranoie.app.minus.presentation.onboarding.availablePeriodsFor
import com.serranoie.app.minus.presentation.onboarding.budgetForPeriod
import com.serranoie.app.minus.presentation.onboarding.label
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorEditor
import com.serranoie.app.minus.presentation.ui.theme.colorOnEditor
import com.serranoie.app.minus.presentation.ui.theme.colorPrimary
import com.serranoie.app.minus.presentation.ui.theme.component.BottomSheetScrollState
import com.serranoie.app.minus.presentation.ui.theme.component.LocalBottomSheetScrollState
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

const val WALLET_SHEET = "wallet"
@Composable
fun Wallet(
	forceChange: Boolean = false,
	activityResultRegistryOwner: ActivityResultRegistryOwner? = null,
	budgetViewModel: BudgetViewModel = hiltViewModel(),
	onClose: () -> Unit = {},
	onOnboardingComplete: () -> Unit = {},
) {
	val tag = "Wallet - ISAAC"
	Log.d(tag, "Wallet composable entered: forceChange=$forceChange")
	
	val uiState by budgetViewModel.uiState.collectAsStateWithLifecycle()
	val budgetSettings = uiState.budgetSettings
	
	Log.d(tag, "budgetSettings from ViewModel: $budgetSettings")

	Surface(modifier = Modifier.fillMaxSize()) {
		EditBudgetContent(
			budgetSettings = budgetSettings,
			title = if (budgetSettings != null) "Editar presupuesto" else "Nuevo presupuesto",
			buttonLabel = if (budgetSettings != null) "Actualizar" else "Aplicar",
			showPreviousValuesChip = budgetSettings != null,
			onBack = onClose,
			onApply = { newSettings ->
				Log.d(tag, "Saving budget settings: $newSettings")
				budgetViewModel.saveBudgetSettings(newSettings)
				if (forceChange || budgetSettings == null) {
					Log.d(tag, "First budget setup - calling onOnboardingComplete()")
					onOnboardingComplete()
				}
				Log.d(tag, "Calling onClose()")
				onClose()
			},
		)
	}
}

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
	val statusBarHeight = LocalWindowInsets.current.calculateTopPadding()
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

	Surface(
		modifier = Modifier
			.fillMaxSize()
			.padding(top = localBottomSheetScrollState.topPadding + statusBarHeight)
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

@Composable
private fun BudgetEditor(
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
	val currencyFormatter = remember(currencyCode) {
		symbolOnlyCurrencyFormat(currencyCode)
	}
	
	// Format initial budget with currency
	var budgetText by remember(initialBudget) {
		mutableStateOf(
			if (initialBudget > BigDecimal.ZERO) {
				currencyFormatter.format(initialBudget).replace(currencyCode, "").trim()
			} else ""
		)
	}

	// All possible periods
	val allPeriods = BudgetPeriod.entries.toList()
	
	// Available periods based on total days
	val available = if (totalDays > 0) availablePeriodsFor(totalDays) else listOf(BudgetPeriod.DAILY)

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
			onValueChange = {
				budgetText = it
				// Remove currency symbols and parse
				val cleanValue = it.replace(Regex("[^0-9.]"), "")
				cleanValue.toBigDecimalOrNull()?.let(onBudgetChange)
			},
			keyboardOptions = KeyboardOptions(
				keyboardType = KeyboardType.Number,
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
							text = "${startDate.format(dateFormatter)} - ${endDate.format(dateFormatter)}",
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
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.Start
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

			val budget = budgetText.replace(Regex("[^0-9.]"), "").toBigDecimalOrNull() ?: BigDecimal.ZERO

			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				allPeriods.chunked(2).forEach { rowPeriods ->
					Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
						rowPeriods.forEach { period ->
							val isAvailable = period in available
							val isSelected = selectedPeriod == period
							val preview = if (budget > BigDecimal.ZERO && totalDays > 0 && isAvailable) {
								budgetForPeriod(budget, totalDays, period)
							} else BigDecimal.ZERO

							PeriodGridCard(
								period = period,
								budgetAmount = preview,
								currencyFormatter = currencyFormatter,
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
			containerColor = backgroundColor,
			disabledContainerColor = backgroundColor
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
private fun BudgetSummary(
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
private fun BudgetStat(label: String, value: String, color: Color) {
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

@Composable
private fun PreviewWrapper(content: @Composable () -> Unit) {
	MinusTheme {
		CompositionLocalProvider(
			LocalWindowInsets provides androidx.compose.foundation.layout.PaddingValues(0.dp),
			LocalBottomSheetScrollState provides BottomSheetScrollState(topPadding = 0.dp),
		) { content() }
	}
}

@Preview(name = "Setup – empty", showBackground = true, device = "id:pixel_5")
@Composable
private fun PreviewSetupEmpty() {
	PreviewWrapper {
		WalletContent(forceChange = true, hasBudgetSettings = false)
	}
}

@Preview(name = "Setup – budget typed, no dates", showBackground = true, device = "id:pixel_5")
@Composable
private fun PreviewSetupBudgetNoDate() {
	PreviewWrapper {
		WalletContent(
			forceChange = true,
			currentBudget = BigDecimal("500.00"),
			period = BudgetPeriod.DAILY,
			hasBudgetSettings = false,
		)
	}
}

@Preview(
	name = "Setup – budget + 7 days, weekly selected", showBackground = true, device = "id:pixel_5"
)
@Composable
private fun PreviewSetup7Days() {
	PreviewWrapper {
		WalletContent(
			forceChange = true,
			currentBudget = BigDecimal("500.00"),
			startDate = LocalDate.now(),
			endDate = LocalDate.now().plusDays(6),
			period = BudgetPeriod.WEEKLY,
			currencyCode = "USD",
			hasBudgetSettings = false,
		)
	}
}

@Preview(name = "Setup – 30 days, monthly selected", showBackground = true, device = "id:pixel_5")
@Composable
private fun PreviewSetup30Days() {
	PreviewWrapper {
		WalletContent(
			forceChange = true,
			currentBudget = BigDecimal("3000.00"),
			startDate = LocalDate.now(),
			endDate = LocalDate.now().plusDays(29),
			period = BudgetPeriod.MONTHLY,
			currencyCode = "USD",
			hasBudgetSettings = false,
		)
	}
}

@Preview(name = "View – healthy budget", showBackground = true, device = "id:pixel_5")
@Composable
private fun PreviewViewHealthy() {
	PreviewWrapper {
		WalletContent(
			currentBudget = BigDecimal("3000.00"),
			currentSpent = BigDecimal("900.00"),
			startDate = LocalDate.now().minusDays(10),
			endDate = LocalDate.now().plusDays(20),
			currencyCode = "USD",
			period = BudgetPeriod.MONTHLY,
			hasBudgetSettings = true,
		)
	}
}

@Preview(name = "View – over budget", showBackground = true, device = "id:pixel_5")
@Composable
private fun PreviewViewOverBudget() {
	PreviewWrapper {
		WalletContent(
			currentBudget = BigDecimal("3000.00"),
			currentSpent = BigDecimal("3450.00"),
			startDate = LocalDate.now().minusDays(28),
			endDate = LocalDate.now().plusDays(2),
			currencyCode = "USD",
			period = BudgetPeriod.MONTHLY,
			hasBudgetSettings = true,
		)
	}
}

@Preview(name = "Edit – weekly MXN", showBackground = true, device = "id:pixel_5")
@Composable
private fun PreviewEditWeeklyMxn() {
	PreviewWrapper {
		WalletContent(
			forceChange = false,
			currentBudget = BigDecimal("1200.00"),
			currentSpent = BigDecimal("300.00"),
			startDate = LocalDate.now().minusDays(3),
			endDate = LocalDate.now().plusDays(3),
			currencyCode = "MXN",
			period = BudgetPeriod.WEEKLY,
			hasBudgetSettings = true,
		)
	}
}

@Preview(
	name = "Dark – view mode",
	showBackground = true,
	uiMode = Configuration.UI_MODE_NIGHT_YES,
	device = "id:pixel_5",
)
@Composable
private fun PreviewDark() {
	PreviewWrapper {
		WalletContent(
			currentBudget = BigDecimal("2500.00"),
			currentSpent = BigDecimal("900.00"),
			startDate = LocalDate.now().minusDays(15),
			endDate = LocalDate.now().plusDays(15),
			currencyCode = "USD",
			period = BudgetPeriod.MONTHLY,
			hasBudgetSettings = true,
		)
	}
}
