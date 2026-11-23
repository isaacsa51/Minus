package com.serranoie.app.minus.data.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate

class CsvImportPreviewTest {

    private val header =
        "date,amount,category,extra_note,is_recurrent,frequency,end_date,sub_day,id,is_credit,is_credit_paid,period_id,created_at,budget_total,budget_period,budget_start_date,budget_end_date,currency_code,days_in_period,rollover_enabled,rollover_carry_forward,remaining_budget_strategy,current_period_started_at_millis,current_period_id,credit_card_cutoff_day,split_mode,is_adjustment,pocket_id"

    private fun preview(vararg lines: String): CsvImportPreview =
        MinusCsvParser()
            .parse(ByteArrayInputStream((listOf(header) + lines).joinToString("\n").toByteArray(StandardCharsets.UTF_8)))
            .toPreview()

    @Test
    fun fullBackup_summarizesCurrentPeriodBudgetArchivesAndRecurring() {
        val p = preview(
            "__META__,,,,,,,,,,,,,3773.00,MONTHLY,2026-10-27,2026-11-26,HKD,31,0,0,ASK_ALWAYS,1793030400000,1793030400000,,DYNAMIC,,",
            "__ARCHIVED__,500.00,,,,,,,,,,1,1700000000000,1000.00,MONTHLY,2026-09-27,2026-10-26,HKD,,,,,,,,,,",
            "2026-12-12 00:39,803.96,present,,1,ONCE,2026-12-12,,1284,0,0,1793030400000,1795192767124,,,,,,,,,,,,,,0,",
            "2026-11-19 20:13,80,medical,,0,,,,1283,0,0,1793030400000,1795090393940,,,,,,,,,,,,,,0,",
            "2026-11-13 17:58,-40,foodie,,0,,,,1261,0,0,1793030400000,1794563924909,,,,,,,,,,,,,,1,",
            "2024-11-06 21:19,188.5,food,,0,,,,1248,0,0,0,1794203503862,,,,,,,,,,,,,,0,",
        )

        assertEquals(2, p.currentPeriodTransactions)
        assertEquals(BigDecimal("3773.00"), p.budgetTotal)
        assertEquals("HKD", p.currencyCode)
        assertEquals(LocalDate.of(2026, 10, 27), p.periodStart)
        assertEquals(LocalDate.of(2026, 11, 26), p.periodEnd)
        assertEquals(1, p.completedPeriods)
        assertEquals(1, p.recurringPayments)
        assertEquals(0, p.discarded)
        assertTrue(p.replacesAllData)
        assertFalse(p.blocked)
    }

    @Test
    fun fullBackupWithBrokenRow_isBlocked() {
        val p = preview(
            "__META__,,,,,,,,,,,,,3773.00,MONTHLY,2026-10-27,2026-11-26,HKD,31,0,0,ASK_ALWAYS,1793030400000,1793030400000,,DYNAMIC,,",
            "not-a-date,80,medical,,0,,,,1283,0,0,1793030400000,1795090393940,,,,,,,,,,,,,,0,",
        )

        assertEquals(1, p.discarded)
        assertTrue(p.blocked)
    }

    @Test
    fun rowsOnlyFile_countsEveryTransactionAndAppends() {
        val p = preview(
            "2026-11-19 20:13,80,medical,,0,,,,0,0,0,0,,,,,,,,,,,,,,,0,",
            "2026-11-18 19:03,68,food,,0,,,,0,0,0,0,,,,,,,,,,,,,,,0,",
            "not-a-date,1,x,,0,,,,0,0,0,0,,,,,,,,,,,,,,,0,",
        )

        assertEquals(2, p.currentPeriodTransactions)
        assertEquals(null, p.budgetTotal)
        assertEquals(1, p.discarded)
        assertFalse(p.replacesAllData)
        assertFalse(p.blocked)
    }
}
