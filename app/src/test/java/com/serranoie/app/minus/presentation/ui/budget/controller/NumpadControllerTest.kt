package com.serranoie.app.minus.presentation.ui.budget.controller

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.presentation.ui.budget.controller.NumpadController.NumpadChange
import org.junit.Test

/**
 * Unit tests for [NumpadController] — a pure state machine, no Android,
 * no coroutines. Each test follows the Given / When / Then BDD convention
 * with a backticked English name (when_X_then_Y; underscores only because
 * the Android DEX bytecode format rejects spaces in method names).
 */
class NumpadControllerTest {

    private fun newController() = NumpadController()

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `when_controller_is_created_then_input_is_empty_and_calculation_is_false`() {
        val controller = newController()

        assertThat(controller.input.value).isEqualTo("")
        assertThat(controller.isCalculation.value).isFalse()
        assertThat(controller.dragProgress.value).isEqualTo(0f)
    }

    // -------------------------------------------------------------------------
    // NumberTapped
    // -------------------------------------------------------------------------

    @Test
    fun `when_number_tapped_then_input_appends_digit_and_change_is_emitted`() {
        val controller = newController()

        val changes = controller.process(NumpadIntent.NumberTapped("1"), currentIsCalculation = false)

        assertThat(controller.input.value).isEqualTo("1")
        assertThat(changes).containsExactly(NumpadChange.InputChanged("1"))
    }

    @Test
    fun `when_number_tapped_twice_then_input_is_two_chars_and_two_changes_are_emitted`() {
        val controller = newController()

        controller.process(NumpadIntent.NumberTapped("1"), currentIsCalculation = false)
        val changes = controller.process(NumpadIntent.NumberTapped("2"), currentIsCalculation = false)

        assertThat(controller.input.value).isEqualTo("12")
        assertThat(changes).containsExactly(NumpadChange.InputChanged("12"))
    }

    // -------------------------------------------------------------------------
    // DotTapped
    // -------------------------------------------------------------------------

    @Test
    fun `when_dot_tapped_on_empty_input_then_input_becomes_zero_dot`() {
        val controller = newController()

        val changes = controller.process(NumpadIntent.DotTapped, currentIsCalculation = false)

        assertThat(controller.input.value).isEqualTo("0.")
        assertThat(changes).containsExactly(NumpadChange.InputChanged("0."))
    }

