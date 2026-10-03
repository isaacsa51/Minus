package com.serranoie.app.minus.presentation.ui.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import logcat.logcat

internal const val TUTORIAL_LOG_TAG = "Tutorial"

@Stable
class TutorialBoxState {
    internal val targetBounds: SnapshotStateMap<Int, Rect> = mutableStateMapOf()

    internal val registrationOrder: SnapshotStateList<Int> = mutableStateListOf()

    internal val currentIndexState = mutableStateOf(-1)

    var isCompleted: Boolean by mutableStateOf(false)
        internal set

    internal val visitedIndices: SnapshotStateSet<Int> = mutableStateSetOf()

    internal val measuredIndices: SnapshotStateSet<Int> = mutableStateSetOf()

    internal val pendingRewindCandidates: SnapshotStateSet<Int> = mutableStateSetOf()

    internal val gatedJumpedIndices: SnapshotStateSet<Int> = mutableStateSetOf()

    internal val targetTouchSignal = mutableIntStateOf(0)

    internal var isActive: Boolean by mutableStateOf(false)

    fun notifyTargetTouched(index: Int) {
        if (!isActive || isCompleted) return
        if (currentIndexState.value != index) return
        targetTouchSignal.intValue++
    }

    val currentBounds: Rect?
        get() = targetBounds[currentIndexState.value]

    fun advance() {
        if (isCompleted) return
        val order = registrationOrder
        if (order.isEmpty()) return

        if (currentIndexState.value in order) {
            visitedIndices.add(currentIndexState.value)
        }

        val currentPos = order.indexOf(currentIndexState.value).coerceAtLeast(0)
        var nextPos = currentPos + 1

        while (nextPos < order.size && (order[nextPos] !in targetBounds || order[nextPos] in visitedIndices)) {
            nextPos++
        }
        if (nextPos >= order.size) {
            isCompleted = true
            logcat(TUTORIAL_LOG_TAG) { "Walk finished" }
        } else {
            currentIndexState.value = order[nextPos]
        }
    }

    fun skipAll() {
        isCompleted = true
        visitedIndices.addAll(registrationOrder)
        logcat(TUTORIAL_LOG_TAG) { "Skipped by the user" }
    }

    fun resetForReplay() {
        isCompleted = false
        currentIndexState.value = if (registrationOrder.isEmpty()) -1 else registrationOrder.first()
        visitedIndices.clear()
        measuredIndices.clear()
        pendingRewindCandidates.clear()
        gatedJumpedIndices.clear()
    }
}

private val DefaultWalkOrder: List<Int> = listOf(0, 1, 2, 3, 4, 8, 5, 6, 7)

internal val VirtualIndices: Set<Int> = setOf(6)

internal val GatedIndices: Set<Int> = setOf(3, 4, 7, 8)

@Composable
fun rememberTutorialBoxState(
    order: List<Int> = DefaultWalkOrder,
    virtual: Set<Int> = VirtualIndices,
): TutorialBoxState {
    val state = remember { TutorialBoxState() }

    LaunchedEffect(order) {
        val currentOrder = state.registrationOrder.toList()
        if (currentOrder != order) {
            state.registrationOrder.clear()
            state.registrationOrder.addAll(order)

            // If we were at -1 or our current index is no longer in the order, reset to first
            if (state.currentIndexState.value == -1 || state.currentIndexState.value !in order) {
                state.currentIndexState.value = order.firstOrNull() ?: -1
            }
        }
    }

    LaunchedEffect(virtual) {
        virtual.forEach { idx ->
            if (idx !in state.targetBounds) {
                state.targetBounds[idx] = Rect.Zero
            }
        }
    }

    return state
}

fun Modifier.markForTutorial(
    state: TutorialBoxState,
    index: Int,
): Modifier = this
    .pointerInput(state, index) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press) state.notifyTargetTouched(index)
            }
        }
    }
    .composed {
    DisposableEffect(index) {
        onDispose {
            state.targetBounds.remove(index)
            state.measuredIndices.remove(index)
        }
    }
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        if (bounds.isFinite && !bounds.isEmpty) {
            val wasRegistered = index in state.registrationOrder
            if (!wasRegistered) {
                state.registrationOrder.add(index)
            }

            val isBecomingVisible = index !in state.targetBounds || state.targetBounds[index]?.isEmpty == true
            state.measuredIndices.add(index)
            state.targetBounds[index] = bounds

            if (state.currentIndexState.value == -1) {
                state.currentIndexState.value = state.registrationOrder.firstOrNull() ?: -1
            }

            if (isBecomingVisible && index !in state.visitedIndices && index !in state.gatedJumpedIndices) {
                val targetPos = state.registrationOrder.indexOf(index)
                val currentPos =
                    state.registrationOrder.indexOf(state.currentIndexState.value).coerceAtLeast(0)

                if (state.isCompleted) {
                    if (index in GatedIndices) state.gatedJumpedIndices.add(index)
                    state.pendingRewindCandidates.add(index)
                } else if (currentPos > targetPos) {
                    if (index in GatedIndices) state.gatedJumpedIndices.add(index)
                    state.pendingRewindCandidates.add(index)
                } else if (
                    index in GatedIndices &&
                    currentPos < targetPos &&
                    state.registrationOrder[currentPos] !in GatedIndices &&
                    state.visitedIndices.isEmpty()
                ) {
                    state.gatedJumpedIndices.add(index)
                    state.pendingRewindCandidates.add(index)
                }
            }
        } else {
            state.targetBounds.remove(index)
        }
    }
}
