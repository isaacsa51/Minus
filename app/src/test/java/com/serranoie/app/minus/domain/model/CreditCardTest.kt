package com.serranoie.app.minus.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CreditCardTest {

    @Test
    fun `test calculatePaymentDueDate - normal case`() {
        val card = CreditCard(cutoffDay = 15, gracePeriodDays = 20)
        val today = LocalDate.of(2024, 7, 10)
        val dueDate = calculatePaymentDueDate(card, today)
        
        assertEquals(LocalDate.of(2024, 7, 5), dueDate)
    }

    @Test
    fun `test calculatePaymentDueDate - saturday adjustment`() {
        val card = CreditCard(cutoffDay = 5, gracePeriodDays = 20)
        val today = LocalDate.of(2024, 5, 26)
        val dueDate = calculatePaymentDueDate(card, today)
        
        assertEquals(LocalDate.of(2024, 5, 27), dueDate)
    }

    @Test
    fun `test calculatePaymentDueDate - sunday adjustment`() {
        val card = CreditCard(cutoffDay = 6, gracePeriodDays = 20)
        val today = LocalDate.of(2024, 5, 28)
        val dueDate = calculatePaymentDueDate(card, today)
        
        assertEquals(LocalDate.of(2024, 5, 27), dueDate)
    }

    @Test
    fun `test calculatePaymentDueDate - transition to next month`() {
        val card = CreditCard(cutoffDay = 15, gracePeriodDays = 20)
        val today = LocalDate.of(2024, 6, 10)
        val dueDate = calculatePaymentDueDate(card, today)
        
        assertEquals(LocalDate.of(2024, 6, 4), dueDate)
    }
}
