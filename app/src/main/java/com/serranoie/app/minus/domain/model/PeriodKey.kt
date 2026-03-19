package com.serranoie.app.minus.domain.model

import java.time.LocalDate

data class PeriodKey(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val period: BudgetPeriod
)
