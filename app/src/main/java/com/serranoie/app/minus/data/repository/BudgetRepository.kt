package com.serranoie.app.minus.data.repository

import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Repository interface for budget-related operations.
 * All operations return Flow for reactive updates.
 */
interface BudgetRepository {

    //region Transactions
    /**
     * Get all transactions ordered by date descending.
     */
    fun getTransactions(): Flow<List<Transaction>>

    /**
     * Get transactions for a specific date range.
     */
    fun getTransactionsForPeriod(start: LocalDate, end: LocalDate): Flow<List<Transaction>>

    /**
     * Add a new transaction.
     */
    suspend fun addTransaction(transaction: Transaction)

    /**
     * Update an existing transaction.
     */
    suspend fun updateTransaction(transaction: Transaction)

    /**
     * Insert or replace transactions by ID in bulk.
     */
    suspend fun upsertTransactions(transactions: List<Transaction>)

    /**
     * Delete a transaction (soft delete or hard delete based on implementation).
     */
    suspend fun deleteTransaction(transaction: Transaction)

    /**
     * Get total spent amount for a specific day.
     */
    fun getSpentForDate(date: LocalDate): Flow<BigDecimal>

    /**
     * Get total spent amount for a date range.
     */
    fun getSpentForPeriod(start: LocalDate, end: LocalDate): Flow<BigDecimal>
    //endregion

    //region Budget Settings
    /**
     * Get current budget settings.
     */
    fun getBudgetSettings(): Flow<BudgetSettings?>

    /**
     * Save budget settings.
     */
    suspend fun saveBudgetSettings(settings: BudgetSettings)

    /**
     * Get budget settings synchronously (for initial state).
     */
    suspend fun getBudgetSettingsSync(): BudgetSettings?
    //endregion

    //region Budget Calculations
    /**
     * Calculate current budget state based on settings and transactions.
     * This combines settings, transactions, and current date to produce display state.
     */
    fun calculateBudgetState(settings: BudgetSettings, currentDate: LocalDate): Flow<BudgetState>
    //endregion
}
