package com.serranoie.app.minus.presentation.ui.budget.controller

import android.content.Context
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.budget.ApplyTransactionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import logcat.logcat
import java.math.BigDecimal
import java.time.LocalDate

class TransactionActionsController(
    private val handler: TransactionHandler,
    private val context: Context,
) {
    companion object {
        private const val TAG = "TransactionActionsController"
    }

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    sealed interface TransactionAction {
        data object ClearInput : TransactionAction
        data object ClearEditorFlags : TransactionAction
        data object TransactionAdded : TransactionAction
        data object TransactionQueuedForNextPeriod : TransactionAction
        data class OpenRecurrentDialog(
            val normalizedInput: String,
            val amount: BigDecimal,
            val comment: String,
        ) : TransactionAction

        data class ShowMessage(val message: String) : TransactionAction
        data object DeleteFailed : TransactionAction
        data object RestoreFailed : TransactionAction
    }

    suspend fun apply(
        input: String,
        isCalculation: Boolean,
        isRecurrentEnabled: Boolean,
        isCreditEnabled: Boolean,
        comment: String,
        budgetSettings: BudgetSettings?,
        resolveActivePeriodId: suspend () -> Long,
    ): List<TransactionAction> {
        val result = handler.apply(
            input = input,
            isCalculation = isCalculation,
            isRecurrentEnabled = isRecurrentEnabled,
            isCreditEnabled = isCreditEnabled,
            comment = comment,
            budgetSettings = budgetSettings,
            resolveActivePeriodId = resolveActivePeriodId,
        )
        return when (result) {
            is ApplyTransactionResult.InvalidInput -> emptyList()
            is ApplyTransactionResult.ShowRecurrentDialog -> listOf(
                TransactionAction.OpenRecurrentDialog(
                    normalizedInput = result.normalizedInput,
                    amount = result.amount,
                    comment = comment,
                ),
            )

            is ApplyTransactionResult.QueuedForNextPeriod -> listOf(
                TransactionAction.ClearInput,
                TransactionAction.ClearEditorFlags,
                TransactionAction.TransactionQueuedForNextPeriod,
                TransactionAction.ShowMessage(context.getString(R.string.expense_queued_for_next_period)),
            )

            is ApplyTransactionResult.Added -> listOf(
                TransactionAction.ClearInput,
                TransactionAction.ClearEditorFlags,
                TransactionAction.TransactionAdded,
            )

            is ApplyTransactionResult.Failed -> {
                logcat(TAG) { "Could not save transaction: ${result.cause}" }
                listOf(
                    TransactionAction.ShowMessage(context.getString(R.string.history_snackbar_save_transaction_failed)),
                )
            }
        }
    }

    suspend fun applyRecurrent(
        frequency: RecurrentFrequency,
        endDate: LocalDate,
        subscriptionDay: Int?,
        pendingAmount: BigDecimal?,
        pendingComment: String,
        resolveActivePeriodId: suspend () -> Long,
        isCredit: Boolean,
        fallbackComment: String,
    ): List<TransactionAction> {
        val applied = handler.applyRecurrent(
            pendingAmount = pendingAmount,
            pendingComment = pendingComment,
            frequency = frequency,
            endDate = endDate,
            subscriptionDay = subscriptionDay,
            resolveActivePeriodId = resolveActivePeriodId,
            isCredit = isCredit,
            fallbackComment = fallbackComment,
        )
        return if (applied) {
            listOf(TransactionAction.ClearInput)
        } else {
            emptyList()
        }
    }

    suspend fun delete(transaction: Transaction): List<TransactionAction> {
        val result = handler.delete(transaction)
        return if (result.isSuccess) {
            emptyList()
        } else {
            listOf(TransactionAction.DeleteFailed)
        }
    }

    suspend fun restore(transaction: Transaction): List<TransactionAction> {
        val result = handler.restore(transaction)
        return if (result.isSuccess) {
            emptyList()
        } else {
            listOf(TransactionAction.RestoreFailed)
        }
    }

    suspend fun edit(transaction: Transaction): Result<Unit> {
        val result = handler.edit(transaction)
        if (result.isFailure) {
            logcat(TAG) { "Failed to edit transaction id=${transaction.id}: ${result.exceptionOrNull()}" }
        }
        return result
    }
}

interface TransactionHandler {
    suspend fun apply(
        input: String,
        isCalculation: Boolean,
        isRecurrentEnabled: Boolean,
        isCreditEnabled: Boolean,
        comment: String,
        budgetSettings: BudgetSettings?,
        resolveActivePeriodId: suspend () -> Long,
    ): ApplyTransactionResult

    suspend fun applyRecurrent(
        pendingAmount: BigDecimal?,
        pendingComment: String,
        frequency: RecurrentFrequency,
        endDate: LocalDate,
        subscriptionDay: Int?,
        resolveActivePeriodId: suspend () -> Long,
        isCredit: Boolean,
        fallbackComment: String,
    ): Boolean

    suspend fun delete(transaction: Transaction): Result<Unit>
    suspend fun restore(transaction: Transaction): Result<Unit>
    suspend fun edit(transaction: Transaction): Result<Unit>
}
