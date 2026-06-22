package com.serranoie.app.minus.presentation.ui.budget.controller

import com.serranoie.app.minus.presentation.ui.editor.AnimState
import com.serranoie.app.minus.presentation.ui.editor.EditMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.time.LocalDate

class EditorStateController {

    private val _state = MutableStateFlow(EditorLocalState())
    val state: StateFlow<EditorLocalState> = _state.asStateFlow()

    sealed interface EditorChange {
        data class EditModeChanged(val mode: EditMode) : EditorChange
        data class AnimStateChanged(val state: AnimState) : EditorChange
        data class CommentChanged(val comment: String) : EditorChange
        data class LockSwipeableChanged(val locked: Boolean) : EditorChange
        data class LockDraggableChanged(val locked: Boolean) : EditorChange
        data class RecurrentEnabledChanged(val enabled: Boolean) : EditorChange
        data class CreditEnabledChanged(val enabled: Boolean) : EditorChange
        data class RecurrentDialogVisibilityChanged(val visible: Boolean) : EditorChange
        data class CreditCutoffDialogVisibilityChanged(val visible: Boolean) : EditorChange
        data class PendingRecurrentAmountChanged(val amount: BigDecimal?) : EditorChange
        data class PendingRecurrentCommentChanged(val comment: String) : EditorChange
        data class SelectedDateChanged(val date: LocalDate) : EditorChange
    }

    fun process(
        intent: EditorIntent,
        hasCreditCardCutoffDay: Boolean,
    ): List<EditorChange> = when (intent) {
        is EditorIntent.SetEditMode -> setEditMode(intent.mode)
        is EditorIntent.SetAnimState -> setAnimState(intent.state)
        is EditorIntent.CommentUpdated -> setComment(intent.comment)
        is EditorIntent.SetLockSwipeable -> setLockSwipeable(intent.locked)
        is EditorIntent.SetLockDraggable -> setLockDraggable(intent.locked)
        is EditorIntent.SetRecurrentEnabled -> setRecurrentEnabled(intent.enabled)
        is EditorIntent.SetCreditEnabled -> setCreditEnabled(intent.enabled, hasCreditCardCutoffDay)
        is EditorIntent.DismissRecurrentDialog -> dismissRecurrentDialog()
        is EditorIntent.DismissCreditCutoffDialog -> dismissCreditCutoffDialog()
        is EditorIntent.DateSelected -> setSelectedDate(intent.date)
    }

    private fun setEditMode(mode: EditMode): List<EditorChange> {
        _state.value = _state.value.copy(editMode = mode)
        return listOf(EditorChange.EditModeChanged(mode))
    }

    private fun setAnimState(animState: AnimState): List<EditorChange> {
        _state.value = _state.value.copy(animState = animState)
        return listOf(EditorChange.AnimStateChanged(animState))
    }

    private fun setComment(comment: String): List<EditorChange> {
        _state.value = _state.value.copy(currentComment = comment)
        return listOf(EditorChange.CommentChanged(comment))
    }

    private fun setLockSwipeable(locked: Boolean): List<EditorChange> {
        _state.value = _state.value.copy(lockSwipeable = locked)
        return listOf(EditorChange.LockSwipeableChanged(locked))
    }

    private fun setLockDraggable(locked: Boolean): List<EditorChange> {
        _state.value = _state.value.copy(lockDraggable = locked)
        return listOf(EditorChange.LockDraggableChanged(locked))
    }

    private fun setRecurrentEnabled(enabled: Boolean): List<EditorChange> {
        _state.value = _state.value.copy(isRecurrentEnabled = enabled)
        return listOf(EditorChange.RecurrentEnabledChanged(enabled))
    }

    private fun setCreditEnabled(enabled: Boolean, hasCutoff: Boolean): List<EditorChange> {
        if (!enabled) {
            val cleared = _state.value.copy(
                isCreditEnabled = false,
                showCreditCutoffDialog = false,
            )
            _state.value = cleared
            return listOf(
                EditorChange.CreditEnabledChanged(false),
                EditorChange.CreditCutoffDialogVisibilityChanged(false),
            )
        }
        val showDialog = !hasCutoff
        val updated = _state.value.copy(
            isCreditEnabled = enabled,
            showCreditCutoffDialog = showDialog,
        )
        _state.value = updated
        return listOf(
            EditorChange.CreditEnabledChanged(enabled),
            EditorChange.CreditCutoffDialogVisibilityChanged(showDialog),
        )
    }

