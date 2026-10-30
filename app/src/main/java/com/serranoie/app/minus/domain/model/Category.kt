package com.serranoie.app.minus.domain.model

import java.math.BigDecimal

data class Category(
    val id: Long = 0,
    val name: String,
    val isHidden: Boolean = false,
    val usageCount: Int = 0,
    val lastUsedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val limit: CategoryLimit? = null,
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

data class CategoryLimit(
    val amount: BigDecimal,
    val unit: CategoryLimitUnit,
)

enum class CategoryLimitUnit {
    WEEKLY,
    PERIOD;

    companion object {
        fun from(value: String?): CategoryLimitUnit = entries.firstOrNull { it.name == value } ?: PERIOD
    }
}
