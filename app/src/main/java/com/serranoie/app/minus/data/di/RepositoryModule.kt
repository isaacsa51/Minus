package com.serranoie.app.minus.data.di

import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.BudgetRepositoryImpl
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.data.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(
        impl: BudgetRepositoryImpl
    ): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}