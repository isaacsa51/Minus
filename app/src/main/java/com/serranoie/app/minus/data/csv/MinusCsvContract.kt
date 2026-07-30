package com.serranoie.app.minus.data.csv

object MinusCsvContract {
    const val FILE_NAME = "minus_export.csv"

    const val COL_DATE = "date"
    const val COL_AMOUNT = "amount"
    const val COL_COMMENT = "comment"
    const val COL_IS_RECURRENT = "is_recurrent"
    const val COL_FREQUENCY = "frequency"
    const val COL_END_DATE = "end_date"
    const val COL_SUB_DAY = "sub_day"
    const val COL_ID = "id"
    const val COL_IS_CREDIT = "is_credit"
    const val COL_IS_CREDIT_PAID = "is_credit_paid"
    const val COL_PERIOD_ID = "period_id"
    const val COL_CREATED_AT = "created_at"

    const val MARKER_META = "__META__"
    const val MARKER_ARCHIVED = "__ARCHIVED__"

    const val COL_BUDGET_TOTAL = "budget_total"
    const val COL_BUDGET_PERIOD = "budget_period"
    const val COL_BUDGET_START_DATE = "budget_start_date"
    const val COL_BUDGET_END_DATE = "budget_end_date"
    const val COL_CURRENCY_CODE = "currency_code"
    const val COL_DAYS_IN_PERIOD = "days_in_period"
    const val COL_ROLLOVER_ENABLED = "rollover_enabled"
    const val COL_ROLLOVER_CARRY_FORWARD = "rollover_carry_forward"
    const val COL_REMAINING_BUDGET_STRATEGY = "remaining_budget_strategy"
    const val COL_CURRENT_PERIOD_STARTED_AT = "current_period_started_at_millis"
    const val COL_CURRENT_PERIOD_ID = "current_period_id"
    const val COL_CREDIT_CARD_CUTOFF_DAY = "credit_card_cutoff_day"
    const val COL_SPLIT_MODE = "split_mode"

    val HEADERS = arrayOf(
        COL_DATE,
        COL_AMOUNT,
        COL_COMMENT,
        COL_IS_RECURRENT,
        COL_FREQUENCY,
        COL_END_DATE,
        COL_SUB_DAY,
        COL_ID,
        COL_IS_CREDIT,
        COL_IS_CREDIT_PAID,
        COL_PERIOD_ID,
        COL_CREATED_AT,
        COL_BUDGET_TOTAL,
        COL_BUDGET_PERIOD,
        COL_BUDGET_START_DATE,
        COL_BUDGET_END_DATE,
        COL_CURRENCY_CODE,
        COL_DAYS_IN_PERIOD,
        COL_ROLLOVER_ENABLED,
        COL_ROLLOVER_CARRY_FORWARD,
        COL_REMAINING_BUDGET_STRATEGY,
        COL_CURRENT_PERIOD_STARTED_AT,
        COL_CURRENT_PERIOD_ID,
        COL_CREDIT_CARD_CUTOFF_DAY,
        COL_SPLIT_MODE,
    )
}
