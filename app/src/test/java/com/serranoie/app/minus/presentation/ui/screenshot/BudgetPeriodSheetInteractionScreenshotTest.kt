package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.OverscrollFactory
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.presentation.ui.editor.sheets.BudgetBehaviourContent
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_EDIT_BUTTON_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BUDGET_PERIOD_SHEET_TAG
import com.serranoie.app.minus.presentation.ui.editor.sheets.BudgetPeriodSheet
import com.serranoie.app.minus.presentation.ui.editor.sheets.budgetPeriodToggleTag
import com.serranoie.app.minus.presentation.ui.editor.sheets.budgetSplitModeOptionTag
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import me.saket.touchrobot.onNode
import me.saket.touchrobot.rememberTouchRobot
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

class BudgetPeriodSheetInteractionScreenshotTest {
	@get:Rule
	val paparazzi = Paparazzi(
		deviceConfig = DeviceConfig.PIXEL_5,
		renderingMode = SessionParams.RenderingMode.NORMAL,
        maxPercentDifference = 10.0,
	)

	@Test
	fun editModeTransition() {
		Locale.setDefault(Locale.US)

		val view = ComposeView(paparazzi.context).apply {
			setContent {
				MinusTheme {
					BudgetPeriodSheet(
						budgetSettings = sampleBudgetSettings,
						budgetState = sampleBudgetState,
						selectedPeriod = BudgetPeriod.DAILY,
						currencyCode = "USD",
						onPeriodSelected = {},
						onSaveBudget = {},
					)

					val touchRobot = rememberTouchRobot()
					LaunchedEffect(Unit) {
						touchRobot.onNode(hasTestTag(BUDGET_PERIOD_EDIT_BUTTON_TAG)).performGesture {
							click(center)
						}
					}
				}
			}
		}

		paparazzi.gif(view, start = 1, end = 1_200)
	}

