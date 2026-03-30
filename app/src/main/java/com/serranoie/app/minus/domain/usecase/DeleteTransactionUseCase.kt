package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.Transaction
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        budgetRepository.deleteTransaction(transaction)
    }
}
