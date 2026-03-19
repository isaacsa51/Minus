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

    val HEADERS = arrayOf(
        COL_DATE,
        COL_AMOUNT,
        COL_COMMENT,
        COL_IS_RECURRENT,
        COL_FREQUENCY,
        COL_END_DATE,
        COL_SUB_DAY,
        COL_ID
    )
}