	@Test
	fun editModeState() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				BudgetPeriodSheet(
					budgetSettings = sampleBudgetSettings,
					budgetState = sampleBudgetState,
					selectedPeriod = BudgetPeriod.DAILY,
					currencyCode = "USD",
					onPeriodSelected = {},
					onSaveBudget = {},
					startInEditMode = true,
					pendingExpensesCount = 3,
				)
			}
		}
	}

	@Test
	fun periodSelectionAndSheetSwipe() {
		Locale.setDefault(Locale.US)

		val view = ComposeView(paparazzi.context).apply {
			setContent {
				MinusTheme {
					val selectedPeriod = remember { mutableStateOf(BudgetPeriod.DAILY) }
					BudgetPeriodSheet(
						budgetSettings = sampleBudgetSettings,
						budgetState = sampleBudgetState,
						selectedPeriod = selectedPeriod.value,
						currencyCode = "USD",
						onPeriodSelected = { selectedPeriod.value = it },
						onSaveBudget = {},
					)

					val touchRobot = rememberTouchRobot()
					LaunchedEffect(Unit) {
						touchRobot.onNode(hasTestTag(budgetPeriodToggleTag(BudgetPeriod.WEEKLY))).performGesture {
							click(center)
						}
						touchRobot.onNode(hasTestTag(BUDGET_PERIOD_SHEET_TAG)).performGesture {
							swipe(
								start = center,
								stop = center.copy(y = center.y - 350),
								duration = 300.milliseconds,
							)
						}
					}
				}
			}
		}

		paparazzi.gif(view, start = 1, end = 1_600)
	}

	@Test
	fun calculatedCardDynamicHelper() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				BudgetPeriodSheet(
					budgetSettings = sampleBudgetSettings.copy(
						splitMode = BudgetSplitMode.DYNAMIC,
					),
					budgetState = sampleBudgetState,
					selectedPeriod = BudgetPeriod.DAILY,
					currencyCode = "USD",
					onPeriodSelected = {},
					onSaveBudget = {},
				)
			}
		}
	}

	@Test
	fun calculatedCardStaticHelper() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				BudgetPeriodSheet(
					budgetSettings = sampleBudgetSettings,
					budgetState = sampleBudgetState,
					selectedPeriod = BudgetPeriod.DAILY,
					currencyCode = "USD",
					onPeriodSelected = {},
					onSaveBudget = {},
				)
			}
		}
	}

	@Test
	fun behaviourStep() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				Surface(
					color = MaterialTheme.colorScheme.surfaceContainerLow,
					modifier = Modifier.height(640.dp),
				) {
					BudgetBehaviourContent(
						strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
						splitMode = BudgetSplitMode.CARRY_OVER,
						onStrategySelected = {},
						onSplitModeSelected = {},
						applyLabel = "Apply",
						onBack = {},
						onApply = {},
					)
				}
			}
		}
	}

	@Test
	fun behaviourListOverscrollsInsteadOfMovingTheSheet() {
		Locale.setDefault(Locale.US)
		var sheetScroll = Offset.Zero
		var sheetFling = Velocity.Zero
		var overscrolled = Offset.Zero
		val sheet = object : NestedScrollConnection {
			override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
				sheetScroll += available
				return Offset.Zero
			}

			override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
				sheetFling += available
				return Velocity.Zero
			}
		}
		val overscrollFactory = object : OverscrollFactory {
			override fun createOverscrollEffect(): OverscrollEffect = object : OverscrollEffect {
				override fun applyToScroll(
					delta: Offset,
					source: NestedScrollSource,
					performScroll: (Offset) -> Offset,
				): Offset {
					val consumed = performScroll(delta)
					overscrolled += delta - consumed
					return consumed
				}

				override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
					performFling(velocity)
				}

				override val isInProgress = false
			}

			override fun hashCode() = 0

			override fun equals(other: Any?) = other === this
		}

		val view = ComposeView(paparazzi.context).apply {
			setContent {
				MinusTheme {
					CompositionLocalProvider(LocalOverscrollFactory provides overscrollFactory) {
						Box {
							Surface(
								color = MaterialTheme.colorScheme.surfaceContainerLow,
								modifier = Modifier
									.height(640.dp)
									.nestedScroll(sheet),
							) {
								BudgetBehaviourContent(
									strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
									splitMode = BudgetSplitMode.CARRY_OVER,
									onStrategySelected = {},
									onSplitModeSelected = {},
									applyLabel = "Apply",
									onBack = {},
									onApply = {},
								)
							}
						}
					}

					val touchRobot = rememberTouchRobot()
					LaunchedEffect(Unit) {
						touchRobot.onNode(hasTestTag(budgetSplitModeOptionTag(BudgetSplitMode.DYNAMIC))).performGesture {
							swipe(
								start = center,
								stop = center.copy(y = center.y + 300),
								duration = 300.milliseconds,
							)
						}
					}
				}
			}
		}

		paparazzi.gif(view, start = 1, end = 1_000)

		assertThat(sheetScroll).isEqualTo(Offset.Zero)
		assertThat(sheetFling).isEqualTo(Velocity.Zero)
		assertThat(overscrolled.y).isGreaterThan(0f)
	}

	private val sampleBudgetSettings = BudgetSettings(
		totalBudget = BigDecimal("900.00"),
		period = BudgetPeriod.MONTHLY,
		startDate = LocalDate.of(2026, 1, 1),
		endDate = LocalDate.of(2026, 1, 30),
		currencyCode = "USD",
		daysInPeriod = 30,
	)

	private val sampleBudgetState = BudgetState(
		remainingToday = BigDecimal("25.00"),
		totalSpentToday = BigDecimal("5.00"),
		dailyBudget = BigDecimal("30.00"),
		daysRemaining = 10,
		progress = 0.67f,
		isOverBudget = false,
		totalBudget = BigDecimal("900.00"),
		totalSpentInPeriod = BigDecimal("600.00"),
		periodTotalDays = 30,
	)
}
