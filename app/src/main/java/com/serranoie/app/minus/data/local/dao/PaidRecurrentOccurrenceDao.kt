package com.serranoie.app.minus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serranoie.app.minus.data.local.entity.PaidRecurrentOccurrenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaidRecurrentOccurrenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markPaid(entity: PaidRecurrentOccurrenceEntity)

    @Query("SELECT * FROM paid_recurrent_occurrences")
    fun getAllPaidOccurrences(): Flow<List<PaidRecurrentOccurrenceEntity>>

    @Query("SELECT occurrenceDateEpochDay FROM paid_recurrent_occurrences WHERE transactionId = :transactionId")
    suspend fun getPaidOccurrenceDatesFor(transactionId: Long): List<Long>

    @Query("DELETE FROM paid_recurrent_occurrences WHERE transactionId = :transactionId")
    suspend fun deleteAllForTransaction(transactionId: Long)
}
