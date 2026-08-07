package com.serranoie.app.minus.presentation.ui.budget.controller

import com.serranoie.app.minus.presentation.ui.budget.BudgetExpressionEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the numpad input string and the calculation-mode toggle. Pure business
 * logic — no Android dependencies, no coroutine launches, no side effects
 * beyond its own [input] / [isCalculation] state.
 *
 * The controller's [process] method takes the current `isCalculation` flag
 * as a read-only input (so it can clear it on backspace) and returns a
 * [NumpadChange] describing the state diff for the parent ViewModel to
 * apply. This keeps the controller a pure state machine that can be
 * exhaustively unit-tested without any Android or coroutine scaffolding.
 */
class NumpadController(
    private val expressionEvaluator: BudgetExpressionEvaluator = BudgetExpressionEvaluator(),
) {
    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _isCalculation = MutableStateFlow(false)
    val isCalculation: StateFlow<Boolean> = _isCalculation.asStateFlow()

    private val _dragProgress = MutableStateFlow(0f)
    val dragProgress: StateFlow<Float> = _dragProgress.asStateFlow()

    /**
     * The result of processing one numpad intent. The parent ViewModel
     * applies these as patches to its own UI state.
     */
    sealed interface NumpadChange {
        /** The numpad input string changed. */
        data class InputChanged(val newInput: String) : NumpadChange
        /** The calculation-mode flag changed. */
        data class CalculationModeChanged(val enabled: Boolean) : NumpadChange
        /** The drag progress changed (e.g. for the editor sheet expand). */
        data class DragProgressChanged(val progress: Float) : NumpadChange
    }

    /**
     * Process one numpad intent. The `currentIsCalculation` is the parent's
     * current value of the calculation flag (used to decide whether to clear
     * it on backspace / reset). The returned [NumpadChange]s are the
     * authoritative state diff for this call — the parent MUST apply them.
     */
    fun process(
        intent: NumpadIntent,
        currentIsCalculation: Boolean,
    ): List<NumpadChange> = when (intent) {
        is NumpadIntent.NumberTapped -> listOf(appendDigit(intent.digit))
        is NumpadIntent.DotTapped -> handleDot()
        is NumpadIntent.BackspaceTapped -> handleBackspace(currentIsCalculation)
        NumpadIntent.ApplyTapped -> emptyList() // Side effect handled by the parent VM
        is NumpadIntent.OperatorTapped -> handleOperator(intent.operator, currentIsCalculation)
        is NumpadIntent.EqualsTapped -> handleEquals(currentIsCalculation)
        is NumpadIntent.ResetInputTapped -> handleReset(currentIsCalculation)
        is NumpadIntent.SetCalculationMode -> setCalculationMode(intent.enabled)
        is NumpadIntent.SetDragProgress -> setDragProgress(intent.progress)
        is NumpadIntent.TriggerTestNotifications -> emptyList()
    }

    private fun appendDigit(digit: String): NumpadChange {
        val updated = _input.value + digit
        _input.value = updated
        return NumpadChange.InputChanged(updated)
    }

    private fun handleDot(): List<NumpadChange> {
        val current = _input.value
        val lastChar = current.lastOrNull()
        val isOperator = { c: Char? -> c != null && c in "+-×÷" }

        val updated = when {
            current.isEmpty() || isOperator(lastChar) -> current + "0."
            else -> {
                val lastOpIndex = current.indexOfLast { it in "+-×÷" }
                val segment = current.substring(lastOpIndex + 1)
                if (segment.contains(".")) return emptyList() // ignore extra dots in segment
                "$current."
            }
        }
        _input.value = updated
        return listOf(NumpadChange.InputChanged(updated))
    }

    private fun handleBackspace(currentIsCalculation: Boolean): List<NumpadChange> {
        val updated = _input.value.dropLast(1)
        _input.value = updated
        return buildList {
            add(NumpadChange.InputChanged(updated))
            if (updated.isEmpty() && currentIsCalculation) {
                add(NumpadChange.CalculationModeChanged(false))
            }
        }
    }

    private fun handleOperator(operator: Char, currentIsCalculation: Boolean): List<NumpadChange> {
        val current = _input.value
        val isUnaryPossible = operator == '+' || operator == '-'
        
        if (current.isEmpty() && !isUnaryPossible) return emptyList()
        
        val lastChar = current.lastOrNull()
        if (lastChar != null && (lastChar in "+-×÷" || lastChar == '.')) return emptyList()

        val updated = "$current$operator"
        _input.value = updated
        return buildList {
            add(NumpadChange.InputChanged(updated))
            if (!currentIsCalculation) {
                add(NumpadChange.CalculationModeChanged(true))
            }
        }
    }

    private fun handleEquals(currentIsCalculation: Boolean): List<NumpadChange> {
        val current = _input.value
        if (current.isEmpty()) return emptyList()
        val result = expressionEvaluator.evaluate(current) ?: return emptyList()
        _input.value = result
        return buildList {
            add(NumpadChange.InputChanged(result))
            if (!currentIsCalculation) {
                add(NumpadChange.CalculationModeChanged(true))
            }
        }
    }

    private fun handleReset(currentIsCalculation: Boolean): List<NumpadChange> {
        _input.value = ""
        return buildList {
            add(NumpadChange.InputChanged(""))
            if (currentIsCalculation) {
                add(NumpadChange.CalculationModeChanged(false))
            }
        }
    }

    private fun setCalculationMode(enabled: Boolean): List<NumpadChange> {
        _isCalculation.value = enabled
        _dragProgress.value = 0f
        return listOf(
            NumpadChange.CalculationModeChanged(enabled),
            NumpadChange.DragProgressChanged(0f),
        )
    }

    private fun setDragProgress(progress: Float): List<NumpadChange> {
        _dragProgress.value = progress
        return listOf(NumpadChange.DragProgressChanged(progress))
    }

    /**
     * Reset the input to an empty string. Called by the parent ViewModel
     * after a successful apply / restore action.
     */
    fun clearInput(): NumpadChange {
        _input.value = ""
        return NumpadChange.InputChanged("")
    }
}

/**
 * The set of numpad intents the controller understands. Mirrors the
 * production [com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent]
 * but kept as a controller-local sealed interface so the controller has no
 * dependency on the MVI intent types.
 */
sealed interface NumpadIntent {
    data class NumberTapped(val digit: String) : NumpadIntent
    data object DotTapped : NumpadIntent
    data object BackspaceTapped : NumpadIntent
    data object ApplyTapped : NumpadIntent
    data class OperatorTapped(val operator: Char) : NumpadIntent
    data object EqualsTapped : NumpadIntent
    data object ResetInputTapped : NumpadIntent
    data class SetCalculationMode(val enabled: Boolean) : NumpadIntent
    data class SetDragProgress(val progress: Float) : NumpadIntent
    data object TriggerTestNotifications : NumpadIntent
}
