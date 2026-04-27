package com.serranoie.app.minus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serranoie.app.minus.data.local.entity.QueuedTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueuedTransactionDao {

    @Query("SELECT * FROM queued_transactions ORDER BY createdAt ASC")
    fun getAllQueuedTransactions(): Flow<List<QueuedTransactionEntity>>

    @Query("SELECT * FROM queued_transactions ORDER BY createdAt ASC")
    suspend fun getAllQueuedTransactionsSync(): List<QueuedTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(queuedTransaction: QueuedTransactionEntity): Long

    @Query("DELETE FROM queued_transactions")
    suspend fun clearAll()
}
