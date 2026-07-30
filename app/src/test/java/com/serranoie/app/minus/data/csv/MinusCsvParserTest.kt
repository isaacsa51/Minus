package com.serranoie.app.minus.data.csv

import com.serranoie.app.minus.domain.model.BudgetSplitMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

class MinusCsvParserTest {

    private val parser = MinusCsvParser()

    @Test
    fun parse_validRows_returnsRows() {
        val csv = """
            date,amount,comment,is_recurrent,frequency,end_date,sub_day,id,is_credit,is_credit_paid,period_id,budget_total,budget_period,budget_start_date,budget_end_date,currency_code,days_in_period,rollover_enabled,rollover_carry_forward,remaining_budget_strategy,current_period_started_at_millis,current_period_id,credit_card_cutoff_day,split_mode
            2026-03-10 09:30,10.50,Coffee,0,,,,1,0,0,7,,,,,,,,,,,,
            2026-03-11 10:00,25.00,Netflix,1,MONTHLY,2026-12-31,15,2,0,0,7,,,,,,,,,,,,
        """.trimIndent()

        val payload = parser.parse(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        val rows = payload.rows

        assertEquals(2, rows.size)
        assertEquals("Coffee", rows[0].comment)
        assertEquals(1L, rows[0].id)
        assertEquals(7L, rows[0].periodId)
    }

    @Test
    fun parse_invalidAmount_discardsRow() {
        val csv = """
            date,amount,comment,is_recurrent,frequency,end_date,sub_day,id,is_credit,is_credit_paid,period_id,budget_total,budget_period,budget_start_date,budget_end_date,currency_code,days_in_period,rollover_enabled,rollover_carry_forward,remaining_budget_strategy,current_period_started_at_millis,current_period_id,credit_card_cutoff_day,split_mode
            2026-03-10 09:30,0.00,Invalid,0,,,,1,0,0,7,,,,,,,,,,,,
        """.trimIndent()

        val payload = parser.parse(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        val rows = payload.rows

        assertTrue(rows.isEmpty())
    }

    @Test
    fun parse_metadata_with_split_mode_returnsCorrectSettings() {
        val csv = """
            date,amount,comment,is_recurrent,frequency,end_date,sub_day,id,is_credit,is_credit_paid,budget_total,budget_period,budget_start_date,budget_end_date,currency_code,days_in_period,rollover_enabled,rollover_carry_forward,remaining_budget_strategy,current_period_started_at_millis,current_period_id,credit_card_cutoff_day,split_mode
            __META__,,,,,,,,,,1000.00,MONTHLY,2026-01-01,2026-01-31,USD,31,0,0,ASK_ALWAYS,1710000000000,101,3,DYNAMIC
        """.trimIndent()

        val payload = parser.parse(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))
        
        val meta = payload.metadata
        assertEquals(BigDecimal("1000.00"), meta?.budgetSettings?.totalBudget)
        assertEquals(BudgetSplitMode.DYNAMIC, meta?.budgetSettings?.splitMode)
    }
}
