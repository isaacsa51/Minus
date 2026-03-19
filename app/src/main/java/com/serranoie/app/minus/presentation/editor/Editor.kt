@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.editor

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.onboarding.periodLabel
import com.serranoie.app.minus.presentation.onboarding.toDays
import com.serranoie.app.minus.presentation.budget.BudgetUiState
import com.serranoie.app.minus.presentation.editor.category.FocusController
import com.serranoie.app.minus.presentation.editor.category.CategoryToolbar
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorGood
import com.serranoie.app.minus.presentation.ui.theme.colorNotGood
import com.serranoie.app.minus.presentation.ui.theme.colorBad
import com.serranoie.app.minus.presentation.ui.theme.colorButton
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetPill
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.EditStage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale

/**
 * Editor composable showing the expense input interface.
 * Matches the Buckwheat app design with pill-shaped budget indicator.
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Editor(
	uiState: BudgetUiState,
	animState: AnimState,
	onFocus: () -> Unit,
	onOpenHistory: () -> Unit,
	onOpenSettings: () -> Unit,
	onOpenAnalytics: () -> Unit = {},
	onOpenWallet: () -> Unit = {},
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

	// Create a focus controller for the tagging toolbar
	val editorFocusController = remember { FocusController() }

	// Track the selected view period separately from the actual budget period
	// This allows the user to switch views (Daily/Weekly/etc.) without changing the actual budget
	var selectedViewPeriod by remember {
		mutableStateOf(uiState.budgetSettings?.period ?: BudgetPeriod.DAILY)
	}

	// Keep the selected view period in sync once settings are loaded/restored.
	LaunchedEffect(uiState.budgetSettings?.period) {
		uiState.budgetSettings?.period?.let { selectedViewPeriod = it }
	}

	// Recurrent expense dialog
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
					.padding(vertical = 8.dp)
					.then(budgetPillHintAnchorModifier)
			)

			Spacer(modifier = Modifier.width(8.dp))

			Box(
				modifier = Modifier.width(116.dp),
				contentAlignment = Alignment.CenterEnd,
			) {
				AnimatedContent(
					targetState = animState == AnimState.EDITING,
					transitionSpec = {
						fadeIn(tween(durationMillis = 120, delayMillis = 90)) togetherWith
								fadeOut(tween(durationMillis = 90))
					},
					label = "topBarTrailingSwitch"
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
								},
								modifier = Modifier
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
									imageVector = Icons.Filled.Settings,
									contentDescription = "Settings",
									tint = MaterialTheme.colorScheme.onSurface,
									modifier = Modifier.size(28.dp),
								)
							}
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
						currencyCode = uiState.budgetSettings?.currencyCode ?: "USD",
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

	// Period switcher bottom sheet (opened by tapping the pill)
	if (showBottomSheet) {
		ModalBottomSheet(
			onDismissRequest = { showBottomSheet = false },
			sheetState = sheetState,
		) {
			// Wrap in Box with heightIn to provide bounded constraints for the scrollable content
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.heightIn(max = 600.dp)
			) {
				PeriodSwitcherSheet(
					budgetSettings = uiState.budgetSettings,
					budgetState = uiState.budgetState,
					selectedPeriod = selectedViewPeriod,
					currencyCode = uiState.budgetSettings?.currencyCode ?: "USD",
					onPeriodSelected = { newPeriod ->
					selectedViewPeriod = newPeriod
					// Also persist the period to budget settings
					onChangePeriod(newPeriod)
				},
					onSaveBudget = { newSettings ->
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
	currencyCode: String,
	tags: List<String>,
	currentComment: String,
	onCommentUpdate: (String) -> Unit,
	editorFocusController: FocusController,
	modifier: Modifier = Modifier
) {
	val currencyFormat = com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat(currencyCode)

	val formattedInput = remember(input, currencyCode) {
		try {
			val value = input.toBigDecimalOrNull() ?: BigDecimal.ZERO
			currencyFormat.format(value)
		} catch (e: Exception) {
			input.ifEmpty { currencyFormat.format(BigDecimal.ZERO) }
		}
	}

	Box(
		modifier = modifier.fillMaxSize()
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp)
				.align(Alignment.TopEnd),
			contentAlignment = Alignment.TopEnd
		) {
			Text(
				text = formattedInput,
				style = MaterialTheme.typography.displayLarge,
				color = MaterialTheme.colorScheme.onSurface,
				textAlign = TextAlign.End
			)
		}

		// Category toolbar at the bottom
		CategoryToolbar(
			tags = tags,
			currentComment = currentComment,
			stage = EditStage.EDIT_SPENT,
			onCommentUpdate = onCommentUpdate,
			editorFocusController = editorFocusController,
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.BottomCenter)
				.padding(bottom = 26.dp)
		)
	}
}

/**
 * Recurrence switch styled with the same pill geometry as BudgetPill.
 */
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
		modifier = modifier.height(50.dp),
		shape = CircleShape,
		colors = CardDefaults.cardColors(
			containerColor = containerColor,
			contentColor = contentColor,
		),
		onClick = { onRecurrentToggle(!isRecurrentEnabled) }
	) {
		Box(
			modifier = Modifier
				.fillMaxHeight()
				.padding(horizontal = 6.dp, vertical = 6.dp)
				.clip(CircleShape)
				.background(if (isRecurrentEnabled) selectedColor else Color.Transparent)
				.padding(horizontal = 14.dp, vertical = 8.dp),
			contentAlignment = Alignment.Center,
		) {
			Text(
				text = "Recurring",
				style = MaterialTheme.typography.titleSmall,
				color = contentColor,
			)
		}
	}
}

@Composable
private fun IdleContent(
	budgetState: BudgetState?, currencyCode: String, modifier: Modifier = Modifier
) {
val cursorVisible = remember { mutableStateOf(true) }

	// Blinking cursor animation
	LaunchedEffect(Unit) {
		while (true) {
			delay(530)
			cursorVisible.value = !cursorVisible.value
		}
	}

	Box(
		modifier = modifier
			.fillMaxWidth()
			.padding(16.dp), contentAlignment = Alignment.CenterEnd
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

@Preview(showBackground = true, device = "id:pixel_5", backgroundColor = 0xFF121212)
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
			), transactions = emptyList(), numpadInput = "25", isNumpadValid = true
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