package com.serranoie.app.minus.domain.calculator

import com.serranoie.app.minus.presentation.ui.budget.BudgetExpressionEvaluator
import com.serranoie.app.minus.presentation.ui.editor.calculation.evaluateCalculation
import org.junit.Assert.assertEquals
import org.junit.Test

class EvaluatorTest {

    @Test
    fun testBudgetExpressionEvaluator() {
        val evaluator = BudgetExpressionEvaluator()
        
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
}
