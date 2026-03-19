package com.serranoie.app.minus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.serranoie.app.minus.data.local.entity.BudgetSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for budget settings.
 * Uses single-row pattern (id = 1).
 */
@Dao
interface BudgetSettingsDao {

    @Query("SELECT * FROM budget_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<BudgetSettingsEntity?>

    @Query("SELECT * FROM budget_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): BudgetSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: BudgetSettingsEntity)

    @Query("DELETE FROM budget_settings")
    suspend fun clear()
}
