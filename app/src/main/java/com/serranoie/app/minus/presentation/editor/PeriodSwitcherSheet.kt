@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.onboarding.FinishDateSelector
import com.serranoie.app.minus.presentation.onboarding.availablePeriodsFor
import com.serranoie.app.minus.presentation.onboarding.budgetForPeriod
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetDisplay
import com.serranoie.app.minus.presentation.ui.theme.component.budget.SpendBudgetCard
import com.serranoie.app.minus.presentation.ui.theme.component.date.DaysLeftCard
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Date
import java.util.Locale

@Composable
fun PeriodSwitcherSheet(
	budgetSettings: BudgetSettings?,
	budgetState: BudgetState?,
	selectedPeriod: BudgetPeriod = budgetSettings?.period ?: BudgetPeriod.DAILY,
	currencyCode: String,
	onPeriodSelected: (BudgetPeriod) -> Unit,
	onSaveBudget: ((BudgetSettings) -> Unit)? = null,
	onEditBudget: (() -> Unit)? = null,
	onFinishEarly: (() -> Unit)? = null,
) {
	val haptic = LocalHapticFeedback.current
	val currencyFormat = remember(currencyCode) {
		symbolOnlyCurrencyFormat(currencyCode, maximumFractionDigits = 0)
	}

	val startDate = budgetSettings?.startDate ?: LocalDate.now()
	val endDate = budgetSettings?.endDate
	val totalBudget = budgetSettings?.totalBudget ?: BigDecimal.ZERO
	var periodCache by remember(selectedPeriod) { mutableStateOf(selectedPeriod) }

	var isEditMode by remember { mutableStateOf(false) }
	var showFinishConfirm by remember { mutableStateOf(false) }

	val totalDays = remember(startDate, endDate) {
		if (endDate != null) ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1 else 30
	}

	fun LocalDate.toDate(): Date = Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())

	val startDateAsDate = remember(startDate) { startDate.toDate() }
	val endDateAsDate = remember(endDate) { endDate?.toDate() }
	val totalSpent = budgetState?.totalSpentInPeriod ?: BigDecimal.ZERO

	val available =
		if (totalDays > 0) availablePeriodsFor(totalDays) else listOf(BudgetPeriod.DAILY)

	LaunchedEffect(available, totalDays) {
		if (periodCache !in available && available.isNotEmpty()) {
			periodCache = available.first()
			onPeriodSelected(periodCache)
		}
	}

	AnimatedContent(
		targetState = isEditMode,
		transitionSpec = {
			if (targetState) {
				// Forward: entering edit mode
				(slideInHorizontally(initialOffsetX = { it / 3 }, animationSpec = tween(300))
						+ fadeIn(tween(250, delayMillis = 50)))
					.togetherWith(
						slideOutHorizontally(
							targetOffsetX = { -it / 3 },
							animationSpec = tween(300)
						) + fadeOut(tween(200))
					)
			} else {
				// Backward: exiting edit mode
				(slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300))
						+ fadeIn(tween(250, delayMillis = 50)))
					.togetherWith(
						slideOutHorizontally(
							targetOffsetX = { it / 3 },
							animationSpec = tween(300)
						) + fadeOut(tween(200))
					)
			}
		},
		label = "sheetContent"
	) { editMode ->
		if (editMode) {
			EditBudgetContent(
				budgetSettings = budgetSettings,
				onBack = { isEditMode = false },
				onApply = { newSettings ->
					onSaveBudget?.invoke(newSettings)
					isEditMode = false
				}
			)
		} else {
			ViewBudgetContent(
				budgetSettings = budgetSettings,
				budgetState = budgetState,
				periodCache = periodCache,
				currencyFormat = currencyFormat,
				currencyCode = currencyCode,
				totalBudget = totalBudget,
				totalSpent = totalSpent,
				totalDays = totalDays,
				startDateAsDate = startDateAsDate,
				endDateAsDate = endDateAsDate,
				available = available,
				onPeriodSelected = { p ->
					periodCache = p
					onPeriodSelected(p)
				},
				onEditClick = {
					if (onSaveBudget != null) {
						isEditMode = true
					} else {
						onEditBudget?.invoke()
					}
				},
				onFinishEarlyClick = { showFinishConfirm = true },
				showFinishEarly = onFinishEarly != null && budgetSettings != null,
			)
		}
	}

	// Finish Early Confirmation Dialog
	if (showFinishConfirm) {
		AlertDialog(
			onDismissRequest = { showFinishConfirm = false },
			title = { Text("¿Finalizar presupuesto?") },
			text = { Text("Esto cerrará el período actual y comenzará uno nuevo. ¿Estás seguro?") },
			confirmButton = {
				TextButton(
					onClick = {
						showFinishConfirm = false
						haptic.performHapticFeedback(HapticFeedbackType.LongPress)
						onFinishEarly?.invoke()
					},
				) { Text("Finalizar", color = MaterialTheme.colorScheme.error) }
			},
			dismissButton = {
				TextButton(onClick = { showFinishConfirm = false }) { Text("Cancelar") }
			},
		)
	}
}

