package com.serranoie.app.minus.presentation.budget

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.usecase.AddTransactionUseCase
import com.serranoie.app.minus.domain.usecase.DeleteTransactionUseCase
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

sealed interface ApplyTransactionResult {
	data class ShowRecurrentDialog(
		val amount: BigDecimal,
		val normalizedInput: String,
	) : ApplyTransactionResult

	data class QueuedForNextPeriod(
		val normalizedInput: String,
	) : ApplyTransactionResult

	data class Added(
		val normalizedInput: String,
	) : ApplyTransactionResult

	data object InvalidInput : ApplyTransactionResult
}

class BudgetTransactionHandler @Inject constructor(
	private val budgetRepository: BudgetRepository,
	private val addTransactionUseCase: AddTransactionUseCase,
	private val deleteTransactionUseCase: DeleteTransactionUseCase,
	private val budgetExpressionEvaluator: BudgetExpressionEvaluator,
) {

	suspend fun applyTransaction(
		input: String,
		isCalculation: Boolean,
		isRecurrentEnabled: Boolean,
		comment: String,
		budgetSettings: BudgetSettings?,
		resolveActivePeriodId: suspend () -> Long,
	): ApplyTransactionResult {
		var normalizedInput = input

		if (isCalculation && normalizedInput.any { it in "+-×÷" }) {
			val calculated = budgetExpressionEvaluator.evaluate(normalizedInput)
			if (calculated != null) {
				normalizedInput = calculated
			}
		}

		if (!validateNumpadInput(normalizedInput)) return ApplyTransactionResult.InvalidInput

		val amount = try {
			BigDecimal(normalizedInput)
		} catch (_: NumberFormatException) {
			return ApplyTransactionResult.InvalidInput
		}

		if (amount < BigDecimal.ZERO) return ApplyTransactionResult.InvalidInput

		if (isRecurrentEnabled) {
			return ApplyTransactionResult.ShowRecurrentDialog(
				amount = amount,
				normalizedInput = normalizedInput,
			)
		}

		val today = LocalDate.now()
		if (budgetSettings != null && today.isAfter(budgetSettings.getPeriodEndDate())) {
			val categoryId: Long? = if (comment.isNotBlank()) {
				budgetRepository.findOrCreateCategory(comment.trim()).id
			} else {
				null
			}

			val pendingTransaction = Transaction.create(
				amount = amount,
				comment = comment,
				date = LocalDateTime.now(),
				periodId = 0L,
				categoryId = categoryId,
			)
			budgetRepository.addQueuedTransaction(pendingTransaction)
			return ApplyTransactionResult.QueuedForNextPeriod(normalizedInput = normalizedInput)
		}

		val activePeriodId = resolveActivePeriodId()
		val categoryId: Long? = if (comment.isNotBlank()) {
			budgetRepository.findOrCreateCategory(comment.trim()).id
		} else {
			null
		}

		val transaction = Transaction.create(
			amount = amount,
			comment = comment,
			date = LocalDateTime.now(),
			periodId = activePeriodId,
			categoryId = categoryId,
		)
		addTransactionUseCase(transaction)
		return ApplyTransactionResult.Added(normalizedInput = normalizedInput)
	}

	suspend fun applyRecurrentExpense(
		pendingAmount: BigDecimal?,
		pendingComment: String,
		frequency: RecurrentFrequency,
		endDate: LocalDate,
		subscriptionDay: Int?,
		resolveActivePeriodId: suspend () -> Long,
	): Boolean {
		val amount = pendingAmount ?: return false
		val rawComment = pendingComment.trim()
		val now = LocalDateTime.now()

		val fallbackComment = when (frequency) {
			RecurrentFrequency.WEEKLY -> "Subscripción semanal sin nombre"
			RecurrentFrequency.BIWEEKLY -> "Subscripción quincenal sin nombre"
			RecurrentFrequency.MONTHLY -> "Subscripción mensual sin nombre"
		}

		val finalComment = rawComment.ifEmpty { fallbackComment }
		val activePeriodId = resolveActivePeriodId()

		val categoryId: Long? = if (finalComment.isNotBlank()) {
			budgetRepository.findOrCreateCategory(finalComment.trim()).id
		} else {
			null
		}

		val transaction = Transaction.create(
			amount = amount,
			comment = finalComment,
			date = now,
			periodId = activePeriodId,
			isRecurrent = true,
			recurrentFrequency = frequency,
			recurrentEndDate = endDate.atTime(now.toLocalTime()),
			subscriptionDay = subscriptionDay,
			categoryId = categoryId,
		)
		addTransactionUseCase(transaction)
		return true
	}

	suspend fun deleteTransaction(transaction: Transaction): Result<Unit> {
		return runCatching {
			deleteTransactionUseCase(transaction)
		}
	}

	suspend fun restoreTransaction(transaction: Transaction): Result<Unit> {
		return runCatching {
			addTransactionUseCase(transaction)
		}
	}

	suspend fun editTransaction(updatedTransaction: Transaction): Boolean {
		if (updatedTransaction.amount < BigDecimal.ZERO) return false
		budgetRepository.updateTransaction(updatedTransaction)
		return true
	}

	private fun validateNumpadInput(input: String): Boolean {
		if (input.isEmpty()) return false
		if (input == ".") return false
		return try {
			val value = BigDecimal(input)
			value > BigDecimal.ZERO
		} catch (_: NumberFormatException) {
			false
		}
	}
}
