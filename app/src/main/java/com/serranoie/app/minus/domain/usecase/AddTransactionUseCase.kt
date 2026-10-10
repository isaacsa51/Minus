package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.Transaction
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    /** Returns the id the inserted row was given. */
    suspend operator fun invoke(transaction: Transaction): Long =
        budgetRepository.addTransaction(transaction)
}
