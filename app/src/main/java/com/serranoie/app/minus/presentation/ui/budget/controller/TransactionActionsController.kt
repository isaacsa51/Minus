package com.serranoie.app.minus.presentation.ui.budget.controller

import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.budget.ApplyTransactionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigDecimal
import java.time.LocalDate

class TransactionActionsController(
    private val handler: TransactionHandler,
) {

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
                TransactionAction.ShowMessage("Gasto en cola para el proximo periodo"),
            )

            is ApplyTransactionResult.Added -> listOf(
                TransactionAction.ClearInput,
                TransactionAction.ClearEditorFlags,
                TransactionAction.TransactionAdded,
            )
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
    ): List<TransactionAction> {
        val applied = handler.applyRecurrent(
            pendingAmount = pendingAmount,
            pendingComment = pendingComment,
            frequency = frequency,
            endDate = endDate,
            subscriptionDay = subscriptionDay,
            resolveActivePeriodId = resolveActivePeriodId,
            isCredit = isCredit,
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

    suspend fun edit(transaction: Transaction) {
        handler.edit(transaction)
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
    ): Boolean

    suspend fun delete(transaction: Transaction): kotlin.Result<Unit>
    suspend fun restore(transaction: Transaction): kotlin.Result<Unit>
    suspend fun edit(transaction: Transaction)
}