    @Test
    fun `when_dot_tapped_after_digit_then_input_gains_a_dot`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("5"), currentIsCalculation = false)

        val changes = controller.process(NumpadIntent.DotTapped, currentIsCalculation = false)

        assertThat(controller.input.value).isEqualTo("5.")
        assertThat(changes).containsExactly(NumpadChange.InputChanged("5."))
    }

    @Test
    fun `when_dot_tapped_twice_in_same_segment_then_second_dot_is_ignored`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("5"), currentIsCalculation = false)
        controller.process(NumpadIntent.DotTapped, currentIsCalculation = false)

        val changes = controller.process(NumpadIntent.DotTapped, currentIsCalculation = false)

        // Second dot in the same segment is a no-op
        assertThat(controller.input.value).isEqualTo("5.")
        assertThat(changes).isEmpty()
    }

    @Test
    fun `when_dot_tapped_after_operator_then_input_becomes_operator_zero_dot`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("5"), currentIsCalculation = false)
        controller.process(NumpadIntent.OperatorTapped('+'), currentIsCalculation = false)

        val changes = controller.process(NumpadIntent.DotTapped, currentIsCalculation = true)

        // After an operator, tapping dot should yield "5+0."
        assertThat(controller.input.value).isEqualTo("5+0.")
    }

    // -------------------------------------------------------------------------
    // BackspaceTapped
    // -------------------------------------------------------------------------

    @Test
    fun `when_backspace_tapped_then_last_char_is_removed_and_change_is_emitted`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("1"), currentIsCalculation = false)
        controller.process(NumpadIntent.NumberTapped("2"), currentIsCalculation = false)

        val changes = controller.process(NumpadIntent.BackspaceTapped, currentIsCalculation = false)

        assertThat(controller.input.value).isEqualTo("1")
        assertThat(changes).containsExactly(NumpadChange.InputChanged("1"))
    }

    @Test
    fun `when_backspace_clears_input_and_is_calculation_then_calculation_flag_is_cleared`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("1"), currentIsCalculation = false)
        // Simulate that we entered calculation mode
        controller.process(NumpadIntent.OperatorTapped('+'), currentIsCalculation = false)
        controller.process(NumpadIntent.BackspaceTapped, currentIsCalculation = true)

        val changes = controller.process(NumpadIntent.BackspaceTapped, currentIsCalculation = true)

        assertThat(controller.input.value).isEqualTo("")
        assertThat(changes).containsExactly(
            NumpadChange.InputChanged(""),
            NumpadChange.CalculationModeChanged(false),
        )
    }

    @Test
    fun `when_backspace_clears_input_but_not_in_calculation_mode_then_calculation_flag_is_not_changed`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("1"), currentIsCalculation = false)

        val changes = controller.process(NumpadIntent.BackspaceTapped, currentIsCalculation = false)

        assertThat(controller.input.value).isEqualTo("")
        assertThat(changes).containsExactly(NumpadChange.InputChanged(""))
    }

    // -------------------------------------------------------------------------
    // OperatorTapped
    // -------------------------------------------------------------------------

    @Test
    fun `when_operator_tapped_on_non_empty_input_then_operator_is_appended_and_calculation_mode_is_set`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("5"), currentIsCalculation = false)

        val changes = controller.process(NumpadIntent.OperatorTapped('+'), currentIsCalculation = false)

        assertThat(controller.input.value).isEqualTo("5+")
        assertThat(changes).containsExactly(
            NumpadChange.InputChanged("5+"),
            NumpadChange.CalculationModeChanged(true),
        )
    }

    @Test
    fun `when_operator_tapped_on_empty_input_and_is_unary_then_operator_is_appended_and_calculation_mode_is_set`() {
        val controller = newController()

        val changesPlus = controller.process(NumpadIntent.OperatorTapped('+'), currentIsCalculation = false)
        assertThat(controller.input.value).isEqualTo("+")
        assertThat(changesPlus).containsExactly(
            NumpadChange.InputChanged("+"),
            NumpadChange.CalculationModeChanged(true),
        )

        controller.process(NumpadIntent.ResetInputTapped, currentIsCalculation = true)

        val changesMinus = controller.process(NumpadIntent.OperatorTapped('-'), currentIsCalculation = false)
        assertThat(controller.input.value).isEqualTo("-")
        assertThat(changesMinus).containsExactly(
            NumpadChange.InputChanged("-"),
            NumpadChange.CalculationModeChanged(true),
        )
    }

    @Test
    fun `when_non_unary_operator_tapped_on_empty_input_then_input_is_unchanged`() {
        val controller = newController()

        val changesMultiply = controller.process(NumpadIntent.OperatorTapped('×'), currentIsCalculation = false)
        assertThat(controller.input.value).isEqualTo("")
        assertThat(changesMultiply).isEmpty()

        val changesDivide = controller.process(NumpadIntent.OperatorTapped('÷'), currentIsCalculation = false)
        assertThat(controller.input.value).isEqualTo("")
        assertThat(changesDivide).isEmpty()
    }

    @Test
    fun `when_operator_tapped_after_another_operator_then_second_operator_is_ignored`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("5"), currentIsCalculation = false)
        controller.process(NumpadIntent.OperatorTapped('+'), currentIsCalculation = false)

        val changes = controller.process(NumpadIntent.OperatorTapped('-'), currentIsCalculation = true)

        // Should NOT replace the existing operator — the production code
        // ignores the second operator entirely.
        assertThat(controller.input.value).isEqualTo("5+")
        assertThat(changes).isEmpty()
    }

    @Test
    fun `when_operator_tapped_after_dot_then_operator_is_ignored`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("5"), currentIsCalculation = false)
        controller.process(NumpadIntent.DotTapped, currentIsCalculation = false)

        val changes = controller.process(NumpadIntent.OperatorTapped('+'), currentIsCalculation = false)

        assertThat(controller.input.value).isEqualTo("5.")
        assertThat(changes).isEmpty()
    }

    @Test
    fun `when_operator_tapped_while_already_in_calculation_mode_then_calculation_flag_is_not_re_emitted`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("5"), currentIsCalculation = false)
        controller.process(NumpadIntent.OperatorTapped('+'), currentIsCalculation = false)
        // Now in calculation mode
        controller.process(NumpadIntent.NumberTapped("3"), currentIsCalculation = true)

        val changes = controller.process(NumpadIntent.OperatorTapped('-'), currentIsCalculation = true)

        // No CalculationModeChanged in the change list — already true
        assertThat(controller.input.value).isEqualTo("5+3-")
        assertThat(changes).containsExactly(NumpadChange.InputChanged("5+3-"))
    }

    // -------------------------------------------------------------------------
    // EqualsTapped
    // -------------------------------------------------------------------------

    @Test
    fun `when_equals_tapped_on_valid_expression_then_input_becomes_evaluation_result`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("2"), currentIsCalculation = false)
        controller.process(NumpadIntent.OperatorTapped('+'), currentIsCalculation = false)
        controller.process(NumpadIntent.NumberTapped("3"), currentIsCalculation = false)

        val changes = controller.process(NumpadIntent.EqualsTapped, currentIsCalculation = false)

        // BudgetExpressionEvaluator evaluates "2+3" -> "5"
        assertThat(controller.input.value).isEqualTo("5")
        assertThat(changes).containsExactly(
            NumpadChange.InputChanged("5"),
            NumpadChange.CalculationModeChanged(true),
        )
    }

    @Test
    fun `when_equals_tapped_on_empty_input_then_input_is_unchanged`() {
        val controller = newController()

        val changes = controller.process(NumpadIntent.EqualsTapped, currentIsCalculation = false)

        assertThat(controller.input.value).isEqualTo("")
        assertThat(changes).isEmpty()
    }

    @Test
    fun `when_equals_tapped_on_invalid_expression_then_input_is_unchanged`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("2"), currentIsCalculation = false)
        controller.process(NumpadIntent.OperatorTapped('+'), currentIsCalculation = false)
        // "2+" is invalid (trailing operator) so evaluator returns null

        val changes = controller.process(NumpadIntent.EqualsTapped, currentIsCalculation = false)

        assertThat(controller.input.value).isEqualTo("2+")
        assertThat(changes).isEmpty()
    }

    // -------------------------------------------------------------------------
    // ResetInputTapped
    // -------------------------------------------------------------------------

    @Test
    fun `when_reset_tapped_then_input_is_cleared_and_calculation_is_cleared_when_in_calculation_mode`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("1"), currentIsCalculation = false)
        controller.process(NumpadIntent.NumberTapped("2"), currentIsCalculation = false)
        controller.process(NumpadIntent.OperatorTapped('+'), currentIsCalculation = false)
        // Now in calculation mode

        val changes = controller.process(NumpadIntent.ResetInputTapped, currentIsCalculation = true)

        assertThat(controller.input.value).isEqualTo("")
        assertThat(changes).containsExactly(
            NumpadChange.InputChanged(""),
            NumpadChange.CalculationModeChanged(false),
        )
    }

    @Test
    fun `when_reset_tapped_not_in_calculation_mode_then_only_input_change_is_emitted`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("1"), currentIsCalculation = false)

        val changes = controller.process(NumpadIntent.ResetInputTapped, currentIsCalculation = false)

        assertThat(controller.input.value).isEqualTo("")
        assertThat(changes).containsExactly(NumpadChange.InputChanged(""))
    }

    // -------------------------------------------------------------------------
    // SetCalculationMode
    // -------------------------------------------------------------------------

    @Test
    fun `when_set_calculation_mode_enabled_then_calculation_flag_and_drag_progress_are_updated`() {
        val controller = newController()
        controller.process(NumpadIntent.SetDragProgress(0.5f), currentIsCalculation = false)

        val changes = controller.process(NumpadIntent.SetCalculationMode(true), currentIsCalculation = false)

        assertThat(controller.isCalculation.value).isTrue()
        assertThat(controller.dragProgress.value).isEqualTo(0f)
        assertThat(changes).containsExactly(
            NumpadChange.CalculationModeChanged(true),
            NumpadChange.DragProgressChanged(0f),
        )
    }

    @Test
    fun `when_set_calculation_mode_disabled_then_calculation_flag_is_false_and_drag_progress_is_reset`() {
        val controller = newController()
        controller.process(NumpadIntent.SetCalculationMode(true), currentIsCalculation = false)
        controller.process(NumpadIntent.SetDragProgress(0.8f), currentIsCalculation = false)

        val changes = controller.process(NumpadIntent.SetCalculationMode(false), currentIsCalculation = true)

        assertThat(controller.isCalculation.value).isFalse()
        assertThat(controller.dragProgress.value).isEqualTo(0f)
        assertThat(changes).containsExactly(
            NumpadChange.CalculationModeChanged(false),
            NumpadChange.DragProgressChanged(0f),
        )
    }

    // -------------------------------------------------------------------------
    // SetDragProgress
    // -------------------------------------------------------------------------

    @Test
    fun `when_set_drag_progress_is_processed_then_value_and_change_are_updated`() {
        val controller = newController()

        val changes = controller.process(NumpadIntent.SetDragProgress(0.42f), currentIsCalculation = false)

        assertThat(controller.dragProgress.value).isEqualTo(0.42f)
        assertThat(changes).containsExactly(NumpadChange.DragProgressChanged(0.42f))
    }

    // -------------------------------------------------------------------------
    // clearInput
    // -------------------------------------------------------------------------

    @Test
    fun `when_clear_input_is_called_then_input_becomes_empty_and_change_is_returned`() {
        val controller = newController()
        controller.process(NumpadIntent.NumberTapped("1"), currentIsCalculation = false)
        controller.process(NumpadIntent.NumberTapped("2"), currentIsCalculation = false)

        val change = controller.clearInput()

        assertThat(controller.input.value).isEqualTo("")
        assertThat(change).isEqualTo(NumpadChange.InputChanged(""))
    }
}
