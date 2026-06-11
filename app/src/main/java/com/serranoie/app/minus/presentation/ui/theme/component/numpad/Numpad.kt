package com.serranoie.app.minus.presentation.ui.theme.component.numpad

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.Utils.abortFeedback
import com.serranoie.app.minus.presentation.util.getFloatDivider
import com.serranoie.app.minus.presentation.util.join
import com.serranoie.app.minus.presentation.util.tryConvertStringToNumber
import java.util.Date

val BUTTON_GAP = 4.dp
private const val TEST_NOTIFICATION_TAP_COUNT = 5

enum class EditMode { ADD, EDIT }

enum class EditStage { IDLE, EDIT_SPENT }

data class Transaction(
	val id: Long, val amount: String, val comment: String, val date: Date
)

data class EditorState(
	val mode: EditMode,
	val rawSpentValue: String,
	val stage: EditStage,
	val currentSpent: String,
	val currentComment: String,
	val editedTransaction: Transaction?
)

@Composable
fun Numpad(
	modifier: Modifier = Modifier,
	editorState: EditorState,
	onNumberInput: (Int) -> Unit = {},
	onDotInput: () -> Unit = {},
	onEqualsInput: () -> Unit = {},
	onBackspace: () -> Unit = {},
	onBackspaceLongPress: () -> Unit = {},
	onDelete: () -> Unit = {},
	onApply: () -> Unit = {},
	isCalculation: Boolean = false,
	onCalculationModeChanged: (Boolean) -> Unit = {},
	onOperatorInput: (Char) -> Unit = {},
	onToggleDebug: (() -> Unit)? = null,
	onShowSnackbar: ((String) -> Unit)? = null,
	onActivateTutorial: (() -> Unit)? = null,
	onTestNotifications: (() -> Unit)? = null,
	numberHintAnchorModifier: Modifier = Modifier,
	applyHintAnchorModifier: Modifier = Modifier,
	onNumberPressedForTutorial: (() -> Unit)? = null,
	onApplyPressedForTutorial: (() -> Unit)? = null,
	onDragProgressChanged: (Float) -> Unit = {},
	dragProgress: Float = 0f,
	rowHeight: Dp = 55.dp,
) {
	val view = LocalView.current
	val haptic = LocalHapticFeedback.current
	var debugProgress by remember { mutableIntStateOf(0) }
	val shouldTriggerTestNotifications: () -> Boolean = {
		if (debugProgress >= TEST_NOTIFICATION_TAP_COUNT) {
			onTestNotifications?.invoke()
			onShowSnackbar?.invoke("Test notifications triggered!")
			debugProgress = 0
			true
		} else {
			false
		}
	}

	val effectiveDragProgress by animateFloatAsState(
		targetValue = if (dragProgress > 0f && dragProgress < 1f) dragProgress else if (isCalculation) 1f else 0f,
		animationSpec = if (dragProgress > 0f && dragProgress < 1f) tween(0) else tween(200),
		label = "NumpadDragProgress"
	)

	val hasOperators by remember {
		derivedStateOf { editorState.rawSpentValue.any { it in "+-×÷" } }
	}

	Column(
		modifier
			.fillMaxWidth()
			.windowInsetsPadding(WindowInsets.navigationBars)
			.padding(horizontal = 14.dp)
			.pointerInput(isCalculation, hasOperators) {
				var accumulatedDrag = 0f
				var lastReportedProgress = 0f
				var lastTickProgress = 0f
				var hasTriggered = false

				detectVerticalDragGestures(
					onDragStart = {
						accumulatedDrag = 0f
						lastTickProgress = 0f
						hasTriggered = false
					},
					onVerticalDrag = { change, dragAmount ->
						change.consume()
						accumulatedDrag += dragAmount
						val progress = when {
							accumulatedDrag < 0f && !isCalculation -> {
								(-accumulatedDrag / 110f).coerceIn(0f, 1f)
							}

							accumulatedDrag > 0f && isCalculation && !hasOperators -> {
								(accumulatedDrag / 110f).coerceIn(0f, 1f)
							}

							else -> 0f
						}

						// Progress haptics (ticks every ~15% of the gesture)
						if (progress > lastTickProgress + 0.15f) {
							haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							lastTickProgress = progress
						} else if (progress < lastTickProgress - 0.15f) {
							haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							lastTickProgress = progress
						}

						if (kotlin.math.abs(progress - lastReportedProgress) >= 0.05f) {
							onDragProgressChanged(progress)
							lastReportedProgress = progress
						}

						// Trigger at 100% displacement
						if (progress >= 1f && !hasTriggered) {
							onCalculationModeChanged(!isCalculation)
							onDragProgressChanged(0f)
							hasTriggered = true
						}
					},
					onDragEnd = {
						if (!hasTriggered && lastReportedProgress > 0.2f) {
							view.abortFeedback()
						}
						onDragProgressChanged(0f)
					},
					onDragCancel = { onDragProgressChanged(0f) })
			}
	) {
		if (effectiveDragProgress > 0.01f || isCalculation) {
			Box(
				Modifier
					.fillMaxWidth()
					.height(rowHeight * effectiveDragProgress)
					.clipToBounds(),
				contentAlignment = androidx.compose.ui.Alignment.BottomCenter
			) {
				Row(
					Modifier
						.fillMaxWidth()
						.height(rowHeight)
						.graphicsLayer(alpha = effectiveDragProgress)
				) {
					val operators = listOf('÷', '×', '+', '-')
					for (operator in operators) {
						NumpadButton(
							modifier = Modifier
								.weight(1F)
								.padding(BUTTON_GAP),
							type = NumpadButtonType.OPERATOR,
							text = operator.toString(),
							onClick = {
								onOperatorInput(operator)
								haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							})
					}
				}
			}
		}

		Row(
			Modifier
				.fillMaxWidth()
				.height(rowHeight * 4)
		) {
			Column(
				modifier = Modifier
					.fillMaxHeight()
					.weight(3F)
			) {
				Row(
					Modifier
						.fillMaxWidth()
						.weight(1F)
				) {
					for (i in 7..9) {
						NumpadButton(
							modifier = Modifier
								.weight(1F)
								.padding(BUTTON_GAP),
							type = NumpadButtonType.DEFAULT,
							text = i.toString(),
							onClick = {
								onNumberInput(i)
								debugProgress = 0
								haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							})
					}
				}

				Row(
					Modifier
						.fillMaxWidth()
						.weight(1F)
				) {
					for (i in 4..6) {
						NumpadButton(
							modifier = Modifier
								.weight(1F)
								.padding(BUTTON_GAP)
								.then(if (i == 5) numberHintAnchorModifier else Modifier as Modifier),
							type = NumpadButtonType.DEFAULT,
							text = i.toString(),
							onClick = {
								onNumberInput(i)
								onNumberPressedForTutorial?.invoke()
								debugProgress = 0
								haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							})
					}
				}

				Row(
					Modifier
						.fillMaxWidth()
						.weight(1F)
				) {
					for (i in 1..3) {
						NumpadButton(
							modifier = Modifier
								.weight(1F)
								.padding(BUTTON_GAP),
							type = NumpadButtonType.DEFAULT,
							text = i.toString(),
							onClick = {
								onNumberInput(i)
								onNumberPressedForTutorial?.invoke()
								debugProgress = 0
								haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							})
					}
				}

				Row(
					Modifier
						.fillMaxWidth()
						.weight(1F)
				) {
					NumpadButton(
						modifier = Modifier
							.weight(if (isCalculation) 1f else 3f)
							.padding(BUTTON_GAP),
						type = NumpadButtonType.DEFAULT,
						text = "0",
						onClick = {
							onNumberInput(0)
							onNumberPressedForTutorial?.invoke()
							haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
						})
					NumpadButton(
						modifier = Modifier
							.weight(if (isCalculation) 1f else 1.5f)
							.padding(BUTTON_GAP),
						type = NumpadButtonType.DEFAULT,
						text = getFloatDivider(),
						onClick = {
							onDotInput()
							debugProgress = (debugProgress + 1).coerceAtMost(TEST_NOTIFICATION_TAP_COUNT)
							haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
						})
					if (isCalculation) {
						NumpadButton(
							modifier = Modifier
								.weight(1F)
								.padding(BUTTON_GAP),
							type = NumpadButtonType.OPERATOR,
							text = "=",
							onClick = {
								onEqualsInput()
								haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							})
					}
				}
			}

			Column(
				modifier = Modifier
					.fillMaxHeight()
					.weight(1F)
			) {
				NumpadButton(
					modifier = Modifier
						.weight(1F)
						.padding(BUTTON_GAP),
					type = NumpadButtonType.TERTIARY,
					icon = Icons.AutoMirrored.Rounded.Backspace,
					onClick = {
						onBackspace()
						debugProgress = 0
						haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
					},
					onLongClick = {
						debugProgress = 0
						onBackspaceLongPress()
						haptic.performHapticFeedback(HapticFeedbackType.LongPress)
					})

				val fixedSpent by remember {
					derivedStateOf {
						tryConvertStringToNumber(editorState.rawSpentValue).join(third = false)
					}
				}

				AnimatedContent(
					modifier = Modifier.weight(3f),
					label = "Delete or Apply",
					targetState = if (isCalculation) false else (fixedSpent == "0" || fixedSpent == "0." || fixedSpent == "0.0") && editorState.mode == EditMode.EDIT,
					transitionSpec = {
						fadeIn(tween(durationMillis = 150)) togetherWith fadeOut(tween(durationMillis = 150))
					}) { targetIsDelete ->
					if (targetIsDelete) {
						NumpadButton(
							modifier = Modifier
								.fillMaxSize()
								.padding(BUTTON_GAP),
							type = NumpadButtonType.DELETE,
							icon = Icons.Default.Delete,
							onClick = {
								onDelete()
								haptic.performHapticFeedback(HapticFeedbackType.LongPress)
							})
					} else {
						NumpadButton(
							modifier = Modifier
								.fillMaxSize()
								.padding(BUTTON_GAP)
								.then(applyHintAnchorModifier),
							type = if (isCalculation) NumpadButtonType.PRIMARY else NumpadButtonType.PRIMARY,
							icon = if (isCalculation) Icons.Default.Done else Icons.Default.Check,
							onClick = {
								if (shouldTriggerTestNotifications()) return@NumpadButton
								debugProgress = 0
								onApplyPressedForTutorial?.invoke()
								onApply()
								haptic.performHapticFeedback(HapticFeedbackType.LongPress)
							})
					}
				}
			}
		}
	}
}

@Preview
@Composable
fun NumpadPreview() {
	MinusTheme {
		Numpad(
			editorState = EditorState(
				mode = EditMode.ADD,
				rawSpentValue = "123",
				stage = EditStage.EDIT_SPENT,
				currentSpent = "123",
				currentComment = "",
				editedTransaction = null
			),
			isCalculation = false,
			onNumberInput = { },
			onDotInput = { },
			onBackspace = { },
			onBackspaceLongPress = { },
			onDelete = { },
			onApply = { },
		)
	}
}

@Preview(name = "Numpad - Calculation ON")
@Composable
fun NumpadPreviewCalculationMode() {
	MinusTheme {
		Numpad(
			editorState = EditorState(
				mode = EditMode.ADD,
				rawSpentValue = "123",
				stage = EditStage.EDIT_SPENT,
				currentSpent = "123",
				currentComment = "",
				editedTransaction = null
			),
			isCalculation = true,
			onNumberInput = { },
			onDotInput = { },
			onBackspace = { },
			onBackspaceLongPress = { },
			onDelete = { },
			onApply = { },
		)
	}
}
