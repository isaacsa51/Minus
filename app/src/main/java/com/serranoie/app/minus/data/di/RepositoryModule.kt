package com.serranoie.app.minus.data.di

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.BudgetRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for repository bindings.
 * Provides dependency injection bindings for repository interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds BudgetRepositoryImpl to BudgetRepository interface.
     * This allows injecting BudgetRepository interface while getting BudgetRepositoryImpl.
     */
    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        impl: BudgetRepositoryImpl
    ): BudgetRepository
}