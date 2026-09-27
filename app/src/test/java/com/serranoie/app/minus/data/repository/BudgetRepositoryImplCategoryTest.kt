package com.serranoie.app.minus.data.repository

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.local.dao.CategoryDao
import com.serranoie.app.minus.data.local.entity.CategoryEntity
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class BudgetRepositoryImplCategoryTest {

    private class FakeCategoryDao : CategoryDao {
        val rows = mutableListOf<CategoryEntity>()
        var nextId = 1L

        override fun getAllCategories(): Flow<List<CategoryEntity>> = flowOf(rows.toList())

        override fun getActiveCategories(): Flow<List<CategoryEntity>> =
            flowOf(rows.filter { !it.isHidden })

        override suspend fun getCategoryByName(name: String): CategoryEntity? =
            rows.firstOrNull { it.name == name }

        override suspend fun insertCategory(category: CategoryEntity): Long {
            val id = nextId++
            rows.add(category.copy(id = id))
            return id
        }

        override suspend fun updateCategory(category: CategoryEntity) {
            val index = rows.indexOfFirst { it.id == category.id }
            if (index >= 0) rows[index] = category
        }

        override suspend fun hideCategory(name: String) = replace(name) { it.copy(isHidden = true) }

        override suspend fun unhideCategory(name: String) =
            replace(name) { it.copy(isHidden = false) }

        override suspend fun incrementUsage(name: String, timestamp: Long) =
            replace(name) { it.copy(usageCount = it.usageCount + 1, lastUsedAt = timestamp) }

        private fun replace(name: String, transform: (CategoryEntity) -> CategoryEntity) {
            val index = rows.indexOfFirst { it.name == name }
            if (index >= 0) rows[index] = transform(rows[index])
        }
    }

    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var repo: BudgetRepositoryImpl

    @Before
    fun setUp() {
        categoryDao = FakeCategoryDao()
        repo = BudgetRepositoryImpl(
            appDatabase = mockk(relaxed = true),
            transactionDao = mockk(relaxed = true),
            settingsDao = mockk(relaxed = true),
            archivedBudgetDao = mockk(relaxed = true),
            categoryDao = categoryDao,
            queuedTransactionDao = mockk(relaxed = true),
            paidRecurrentOccurrenceDao = mockk(relaxed = true),
        )
    }

    @Test
    fun `createCategory inserts an unused visible category`() = runTest {
        repo.createCategory("Coffee")

        val row = categoryDao.rows.single()
        assertThat(row.name).isEqualTo("Coffee")
        assertThat(row.usageCount).isEqualTo(0)
        assertThat(row.isHidden).isFalse()
    }

    @Test
    fun `createCategory restores a hidden category instead of duplicating it`() = runTest {
        repo.findOrCreateCategory("Coffee")
        repo.hideCategory("Coffee")

        repo.createCategory("Coffee")

        val row = categoryDao.rows.single()
        assertThat(row.isHidden).isFalse()
        assertThat(row.usageCount).isEqualTo(1)
    }

    @Test
    fun `createCategory leaves an existing visible category untouched`() = runTest {
        repo.findOrCreateCategory("Coffee")
        repo.incrementCategoryUsage("Coffee")

        repo.createCategory("Coffee")

        val row = categoryDao.rows.single()
        assertThat(row.id).isEqualTo(1L)
        assertThat(row.usageCount).isEqualTo(2)
    }
}
