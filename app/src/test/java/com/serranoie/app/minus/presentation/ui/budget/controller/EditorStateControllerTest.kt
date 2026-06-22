package com.serranoie.app.minus.presentation.ui.budget.controller

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.presentation.ui.budget.controller.EditorStateController.EditorChange
import com.serranoie.app.minus.presentation.ui.editor.AnimState
import com.serranoie.app.minus.presentation.ui.editor.EditMode
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Unit tests for [EditorStateController] — a pure state machine for the
 * editor sheet's local state. No Android, no coroutines. Backticked English
 * test names (when_X_then_Y) following the BDD Given/When/Then convention.
 */
class EditorStateControllerTest {

    private fun newController() = EditorStateController()

    @Test
    fun `when_controller_is_created_then_state_has_defaults`() {
        val controller = newController()

        val state = controller.state.value
        assertThat(state.editMode).isEqualTo(EditMode.ADD)
        assertThat(state.animState).isEqualTo(AnimState.IDLE)
        assertThat(state.currentComment).isEqualTo("")
        assertThat(state.lockSwipeable).isFalse()
        assertThat(state.lockDraggable).isFalse()
        assertThat(state.isRecurrentEnabled).isFalse()
        assertThat(state.isCreditEnabled).isFalse()
        assertThat(state.showRecurrentDialog).isFalse()
        assertThat(state.showCreditCutoffDialog).isFalse()
        assertThat(state.pendingRecurrentAmount).isNull()
        assertThat(state.pendingRecurrentComment).isEqualTo("")
    }

    // -------------------------------------------------------------------------
    // SetEditMode
    // -------------------------------------------------------------------------

    @Test
    fun `when_set_edit_mode_is_processed_then_edit_mode_and_change_are_updated`() {
        val controller = newController()

        val changes = controller.process(EditorIntent.SetEditMode(EditMode.EDIT), hasCreditCardCutoffDay = false)

        assertThat(controller.state.value.editMode).isEqualTo(EditMode.EDIT)
        assertThat(changes).containsExactly(EditorChange.EditModeChanged(EditMode.EDIT))
    }

    // -------------------------------------------------------------------------
    // SetAnimState
    // -------------------------------------------------------------------------

    @Test
    fun `when_set_anim_state_is_processed_then_anim_state_and_change_are_updated`() {
        val controller = newController()

        val changes = controller.process(EditorIntent.SetAnimState(AnimState.EDITING), hasCreditCardCutoffDay = false)

        assertThat(controller.state.value.animState).isEqualTo(AnimState.EDITING)
        assertThat(changes).containsExactly(EditorChange.AnimStateChanged(AnimState.EDITING))
    }

    // -------------------------------------------------------------------------
    // CommentUpdated
    // -------------------------------------------------------------------------

    @Test
    fun `when_comment_updated_is_processed_then_comment_and_change_are_updated`() {
        val controller = newController()

        val changes = controller.process(EditorIntent.CommentUpdated("Lunch"), hasCreditCardCutoffDay = false)

        assertThat(controller.state.value.currentComment).isEqualTo("Lunch")
        assertThat(changes).containsExactly(EditorChange.CommentChanged("Lunch"))
    }

    // -------------------------------------------------------------------------
    // Lock toggles
    // -------------------------------------------------------------------------

    @Test
    fun `when_set_lock_swipeable_is_processed_then_lock_swipeable_and_change_are_updated`() {
        val controller = newController()

        val changes = controller.process(EditorIntent.SetLockSwipeable(true), hasCreditCardCutoffDay = false)

        assertThat(controller.state.value.lockSwipeable).isTrue()
        assertThat(changes).containsExactly(EditorChange.LockSwipeableChanged(true))
    }

    @Test
    fun `when_set_lock_draggable_is_processed_then_lock_draggable_and_change_are_updated`() {
        val controller = newController()

        val changes = controller.process(EditorIntent.SetLockDraggable(true), hasCreditCardCutoffDay = false)

        assertThat(controller.state.value.lockDraggable).isTrue()
        assertThat(changes).containsExactly(EditorChange.LockDraggableChanged(true))
    }

