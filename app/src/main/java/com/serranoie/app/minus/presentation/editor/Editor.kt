@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import logcat.logcat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.budget.BudgetUiState
import com.serranoie.app.minus.presentation.editor.category.CategoryToolbar
import com.serranoie.app.minus.presentation.editor.category.FocusController
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorButton
import com.serranoie.app.minus.presentation.ui.theme.component.AutoResizeBasicTextField
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetPill
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate




@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Editor(
	uiState: BudgetUiState,
	animState: AnimState,
	onInputChange: (String) -> Unit = {},
	onFocus: () -> Unit,
	onOpenHistory: () -> Unit,
	onOpenSettings: () -> Unit,
	onOpenAnalytics: () -> Unit = {},
	onOpenWallet: () -> Unit = {},
	openWalletOnStart: Boolean = false,
	forceWalletSetup: Boolean = false,
	onCommentClick: () -> Unit,
	onBudgetPillClickForTutorial: () -> Unit = {},
	onAnalyticsClickForTutorial: () -> Unit = {},
	onChangePeriod: (BudgetPeriod) -> Unit = {},
	onFinishBudgetEarly: () -> Unit = {},
	onSaveBudget: (BudgetSettings) -> Unit = {},
	onCommentUpdate: (String) -> Unit = {},
	onRecurrentToggle: (Boolean) -> Unit = {},
	onDismissRecurrentDialog: () -> Unit = {},
	onRecurrentExpenseConfirm: (com.serranoie.app.minus.domain.model.RecurrentFrequency, LocalDate, Int?) -> Unit = { _, _, _ -> },
	budgetPillHintAnchorModifier: Modifier = Modifier,
	analyticsHintAnchorModifier: Modifier = Modifier,
	modifier: Modifier = Modifier,
) {
	val scope = rememberCoroutineScope()
	val sheetState = rememberModalBottomSheetState()
	var showBottomSheet by remember { mutableStateOf(false) }

	LaunchedEffect(openWalletOnStart) {
		if (openWalletOnStart) {
			showBottomSheet = true
		}
	}

	val editorFocusController = remember { FocusController() }

	var selectedViewPeriod by remember {
		mutableStateOf(uiState.budgetSettings?.period ?: BudgetPeriod.DAILY)
	}

	LaunchedEffect(uiState.budgetSettings?.period) {
		uiState.budgetSettings?.period?.let {
			logcat { "Sync selectedViewPeriod from uiState period=$it (previous=$selectedViewPeriod)" }
			selectedViewPeriod = it
		}
	}

	if (uiState.showRecurrentDialog) {
		RecurrentExpenseDialog(
			budgetSettings = uiState.budgetSettings,
			onDismiss = onDismissRecurrentDialog,
			onConfirm = onRecurrentExpenseConfirm
		)
	}

	Column(
		modifier = modifier
			.fillMaxSize()
			.background(colorButton)
			.statusBarsPadding()
			.clickable(
				interactionSource = remember { MutableInteractionSource() }, indication = null
			) { onFocus() }) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			BudgetPill(
				budgetState = uiState.budgetState,
				budgetSettings = uiState.budgetSettings,
				viewPeriod = selectedViewPeriod,
				currencyCode = uiState.budgetSettings?.currencyCode ?: "USD",
				onOpenSettings = onOpenSettings,
				onOpenBudgetSheet = {
					onBudgetPillClickForTutorial()
					showBottomSheet = true
				},
				modifier = Modifier
					.weight(1f)
					.animateContentSize(animationSpec = tween(200))
					.padding(top = 8.dp, bottom = 8.dp, end = 8.dp)
					.then(budgetPillHintAnchorModifier)
			)

			AnimatedContent(
				targetState = animState == AnimState.EDITING, transitionSpec = {
					slideInHorizontally(animationSpec = tween(200)) { it } + fadeIn(tween(200)) togetherWith slideOutHorizontally(
						animationSpec = tween(200)
					) { -it } + fadeOut(
						tween(
							200
						)
					)
				}, label = "topBarTrailingSwitch"
			) { isEditing ->
				if (isEditing) {
					RecurrenceModeToggle(
						isRecurrentEnabled = uiState.isRecurrentEnabled,
						onRecurrentToggle = onRecurrentToggle,
					)
				} else {
					Row(verticalAlignment = Alignment.CenterVertically) {
						IconButton(
							onClick = {
								onAnalyticsClickForTutorial()
								onOpenAnalytics()
							}, modifier = Modifier
								.size(48.dp)
								.then(analyticsHintAnchorModifier)
						) {
							Icon(
								imageVector = Icons.Rounded.BarChart,
								contentDescription = "Analytics",
								tint = MaterialTheme.colorScheme.onSurface,
								modifier = Modifier.size(28.dp),
							)
						}

						IconButton(
							onClick = { onOpenSettings() }, modifier = Modifier.size(48.dp)
						) {
							Icon(
								imageVector = Icons.Rounded.Settings,
								contentDescription = "Settings",
								tint = MaterialTheme.colorScheme.onSurface,
								modifier = Modifier.size(28.dp),
							)
						}
					}
				}
			}
		}

		AnimatedContent(
			targetState = animState, transitionSpec = {
				when (targetState) {
					AnimState.EDITING -> fadeIn(tween(200)) togetherWith fadeOut(tween(200))
					AnimState.IDLE -> fadeIn(tween(300)) togetherWith fadeOut(tween(200))
					else -> fadeIn(tween(200)) togetherWith fadeOut(tween(200))
				}
			}, label = "editorContent"
		) { state ->
			when (state) {
				AnimState.EDITING -> {
					EditingContent(
						input = uiState.numpadInput,
						onInputChange = onInputChange,
						currencyCode = uiState.budgetSettings?.currencyCode ?: "USD",
						isCalculation = uiState.isCalculation,
						tags = uiState.tags,
						currentComment = uiState.currentComment,
						onCommentUpdate = onCommentUpdate,
						editorFocusController = editorFocusController,
						modifier = Modifier
							.fillMaxWidth()
							.weight(1f)
					)
				}

				AnimState.IDLE, AnimState.RESET -> {
					IdleContent(
						budgetState = uiState.budgetState,
						currencyCode = uiState.budgetSettings?.currencyCode ?: "USD",
						modifier = Modifier.fillMaxWidth()
					)
				}

				else -> {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.weight(1f),
						contentAlignment = Alignment.Center
					) {
						Text(
							text = "Saving...",
							style = MaterialTheme.typography.bodyLarge,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				}
			}
		}
	}

	if (showBottomSheet) {
		ModalBottomSheet(
			onDismissRequest = { showBottomSheet = false },
			sheetState = sheetState,
		) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.heightIn(max = 600.dp)
			) {
				val shouldForceSetupMode = forceWalletSetup && uiState.budgetSettings == null
				logcat {
					"Opening PeriodSwitcherSheet: forceWalletSetup=$forceWalletSetup, hasBudgetSettings=${uiState.budgetSettings != null}, startInEditMode=$shouldForceSetupMode"
				}
				PeriodSwitcherSheet(
					budgetSettings = uiState.budgetSettings,
					budgetState = uiState.budgetState,
					selectedPeriod = selectedViewPeriod,
					currencyCode = uiState.budgetSettings?.currencyCode ?: "USD",
					startInEditMode = shouldForceSetupMode,
					onPeriodSelected = { newPeriod ->
						logcat { "PeriodSwitcher onPeriodSelected -> newPeriod=$newPeriod, previousSelectedViewPeriod=$selectedViewPeriod" }
						selectedViewPeriod = newPeriod
						onChangePeriod(newPeriod)
					},
					onSaveBudget = { newSettings ->
						logcat { "PeriodSwitcher onSaveBudget -> $newSettings" }
						onSaveBudget(newSettings)
						scope.launch { sheetState.hide() }
						showBottomSheet = false
					},
					onEditBudget = {
						onOpenWallet()
						scope.launch { sheetState.hide() }
						showBottomSheet = false
					},
					onFinishEarly = {
						onFinishBudgetEarly()
						onOpenAnalytics()
						scope.launch { sheetState.hide() }
						showBottomSheet = false
					})
			}
		}
	}
}

