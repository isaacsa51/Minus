package com.serranoie.app.minus.data.repository

import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate

interface BudgetRepository {

	fun getTransactions(): Flow<List<Transaction>>

	fun getQueuedTransactions(): Flow<List<Transaction>>

	fun getTransactionsForPeriod(start: LocalDate, end: LocalDate): Flow<List<Transaction>>

	suspend fun addTransaction(transaction: Transaction)

	suspend fun addQueuedTransaction(transaction: Transaction)

	suspend fun addTransactionIfAbsent(transaction: Transaction): Boolean

	suspend fun updateTransaction(transaction: Transaction)

	suspend fun assignQueuedTransactionsToPeriod(periodId: Long)

	suspend fun upsertTransactions(transactions: List<Transaction>)

	suspend fun existsTransactionByClientGeneratedId(clientGeneratedId: String): Boolean

	suspend fun getRecentTransactions(limit: Int): List<Transaction>

	suspend fun deleteTransaction(transaction: Transaction)

	fun getSpentForDate(date: LocalDate): Flow<BigDecimal>

	fun getSpentForPeriod(start: LocalDate, end: LocalDate): Flow<BigDecimal>

	fun getBudgetSettings(): Flow<BudgetSettings?>

	suspend fun saveBudgetSettings(settings: BudgetSettings)

	suspend fun getBudgetSettingsSync(): BudgetSettings?

	fun calculateBudgetState(settings: BudgetSettings, currentDate: LocalDate): Flow<BudgetState>

	fun getActiveCategories(): Flow<List<com.serranoie.app.minus.domain.model.Category>>

	suspend fun findOrCreateCategory(name: String): com.serranoie.app.minus.domain.model.Category

	suspend fun hideCategory(name: String)

	suspend fun incrementCategoryUsage(name: String)

	suspend fun getPeriodCount(): Int
}
