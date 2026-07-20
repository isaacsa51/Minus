package com.serranoie.app.minus.presentation.ui.tutorial

import androidx.compose.ui.geometry.Rect
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class TutorialBoxStateTest {

    private lateinit var state: TutorialBoxState

    @Before
    fun setUp() {
        state = TutorialBoxState()
    }

    @Test
    fun `initial state is not completed and index is -1`() {
        assertThat(state.isCompleted).isFalse()
        assertThat(state.currentIndexState.value).isEqualTo(-1)
    }

    @Test
    fun `advance with empty order does nothing`() {
        state.advance()
        assertThat(state.isCompleted).isFalse()
        assertThat(state.currentIndexState.value).isEqualTo(-1)
    }

    @Test
    fun `advance skips indices with no bounds`() {
        state.registrationOrder.addAll(listOf(0, 1, 2))
        state.targetBounds[0] = Rect(0f, 0f, 10f, 10f)
        state.targetBounds[2] = Rect(20f, 20f, 30f, 30f)

        state.currentIndexState.value = 0
        state.advance()

        assertThat(state.currentIndexState.value).isEqualTo(2)
        assertThat(state.isCompleted).isFalse()
    }

    @Test
    fun `advance skips already visited indices`() {
        state.registrationOrder.addAll(listOf(0, 1, 2))
        state.targetBounds[0] = Rect(0f, 0f, 10f, 10f)
        state.targetBounds[1] = Rect(10f, 10f, 20f, 20f)
        state.targetBounds[2] = Rect(20f, 20f, 30f, 30f)
        
        state.visitedIndices.add(1)

        state.currentIndexState.value = 0
        state.advance()

        assertThat(state.currentIndexState.value).isEqualTo(2)
    }

    @Test
    fun `advance marks as completed when no more steps available`() {
        state.registrationOrder.addAll(listOf(0, 1))
        state.targetBounds[0] = Rect(0f, 0f, 10f, 10f)
        state.targetBounds[1] = Rect(10f, 10f, 20f, 20f)

        state.currentIndexState.value = 1
        state.advance()

        assertThat(state.isCompleted).isTrue()
    }

    @Test
    fun `resetForReplay clears state`() {
        state.registrationOrder.addAll(listOf(0, 1))
        state.targetBounds[0] = Rect(0f, 0f, 10f, 10f)
        state.targetBounds[1] = Rect(10f, 10f, 20f, 20f)
        state.visitedIndices.add(0)
        state.isCompleted = true

        state.resetForReplay()

        assertThat(state.isCompleted).isFalse()
        assertThat(state.visitedIndices).isEmpty()
        assertThat(state.currentIndexState.value).isEqualTo(0)
    }
}
