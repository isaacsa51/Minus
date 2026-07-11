package com.serranoie.app.minus.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val isHidden: Boolean = false,
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun create(
            name: String,
            isHidden: Boolean = false,
            usageCount: Int = 0,
            lastUsedAt: Long? = null,
            createdAt: Long = System.currentTimeMillis()
        ): Category = Category(
            id = 0,
            name = name,
            isHidden = isHidden,
            usageCount = usageCount,
            lastUsedAt = lastUsedAt,
            createdAt = createdAt
        )
    }
}