    // -------------------------------------------------------------------------
    // Recurrent toggle
    // -------------------------------------------------------------------------

    @Test
    fun `when_set_recurrent_enabled_true_is_processed_then_flag_and_change_are_updated`() {
        val controller = newController()

        val changes = controller.process(EditorIntent.SetRecurrentEnabled(true), hasCreditCardCutoffDay = false)

        assertThat(controller.state.value.isRecurrentEnabled).isTrue()
        assertThat(changes).containsExactly(EditorChange.RecurrentEnabledChanged(true))
    }

    @Test
    fun `when_set_recurrent_enabled_false_is_processed_then_flag_is_cleared`() {
        val controller = newController()
        controller.process(EditorIntent.SetRecurrentEnabled(true), hasCreditCardCutoffDay = false)

        val changes = controller.process(EditorIntent.SetRecurrentEnabled(false), hasCreditCardCutoffDay = false)

        assertThat(controller.state.value.isRecurrentEnabled).isFalse()
        assertThat(changes).containsExactly(EditorChange.RecurrentEnabledChanged(false))
    }

    // -------------------------------------------------------------------------
    // Credit toggle
    // -------------------------------------------------------------------------

    @Test
    fun `when_set_credit_enabled_true_with_no_cutoff_day_then_credit_flag_and_dialog_are_both_set`() {
        val controller = newController()

        val changes = controller.process(EditorIntent.SetCreditEnabled(true), hasCreditCardCutoffDay = false)

        val state = controller.state.value
        assertThat(state.isCreditEnabled).isTrue()
        assertThat(state.showCreditCutoffDialog).isTrue()
        assertThat(changes).containsExactly(
            EditorChange.CreditEnabledChanged(true),
            EditorChange.CreditCutoffDialogVisibilityChanged(true),
        )
    }

    @Test
    fun `when_set_credit_enabled_true_with_cutoff_day_then_credit_flag_is_set_but_dialog_is_not_shown`() {
        val controller = newController()

        val changes = controller.process(EditorIntent.SetCreditEnabled(true), hasCreditCardCutoffDay = true)

        val state = controller.state.value
        assertThat(state.isCreditEnabled).isTrue()
        assertThat(state.showCreditCutoffDialog).isFalse()
        assertThat(changes).containsExactly(
            EditorChange.CreditEnabledChanged(true),
            EditorChange.CreditCutoffDialogVisibilityChanged(false),
        )
    }

    @Test
    fun `when_set_credit_enabled_false_is_processed_then_credit_flag_and_dialog_are_both_cleared`() {
        val controller = newController()
        controller.process(EditorIntent.SetCreditEnabled(true), hasCreditCardCutoffDay = false)

        val changes = controller.process(EditorIntent.SetCreditEnabled(false), hasCreditCardCutoffDay = true)

        val state = controller.state.value
        assertThat(state.isCreditEnabled).isFalse()
        assertThat(state.showCreditCutoffDialog).isFalse()
        assertThat(changes).containsExactly(
            EditorChange.CreditEnabledChanged(false),
            EditorChange.CreditCutoffDialogVisibilityChanged(false),
        )
    }

    // -------------------------------------------------------------------------
    // Recurrent dialog
    // -------------------------------------------------------------------------

    @Test
    fun `when_show_recurrent_dialog_is_called_then_dialog_amount_and_comment_are_set`() {
        val controller = newController()

        val changes = controller.showRecurrentDialog(
            amount = BigDecimal("12.34"),
            comment = "Coffee",
        )

        val state = controller.state.value
        assertThat(state.showRecurrentDialog).isTrue()
        assertThat(state.pendingRecurrentAmount).isEqualTo(BigDecimal("12.34"))
        assertThat(state.pendingRecurrentComment).isEqualTo("Coffee")
        assertThat(changes).containsExactly(
            EditorChange.RecurrentDialogVisibilityChanged(true),
            EditorChange.PendingRecurrentAmountChanged(BigDecimal("12.34")),
            EditorChange.PendingRecurrentCommentChanged("Coffee"),
        )
    }

