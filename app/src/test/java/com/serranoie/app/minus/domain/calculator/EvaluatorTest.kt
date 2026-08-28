package com.serranoie.app.minus.domain.calculator

import com.serranoie.app.minus.presentation.ui.budget.BudgetExpressionEvaluator
import com.serranoie.app.minus.presentation.ui.editor.calculation.evaluateCalculation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EvaluatorTest {

    private val evaluator = BudgetExpressionEvaluator()

    @Test
    fun testBudgetExpressionEvaluator() {
        assertEquals("100", evaluator.evaluate("+100"))
        assertEquals("-100", evaluator.evaluate("-100"))
        assertEquals("150", evaluator.evaluate("+100+50"))
        assertEquals("-150", evaluator.evaluate("-100-50"))
        assertEquals("50", evaluator.evaluate("+100-50"))
        assertEquals("-50", evaluator.evaluate("-100+50"))

        assertEquals("-150.5", evaluator.evaluate("-100.5-50"))
        assertEquals("150.5", evaluator.evaluate("+100.5+50"))
    }

    @Test
    fun testEditorCalculations() {
        assertEquals("100", evaluateCalculation("+100"))
        assertEquals("-100", evaluateCalculation("-100"))
        assertEquals("150", evaluateCalculation("+100+50"))
        assertEquals("-150", evaluateCalculation("-100-50"))
        assertEquals("50", evaluateCalculation("+100-50"))
        assertEquals("-50", evaluateCalculation("-100+50"))
    }

    @Test
    fun `multiplication and division bind tighter than addition and subtraction`() {
        assertEquals("14", evaluateExpression("2+3*4"))
        assertEquals("14", evaluateExpression("2+3×4"))
        assertEquals("10", evaluateExpression("2*3+4"))
        assertEquals("4", evaluateExpression("10-2*3"))
        assertEquals("26", evaluateExpression("2*3+4*5"))
        assertEquals("4.5", evaluateExpression("2+10/4"))
        assertEquals("4.5", evaluateExpression("2+10÷4"))
        assertEquals("1", evaluateExpression("2-10/10"))
    }

    @Test
    fun `equal precedence operators evaluate left to right`() {
        assertEquals("1", evaluateExpression("10-5-4"))
        assertEquals("5", evaluateExpression("100/10/2"))
        assertEquals("24", evaluateExpression("2*3*4"))
        assertEquals("2", evaluateExpression("2+3-4+1"))
    }

    @Test
    fun `leading unary sign only negates the first operand`() {
        assertEquals("-150", evaluateExpression("-100-50"))
        assertEquals("-5", evaluateExpression("-2*3+1"))
        assertEquals("100", evaluateExpression("+100"))
    }

    @Test
    fun `malformed expressions return null`() {
        assertNull(evaluateExpression(""))
        assertNull(evaluateExpression("   "))
        assertNull(evaluateExpression("2+"))
        assertNull(evaluateExpression("2**3"))
        assertNull(evaluateExpression("2/0"))
        assertNull(evaluateExpression("10+5/0"))
    }
}