// ─── View Mode ───────────────────────────────────────────────────────────────

@Composable
private fun ViewBudgetContent(
	budgetSettings: BudgetSettings?,
	budgetState: BudgetState?,
	periodCache: BudgetPeriod,
	currencyFormat: NumberFormat,
	currencyCode: String,
	totalBudget: BigDecimal,
	totalSpent: BigDecimal,
	totalDays: Int,
	startDateAsDate: Date,
	endDateAsDate: Date?,
	available: List<BudgetPeriod>,
	onPeriodSelected: (BudgetPeriod) -> Unit,
	onEditClick: () -> Unit,
	onFinishEarlyClick: () -> Unit,
	showFinishEarly: Boolean,
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 20.dp)
			.padding(top = 4.dp, bottom = 32.dp)
			.verticalScroll(rememberScrollState()),
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 16.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically,
		) {
			Text(
				text = "Presupuesto",
				style = MaterialTheme.typography.headlineMediumEmphasized,
				fontWeight = FontWeight.Bold,
			)
			IconButton(
				onClick = onEditClick,
				modifier = Modifier.size(40.dp)
			) {
				Icon(
					imageVector = Icons.Default.Edit,
					contentDescription = "Editar presupuesto",
					tint = MaterialTheme.colorScheme.onSurface,
				)
			}
		}

		Spacer(modifier = Modifier.height(16.dp))

		SpendBudgetCard(
			spend = totalSpent,
			budget = totalBudget,
			modifier = Modifier.fillMaxWidth(),
		)

		Spacer(modifier = Modifier.height(12.dp))

		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
		) {
			BudgetDisplay(
				budget = totalBudget,
				budgetState = budgetState,
				budgetSettings = budgetSettings,
				currencyCode = currencyCode,
				bigVariant = false,
				modifier = Modifier.weight(1.5f),
				startDate = startDateAsDate,
				finishDate = endDateAsDate
			)

			if (endDateAsDate != null) {
				DaysLeftCard(
					startDate = startDateAsDate,
					finishDate = endDateAsDate,
					modifier = Modifier.weight(1f)
				)
			} else {
				Card(
					modifier = Modifier
						.weight(1f)
						.heightIn(min = 100.dp),
					colors = CardDefaults.cardColors(
						containerColor = MaterialTheme.colorScheme.surfaceVariant
					),
					shape = RoundedCornerShape(12.dp)
				) {
					Box(
						modifier = Modifier.fillMaxWidth(),
						contentAlignment = Alignment.Center
					) {
						Text(
							text = "Sin fecha de fin",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
							modifier = Modifier.padding(16.dp)
						)
					}
				}
			}
		}

		Spacer(modifier = Modifier.height(24.dp))

		Text(
			text = "¿Cómo repartir el presupuesto?",
			style = MaterialTheme.typography.titleMedium,
			fontWeight = FontWeight.Medium,
			modifier = Modifier.padding(bottom = 12.dp),
		)

		if (totalDays > 0) {
			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				available.chunked(2).forEach { rowPeriods ->
					Row(
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						modifier = Modifier.fillMaxWidth()
					) {
						rowPeriods.forEach { p ->
							val isSelected = p == periodCache
							val preview = if (totalBudget > BigDecimal.ZERO && totalDays > 0) {
								budgetForPeriod(totalBudget, totalDays, p)
							} else BigDecimal.ZERO

							CompactPeriodCard(
								period = p,
								budgetAmount = preview,
								currencyFormat = currencyFormat,
								isSelected = isSelected,
								onClick = { onPeriodSelected(p) },
								modifier = Modifier.weight(1f)
							)
						}
						if (rowPeriods.size == 1) {
							Spacer(modifier = Modifier.weight(1f))
						}
					}
				}
			}
		}

		Spacer(modifier = Modifier.height(24.dp))

		HorizontalDivider()

		Spacer(modifier = Modifier.height(16.dp))

		if (showFinishEarly) {
			OutlinedButton(
				onClick = onFinishEarlyClick,
				modifier = Modifier.fillMaxWidth(),
				colors = ButtonDefaults.outlinedButtonColors(
					contentColor = MaterialTheme.colorScheme.error,
				),
			) {
				Text(
					text = "Finalizar presupuesto temprano",
					style = MaterialTheme.typography.bodyMedium,
				)
			}
		}
	}
}