/**
 * Content shown when editing (typing numbers).
 * Number is positioned like the idle cursor, tagging toolbar at bottom.
 */
@Composable
private fun EditingContent(
	input: String,
	onInputChange: (String) -> Unit,
	currencyCode: String,
	isCalculation: Boolean,
	tags: List<String>,
	currentComment: String,
	onCommentUpdate: (String) -> Unit,
	editorFocusController: FocusController,
	modifier: Modifier = Modifier
) {
	val currencyFormat = symbolOnlyCurrencyFormat(currencyCode)
	val currencySymbol = SupportedCurrency.findByCode(currencyCode)?.symbol ?: "$"

	val hasExpressionOperators = remember(input) { input.any { it in "+-×÷" } }
	val showCalculationUi = hasExpressionOperators

	val calculationResult = remember(input, showCalculationUi) {
		if (!showCalculationUi || input.isEmpty()) return@remember null

		val last = input.lastOrNull()
		if (last != null && (last in "+-×÷" || last == '.')) {
			null
		} else {
			evaluateCalculation(input)
		}
	}

	val displayContent = if (showCalculationUi) {
		"$currencySymbol $input"
	} else {
		try {
			val value = input.toBigDecimalOrNull() ?: BigDecimal.ZERO
			currencyFormat.format(value)
		} catch (e: Exception) {
			input.ifEmpty { currencyFormat.format(BigDecimal.ZERO) }
		}
	}

	val baseTextStyle = MaterialTheme.typography.displayLarge.copy(
		fontWeight = FontWeight.Bold
	)

	BoxWithConstraints(
		modifier = modifier.fillMaxSize()
	) {
		val density = LocalDensity.current
		val availableWidth = maxWidth - 32.dp
		val amountSlotHeight = 124.dp
		val containerSizePx = remember(availableWidth, amountSlotHeight, density) {
			with(density) {
				androidx.compose.ui.unit.IntSize(
					width = availableWidth.toPx().toInt(),
					height = amountSlotHeight.toPx().toInt()
				)
			}
		}

		Column(
			modifier = Modifier.fillMaxSize()
		) {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(amountSlotHeight)
					.padding(start = 16.dp, end = 16.dp),
				contentAlignment = Alignment.TopEnd
			) {
				AnimatedContent(
					targetState = if (showCalculationUi && calculationResult != null) "result" else "input",
					transitionSpec = {
						(fadeIn(animationSpec = tween(200)) + slideInHorizontally(
							animationSpec = tween(200)
						) { it / 4 }) togetherWith (fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
							animationSpec = tween(200)
						) { -it / 4 })
					},
					label = "EditorNumberTransition",
					modifier = Modifier.fillMaxWidth()
				) { state ->
					if (state == "result" && showCalculationUi && calculationResult != null) {
						Column(
							horizontalAlignment = Alignment.End,
							modifier = Modifier.fillMaxWidth()
						) {
							AutoResizeBasicTextField(
								value = displayContent,
								onValueChange = {},
								readOnly = true,
								modifier = Modifier.fillMaxWidth(),
								textStyle = baseTextStyle.copy(
									color = MaterialTheme.colorScheme.onSurface,
									textAlign = TextAlign.End
								),
								singleLine = true,
								minFontSize = 20.sp,
								maxFontSize = 57.sp,
								containerSize = containerSizePx
							)
							AutoResizeBasicTextField(
								value = "= ${currencySymbol}$calculationResult",
								onValueChange = {},
								readOnly = true,
								modifier = Modifier
									.fillMaxWidth()
									.padding(top = 4.dp),
								textStyle = MaterialTheme.typography.headlineMedium.copy(
									color = MaterialTheme.colorScheme.onSurfaceVariant,
									textAlign = TextAlign.End
								),
								singleLine = true,
								minFontSize = 16.sp,
								maxFontSize = 36.sp,
								containerSize = containerSizePx
							)
						}
					} else {
						AutoResizeBasicTextField(
							value = displayContent,
							onValueChange = {},
							readOnly = true,
							modifier = Modifier
								.fillMaxWidth(),
							textStyle = baseTextStyle.copy(
								color = MaterialTheme.colorScheme.onSurface,
								textAlign = TextAlign.End
							),
							singleLine = true,
							minFontSize = 20.sp,
							maxFontSize = 57.sp,
							containerSize = containerSizePx,
							decorationBox = { innerTextField ->
								Box { innerTextField() }
							}
						)
					}
				}
			}

			Spacer(modifier = Modifier.weight(1f))

			CategoryToolbar(
				tags = tags,
				currentComment = currentComment,
				stage = EditStage.EDIT_SPENT,
				onCommentUpdate = onCommentUpdate,
				editorFocusController = editorFocusController,
				modifier = Modifier
					.fillMaxWidth()
					.padding(bottom = 26.dp)
			)
		}
	}
}

