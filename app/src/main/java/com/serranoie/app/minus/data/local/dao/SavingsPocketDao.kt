package com.serranoie.app.minus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serranoie.app.minus.data.local.entity.SavingsPocketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsPocketDao {
    @Query("SELECT * FROM savings_pockets ORDER BY createdAt ASC")
    fun getAll(): Flow<List<SavingsPocketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pocket: SavingsPocketEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM savings_pockets WHERE id = :id)")
    suspend fun exists(id: Long): Boolean

    @Query("DELETE FROM savings_pockets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
