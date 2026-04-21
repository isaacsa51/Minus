package com.serranoie.app.minus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.serranoie.app.minus.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY usageCount DESC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE isHidden = 0 ORDER BY usageCount DESC, name ASC")
    fun getActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE category SET isHidden = 1 WHERE name = :name")
    suspend fun hideCategory(name: String)

    @Query("UPDATE category SET isHidden = 0 WHERE name = :name")
    suspend fun unhideCategory(name: String)

    @Query("UPDATE category SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE name = :name")
    suspend fun incrementUsage(name: String, timestamp: Long = System.currentTimeMillis())
}