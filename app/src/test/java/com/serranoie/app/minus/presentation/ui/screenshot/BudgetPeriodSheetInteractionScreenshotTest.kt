package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.hasTestTag
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.editor.BUDGET_PERIOD_EDIT_BUTTON_TAG
import com.serranoie.app.minus.presentation.ui.editor.BUDGET_PERIOD_SHEET_TAG
import com.serranoie.app.minus.presentation.ui.editor.BudgetPeriodSheet
import com.serranoie.app.minus.presentation.ui.editor.budgetPeriodCardTag
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
						touchRobot.onNode(hasTestTag(budgetPeriodCardTag(BudgetPeriod.WEEKLY))).performGesture {
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
	)
}
