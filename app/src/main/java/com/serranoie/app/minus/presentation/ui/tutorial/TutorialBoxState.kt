package com.serranoie.app.minus.presentation.ui.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
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
import logcat.logcat

internal const val TUTORIAL_LOG_TAG = "IMPL:TUTORIAL"

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

    val currentBounds: Rect?
        get() = targetBounds[currentIndexState.value]

    fun advance() {
        if (isCompleted) {
            logcat(TUTORIAL_LOG_TAG) { "advance: ignored, already completed" }
            return
        }
        val order = registrationOrder
        if (order.isEmpty()) {
            logcat(TUTORIAL_LOG_TAG) { "advance: ignored, registrationOrder is empty" }
            return
        }

        if (currentIndexState.value in order) {
            visitedIndices.add(currentIndexState.value)
        }

        val currentPos = order.indexOf(currentIndexState.value).coerceAtLeast(0)
        var nextPos = currentPos + 1

        while (nextPos < order.size && (order[nextPos] !in targetBounds || order[nextPos] in visitedIndices)) {
            val skipIndex = order[nextPos]
            val reason = if (skipIndex !in targetBounds) "no bounds yet" else "already shown"
            logcat(TUTORIAL_LOG_TAG) {
                "advance: skipping position=$nextPos index=$skipIndex ($reason)"
            }
            nextPos++
        }
        logcat(TUTORIAL_LOG_TAG) {
            "advance: from position=$currentPos (index=${order.getOrNull(currentPos)}) " + "→ next position=$nextPos (index=${
                order.getOrNull(
                    nextPos
                )
            }) " + "visitedIndices=$visitedIndices " + "registrationOrder=$order"
        }
        if (nextPos >= order.size) {
            isCompleted = true
            logcat(TUTORIAL_LOG_TAG) { "advance: walk finished, isCompleted=true" }
        } else {
            currentIndexState.value = order[nextPos]
        }
    }

    fun resetForReplay() {
        isCompleted = false
        currentIndexState.value =
            if (registrationOrder.isEmpty()) -1 else registrationOrder.first()
        visitedIndices.clear()
        measuredIndices.clear()
        pendingRewindCandidates.clear()
    }
}

private val DefaultWalkOrder: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6)

internal val VirtualIndices: Set<Int> = setOf(6)

internal val GatedIndices: Set<Int> = setOf(3, 4)

@Composable
fun rememberTutorialBoxState(): TutorialBoxState = remember {
    TutorialBoxState().also {
        it.registrationOrder.addAll(DefaultWalkOrder)
        VirtualIndices.forEach { idx -> it.targetBounds[idx] = Rect.Zero }
    }
}

fun Modifier.markForTutorial(
    state: TutorialBoxState,
    index: Int,
): Modifier = this.composed {
    DisposableEffect(index) {
        onDispose {
            state.targetBounds.remove(index)
            state.measuredIndices.remove(index)
            logcat(TUTORIAL_LOG_TAG) {
                "markForTutorial: index=$index DISPOSED, cleared bounds " + "currentIndex=${state.currentIndexState.value} " + "isCompleted=${state.isCompleted}"
            }
        }
    }
    onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        if (bounds.isFinite && !bounds.isEmpty) {
            val wasRegistered = index in state.registrationOrder
            if (!wasRegistered) {
                state.registrationOrder.add(index)
            }

            val isFirstMeasurement = index !in state.measuredIndices
            state.measuredIndices.add(index)
            state.targetBounds[index] = bounds

            if (state.currentIndexState.value == -1) {
                state.currentIndexState.value = state.registrationOrder.first()
            }

            if (isFirstMeasurement && index !in state.visitedIndices) {
                val targetPos = state.registrationOrder.indexOf(index)
                val currentPos =
                    state.registrationOrder.indexOf(state.currentIndexState.value).coerceAtLeast(0)
                if (state.isCompleted) {
                    state.pendingRewindCandidates.add(index)
                } else if (currentPos > targetPos) {
                    state.pendingRewindCandidates.add(index)
                } else if (index in GatedIndices && currentPos < targetPos && state.registrationOrder[currentPos] !in GatedIndices) {
                    state.pendingRewindCandidates.add(index)
                }
            }
        } else {
            state.targetBounds.remove(index)
        }
    }
}
