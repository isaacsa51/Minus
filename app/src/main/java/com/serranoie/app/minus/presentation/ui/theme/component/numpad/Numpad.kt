package com.serranoie.app.minus.presentation.ui.theme.component.numpad

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.util.getFloatDivider
import com.serranoie.app.minus.presentation.util.tryConvertStringToNumber
import com.serranoie.app.minus.presentation.util.join
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.domain.model.Transaction
import java.util.Date

val BUTTON_GAP = 3.dp

enum class EditMode { ADD, EDIT }

enum class EditStage { IDLE, EDIT_SPENT }

data class Transaction(
	val id: Long = 0, val amount: String, val comment: String = "", val date: Date = Date()
)


data class EditorState(
	val mode: EditMode = EditMode.ADD,
	val rawSpentValue: String = "",
	val stage: EditStage = EditStage.IDLE,
	val currentSpent: String = "",
	val currentComment: String = "",
	val editedTransaction: Transaction? = null
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
) {
	val haptic = LocalHapticFeedback.current
	var debugProgress by remember { mutableIntStateOf(0) }

	// Use derivedStateOf to only recompute when rawSpentValue actually changes
	val hasOperators by remember(editorState.rawSpentValue) {
		derivedStateOf { editorState.rawSpentValue.any { it in "+-×÷" } }
	}
	val topRowWeight = if (isCalculation) 0.75f else 1f

	Column(
		modifier
			.fillMaxSize()
			.padding(14.dp)
			.pointerInput(isCalculation, hasOperators) {
				var accumulatedDrag = 0f
				detectVerticalDragGestures(
					onDragStart = { accumulatedDrag = 0f },
					onVerticalDrag = { _, dragAmount ->
						accumulatedDrag += dragAmount
						val progress = when {
							accumulatedDrag < 0f && !isCalculation -> {
								(-accumulatedDrag / 100f).coerceIn(0f, 1f)
							}

							accumulatedDrag > 0f && isCalculation && !hasOperators -> {
								(accumulatedDrag / 100f).coerceIn(0f, 1f)
							}

							isCalculation && accumulatedDrag <= 0f -> 1f
							!isCalculation && accumulatedDrag >= 0f -> 0f
							else -> 0f
						}
						onDragProgressChanged(progress)

						if (dragAmount < -20f && !isCalculation) {
							onCalculationModeChanged(true)
							onDragProgressChanged(0f)
						} else if (dragAmount > 20f && isCalculation && !hasOperators) {
							onCalculationModeChanged(false)
							onDragProgressChanged(0f)
						}
					},
					onDragEnd = { onDragProgressChanged(0f) },
					onDragCancel = { onDragProgressChanged(0f) })
			}) {
		AnimatedContent(
			modifier = Modifier
				.fillMaxWidth()
				.weight(topRowWeight),
			targetState = isCalculation,
			label = "TopRowModeTransition",
			transitionSpec = {
				fadeIn(tween(durationMillis = 100)) togetherWith fadeOut(tween(durationMillis = 100))
			}) { calcTopRow ->
			Row(Modifier.fillMaxSize()) {
				if (calcTopRow) {
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
				} else {
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
						},
					)
				}
			}
		}
		AnimatedContent(
			modifier = Modifier
				.fillMaxWidth()
				.weight(4f - topRowWeight),
			targetState = isCalculation,
			label = "SwipeModeButtonsTransition",
			transitionSpec = {
				if (targetState) {
					(slideInVertically(animationSpec = tween(150)) { it / 5 } + fadeIn(tween(150))) togetherWith (slideOutVertically(
						animationSpec = tween(100)
					) { -it / 5 } + fadeOut(
						tween(100)
					))
				} else {
					(slideInVertically(animationSpec = tween(150)) { -it / 5 } + fadeIn(tween(150))) togetherWith (slideOutVertically(
						animationSpec = tween(100)
					) { it / 5 } + fadeOut(
						tween(100)
					))
				}.using(SizeTransform(clip = true))
			}) { calcMode ->
			if (calcMode) {
				Column(
					modifier = Modifier.fillMaxSize()
				) {
					Row(
						Modifier
							.fillMaxSize()
							.weight(3F)
					) {
						Column(
							modifier = Modifier
								.fillMaxSize()
								.weight(3F)
						) {
							Row(
								Modifier
									.fillMaxSize()
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
									.fillMaxSize()
									.weight(1F)
							) {
								for (i in 4..6) {
									NumpadButton(
										modifier = Modifier
											.weight(1F)
											.padding(BUTTON_GAP)
											.then(if (i == 5) numberHintAnchorModifier else Modifier),
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
									.fillMaxSize()
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
						}

						Column(
							modifier = Modifier
								.fillMaxSize()
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

							NumpadButton(
								modifier = Modifier
									.weight(2F)
									.padding(BUTTON_GAP)
									.then(applyHintAnchorModifier),
								type = NumpadButtonType.PRIMARY,
								icon = Icons.Default.Done,
								onClick = {
									if (debugProgress == -1) {
										onTestNotifications?.invoke()
										onShowSnackbar?.invoke("Test notifications triggered!")
										debugProgress = 0
										return@NumpadButton
									}

									debugProgress = 0
									onApplyPressedForTutorial?.invoke()
									onApply()
									haptic.performHapticFeedback(HapticFeedbackType.LongPress)
								})
						}
					}

					Row(
						Modifier
							.fillMaxSize()
							.weight(1F)
					) {
						NumpadButton(
							modifier = Modifier
								.weight(1F)
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
								.weight(1F)
								.padding(BUTTON_GAP),
							type = NumpadButtonType.DEFAULT,
							text = getFloatDivider(),
							onClick = {
								onDotInput()
								debugProgress = if (debugProgress == 7) -1 else (debugProgress + 1)
								haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							})
						NumpadButton(
							modifier = Modifier
								.weight(2F)
								.padding(BUTTON_GAP),
							type = NumpadButtonType.OPERATOR,
							text = "=",
							onClick = {
								onEqualsInput()
								haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
							})
					}
				}
			} else {
				Row(
					Modifier.fillMaxSize()
				) {
					Column(
						Modifier
							.fillMaxSize()
							.weight(3F)
					) {
						Row(
							Modifier
								.fillMaxSize()
								.weight(1F)
						) {
							for (i in 4..6) {
								NumpadButton(
									modifier = Modifier
										.weight(1F)
										.padding(BUTTON_GAP)
										.then(if (i == 5) numberHintAnchorModifier else Modifier),
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
								.fillMaxSize()
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
								.fillMaxSize()
								.weight(1F)
						) {
							NumpadButton(
								modifier = Modifier
									.weight(3F)
									.padding(BUTTON_GAP),
								type = NumpadButtonType.DEFAULT,
								text = "0",
								onClick = {
									onNumberInput(0)
									onNumberPressedForTutorial?.invoke()
									haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
								},
							)
							NumpadButton(
								modifier = Modifier
									.weight(1.5F)
									.padding(BUTTON_GAP),
								type = NumpadButtonType.DEFAULT,
								text = getFloatDivider(),
								onClick = {
									onDotInput()
									debugProgress =
										if (debugProgress == 7) -1 else (debugProgress + 1)
									haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
								})
						}
					}
					Column(
						Modifier
							.fillMaxSize()
							.weight(1F)
					) {
						val fixedSpent by remember(editorState.rawSpentValue, editorState.mode) {
							derivedStateOf {
								tryConvertStringToNumber(editorState.rawSpentValue).join(third = false)
							}
						}

						AnimatedContent(
							label = "Delete or Apply",
							targetState = (fixedSpent == "0" || fixedSpent == "0." || fixedSpent == "0.0") && editorState.mode == EditMode.EDIT,
							transitionSpec = {
								(fadeIn(tween(durationMillis = 150)) togetherWith fadeOut(tween(durationMillis = 150)))
									.using(SizeTransform(clip = false))
							}) { targetIsDelete ->
							if (targetIsDelete) {
								NumpadButton(
									modifier = Modifier
										.weight(1F)
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
										.weight(1F)
										.padding(BUTTON_GAP)
										.then(applyHintAnchorModifier),
									type = NumpadButtonType.PRIMARY,
									icon = Icons.Default.Check,
									onClick = {
										if (debugProgress == -1) {
											onTestNotifications?.invoke()
											onShowSnackbar?.invoke("Test notifications triggered!")
											debugProgress = 0
											return@NumpadButton
										}

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
	}
}

@Preview
@Composable
fun NumpadPreview() {
	MinusTheme {
		Numpad(
			editorState = EditorState(
				mode = EditMode.ADD, rawSpentValue = "123", stage = EditStage.EDIT_SPENT
			),
			isCalculation = false,
			onNumberInput = { Log.d("NumpadPreview", "Number: $it") },
			onDotInput = { Log.d("NumpadPreview", "Dot pressed") },
			onBackspace = { Log.d("NumpadPreview", "Backspace") },
			onBackspaceLongPress = { Log.d("NumpadPreview", "Backspace long press") },
			onDelete = { Log.d("NumpadPreview", "Delete") },
			onApply = { Log.d("NumpadPreview", "Apply") },
		)
	}
}

@Preview
@Composable
fun NumpadPreviewEditMode() {
	MinusTheme {
		Numpad(
			editorState = EditorState(
				mode = EditMode.EDIT, rawSpentValue = "0", stage = EditStage.EDIT_SPENT
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
				mode = EditMode.ADD, rawSpentValue = "123", stage = EditStage.EDIT_SPENT
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
