package com.serranoie.app.minus.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class ReceiptParserTest {

    @Test
    fun reads_merchant_and_total_from_spanish_receipt() {
        val text = """
            OXXO
            CADENA COMERCIAL OXXO SA DE CV
            RFC CCO8605231N4
            27/10/2026 14:32
            COCA COLA 600ML      18.50
            SABRITAS              21.00
            SUBTOTAL              33.97
            IVA                    5.53
            TOTAL $               39.50
            EFECTIVO             100.00
            CAMBIO                60.50
        """.trimIndent()

        val scan = ReceiptParser.parse(text)

        assertEquals("OXXO", scan?.merchant)
        assertEquals(BigDecimal("39.50"), scan?.total)
    }

    @Test
    fun reads_grouped_total_with_comma_thousands() {
        val text = """
            Walmart
            Supercenter
            TOTAL A PAGAR   $1,234.50
        """.trimIndent()

        assertEquals(BigDecimal("1234.50"), ReceiptParser.parse(text)?.total)
    }

    @Test
    fun reads_comma_decimal_total() {
        val text = """
            MERCADONA S.A.
            TOTAL (€)   23,45
        """.trimIndent()

        val scan = ReceiptParser.parse(text)

        assertEquals("MERCADONA S.A.", scan?.merchant)
        assertEquals(BigDecimal("23.45"), scan?.total)
    }

    @Test
    fun takes_total_from_next_line_when_keyword_line_has_no_amount() {
        val text = """
            Starbucks Coffee
            TOTAL
            $ 87.00
        """.trimIndent()

        assertEquals(BigDecimal("87.00"), ReceiptParser.parse(text)?.total)
    }

    @Test
    fun total_line_without_amount_does_not_take_cash_from_the_next_line() {
        val text = """
            Tienda
            Tacos          45.00
            TOTAL
            EFECTIVO      100.00
        """.trimIndent()

        assertEquals(BigDecimal("45.00"), ReceiptParser.parse(text)?.total)
    }

    @Test
    fun total_line_without_amount_does_not_take_tax_or_subtotal_from_the_next_line() {
        assertEquals(BigDecimal("50.00"), ReceiptParser.parse("Tienda\nCafe 50.00\nTOTAL\nIVA 8.00")?.total)
        assertEquals(BigDecimal("50.00"), ReceiptParser.parse("Tienda\nCafe 50.00\nTOTAL\nSubtotal 43.10")?.total)
    }

    @Test
    fun falls_back_to_largest_amount_ignoring_cash_and_change() {
        val text = """
            La Esquina
            Tacos          45.00
            Refresco       25.00
            Suma           70.00
            Efectivo      200.00
            Cambio        130.00
        """.trimIndent()

        assertEquals(BigDecimal("70.00"), ReceiptParser.parse(text)?.total)
    }

    @Test
    fun ignores_dates_and_times_as_amounts() {
        val text = """
            Farmacia del Ahorro
            Fecha 27.10.26 12:30
            TOTAL  15.00
        """.trimIndent()

        assertEquals(BigDecimal("15.00"), ReceiptParser.parse(text)?.total)
    }

    @Test
    fun skips_noise_lines_when_picking_merchant() {
        val text = """
            ****
            TICKET DE VENTA
            Soriana
            TOTAL 10.00
        """.trimIndent()

        assertEquals("Soriana", ReceiptParser.parse(text)?.merchant)
    }

    @Test
    fun reads_whole_number_total_on_total_line() {
        assertEquals(BigDecimal("150"), ReceiptParser.parse("Mi Tienda\nTotal: \$150")?.total)
    }

    @Test
    fun reads_grouped_whole_number_total_on_total_line() {
        assertEquals(BigDecimal("1234"), ReceiptParser.parse("Mi Tienda\nTotal: \$1,234")?.total)
    }

    @Test
    fun repairs_ocr_spaces_around_the_decimal_point() {
        assertEquals(BigDecimal("123.45"), ReceiptParser.parse("Mi Tienda\nTotal: \$123. 45")?.total)
        assertEquals(BigDecimal("123.45"), ReceiptParser.parse("Mi Tienda\nTotal: \$123 .45")?.total)
    }

    @Test
    fun repairs_ocr_letters_misread_inside_amounts() {
        assertEquals(BigDecimal("123.45"), ReceiptParser.parse("Mi Tienda\nTotal: \$l23.4S")?.total)
        assertEquals(BigDecimal("100.50"), ReceiptParser.parse("Mi Tienda\nTotal: \$1O0.5O")?.total)
    }

    @Test
    fun keeps_dollar_misread_as_s_out_of_the_amount() {
        assertEquals(BigDecimal("123.45"), ReceiptParser.parse("Mi Tienda\nTotal: S123.45")?.total)
    }

    @Test
    fun fallback_without_total_keyword_ignores_whole_numbers() {
        val text = """
            Mi Tienda
            Ticket 4521
            Coca Cola     18.50
        """.trimIndent()

        assertEquals(BigDecimal("18.50"), ReceiptParser.parse(text)?.total)
    }

    @Test
    fun merchant_text_is_not_rewritten_by_amount_repair() {
        assertEquals("Soriana Hiper", ReceiptParser.parse("Soriana Hiper\nTotal: \$l23.45")?.merchant)
    }

    @Test
    fun reads_one_decimal_total_from_real_device_ocr() {
        val text = """
            SAUP NAME

            Address: Lorem Ipsum, 23-10
            Telp. 11223344

            CASH RECEIPT

            Description Price
            Lorem 1.1
            ipsum 2.2
            Dolor sit amet 33

            + Consectetur 44

            + Adipiscing elit 5.5
            Total 16.5
            Cash 20.0
            Change 3.5
            Bank card sro 234

            | Approval Code #123456

            AAA

            THANK YOU!

            DR 0
        """.trimIndent()

        val scan = ReceiptParser.parse(text)

        assertEquals(BigDecimal("16.5"), scan?.total)
        assertEquals("SAUP NAME", scan?.merchant)
    }

    @Test
    fun fallback_reads_one_decimal_amounts() {
        assertEquals(BigDecimal("5.5"), ReceiptParser.parse("Mi Tienda\nTacos 5.5\nAgua 1.1")?.total)
    }

    @Test
    fun returns_null_when_no_amount_is_found() {
        assertNull(ReceiptParser.parse("blurry ~~ nothing here"))
        assertNull(ReceiptParser.parse(""))
    }
}
