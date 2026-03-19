package com.serranoie.app.minus.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.rememberSwipeableState
import com.serranoie.app.minus.LocalWindowInsets
import com.serranoie.app.minus.LocalWindowSize
import com.serranoie.app.minus.ONBOARDING_COMPLETED_KEY
import com.serranoie.app.minus.presentation.budget.BudgetUiEvent
import com.serranoie.app.minus.presentation.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.budget.NumpadWithViewModel
import com.serranoie.app.minus.presentation.editor.AnimState
import com.serranoie.app.minus.presentation.editor.EditorWithViewModel
import com.serranoie.app.minus.presentation.history.History
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorEditor
import com.serranoie.app.minus.presentation.ui.theme.colorOnEditor
import com.serranoie.app.minus.presentation.tutorial.FIRST_LAUNCH_TUTORIAL_STAGE_KEY
import com.serranoie.app.minus.presentation.tutorial.FirstLaunchTutorialStage
import com.serranoie.app.minus.presentation.tutorial.firstLaunchTutorialStageFlow
import com.serranoie.app.minus.presentation.ui.theme.component.TopSheetLayout
import com.serranoie.app.minus.presentation.ui.theme.component.TopSheetValue
import com.serranoie.app.minus.presentation.ui.theme.component.tooltip.AnchorPosition
import com.serranoie.app.minus.presentation.ui.theme.component.tooltip.HintTip
import com.serranoie.app.minus.presentation.ui.theme.component.tooltip.hintTipAnchor
import com.serranoie.app.minus.presentation.ui.theme.component.tooltip.rememberHintTipState
import com.serranoie.app.minus.presentation.ui.theme.isNightMode
import com.serranoie.app.minus.settingsDataStore
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun MainScreen(
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToWallet: () -> Unit = {}
) {
	val topSheetState = rememberSwipeableState(TopSheetValue.HalfExpanded)
	var nightMode by remember { mutableStateOf(false) }
	val context = LocalContext.current
	val coroutineScope = rememberCoroutineScope()
	val budgetViewModel: BudgetViewModel = hiltViewModel()
	val budgetUiState by budgetViewModel.uiState.collectAsStateWithLifecycle()

	val localDensity = LocalDensity.current
	val windowSizeClass = LocalWindowSize.current
	val windowInsets = LocalWindowInsets.current

	val onboardingCompleted by context.settingsDataStore.data
		.map { it[ONBOARDING_COMPLETED_KEY] ?: false }
		.collectAsStateWithLifecycle(initialValue = false)
	val tutorialStage by context.firstLaunchTutorialStageFlow()
		.collectAsStateWithLifecycle(initialValue = FirstLaunchTutorialStage.COMPLETED)

	val numberHintState = rememberHintTipState("tutorial_number_hint")
	val doneHintState = rememberHintTipState("tutorial_done_hint")
	val budgetHintState = rememberHintTipState("tutorial_budget_hint")
	val analyticsHintState = rememberHintTipState("tutorial_analytics_hint")
	val historyHintState = rememberHintTipState("tutorial_history_hint")

	var shownStage by remember { mutableStateOf<FirstLaunchTutorialStage?>(null) }

	fun advanceTutorialIfCurrent(expected: FirstLaunchTutorialStage) {
		if (tutorialStage != expected) return
		coroutineScope.launch {
			context.settingsDataStore.edit { prefs ->
				prefs[FIRST_LAUNCH_TUTORIAL_STAGE_KEY] = expected.next().name
			}
		}
	}

	val isHistoryVisible = windowSizeClass != WindowWidthSizeClass.Compact || topSheetState.currentValue == TopSheetValue.Expanded
	val quickLogSwipeModifier = Modifier.pointerInput(isHistoryVisible) {
		if (!isHistoryVisible) return@pointerInput
		var totalDrag = 0f
		detectHorizontalDragGestures(
			onHorizontalDrag = { _, dragAmount ->
				totalDrag += dragAmount
			},
			onDragEnd = {
				if (kotlin.math.abs(totalDrag) > 120f) {
					budgetViewModel.onEvent(BudgetUiEvent.OnSetAnimState(AnimState.EDITING))
					coroutineScope.launch {
						runCatching { topSheetState.animateTo(TopSheetValue.HalfExpanded) }
					}
				}
				totalDrag = 0f
			}
		)
	}

	LaunchedEffect(tutorialStage, onboardingCompleted, budgetUiState.numpadInput, isHistoryVisible) {
		if (!onboardingCompleted || tutorialStage == FirstLaunchTutorialStage.COMPLETED) return@LaunchedEffect
		if (shownStage == tutorialStage) return@LaunchedEffect

		when (tutorialStage) {
			FirstLaunchTutorialStage.TAP_ANY_NUMBER -> {
				numberHintState.show {
					HintTip(position = AnchorPosition.Center) {
						Text("Tap any number to start adding your expense")
					}
				}
				shownStage = tutorialStage
			}
			FirstLaunchTutorialStage.TAP_DONE_SAVE -> {
				if (budgetUiState.numpadInput.isNotEmpty()) {
					doneHintState.show {
						HintTip(position = AnchorPosition.End) {
							Text("Now tap Done to save this expense")
						}
					}
					shownStage = tutorialStage
				}
			}
			FirstLaunchTutorialStage.TAP_BUDGET_PILL -> {
				budgetHintState.show {
					HintTip(position = AnchorPosition.Start) {
						Text("Tap here to open your budget details")
					}
				}
				shownStage = tutorialStage
			}
			FirstLaunchTutorialStage.TAP_ANALYTICS -> {
				analyticsHintState.show {
					HintTip(position = AnchorPosition.End) {
						Text("Tap Analytics to see your spending insights")
					}
				}
				shownStage = tutorialStage
			}
			FirstLaunchTutorialStage.HISTORY_GESTURES -> {
				if (isHistoryVisible) {
					historyHintState.show(onClose = {
						advanceTutorialIfCurrent(FirstLaunchTutorialStage.HISTORY_GESTURES)
					}) {
						HintTip(position = AnchorPosition.Center) {
							Text("In History, swipe an item left or right to edit or delete")
						}
					}
					shownStage = tutorialStage
				}
			}
			FirstLaunchTutorialStage.COMPLETED -> Unit
		}
	}

	nightMode = isNightMode()

	BoxWithConstraints(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.surface),
	) {
		val contentHeight = constraints.maxHeight.toFloat()
		val contentWidth = constraints.maxWidth.toFloat()

		val keyboardAdditionalOffset =
			windowInsets.calculateBottomPadding().minus(16.dp).coerceAtLeast(0.dp)

		val navigationBarOffset = windowInsets.calculateBottomPadding().coerceAtLeast(16.dp)

		val internalKeyboardHeight = if (windowSizeClass == WindowWidthSizeClass.Compact) {
			contentWidth
		} else {
			contentWidth / 2f
		}.coerceAtMost(with(localDensity) { 500.dp.toPx() }).coerceAtMost(contentHeight / 2)

		val systemKeyboardHeight = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
		val isShowSystemKeyboard = systemKeyboardHeight != 0.dp

		val currentKeyboardHeight = if (isShowSystemKeyboard) {
			with(localDensity) { systemKeyboardHeight.toPx() }
		} else {
			internalKeyboardHeight
		}

		val editorHeight by remember(
			contentHeight, currentKeyboardHeight, keyboardAdditionalOffset, navigationBarOffset
		) {
			derivedStateOf {
				contentHeight.minus(
					currentKeyboardHeight.plus(with(localDensity) {
						keyboardAdditionalOffset.toPx()
					}).coerceAtLeast(0f)
				)
					.coerceAtMost(contentHeight - with(localDensity) { navigationBarOffset.toPx() + 96.dp.toPx() })
			}
		}

		val editorHeightAnimated by animateFloatAsState(
			label = "editorHeightAnimatedValue",
			targetValue = editorHeight,
			animationSpec = tween(durationMillis = 350),
		)

		Row {
			// Tablet/Desktop layout: History always visible on left
			if (windowSizeClass != WindowWidthSizeClass.Compact) {
				Surface(
					color = colorEditor,
					modifier = Modifier
						.fillMaxSize()
						.weight(1f)
						.navigationBarsPadding(),
				) {
					Box {
						History(
							modifier = (if (tutorialStage == FirstLaunchTutorialStage.HISTORY_GESTURES) {
								Modifier.hintTipAnchor(historyHintState)
							} else {
								Modifier
							}).then(quickLogSwipeModifier)
						)
						StatusBarStub()
					}
				}
				Spacer(
					Modifier
						.fillMaxHeight()
						.width(16.dp)
				)
			}

			Box(
				Modifier
					.fillMaxSize()
					.weight(1f)
			) {
				// Phone layout
				if (windowSizeClass == WindowWidthSizeClass.Compact) {
					val currentEditorHeight = with(localDensity) {
						val halfExpanedOffset =
							(-contentHeight + navigationBarOffset.toPx() + 16.dp.toPx() + editorHeightAnimated).coerceAtMost(
								0f
							)

						(topSheetState.offset.value.coerceIn(
							halfExpanedOffset, 0f
						) + contentHeight - navigationBarOffset.toPx() - 16.dp.toPx()).toDp()
					}

					Box(
						modifier = Modifier
							.fillMaxSize()
							.padding(bottom = keyboardAdditionalOffset),
						contentAlignment = Alignment.BottomCenter,
					) {
						Card(
							shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
							colors = CardDefaults.cardColors(
								containerColor = MaterialTheme.colorScheme.surface,
								contentColor = MaterialTheme.colorScheme.onSurface,
							),
							modifier = Modifier
								.fillMaxWidth()
								.height(with(localDensity) { internalKeyboardHeight.toDp() })
						) {
							Box(
								modifier = Modifier
									.fillMaxSize()
									.navigationBarsPadding()
							) {
								NumpadWithViewModel(
									numberHintAnchorModifier = if (tutorialStage == FirstLaunchTutorialStage.TAP_ANY_NUMBER) {
										Modifier.hintTipAnchor(numberHintState)
									} else Modifier,
									applyHintAnchorModifier = if (tutorialStage == FirstLaunchTutorialStage.TAP_DONE_SAVE) {
										Modifier.hintTipAnchor(doneHintState)
									} else Modifier,
									onAnyNumberTapped = {
										advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_ANY_NUMBER)
									},
									onApplyTapped = {
										advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_DONE_SAVE)
									}
								)
							}
						}
					}

					TopSheetLayout(
						swipeableState = topSheetState,
						customHalfHeight = editorHeightAnimated,
						onDismiss = {
							// When user swipes up to dismiss, collapse to half-expanded
							// This gives a natural "close" gesture
						},
						sheetContentHalfExpand = {
						EditorWithViewModel(
							modifier = Modifier.requiredHeight(currentEditorHeight),
							onOpenHistory = {
								// Animate to expanded to show history
							},
							onOpenSettings = onNavigateToSettings,
							onOpenAnalytics = onNavigateToAnalytics,
							onOpenWallet = onNavigateToWallet,
							onBudgetPillClickForTutorial = {
								advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_BUDGET_PILL)
							},
							onAnalyticsClickForTutorial = {
								advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_ANALYTICS)
							},
							budgetPillHintAnchorModifier = if (tutorialStage == FirstLaunchTutorialStage.TAP_BUDGET_PILL) {
								Modifier.hintTipAnchor(budgetHintState)
							} else Modifier,
							analyticsHintAnchorModifier = if (tutorialStage == FirstLaunchTutorialStage.TAP_ANALYTICS) {
								Modifier.hintTipAnchor(analyticsHintState)
							} else Modifier
						)
					},
						sheetContentExpand = {
							History(
								modifier = (if (tutorialStage == FirstLaunchTutorialStage.HISTORY_GESTURES) {
									Modifier.hintTipAnchor(historyHintState)
								} else Modifier).then(quickLogSwipeModifier)
							)
						})

					StatusBarStub()
				} else {
					// Tablet layout - Editor on top, Numpad below
					Column(
						modifier = Modifier.fillMaxSize()
					) {
						// Editor card
						Card(
							shape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp),
							colors = CardDefaults.cardColors(
								containerColor = colorEditor,
								contentColor = colorOnEditor,
							),
							modifier = Modifier.weight(1f)
						) {
							EditorWithViewModel(
							modifier = Modifier.fillMaxSize(),
							onOpenHistory = {},
							onOpenSettings = onNavigateToSettings,
							onOpenAnalytics = onNavigateToAnalytics,
							onOpenWallet = onNavigateToWallet,
							onBudgetPillClickForTutorial = {
								advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_BUDGET_PILL)
							},
							onAnalyticsClickForTutorial = {
								advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_ANALYTICS)
							},
							budgetPillHintAnchorModifier = if (tutorialStage == FirstLaunchTutorialStage.TAP_BUDGET_PILL) {
								Modifier.hintTipAnchor(budgetHintState)
							} else Modifier,
							analyticsHintAnchorModifier = if (tutorialStage == FirstLaunchTutorialStage.TAP_ANALYTICS) {
								Modifier.hintTipAnchor(analyticsHintState)
							} else Modifier
						)
						}

						// Numpad at the bottom
						Card(
							shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
							colors = CardDefaults.cardColors(
								containerColor = MaterialTheme.colorScheme.surface,
								contentColor = MaterialTheme.colorScheme.onSurface,
							),
							modifier = Modifier
								.fillMaxWidth()
								.height(with(localDensity) { internalKeyboardHeight.toDp() })
						) {
							Box(
								modifier = Modifier
									.fillMaxSize()
									.navigationBarsPadding()
							) {
								NumpadWithViewModel(
									numberHintAnchorModifier = if (tutorialStage == FirstLaunchTutorialStage.TAP_ANY_NUMBER) {
										Modifier.hintTipAnchor(numberHintState)
									} else Modifier,
									applyHintAnchorModifier = if (tutorialStage == FirstLaunchTutorialStage.TAP_DONE_SAVE) {
										Modifier.hintTipAnchor(doneHintState)
									} else Modifier,
									onAnyNumberTapped = {
										advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_ANY_NUMBER)
									},
									onApplyTapped = {
										advanceTutorialIfCurrent(FirstLaunchTutorialStage.TAP_DONE_SAVE)
									}
								)
							}
						}
					}
				}
			}
		}
	}
}

@Composable
fun StatusBarStub() {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.requiredHeight(
				LocalWindowInsets.current.calculateTopPadding()
			)
			.background(colorEditor.copy(alpha = 0.9F))
	)
}

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
private fun MainScreenPreview() {
	MinusTheme {
		CompositionLocalProvider(
			LocalWindowSize provides WindowWidthSizeClass.Compact,
			LocalWindowInsets provides PaddingValues(0.dp)
		) {
			MainScreen()
		}
	}
}
