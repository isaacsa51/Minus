package com.serranoie.app.minus.domain.receipt

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ReceiptParserLanguagesTest {

    private fun total(text: String) = ReceiptParser.parse(text.trimIndent())?.total

    private fun merchant(text: String) = ReceiptParser.parse(text.trimIndent())?.merchant

    @Test
    fun german() {
        val text = """
            REWE Markt
            Zwischensumme 20,00
            Summe EUR 23,45
            Gegeben Bar 50,00
            Rückgeld 26,55
        """
        assertEquals(BigDecimal("23.45"), total(text))
        assertEquals("REWE Markt", merchant(text))
    }

    @Test
    fun french() {
        assertEquals(
            BigDecimal("21.84"),
            total(
                """
                CARREFOUR
                Sous-total 18,20
                Total TTC 21,84
                Espèces 30,00
                Rendu monnaie 8,16
                """,
            ),
        )
    }

    @Test
    fun italian() {
        assertEquals(
            BigDecimal("12.20"),
            total(
                """
                COOP Italia
                Subtotale 10,00
                Totale complessivo 12,20
                Contanti 20,00
                Resto 7,80
                """,
            ),
        )
    }

    @Test
    fun portuguese() {
        assertEquals(
            BigDecimal("9.96"),
            total(
                """
                PINGO DOCE
                Subtotal 8,10
                Total a pagar 9,96
                Dinheiro 10,00
                Troco 0,04
                """,
            ),
        )
    }

    @Test
    fun russian() {
        val text = """
            ПЯТЁРОЧКА
            Подытог 450,00
            ИТОГО 512,30
            Наличные 1000,00
            Сдача 487,70
        """
        assertEquals(BigDecimal("512.30"), total(text))
        assertEquals("ПЯТЁРОЧКА", merchant(text))
    }

    @Test
    fun bulgarian() {
        assertEquals(
            BigDecimal("18.00"),
            total(
                """
                ЛИДЛ България
                Междинна сума 15,00
                Обща сума 18,00
                В брой 20,00
                Ресто 2,00
                """,
            ),
        )
    }

    @Test
    fun greek_uppercase_without_accents() {
        assertEquals(
            BigDecimal("11.78"),
            total(
                """
                ΣΚΛΑΒΕΝΙΤΗΣ
                ΜΕΡΙΚΟ ΣΥΝΟΛΟ 9,50
                ΣΥΝΟΛΟ 11,78
                ΜΕΤΡΗΤΑ 20,00
                ΡΕΣΤΑ 8,22
                """,
            ),
        )
    }

    @Test
    fun persian_with_persian_digits() {
        val text = """
            فروشگاه رفاه
            جمع کل ۱۲۵٬۰۰۰ ریال
            نقد ۲۰۰٬۰۰۰
            باقیمانده ۷۵٬۰۰۰
        """
        assertEquals(BigDecimal("125000"), total(text))
        assertEquals("فروشگاه رفاه", merchant(text))
    }

    @Test
    fun persian_written_with_arabic_letters_and_digits() {
        assertEquals(BigDecimal("123"), total("فروشگاه\nجمع كل ١٢٣"))
    }

    @Test
    fun hindi() {
        val text = """
            बिग बाज़ार
            उप योग 400.00
            कुल राशि 472.00
            नकद 500.00
            वापसी 28.00
        """
        assertEquals(BigDecimal("472.00"), total(text))
        assertEquals("बिग बाज़ार", merchant(text))
    }

    @Test
    fun japanese() {
        assertEquals(
            BigDecimal("1100"),
            total(
                """
                ローソン
                小計 ¥1,000
                消費税 ¥100
                合計 ¥1,100
                お預り ¥2,000
                お釣り ¥900
                """,
            ),
        )
    }

    @Test
    fun japanese_full_width_digits_and_tax_included_total() {
        assertEquals(BigDecimal("1100"), total("ファミリーマート\n合計(税込) ￥１，１００"))
    }

    @Test
    fun korean() {
        assertEquals(
            BigDecimal("10000"),
            total(
                """
                이마트
                소계 9,091
                부가세 909
                합계 10,000
                현금 20,000
                거스름돈 10,000
                """,
            ),
        )
    }

    @Test
    fun chinese_with_two_character_store_name() {
        val text = """
            全家
            小计 45.00
            合计 50.00
            现金 100.00
            找零 50.00
        """
        assertEquals(BigDecimal("50.00"), total(text))
        assertEquals("全家", merchant(text))
    }

    @Test
    fun tax_word_on_the_total_line_does_not_hide_it() {
        assertEquals(BigDecimal("116.00"), total("Tienda\nTOTAL (IVA incluido) 116,00\nIVA 16,00"))
        assertEquals(BigDecimal("116.00"), total("Shop\nGrand Total incl. VAT 116.00\nVAT 16.00"))
    }

    @Test
    fun cash_tendered_on_a_total_line_is_still_ignored() {
        assertEquals(BigDecimal("16.50"), total("Shop\nTotal 16.50\nTotal cash 20.00"))
    }

    @Test
    fun fallback_prefers_amounts_next_to_a_currency() {
        assertEquals(BigDecimal("4.50"), total("Store\nRef 9876.50\nCoffee €4.50"))
    }
}
