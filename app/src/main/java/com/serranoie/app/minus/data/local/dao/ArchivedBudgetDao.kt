package com.serranoie.app.minus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serranoie.app.minus.data.local.entity.ArchivedBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchivedBudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(archivedBudget: ArchivedBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(archivedBudgets: List<ArchivedBudgetEntity>)

    @Query("SELECT * FROM archived_budgets ORDER BY startDate DESC")
    fun getAllArchivedBudgets(): Flow<List<ArchivedBudgetEntity>>

    @Query("SELECT * FROM archived_budgets WHERE periodId = :periodId LIMIT 1")
    suspend fun getArchivedBudgetById(periodId: Long): ArchivedBudgetEntity?

    @Query("DELETE FROM archived_budgets WHERE periodId = :periodId")
    suspend fun deleteById(periodId: Long)
}