private fun evaluateCalculation(input: String): String? {
	if (input.isBlank()) return null

	return try {
		val normalized = input.trim().replace("×", "*").replace("÷", "/")

		normalized.lastOrNull()?.let { if (it in "+-*/") return null }

		val hasOperator = normalized.any { it in "+-*/" }

		if (!hasOperator) {
			val num = normalized.toBigDecimalOrNull() ?: return null
			return if (num.scale() <= 0 || num.stripTrailingZeros().scale() <= 0) {
				num.toBigInteger().toString()
			} else {
				num.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
			}
		}

		val tokenPattern = Regex("([+\\-*/])")
		val parts = tokenPattern.split(normalized).filter { it.isNotEmpty() }
		val operators = tokenPattern.findAll(normalized).map { it.value }.toList()

		if (parts.isEmpty() || parts[0].isEmpty()) return null

		if (operators.size > parts.size - 1) return null

		var result = parts[0].toBigDecimalOrNull() ?: return null

		for (i in operators.indices) {
			if (i + 1 >= parts.size) break
			val operator = operators[i]
			val nextNum = parts[i + 1].toBigDecimalOrNull() ?: return null

			result = when (operator) {
				"+" -> result + nextNum
				"-" -> result - nextNum
				"*" -> result * nextNum
				"/" -> {
					if (nextNum.compareTo(BigDecimal.ZERO) == 0) return null // Division by zero
					result.divide(nextNum, 2, java.math.RoundingMode.HALF_UP)
				}

				else -> return null
			}
		}

		if (result.scale() <= 0 || result.stripTrailingZeros().scale() <= 0) {
			result.toBigInteger().toString()
		} else {
			result.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
		}
	} catch (e: Exception) {
		null
	}
}

