package com.serranoie.app.minus.presentation.settings

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CsvTransferEntryPoint {
    fun csvTransferManager(): CsvTransferManager
}
