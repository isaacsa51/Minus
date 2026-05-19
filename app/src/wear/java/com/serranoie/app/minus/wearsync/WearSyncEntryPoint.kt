package com.serranoie.app.minus.wearsync

import com.serranoie.app.minus.data.repository.BudgetRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WearSyncEntryPoint {
    fun budgetRepository(): BudgetRepository
    fun wearExpenseIngestor(): WearExpenseIngestor
}
