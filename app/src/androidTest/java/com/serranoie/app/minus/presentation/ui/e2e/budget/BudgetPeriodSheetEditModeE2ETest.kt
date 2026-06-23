package com.serranoie.app.minus.presentation.ui.e2e.budget

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.editor.BUDGET_PERIOD_APPLY_BUTTON_TAG
import com.serranoie.app.minus.presentation.ui.editor.BUDGET_PERIOD_BUDGET_INPUT_TAG
import com.serranoie.app.minus.presentation.ui.editor.BUDGET_PERIOD_EDIT_BUTTON_TAG
import com.serranoie.app.minus.presentation.ui.editor.BUDGET_PERIOD_SHEET_TAG
import com.serranoie.app.minus.presentation.ui.editor.BudgetPeriodSheet
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class BudgetPeriodSheetEditModeE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun renderSheet(
        budgetSettings: BudgetSettings? = null,
        budgetState: BudgetState? = null,
        startInEditMode: Boolean = false,
        onSaveBudget: ((BudgetSettings) -> Unit)? = null,
        onEditBudget: (() -> Unit)? = null,
        onFinishEarly: (() -> Unit)? = null,
        selectedPeriod: BudgetPeriod = BudgetPeriod.DAILY,
    ) {
        composeTestRule.setContent {
            MinusTheme {
                BudgetPeriodSheet(
                    budgetSettings = budgetSettings,
                    budgetState = budgetState,
                    selectedPeriod = selectedPeriod,
                    currencyCode = "USD",
                    onPeriodSelected = {},
                    onSaveBudget = onSaveBudget,
                    onEditBudget = onEditBudget,
                    onFinishEarly = onFinishEarly,
                    startInEditMode = startInEditMode,
                    pendingExpensesCount = 0,
                )
            }
        }
    }

    @Test
    fun when_no_active_budget_then_sheet_opens_in_edit_mode_and_budget_input_is_visible() {
        // Given
        renderSheet(
            budgetSettings = null,
            startInEditMode = true,
            onSaveBudget = {},
        )

        // Then
        composeTestRule
            .onNodeWithTag(BUDGET_PERIOD_BUDGET_INPUT_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun when_no_active_budget_then_sheet_opens_in_edit_mode_and_apply_button_is_visible() {
        // Given
        renderSheet(
            budgetSettings = null,
            startInEditMode = true,
            onSaveBudget = {},
        )

        // Then
        composeTestRule
            .onNodeWithTag(BUDGET_PERIOD_APPLY_BUTTON_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun when_no_active_budget_then_sheet_opens_in_edit_mode_and_view_mode_edit_button_is_hidden() {
        // Given
        renderSheet(
            budgetSettings = null,
            startInEditMode = true,
            onSaveBudget = {},
        )

        // Then
        composeTestRule
            .onNodeWithTag(BUDGET_PERIOD_EDIT_BUTTON_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun when_no_active_budget_then_sheet_opens_in_edit_mode_and_title_is_new_budget_period() {
        // Given
        renderSheet(
            budgetSettings = null,
            startInEditMode = true,
            onSaveBudget = {},
        )

        // Then
        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(R.string.new_budget_period))
            .assertIsDisplayed()
    }

    @Test
    fun when_no_active_budget_then_sheet_opens_in_edit_mode_and_sheet_root_tag_is_present() {
        // Given
        renderSheet(
            budgetSettings = null,
            startInEditMode = true,
            onSaveBudget = {},
        )

        // Then
        composeTestRule
            .onNodeWithTag(BUDGET_PERIOD_SHEET_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun when_existing_budget_then_sheet_opens_in_view_mode_and_edit_button_is_visible() {
        // Given
        val settings = sampleBudgetSettings()
        renderSheet(
            budgetSettings = settings,
            startInEditMode = false,
            onEditBudget = {},
        )

        // Then
        composeTestRule
            .onNodeWithTag(BUDGET_PERIOD_EDIT_BUTTON_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun when_existing_budget_then_sheet_opens_in_view_mode_and_apply_button_is_hidden() {
        // Given
        val settings = sampleBudgetSettings()
        renderSheet(
            budgetSettings = settings,
            startInEditMode = false,
            onEditBudget = {},
        )

        // Then
        composeTestRule
            .onNodeWithTag(BUDGET_PERIOD_APPLY_BUTTON_TAG)
            .assertIsNotDisplayed()
    }

    @Test
    fun when_existing_budget_then_sheet_opens_in_view_mode_and_budget_input_is_hidden() {
        // Given
        val settings = sampleBudgetSettings()
        renderSheet(
            budgetSettings = settings,
            startInEditMode = false,
            onEditBudget = {},
        )

        // Then
        composeTestRule
            .onNodeWithTag(BUDGET_PERIOD_BUDGET_INPUT_TAG)
            .assertIsNotDisplayed()
    }

    private fun sampleBudgetSettings(
        totalBudget: BigDecimal = BigDecimal("1000.00"),
        period: BudgetPeriod = BudgetPeriod.MONTHLY,
        startDate: LocalDate = LocalDate.of(2026, 1, 1),
    ): BudgetSettings = BudgetSettings(
        totalBudget = totalBudget,
        period = period,
        startDate = startDate,
        endDate = startDate.plusDays(period.days().toLong() - 1L),
        currencyCode = "USD",
        daysInPeriod = period.days(),
        rollOverEnabled = false,
    )

    private fun BudgetPeriod.days(): Int = when (this) {
        BudgetPeriod.DAILY -> 1
        BudgetPeriod.WEEKLY -> 7
        BudgetPeriod.BIWEEKLY -> 14
        BudgetPeriod.MONTHLY -> 30
    }

    @Test
    fun when_sample_budget_settings_built_then_total_budget_and_period_are_defaults() {
        // Given / When
        val settings = sampleBudgetSettings()

        // Then
        Truth.assertThat(settings.totalBudget).isEqualTo(BigDecimal("1000.00"))
        Truth.assertThat(settings.period).isEqualTo(BudgetPeriod.MONTHLY)
    }
}