    @Test
    fun `when_dismiss_recurrent_dialog_is_processed_then_dialog_and_pending_values_are_cleared`() {
        val controller = newController()
        controller.showRecurrentDialog(BigDecimal("12.34"), "Coffee")

        val changes = controller.process(EditorIntent.DismissRecurrentDialog, hasCreditCardCutoffDay = false)

        val state = controller.state.value
        assertThat(state.showRecurrentDialog).isFalse()
        assertThat(state.pendingRecurrentAmount).isNull()
        assertThat(state.pendingRecurrentComment).isEqualTo("")
        assertThat(changes).containsExactly(
            EditorChange.RecurrentDialogVisibilityChanged(false),
            EditorChange.PendingRecurrentAmountChanged(null),
            EditorChange.PendingRecurrentCommentChanged(""),
        )
    }

    @Test
    fun `when_apply_recurrent_dialog_is_called_then_dialog_amount_comment_and_toggles_are_cleared`() {
        val controller = newController()
        controller.showRecurrentDialog(BigDecimal("12.34"), "Coffee")
        controller.process(EditorIntent.SetRecurrentEnabled(true), hasCreditCardCutoffDay = false)
        controller.process(EditorIntent.SetCreditEnabled(true), hasCreditCardCutoffDay = true)

        val changes = controller.applyRecurrentDialog()

        val state = controller.state.value
        assertThat(state.showRecurrentDialog).isFalse()
        assertThat(state.pendingRecurrentAmount).isNull()
        assertThat(state.pendingRecurrentComment).isEqualTo("")
        assertThat(state.isRecurrentEnabled).isFalse()
        assertThat(state.isCreditEnabled).isFalse()
        assertThat(changes).containsExactly(
            EditorChange.RecurrentDialogVisibilityChanged(false),
            EditorChange.PendingRecurrentAmountChanged(null),
            EditorChange.PendingRecurrentCommentChanged(""),
            EditorChange.RecurrentEnabledChanged(false),
            EditorChange.CreditEnabledChanged(false),
        )
    }

    // -------------------------------------------------------------------------
    // Credit cutoff dialog
    // -------------------------------------------------------------------------

    @Test
    fun `when_dismiss_credit_cutoff_dialog_is_processed_then_dialog_and_credit_flag_are_cleared`() {
        val controller = newController()
        controller.process(EditorIntent.SetCreditEnabled(true), hasCreditCardCutoffDay = false)

        val changes = controller.process(EditorIntent.DismissCreditCutoffDialog, hasCreditCardCutoffDay = true)

        val state = controller.state.value
        assertThat(state.showCreditCutoffDialog).isFalse()
        assertThat(state.isCreditEnabled).isFalse()
        assertThat(changes).containsExactly(
            EditorChange.CreditCutoffDialogVisibilityChanged(false),
            EditorChange.CreditEnabledChanged(false),
        )
    }

    @Test
    fun `when_apply_credit_cutoff_day_is_called_then_dialog_is_hidden_and_credit_is_enabled`() {
        val controller = newController()
        controller.process(EditorIntent.SetCreditEnabled(true), hasCreditCardCutoffDay = false)

        val changes = controller.applyCreditCutoffDay()

        val state = controller.state.value
        assertThat(state.showCreditCutoffDialog).isFalse()
        assertThat(state.isCreditEnabled).isTrue()
        assertThat(changes).containsExactly(
            EditorChange.CreditCutoffDialogVisibilityChanged(false),
            EditorChange.CreditEnabledChanged(true),
        )
    }

    // -------------------------------------------------------------------------
    // DateSelected
    // -------------------------------------------------------------------------

    @Test
    fun `when_date_selected_is_processed_then_selected_date_and_change_are_updated`() {
        val controller = newController()
        val date = LocalDate.of(2026, 6, 21)

        val changes = controller.process(EditorIntent.DateSelected(date), hasCreditCardCutoffDay = false)

        assertThat(controller.state.value.selectedDate).isEqualTo(date)
        assertThat(changes).containsExactly(EditorChange.SelectedDateChanged(date))
    }
}
