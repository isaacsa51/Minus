package com.serranoie.app.minus.data.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class MinusCsvParserTest {

    private val parser = MinusCsvParser()

    @Test
    fun parse_validRows_returnsRows() {
        val csv = """
            date,amount,comment,is_recurrent,frequency,end_date,sub_day,id
            2026-03-10 09:30,10.50,Coffee,0,,, ,1
            2026-03-11 10:00,25.00,Netflix,1,MONTHLY,2026-12-31,15,2
        """.trimIndent()

        val (rows, errors) = parser.parse(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))

        assertEquals(2, rows.size)
        assertTrue(errors.isEmpty())
        assertEquals("Coffee", rows[0].comment)
        assertEquals(1L, rows[0].id)
    }

    @Test
    fun parse_invalidAmount_discardsRow() {
        val csv = """
            date,amount,comment,is_recurrent,frequency,end_date,sub_day,id
            2026-03-10 09:30,0.00,Invalid,0,,, ,1
        """.trimIndent()

        val (rows, errors) = parser.parse(ByteArrayInputStream(csv.toByteArray(StandardCharsets.UTF_8)))

        assertTrue(rows.isEmpty())
        assertEquals(1, errors.size)
    }
}
