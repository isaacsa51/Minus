package com.serranoie.app.minus.presentation.ui.history.sections

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

class HistoryFilterAmountTest {
    private val fifty = BigDecimal("50")

    @Test
    fun `a valid edit replaces the bound and a decimal comma is read`() {
        assertThat(filterAmountEdit("120", fifty)).isEqualTo(BigDecimal("120"))
        assertThat(filterAmountEdit("12,5", fifty)).isEqualTo(BigDecimal("12.5"))
    }

    @Test
    fun `clearing the field is the only way to drop the bound`() {
        assertThat(filterAmountEdit("", fifty)).isNull()
    }

    @Test
    fun `malformed text keeps the last valid bound`() {
        assertThat(filterAmountEdit("1.2.3", fifty)).isEqualTo(fifty)
        assertThat(filterAmountEdit(".", fifty)).isEqualTo(fifty)
        assertThat(filterAmountEdit("5,0,0", null)).isNull()
    }
}
