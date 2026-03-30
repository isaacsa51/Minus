package com.serranoie.app.minus.domain.usecase

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.domain.model.Transaction
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        budgetRepository.addTransaction(transaction)
    }
}
