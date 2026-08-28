package com.serranoie.app.minus.presentation.ui.budget

import com.serranoie.app.minus.domain.calculator.evaluateExpression
import javax.inject.Inject

class BudgetExpressionEvaluator @Inject constructor() {

    fun evaluate(input: String): String? = evaluateExpression(input)
}