/**
 * Reusable budget editor content used in both PeriodSwitcherSheet and Wallet/Onboarding.
 */
@Composable
fun EditBudgetContent(
	budgetSettings: BudgetSettings?,
	onBack: () -> Unit = {},
	onApply: (BudgetSettings) -> Unit,
	title: String = "Nuevo presupuesto",
	buttonLabel: String = "Aplicar",
	showPreviousValuesChip: Boolean = true,
) {
	val haptic = LocalHapticFeedback.current
	val dateFormatter = remember {
		DateTimeFormatter.ofPattern("d MMMM", Locale("es", "ES"))
	}

	val currentBudget = budgetSettings?.totalBudget ?: BigDecimal.ZERO
	val currentStart = budgetSettings?.startDate ?: LocalDate.now()
	val currentEnd = budgetSettings?.endDate
	val currentCurrency = budgetSettings?.currencyCode ?: "USD"
	val currentStrategy =
		budgetSettings?.remainingBudgetStrategy ?: RemainingBudgetStrategy.ASK_ALWAYS

	val previousPeriodDays = remember(currentStart, currentEnd) {
		if (currentEnd != null) ChronoUnit.DAYS.between(currentStart, currentEnd)
			.toInt() + 1 else 0
	}

	val currencySymbol = remember(currentCurrency) {
		SupportedCurrency.findByCode(currentCurrency)?.symbol ?: "$"
	}

	var budgetText by remember {
		mutableStateOf(
			if (currentBudget > BigDecimal.ZERO) currentBudget.toPlainString() else ""
		)
	}
	var startCache by remember { mutableStateOf(LocalDate.now()) }
	var endCache by remember { mutableStateOf<LocalDate?>(null) }
	var currencyCache by remember { mutableStateOf(currentCurrency) }
	var strategyCache by remember { mutableStateOf(currentStrategy) }

	var showDateSelector by remember { mutableStateOf(false) }
	var showCurrencyPicker by remember { mutableStateOf(false) }
	var showStrategyPicker by remember { mutableStateOf(false) }
	var showPreviousValues by remember { mutableStateOf(false) }

	val activeCurrencySymbol = remember(currencyCache) {
		SupportedCurrency.findByCode(currencyCache)?.symbol ?: "$"
	}

	val parsedBudget =
		budgetText.replace(Regex("[^0-9.]"), "").toBigDecimalOrNull() ?: BigDecimal.ZERO
	val totalDays = endCache?.let { ChronoUnit.DAYS.between(startCache, it).toInt() + 1 } ?: 0
	val canApply = parsedBudget > BigDecimal.ZERO && totalDays > 0

	// Validation message
	val validationMessage = when {
		parsedBudget <= BigDecimal.ZERO && budgetText.isNotEmpty() -> "El presupuesto debe ser mayor a 0"
		endCache == null -> "Sin fecha final definida"
		totalDays < 1 -> "Calcula el presupuesto para al menos un día"
		else -> null
	}

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 20.dp)
			.padding(top = 4.dp, bottom = 32.dp)
			.verticalScroll(rememberScrollState()),
	) {
		// Title
		Text(
			text = title,
			style = MaterialTheme.typography.headlineMediumEmphasized,
			fontWeight = FontWeight.Bold,
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 16.dp),
			textAlign = TextAlign.Center,
		)

		// Valores anteriores chip
		if (showPreviousValuesChip && currentBudget > BigDecimal.ZERO) {
			AssistChip(
				onClick = { showPreviousValues = !showPreviousValues },
				label = { Text("Valores anteriores") },
				leadingIcon = {
					Icon(
						imageVector = Icons.Rounded.Sync,
						contentDescription = null,
						modifier = Modifier.size(18.dp)
					)
				},
				colors = AssistChipDefaults.assistChipColors(
					containerColor = MaterialTheme.colorScheme.secondaryContainer,
					labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
				),
			)

			if (showPreviousValues) {
				Spacer(modifier = Modifier.height(8.dp))
				Card(
					modifier = Modifier.fillMaxWidth(),
					colors = CardDefaults.cardColors(
						containerColor = MaterialTheme.colorScheme.surfaceVariant,
					),
					shape = RoundedCornerShape(12.dp),
				) {
					Column(modifier = Modifier.padding(12.dp)) {
						Text(
							text = "Presupuesto anterior: $currencySymbol${
								currentBudget.toPlainString()
							}",
							style = MaterialTheme.typography.bodyMedium,
						)
						if (currentEnd != null) {
							Text(
								text = "Período: $previousPeriodDays días",
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
							)
						}
						Spacer(modifier = Modifier.height(8.dp))
						AssistChip(
							onClick = {
								budgetText = currentBudget.toPlainString()
								if (previousPeriodDays > 0) {
									startCache = LocalDate.now()
									endCache =
										LocalDate.now().plusDays(previousPeriodDays.toLong() - 1)
								}
								currencyCache = currentCurrency
								strategyCache = currentStrategy
								showPreviousValues = false
							},
							label = { Text("Aplicar valores anteriores") },
							leadingIcon = {
								Icon(
									imageVector = Icons.Default.Check,
									contentDescription = null,
									modifier = Modifier.size(18.dp)
								)
							},
						)
					}
				}
			}
		}

		Spacer(modifier = Modifier.height(24.dp))

		// Budget amount input - large centered text with currency symbol
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 24.dp),
			contentAlignment = Alignment.Center,
		) {
			BasicTextField(
				value = budgetText,
				onValueChange = { newValue ->
					val filtered = newValue.filter { it.isDigit() || it == '.' }
					if (filtered.count { it == '.' } <= 1) {
						budgetText = filtered
					}
				},
				textStyle = TextStyle(
					fontSize = 48.sp,
					fontWeight = FontWeight.Bold,
					color = MaterialTheme.colorScheme.onSurface,
					textAlign = TextAlign.Center,
				),
				singleLine = true,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
				cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
				decorationBox = { innerTextField ->
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.Center,
						verticalAlignment = Alignment.CenterVertically,
					) {
						// Currency symbol prefix
						Text(
							text = activeCurrencySymbol,
							style = TextStyle(
								fontSize = 32.sp,
								fontWeight = FontWeight.Medium,
								color = MaterialTheme.colorScheme.onSurface.copy(
									alpha = if (budgetText.isEmpty()) 0.3f else 0.6f
								),
							),
						)
						Spacer(modifier = Modifier.width(4.dp))
						Box {
							if (budgetText.isEmpty()) {
								Text(
									text = "0",
									style = TextStyle(
										fontSize = 48.sp,
										fontWeight = FontWeight.Bold,
										color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
									),
								)
							}
							innerTextField()
						}
					}
				},
				modifier = Modifier.fillMaxWidth(),
			)
		}

		Spacer(modifier = Modifier.height(16.dp))

		// Date row - formatted as "16 Marzo a 15 Abril"
		SettingsRow(
			icon = Icons.Outlined.DateRange,
			label = if (endCache != null) {
				"${startCache.format(dateFormatter)} a ${endCache?.format(dateFormatter)}"
			} else {
				"Sin fecha final definida"
			},
			onClick = { showDateSelector = true },
		)

		// Previous days chip
		if (previousPeriodDays > 0 && endCache == null) {
			Spacer(modifier = Modifier.height(4.dp))
			Row(modifier = Modifier.fillMaxWidth()) {
				AssistChip(
					onClick = {
						startCache = LocalDate.now()
						endCache = LocalDate.now().plusDays(previousPeriodDays.toLong() - 1)
					},
					label = { Text("Usar $previousPeriodDays días") },
					leadingIcon = {
						Icon(
							Icons.Rounded.Sync,
							contentDescription = null,
							modifier = Modifier.size(18.dp)
						)
					},
					colors = AssistChipDefaults.assistChipColors(
						containerColor = MaterialTheme.colorScheme.secondaryContainer,
						labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
					),
				)
			}
		}

		Spacer(modifier = Modifier.height(8.dp))

		// Sobrante (remaining budget strategy)
		SettingsRow(
			icon = Icons.Default.Check,
			label = "Sobrante",
			trailingText = when (strategyCache) {
				RemainingBudgetStrategy.ASK_ALWAYS -> "Preguntarme siempre"
				RemainingBudgetStrategy.SPLIT_EQUALLY -> "Repartir entre días"
				RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> "Agregar al primer día"
			},
			onClick = { showStrategyPicker = true },
		)

		Spacer(modifier = Modifier.height(8.dp))

		// Currency
		val currencyDisplay = SupportedCurrency.findByCode(currencyCache)
		SettingsRow(
			icon = Icons.Default.Edit,
			label = "Divisa",
			trailingText = if (currencyDisplay != null) {
				"${currencyDisplay.symbol} ${currencyDisplay.code}"
			} else {
				currencyCache
			},
			onClick = { showCurrencyPicker = true },
		)

		// Validation message
		if (validationMessage != null) {
			Spacer(modifier = Modifier.height(16.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
			) {
				Icon(
					imageVector = Icons.Outlined.Info,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
					modifier = Modifier.size(20.dp),
				)
				Spacer(modifier = Modifier.width(8.dp))
				Text(
					text = validationMessage,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
				)
			}
		}

		Spacer(modifier = Modifier.height(24.dp))

		// Apply button
		Button(
			onClick = {
				haptic.performHapticFeedback(HapticFeedbackType.LongPress)
				val period = budgetSettings?.period ?: BudgetPeriod.DAILY
				val newSettings = (budgetSettings ?: BudgetSettings.DEFAULT).copy(
					totalBudget = parsedBudget,
					startDate = startCache,
					endDate = endCache,
					currencyCode = currencyCache,
					remainingBudgetStrategy = strategyCache,
					period = period,
				)
				onApply(newSettings)
			},
			modifier = Modifier
				.fillMaxWidth()
				.heightIn(min = 56.dp),
			enabled = canApply,
		) {
			Text(buttonLabel)
			Spacer(modifier = Modifier.width(8.dp))
			Icon(
				imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
				contentDescription = null,
			)
		}
	}

	// Date Selector
	if (showDateSelector) {
		FinishDateSelector(
			totalBudget = parsedBudget,
			currencyCode = currencyCache,
			onBackPressed = { showDateSelector = false },
			onApply = { newStart, newEnd, _ ->
				startCache = newStart
				endCache = newEnd
				showDateSelector = false
			},
		)
	}

	// Currency Picker Dialog
	if (showCurrencyPicker) {
		CurrencyPickerDialog(
			currentCode = currencyCache,
			onDismiss = { showCurrencyPicker = false },
			onSelect = { code ->
				currencyCache = code
				showCurrencyPicker = false
			}
		)
	}

	// Strategy Picker Dialog
	if (showStrategyPicker) {
		StrategyPickerDialog(
			currentStrategy = strategyCache,
			onDismiss = { showStrategyPicker = false },
			onSelect = { strategy ->
				strategyCache = strategy
				showStrategyPicker = false
			}
		)
	}
}

