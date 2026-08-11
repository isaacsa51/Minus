package com.serranoie.app.minus.presentation.util

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent

fun handleHardwareNumpadKeyEvent(
    keyEvent: KeyEvent,
    onIntent: (BudgetNumpadIntent) -> Unit
): Boolean {
    if (keyEvent.type != KeyEventType.KeyUp) return false

    return when (keyEvent.key) {
        Key.Zero, Key.NumPad0 -> {
            onIntent(BudgetNumpadIntent.NumberTapped("0")); true
        }

        Key.One, Key.NumPad1 -> {
            onIntent(BudgetNumpadIntent.NumberTapped("1")); true
        }

        Key.Two, Key.NumPad2 -> {
            onIntent(BudgetNumpadIntent.NumberTapped("2")); true
        }

        Key.Three, Key.NumPad3 -> {
            onIntent(BudgetNumpadIntent.NumberTapped("3")); true
        }

        Key.Four, Key.NumPad4 -> {
            onIntent(BudgetNumpadIntent.NumberTapped("4")); true
        }

        Key.Five, Key.NumPad5 -> {
            onIntent(BudgetNumpadIntent.NumberTapped("5")); true
        }

        Key.Six, Key.NumPad6 -> {
            onIntent(BudgetNumpadIntent.NumberTapped("6")); true
        }

        Key.Seven, Key.NumPad7 -> {
            onIntent(BudgetNumpadIntent.NumberTapped("7")); true
        }

        Key.Eight -> {
            if (keyEvent.isShiftPressed) {
                onIntent(BudgetNumpadIntent.OperatorTapped('×')); true
            } else {
                onIntent(BudgetNumpadIntent.NumberTapped("8")); true
            }
        }
        Key.NumPad8 -> {
            onIntent(BudgetNumpadIntent.NumberTapped("8")); true
        }

        Key.Nine, Key.NumPad9 -> {
            onIntent(BudgetNumpadIntent.NumberTapped("9")); true
        }

        Key.Period, Key.NumPadDot -> {
            onIntent(BudgetNumpadIntent.DotTapped); true
        }

        Key.Backspace -> {
            onIntent(BudgetNumpadIntent.BackspaceTapped); true
        }

        Key.Enter, Key.NumPadEnter -> {
            onIntent(BudgetNumpadIntent.ApplyTapped); true
        }

        Key.Plus, Key.NumPadAdd -> {
            onIntent(BudgetNumpadIntent.OperatorTapped('+')); true
        }

        Key.Minus, Key.NumPadSubtract -> {
            onIntent(BudgetNumpadIntent.OperatorTapped('-')); true
        }

        Key.Multiply, Key.NumPadMultiply -> {
            onIntent(BudgetNumpadIntent.OperatorTapped('×')); true
        }

        Key.Slash, Key.NumPadDivide -> {
            onIntent(BudgetNumpadIntent.OperatorTapped('÷')); true
        }

        Key.Equals, Key.NumPadEquals -> {
            if (keyEvent.isShiftPressed) {
                onIntent(BudgetNumpadIntent.OperatorTapped('+')); true
            } else {
                onIntent(BudgetNumpadIntent.EqualsTapped); true
            }
        }

        Key.Escape -> {
            onIntent(BudgetNumpadIntent.ResetInputTapped); true
        }

        else -> false
    }
}
