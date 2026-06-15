package com.serranoie.app.minus.presentation.ui.budget

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.usecase.AddTransactionUseCase
import com.serranoie.app.minus.domain.usecase.DeleteTransactionUseCase
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import logcat.logcat

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
	private val notificationScheduler: NotificationScheduler,
) {
	companion object {
		private const val TAG = "BudgetTransactionHandler"
	}

	suspend fun applyTransaction(
		input: String,
		isCalculation: Boolean,
		isRecurrentEnabled: Boolean,
		isCreditEnabled: Boolean,
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
				isCredit = isCreditEnabled,
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
			isCredit = isCreditEnabled,
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
		isCredit: Boolean = false,
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
		val transactionDate = resolveRecurrentTransactionDate(
			frequency = frequency,
			subscriptionDay = subscriptionDay,
			now = now
		)

		val categoryId: Long? = if (finalComment.isNotBlank()) {
			budgetRepository.findOrCreateCategory(finalComment.trim()).id
		} else {
			null
		}

		val transaction = Transaction.create(
			amount = amount,
			comment = finalComment,
			date = transactionDate,
			periodId = activePeriodId,
			isRecurrent = true,
			recurrentFrequency = frequency,
			recurrentEndDate = endDate.atTime(now.toLocalTime()),
			subscriptionDay = subscriptionDay,
			categoryId = categoryId,
			isCredit = isCredit,
		)
		addTransactionUseCase(transaction)
		notificationScheduler.rescheduleRecurrentExpenseNotifications()
		return true
	}

	private fun resolveRecurrentTransactionDate(
		frequency: RecurrentFrequency,
		subscriptionDay: Int?,
		now: LocalDateTime,
	): LocalDateTime {
		if (frequency != RecurrentFrequency.MONTHLY || subscriptionDay == null) {
			return now
		}

		val targetDay = subscriptionDay.coerceIn(1, now.toLocalDate().lengthOfMonth())
		return now.withDayOfMonth(targetDay)
	}

	suspend fun deleteTransaction(transaction: Transaction): Result<Unit> {
		return runCatching {
			// For virtual recurrent occurrences, use sourceTransactionId to delete the real saved transaction
			val realId = transaction.sourceTransactionId ?: transaction.id
			val transactionToDelete = if (transaction.sourceTransactionId != null) {
				transaction.copy(id = realId, sourceTransactionId = null)
			} else {
				transaction
			}
			notificationScheduler.cancelRecurrentExpenseNotification(transactionToDelete)
			deleteTransactionUseCase(transactionToDelete)
			val stillExists = budgetRepository.getTransactionById(realId)
			if (stillExists != null && !stillExists.isDeleted) {
				logcat(TAG) { "deleteTransaction failed: row with id $realId still exists after delete" }
				throw IllegalStateException("Delete operation did not remove the transaction")
			}
		}
	}

	suspend fun restoreTransaction(transaction: Transaction): Result<Unit> {
		return runCatching {
			addTransactionUseCase(transaction)
			if (transaction.isRecurrent) {
				notificationScheduler.rescheduleRecurrentExpenseNotifications()
			}
		}
	}

	suspend fun editTransaction(updatedTransaction: Transaction): Boolean {
		if (updatedTransaction.amount < BigDecimal.ZERO) return false
		budgetRepository.updateTransaction(updatedTransaction)
		if (updatedTransaction.isRecurrent) {
			notificationScheduler.scheduleRecurrentExpenseNotification(updatedTransaction)
		} else {
			notificationScheduler.cancelRecurrentExpenseNotification(updatedTransaction)
		}
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
