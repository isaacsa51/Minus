package com.serranoie.app.minus.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class SavingsPocket(
    val id: Long = 0,
    val name: String,
    val target: BigDecimal? = null,
    val targetDate: LocalDate? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isGoalPocket: Boolean get() = name.isBlank()
}
