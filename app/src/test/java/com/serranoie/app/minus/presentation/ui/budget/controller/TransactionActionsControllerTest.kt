package com.serranoie.app.minus.presentation.ui.budget.controller

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.budget.ApplyTransactionResult
import com.serranoie.app.minus.presentation.ui.budget.controller.TransactionActionsController.TransactionAction
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for [TransactionActionsController]. The controller delegates
 * the heavy lifting to a [TransactionHandler] strategy; tests provide a
 * fake that returns pre-canned [ApplyTransactionResult]s and Result-wrapped
 * operations. Backticked English test names (when_X_then_Y) following
 * the BDD Given/When/Then convention.
 */
class TransactionActionsControllerTest {

    /**
     * A configurable fake handler so each test can pin the exact result it
     * wants to assert against.
     */
    private class FakeHandler : TransactionHandler {
        var applyResult: ApplyTransactionResult = ApplyTransactionResult.InvalidInput
        var applyRecurrentResult: Boolean = false
        var deleteResult: kotlin.Result<Unit> = kotlin.Result.success(Unit)
        var restoreResult: kotlin.Result<Unit> = kotlin.Result.success(Unit)
        var editCalls: MutableList<Transaction> = mutableListOf()

        override suspend fun apply(
            input: String,
            isCalculation: Boolean,
            isRecurrentEnabled: Boolean,
            isCreditEnabled: Boolean,
            comment: String,
            budgetSettings: BudgetSettings?,
            resolveActivePeriodId: suspend () -> Long,
        ): ApplyTransactionResult = applyResult

        override suspend fun applyRecurrent(
            pendingAmount: BigDecimal?,
            pendingComment: String,
            frequency: RecurrentFrequency,
            endDate: LocalDate,
            subscriptionDay: Int?,
            resolveActivePeriodId: suspend () -> Long,
            isCredit: Boolean,
        ): Boolean = applyRecurrentResult

        override suspend fun delete(transaction: Transaction): kotlin.Result<Unit> = deleteResult
        override suspend fun restore(transaction: Transaction): kotlin.Result<Unit> = restoreResult
        override suspend fun edit(transaction: Transaction) {
            editCalls.add(transaction)
        }
    }

    private fun newController(handler: FakeHandler = FakeHandler()) =
        TransactionActionsController(handler)

    private fun sampleTransaction() = Transaction(
        id = 1L,
        amount = BigDecimal("12.34"),
        comment = "Coffee",
        date = LocalDateTime.of(2026, 1, 1, 9, 0),
    )