@Composable
private fun RecurrenceModeToggle(
	isRecurrentEnabled: Boolean,
	onRecurrentToggle: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
) {
	val containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
	val contentColor = MaterialTheme.colorScheme.tertiary
	val selectedColor = contentColor.copy(alpha = 0.22f)

	Card(
		modifier = modifier.height(50.dp), shape = CircleShape, colors = CardDefaults.cardColors(
			containerColor = containerColor,
			contentColor = contentColor,
		), onClick = { onRecurrentToggle(!isRecurrentEnabled) }) {
		Box(
			modifier = Modifier
				.fillMaxHeight()
				.padding(horizontal = 6.dp, vertical = 6.dp)
				.clip(CircleShape)
				.background(if (isRecurrentEnabled) selectedColor else Color.Transparent)
				.padding(horizontal = 14.dp, vertical = 8.dp),
			contentAlignment = Alignment.Center,
		) {
			Icon(
				imageVector = Icons.Rounded.EventRepeat,
				contentDescription = "Recurrent payment",
				modifier = Modifier.size(24.dp)
			)
		}
	}
}

@Composable
private fun IdleContent(
	budgetState: BudgetState?, currencyCode: String, modifier: Modifier = Modifier
) {
	val cursorVisible = remember { mutableStateOf(true) }
	LaunchedEffect(Unit) {
		while (true) {
			delay(530)
			cursorVisible.value = !cursorVisible.value
		}
	}

	Box(
		modifier = modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp),
		contentAlignment = Alignment.CenterEnd
	) {
		Text(
			text = if (cursorVisible.value) "|" else "",
			style = MaterialTheme.typography.displayLarge,
			color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
		)
	}
}

@Preview(showBackground = true, device = "id:pixel_5", backgroundColor = 0xFF121212)
@Composable
fun EditorPreview_Idle() {
	MinusTheme {
		Editor(
			uiState = BudgetUiState(
				budgetSettings = BudgetSettings(
					totalBudget = BigDecimal("500.00"),
					period = BudgetPeriod.DAILY,
					startDate = LocalDate.now(),
					currencyCode = "USD"
				), budgetState = BudgetState(
					remainingToday = BigDecimal("110.00"),
					totalSpentToday = BigDecimal("12.50"),
					dailyBudget = BigDecimal("122.50"),
					daysRemaining = 15,
					progress = 0.1f,
					isOverBudget = false,
					totalBudget = BigDecimal("500.00"),
					totalSpentInPeriod = BigDecimal("12.50")
				), transactions = emptyList(), numpadInput = "", isNumpadValid = false
			),
			animState = AnimState.IDLE,
			onFocus = {},
			onOpenHistory = {},
			onOpenSettings = {},
			onCommentClick = {},
			onCommentUpdate = {},
			onRecurrentToggle = {},
			onDismissRecurrentDialog = {},
			onRecurrentExpenseConfirm = { _, _, _ -> })
	}
}

@Preview(showBackground = true, device = "id:Nexus One", backgroundColor = 0xFF121212)
@Composable
fun EditorPreview_Editing() {
	MinusTheme {
		Editor(
			uiState = BudgetUiState(
				budgetSettings = BudgetSettings(
					totalBudget = BigDecimal("500.00"),
					period = BudgetPeriod.DAILY,
					startDate = LocalDate.now(),
					currencyCode = "USD"
				), budgetState = BudgetState(
					remainingToday = BigDecimal("110.00"),
					totalSpentToday = BigDecimal("12.50"),
					dailyBudget = BigDecimal("122.50"),
					daysRemaining = 15,
					progress = 0.1f,
					isOverBudget = false,
					totalBudget = BigDecimal("500.00"),
					totalSpentInPeriod = BigDecimal("12.50")
				), transactions = emptyList(), numpadInput = "250", isNumpadValid = true
			),
			animState = AnimState.EDITING,
			onFocus = {},
			onOpenHistory = {},
			onOpenSettings = {},
			onCommentClick = {},
			onCommentUpdate = {},
			onRecurrentToggle = {},
			onDismissRecurrentDialog = {},
			onRecurrentExpenseConfirm = { _, _, _ -> })
	}
}