// ─── Settings Row ────────────────────────────────────────────────────────────

@Composable
private fun SettingsRow(
	icon: ImageVector,
	label: String,
	trailingText: String? = null,
	onClick: () -> Unit,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clip(RoundedCornerShape(12.dp))
			.clickable(onClick = onClick)
			.padding(vertical = 14.dp),
		verticalAlignment = Alignment.CenterVertically,
	) {
		Icon(
			imageVector = icon,
			contentDescription = null,
			tint = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.size(24.dp),
		)
		Spacer(modifier = Modifier.width(16.dp))
		Text(
			text = label,
			style = MaterialTheme.typography.bodyLarge,
			fontWeight = FontWeight.Medium,
			modifier = Modifier.weight(1f),
		)
		if (trailingText != null) {
			Text(
				text = trailingText,
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}
	}
}

@Composable
private fun CurrencyPickerDialog(
	currentCode: String,
	onDismiss: () -> Unit,
	onSelect: (String) -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Seleccionar divisa") },
		text = {
			Column(
				modifier = Modifier
					.heightIn(max = 400.dp)
					.verticalScroll(rememberScrollState()),
				verticalArrangement = Arrangement.spacedBy(2.dp),
			) {
				SupportedCurrency.ALL.forEach { currency ->
					val isSelected = currency.code == currentCode
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clip(RoundedCornerShape(8.dp))
							.clickable { onSelect(currency.code) }
							.padding(vertical = 12.dp, horizontal = 8.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						Text(
							text = currency.symbol,
							style = MaterialTheme.typography.titleMedium,
							fontWeight = FontWeight.Bold,
							modifier = Modifier.width(40.dp),
						)
						Column(modifier = Modifier.weight(1f)) {
							Text(
								text = currency.code,
								style = MaterialTheme.typography.bodyMedium,
								fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
								color = if (isSelected) MaterialTheme.colorScheme.primary
								else MaterialTheme.colorScheme.onSurface,
							)
							Text(
								text = currency.name,
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
						}
						if (isSelected) {
							Icon(
								imageVector = Icons.Default.Check,
								contentDescription = "Seleccionado",
								tint = MaterialTheme.colorScheme.primary,
								modifier = Modifier.size(20.dp),
							)
						}
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) { Text("Cerrar") }
		},
	)
}

@Composable
private fun StrategyPickerDialog(
	currentStrategy: RemainingBudgetStrategy,
	onDismiss: () -> Unit,
	onSelect: (RemainingBudgetStrategy) -> Unit,
) {
	val options = listOf(
		Triple(
			RemainingBudgetStrategy.ASK_ALWAYS,
			"Preguntarme siempre",
			"Al final del período te preguntaremos qué hacer con el sobrante"
		),
		Triple(
			RemainingBudgetStrategy.SPLIT_EQUALLY,
			"Repartir entre todos los días",
			"El sobrante se divide equitativamente entre todos los días del nuevo período"
		),
		Triple(
			RemainingBudgetStrategy.ADD_TO_FIRST_DAY,
			"Agregar al primer día",
			"Todo el sobrante se suma al primer día del nuevo período"
		),
	)

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Sobrante del presupuesto") },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				Text(
					text = "Puedes elegir como distribuir el balance del presupuesto al final de cada día.\n\n" +
							"Por ejemplo, tienes un presupuesto de 500 al día, y ayer gastaste 400: " +
							"puedes repartir 100 en los días que queda, o gastar 600 " +
							"(presupuesto por día + sobrante del periodo anterior) hoy.",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
				Spacer(modifier = Modifier.height(4.dp))
				options.forEach { (strategy, title, description) ->
					val isSelected = strategy == currentStrategy
					OutlinedCard(
						onClick = { onSelect(strategy) },
						modifier = Modifier.fillMaxWidth(),
						border = BorderStroke(
							width = if (isSelected) 2.dp else 1.dp,
							color = if (isSelected) MaterialTheme.colorScheme.primary
							else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
						),
						colors = CardDefaults.outlinedCardColors(
							containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(
								alpha = 0.3f
							)
							else MaterialTheme.colorScheme.surface,
						),
					) {
						Column(modifier = Modifier.padding(12.dp)) {
							Row(verticalAlignment = Alignment.CenterVertically) {
								Text(
									text = title,
									style = MaterialTheme.typography.bodyMedium,
									fontWeight = FontWeight.Medium,
									modifier = Modifier.weight(1f),
								)
								if (isSelected) {
									Icon(
										imageVector = Icons.Default.Check,
										contentDescription = null,
										tint = MaterialTheme.colorScheme.primary,
										modifier = Modifier.size(18.dp),
									)
								}
							}
							Text(
								text = description,
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
						}
					}
				}
			}
		},
		confirmButton = {
			TextButton(onClick = onDismiss) { Text("Cerrar") }
		},
	)
}