    private fun sampleSettings() = BudgetSettings(
        totalBudget = BigDecimal("1000.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 1, 30),
        currencyCode = "USD",
        daysInPeriod = 30,
    )

    // -------------------------------------------------------------------------
    // apply
    // -------------------------------------------------------------------------

    @Test
    fun `when_handler_returns_invalid_input_then_no_actions_are_emitted`() = runTest {
        val controller = newController(
            FakeHandler().apply { applyResult = ApplyTransactionResult.InvalidInput }
        )

        val actions = controller.apply(
            input = "abc",
            isCalculation = false,
            isRecurrentEnabled = false,
            isCreditEnabled = false,
            comment = "",
            budgetSettings = sampleSettings(),
            resolveActivePeriodId = { 1L },
        )

        assertThat(actions).isEmpty()
    }

    @Test
    fun `when_handler_returns_added_then_clear_input_clear_editor_flags_and_added_action_are_emitted`() = runTest {
        val controller = newController(
            FakeHandler().apply { applyResult = ApplyTransactionResult.Added(normalizedInput = "12.34") }
        )

        val actions = controller.apply(
            input = "12.34",
            isCalculation = false,
            isRecurrentEnabled = false,
            isCreditEnabled = false,
            comment = "Lunch",
            budgetSettings = sampleSettings(),
            resolveActivePeriodId = { 1L },
        )

        assertThat(actions).containsExactly(
            TransactionAction.ClearInput,
            TransactionAction.ClearEditorFlags,
            TransactionAction.TransactionAdded,
        )
    }

    @Test
    fun `when_handler_returns_queued_for_next_period_then_clear_input_clear_flags_queued_and_show_message_actions_are_emitted`() = runTest {
        val controller = newController(
            FakeHandler().apply { applyResult = ApplyTransactionResult.QueuedForNextPeriod(normalizedInput = "12.34") }
        )

        val actions = controller.apply(
            input = "12.34",
            isCalculation = false,
            isRecurrentEnabled = false,
            isCreditEnabled = false,
            comment = "Lunch",
            budgetSettings = sampleSettings(),
            resolveActivePeriodId = { 1L },
        )

        assertThat(actions).containsExactly(
            TransactionAction.ClearInput,
            TransactionAction.ClearEditorFlags,
            TransactionAction.TransactionQueuedForNextPeriod,
            TransactionAction.ShowMessage("Gasto en cola para el proximo periodo"),
        )
    }

    @Test
    fun `when_handler_returns_show_recurrent_dialog_then_open_dialog_action_is_emitted_with_amount_and_comment`() = runTest {
        val amount = BigDecimal("12.34")
        val normalized = "12.34"
        val controller = newController(
            FakeHandler().apply {
                applyResult = ApplyTransactionResult.ShowRecurrentDialog(
                    normalizedInput = normalized,
                    amount = amount,
                )
            }
        )

        val actions = controller.apply(
            input = "12.34",
            isCalculation = false,
            isRecurrentEnabled = true,
            isCreditEnabled = false,
            comment = "Subscription",
            budgetSettings = sampleSettings(),
            resolveActivePeriodId = { 1L },
        )

        assertThat(actions).containsExactly(
            TransactionAction.OpenRecurrentDialog(
                normalizedInput = normalized,
                amount = amount,
                comment = "Subscription",
            ),
        )
    }

    // -------------------------------------------------------------------------
    // applyRecurrent
    // -------------------------------------------------------------------------

    @Test
    fun `when_handler_reports_applied_recurrent_successfully_then_clear_input_action_is_emitted`() = runTest {
        val controller = newController(
            FakeHandler().apply { applyRecurrentResult = true }
        )

        val actions = controller.applyRecurrent(
            frequency = RecurrentFrequency.MONTHLY,
            endDate = LocalDate.of(2027, 1, 1),
            subscriptionDay = 15,
            pendingAmount = BigDecimal("9.99"),
            pendingComment = "Subscription",
            resolveActivePeriodId = { 1L },
            isCredit = false,
        )

        assertThat(actions).containsExactly(TransactionAction.ClearInput)
    }

    @Test
    fun `when_handler_reports_recurrent_failure_then_no_actions_are_emitted`() = runTest {
        val controller = newController(
            FakeHandler().apply { applyRecurrentResult = false }
        )

        val actions = controller.applyRecurrent(
            frequency = RecurrentFrequency.MONTHLY,
            endDate = LocalDate.of(2027, 1, 1),
            subscriptionDay = null,
            pendingAmount = BigDecimal("9.99"),
            pendingComment = "Subscription",
            resolveActivePeriodId = { 1L },
            isCredit = false,
        )

        assertThat(actions).isEmpty()
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    fun `when_handler_reports_delete_success_then_no_actions_are_emitted`() = runTest {
        val controller = newController(
            FakeHandler().apply { deleteResult = kotlin.Result.success(Unit) }
        )

        val actions = controller.delete(sampleTransaction())

        assertThat(actions).isEmpty()
    }

    @Test
    fun `when_handler_reports_delete_failure_then_delete_failed_action_is_emitted`() = runTest {
        val controller = newController(
            FakeHandler().apply { deleteResult = kotlin.Result.failure(RuntimeException("boom")) }
        )

        val actions = controller.delete(sampleTransaction())

        assertThat(actions).containsExactly(TransactionAction.DeleteFailed)
    }

    // -------------------------------------------------------------------------
    // restore
    // -------------------------------------------------------------------------

    @Test
    fun `when_handler_reports_restore_success_then_no_actions_are_emitted`() = runTest {
        val controller = newController(
            FakeHandler().apply { restoreResult = kotlin.Result.success(Unit) }
        )

        val actions = controller.restore(sampleTransaction())

        assertThat(actions).isEmpty()
    }

    @Test
    fun `when_handler_reports_restore_failure_then_restore_failed_action_is_emitted`() = runTest {
        val controller = newController(
            FakeHandler().apply { restoreResult = kotlin.Result.failure(RuntimeException("boom")) }
        )

        val actions = controller.restore(sampleTransaction())

        assertThat(actions).containsExactly(TransactionAction.RestoreFailed)
    }

    // -------------------------------------------------------------------------
    // edit
    // -------------------------------------------------------------------------

    @Test
    fun `when_edit_is_called_then_handler_receives_the_transaction`() = runTest {
        val handler = FakeHandler()
        val controller = newController(handler)
        val transaction = sampleTransaction()

        controller.edit(transaction)

        assertThat(handler.editCalls).containsExactly(transaction)
    }
}
