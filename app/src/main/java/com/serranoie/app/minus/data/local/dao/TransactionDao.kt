package com.serranoie.app.minus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.serranoie.app.minus.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for transaction operations.
 * All queries return Flow for reactive updates.
 */
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :startDate AND date < :endDate 
        ORDER BY date DESC
    """)
    fun getTransactionsForDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :startOfDay AND date < :endOfDay 
        ORDER BY date DESC
    """)
    fun getTransactionsForDay(
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<TransactionEntity>>

    @Query("""
        SELECT SUM(CAST(amount AS REAL)) FROM transactions 
        WHERE date >= :startOfDay AND date < :endOfDay
    """)
    fun getTotalSpentForDay(
        startOfDay: Long,
        endOfDay: Long
    ): Flow<Double?>

    @Query("""
        SELECT SUM(CAST(amount AS REAL)) FROM transactions 
        WHERE date >= :startDate AND date < :endDate
    """)
    fun getTotalSpentForPeriod(
        startDate: Long,
        endDate: Long
    ): Flow<Double?>

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllOrReplace(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Long)

    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: Long): TransactionEntity?
}
