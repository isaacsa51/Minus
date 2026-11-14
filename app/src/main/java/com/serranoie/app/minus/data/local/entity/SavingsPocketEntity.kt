package com.serranoie.app.minus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_pockets")
data class SavingsPocketEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAmount: String? = null,
    val targetEpochDay: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