// ─── CompactPeriodCard ───────────────────────────────────────────────────────

@Composable
private fun CompactPeriodCard(
	period: BudgetPeriod,
	budgetAmount: BigDecimal,
	currencyFormat: NumberFormat,
	isSelected: Boolean,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val backgroundColor = if (isSelected) {
		MaterialTheme.colorScheme.primaryContainer
	} else {
		MaterialTheme.colorScheme.surface
	}
	val borderColor = if (isSelected) {
		MaterialTheme.colorScheme.primary
	} else {
		MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
	}
	val textColor = if (isSelected) {
		MaterialTheme.colorScheme.onPrimaryContainer
	} else {
		MaterialTheme.colorScheme.onSurface
	}

	OutlinedCard(
		modifier = modifier.clickable(onClick = onClick),
		onClick = onClick,
		shape = RoundedCornerShape(12.dp),
		border = BorderStroke(
			width = if (isSelected) 2.dp else 1.dp,
			color = borderColor
		),
		colors = CardDefaults.outlinedCardColors(containerColor = backgroundColor),
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
			verticalArrangement = Arrangement.Center
		) {
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

			Text(
				text = currencyFormat.format(budgetAmount),
				style = MaterialTheme.typography.bodySmall,
				fontWeight = FontWeight.Bold,
				color = if (isSelected) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f)
			)
		}
	}
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showSystemUi = false, showBackground = true)
@Composable
private fun PeriodSwitcherSheetPreview() {
	MinusTheme {
		PeriodSwitcherSheet(
			budgetSettings = BudgetSettings(
				totalBudget = BigDecimal("17725"),
				period = BudgetPeriod.DAILY,
				startDate = LocalDate.now().minusDays(29),
				endDate = LocalDate.now(),
				currencyCode = "MXN",
				daysInPeriod = 30,
				rollOverEnabled = false,
				rollOverLimit = null,
				rollOverCarryForward = false
			),
			budgetState = BudgetState(
				remainingToday = BigDecimal("17675"),
				totalSpentToday = BigDecimal("5072"),
				dailyBudget = BigDecimal("5900"),
				daysRemaining = 1,
				progress = 0.03f,
				isOverBudget = false,
				totalBudget = BigDecimal("17725"),
				totalSpentInPeriod = BigDecimal("5072"),
			),
			selectedPeriod = BudgetPeriod.DAILY,
			currencyCode = "MXN",
			onPeriodSelected = { },
			onSaveBudget = { },
			onFinishEarly = { }
		)
	}
}

@Preview(showSystemUi = false, showBackground = true)
@Composable
private fun EditModePreview() {
	MinusTheme {
		EditBudgetContent(
			budgetSettings = BudgetSettings(
				totalBudget = BigDecimal("17725"),
				period = BudgetPeriod.DAILY,
				startDate = LocalDate.now().minusDays(29),
				endDate = LocalDate.now(),
				currencyCode = "MXN",
				daysInPeriod = 30,
			),
			onBack = { },
			onApply = { },
		)
	}
}
