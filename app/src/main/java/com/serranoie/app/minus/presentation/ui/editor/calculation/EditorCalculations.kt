package com.serranoie.app.minus.presentation.ui.editor.calculation

import com.serranoie.app.minus.domain.calculator.evaluateExpression

/**
 * Evaluate a free-form arithmetic expression entered by the user in the
 * editor's amount slot.
 *
 * Thin wrapper around [evaluateExpression] — see that function for supported
 * syntax, operator precedence and the display-normalization rules. Kept as a
 * separate entry point so the editor call sites don't have to reach into the
 * domain layer directly.
 */
internal fun evaluateCalculation(input: String): String? = evaluateExpression(input)