    private fun dismissRecurrentDialog(): List<EditorChange> {
        val updated = _state.value.copy(
            showRecurrentDialog = false,
            pendingRecurrentAmount = null,
            pendingRecurrentComment = "",
        )
        _state.value = updated
        return listOf(
            EditorChange.RecurrentDialogVisibilityChanged(false),
            EditorChange.PendingRecurrentAmountChanged(null),
            EditorChange.PendingRecurrentCommentChanged(""),
        )
    }

    private fun dismissCreditCutoffDialog(): List<EditorChange> {
        val updated = _state.value.copy(
            showCreditCutoffDialog = false,
            isCreditEnabled = false,
        )
        _state.value = updated
        return listOf(
            EditorChange.CreditCutoffDialogVisibilityChanged(false),
            EditorChange.CreditEnabledChanged(false),
        )
    }

    private fun setSelectedDate(date: LocalDate): List<EditorChange> {
        _state.value = _state.value.copy(selectedDate = date)
        return listOf(EditorChange.SelectedDateChanged(date))
    }

    fun showRecurrentDialog(amount: BigDecimal, comment: String): List<EditorChange> {
        val updated = _state.value.copy(
            showRecurrentDialog = true,
            pendingRecurrentAmount = amount,
            pendingRecurrentComment = comment,
        )
        _state.value = updated
        return listOf(
            EditorChange.RecurrentDialogVisibilityChanged(true),
            EditorChange.PendingRecurrentAmountChanged(amount),
            EditorChange.PendingRecurrentCommentChanged(comment),
        )
    }

    fun applyRecurrentDialog(): List<EditorChange> {
        val updated = _state.value.copy(
            showRecurrentDialog = false,
            pendingRecurrentAmount = null,
            pendingRecurrentComment = "",
            isRecurrentEnabled = false,
            isCreditEnabled = false,
        )
        _state.value = updated
        return listOf(
            EditorChange.RecurrentDialogVisibilityChanged(false),
            EditorChange.PendingRecurrentAmountChanged(null),
            EditorChange.PendingRecurrentCommentChanged(""),
            EditorChange.RecurrentEnabledChanged(false),
            EditorChange.CreditEnabledChanged(false),
        )
    }

    fun applyCreditCutoffDay(): List<EditorChange> {
        val updated = _state.value.copy(
            showCreditCutoffDialog = false,
            isCreditEnabled = true,
        )
        _state.value = updated
        return listOf(
            EditorChange.CreditCutoffDialogVisibilityChanged(false),
            EditorChange.CreditEnabledChanged(true),
        )
    }

    fun deleteTag(tag: String): EditorChange.PendingRecurrentCommentChanged {
        // Tag management is handled in the parent VM (it persists the
        // hide via BudgetRepository). The controller only clears the
        // pendingComment to keep its state consistent. This is a
        // minimal hook — full tag list management stays in the VM
        // for now.
        return EditorChange.PendingRecurrentCommentChanged(_state.value.currentComment)
    }
}

data class EditorLocalState(
    val editMode: EditMode = EditMode.ADD,
    val animState: AnimState = AnimState.IDLE,
    val currentComment: String = "",
    val lockSwipeable: Boolean = false,
    val lockDraggable: Boolean = false,
    val isRecurrentEnabled: Boolean = false,
    val isCreditEnabled: Boolean = false,
    val showRecurrentDialog: Boolean = false,
    val showCreditCutoffDialog: Boolean = false,
    val pendingRecurrentAmount: BigDecimal? = null,
    val pendingRecurrentComment: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
)

sealed interface EditorIntent {
    data class SetEditMode(val mode: EditMode) : EditorIntent
    data class SetAnimState(val state: AnimState) : EditorIntent
    data class CommentUpdated(val comment: String) : EditorIntent
    data class SetLockSwipeable(val locked: Boolean) : EditorIntent
    data class SetLockDraggable(val locked: Boolean) : EditorIntent
    data class SetRecurrentEnabled(val enabled: Boolean) : EditorIntent
    data class SetCreditEnabled(val enabled: Boolean) : EditorIntent
    data object DismissRecurrentDialog : EditorIntent
    data object DismissCreditCutoffDialog : EditorIntent
    data class DateSelected(val date: LocalDate) : EditorIntent
}
