package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.budget.BudgetUiState
import com.serranoie.app.minus.presentation.ui.editor.AnimState
import com.serranoie.app.minus.presentation.ui.editor.Editor
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class EditorScreenshotTest {
	@get:Rule
	val paparazzi = Paparazzi(
		deviceConfig = DeviceConfig.PIXEL_5,
		renderingMode = SessionParams.RenderingMode.NORMAL,
	)

	@Test
	fun editorIdleWithHealthyBudget() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				Editor(
					uiState = sampleBudgetUiState(),
					animState = AnimState.IDLE,
					onFocus = {},
					onOpenHistory = {},
					onOpenSettings = {},
					onCommentClick = {},
					modifier = Modifier.fillMaxSize(),
				)
			}
		}
	}

	@Test
	fun editorEditingAmountWithTagsAndComment() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				Editor(
					uiState = sampleBudgetUiState().copy(
						numpadInput = "24.50",
						isNumpadValid = true,
						animState = AnimState.EDITING,
						currentComment = "Lunch with team",
						tags = listOf("food", "work"),
						isRecurrentEnabled = true,
						isCreditEnabled = true,
					),
					animState = AnimState.EDITING,
					onFocus = {},
					onOpenHistory = {},
					onOpenSettings = {},
					onCommentClick = {},
					onCommentUpdate = {},
					onDeleteTag = {},
					onRecurrentToggle = {},
					onCreditToggle = {},
					showCreditQuickToggleFeature = true,
					modifier = Modifier.fillMaxSize(),
				)
			}
		}
	}

	@Test
	fun editorEditingCalculationExpression() {
		Locale.setDefault(Locale.US)

		paparazzi.snapshot {
			MinusTheme {
				Editor(
					uiState = sampleBudgetUiState().copy(
						numpadInput = "18.50+6.25",
						isNumpadValid = true,
						isCalculation = true,
						animState = AnimState.EDITING,
						currentComment = "Groceries split",
						tags = listOf("groceries"),
					),
					animState = AnimState.EDITING,
					onFocus = {},
					onOpenHistory = {},
					onOpenSettings = {},
					onCommentClick = {},
					onCommentUpdate = {},
					onDeleteTag = {},
					modifier = Modifier.fillMaxSize(),
				)
			}
		}
	}

	private fun sampleBudgetUiState(): BudgetUiState = BudgetUiState(
		budgetSettings = BudgetSettings(
			totalBudget = BigDecimal("900.00"),
			period = BudgetPeriod.MONTHLY,
			startDate = LocalDate.of(2026, 1, 1),
			endDate = LocalDate.of(2026, 1, 30),
			currencyCode = "USD",
			daysInPeriod = 30,
		),
		budgetState = BudgetState(
			remainingToday = BigDecimal("31.25"),
			totalSpentToday = BigDecimal("18.75"),
			dailyBudget = BigDecimal("50.00"),
			daysRemaining = 12,
			progress = 0.58f,
			isOverBudget = false,
			totalBudget = BigDecimal("900.00"),
			totalSpentInPeriod = BigDecimal("522.45"),
		),
		transactions = emptyList(),
		selectedDate = LocalDate.of(2026, 1, 15),
		currentPeriodStartedAtMillis = LocalDate.of(2026, 1, 1).toEpochDay(),
		currentPeriodId = 7L,
	)
}
