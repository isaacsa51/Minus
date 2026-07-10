package com.serranoie.app.minus.presentation.ui.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import logcat.logcat

internal const val TUTORIAL_LOG_TAG = "IMPL:TUTORIAL"

/**
 * Holds the runtime state for a first-launch tutorial session.
 *
 * Each [markForTutorial] call site records its [index] and on-screen bounds into this
 * state. [TutorialBox] reads the state to know which target to highlight, and advances
 * [currentIndex] on click. After the last registered target is consumed, [isCompleted]
 * flips to `true` and the parent typically calls `onTutorialCompleted`.
 *
 * @see rememberTutorialBoxState
 * @see markForTutorial
 * @see TutorialBox
 */
@Stable
class TutorialBoxState {
    /**
     * Backing map of target index → on-screen bounds, in window coordinates.
     * Populated by [markForTutorial] via [onGloballyPositioned].
     */
    internal val targetBounds: SnapshotStateMap<Int, Rect> = androidx.compose.runtime.mutableStateMapOf()

    /**
     * Order in which indices were registered. The walk order for the coachmark is the
     * registration order, not the natural numeric order — this way callers can pass
     * arbitrary `index` ints (including negative ones or large ones) without worrying
     * about sorting.
     */
    internal val registrationOrder: SnapshotStateList<Int> = mutableStateListOf()

    /** Index of the target currently being highlighted. `-1` before any advance. */
    internal val currentIndexState = mutableStateOf(-1)

    /** `true` once the user has tapped through every registered target. */
    var isCompleted: Boolean by mutableStateOf(false)
        internal set

    /**
     * Indices the overlay has actually rendered at least once. Used by the rewind
     * logic in [markForTutorial] to avoid re-showing a gated tutorial every time
     * the user enters edit mode after the walk has already been completed once.
     */
    internal val visitedIndices: androidx.compose.runtime.snapshots.SnapshotStateSet<Int> =
        androidx.compose.runtime.mutableStateSetOf()

    /**
     * Indices that have been measured with non-empty bounds at least once. Tracked
     * separately from [registrationOrder] (which is pre-populated with the full walk
     * order) so the rewind logic in [markForTutorial] can distinguish "this target
     * is in the walk plan" from "this target has actually been laid out". Without
     * this distinction, the pre-populated entries would look like first-time
     * registrations every frame and the rewind would never fire.
     */
    internal val measuredIndices: androidx.compose.runtime.snapshots.SnapshotStateSet<Int> =
        androidx.compose.runtime.mutableStateSetOf()

    /** Returns the bounds of the currently highlighted target, or `null` if none. */
    val currentBounds: Rect?
        get() = targetBounds[currentIndexState.value]

    /** Total number of distinct targets that have registered themselves. */
    val totalTargets: Int
        get() = registrationOrder.size

    /**
     * Returns the bounds for the target at [index], or `null` if it hasn't been laid
     * out yet (composable that called [markForTutorial] hasn't been measured).
     */
    fun boundsFor(index: Int): Rect? = targetBounds[index]

    /**
     * Advance to the next registered target. If this is the last target, flips
     * [isCompleted] to `true`. No-op when already completed or no targets registered.
     */
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
        // Mark the current target as visited — the user is moving past it now.
        if (currentIndexState.value in order) {
            visitedIndices.add(currentIndexState.value)
        }
        // currentIndexState stores the index value (e.g. 0, 1, 4), not the position
        // in `order`. We need the position to find the next walk step.
        val currentPos = order.indexOf(currentIndexState.value).coerceAtLeast(0)
        var nextPos = currentPos + 1
        // Skip indices whose target hasn't measured bounds yet (e.g. category tag
        // when editor is in IDLE mode, recurrent toggle when no spend is being
        // edited). Without this, the walk would advance to an unrenderable step
        // and the user would be stuck because the overlay wouldn't show.
        while (nextPos < order.size && order[nextPos] !in targetBounds) {
            logcat(TUTORIAL_LOG_TAG) {
                "advance: skipping position=$nextPos index=${order[nextPos]} (no bounds yet)"
            }
            nextPos++
        }
        logcat(TUTORIAL_LOG_TAG) {
            "advance: from position=$currentPos (index=${order.getOrNull(currentPos)}) " +
                "→ next position=$nextPos (index=${order.getOrNull(nextPos)}) " +
                "visitedIndices=$visitedIndices " +
                "registrationOrder=$order"
        }
        if (nextPos >= order.size) {
            isCompleted = true
            logcat(TUTORIAL_LOG_TAG) { "advance: walk finished, isCompleted=true" }
        } else {
            currentIndexState.value = order[nextPos]
        }
    }

    /**
     * Move the highlight back to the first registered target. Used when the user
     * re-enters the screen and we want the walk to start over.
     */
    internal fun reset() {
        isCompleted = false
        currentIndexState.value = if (registrationOrder.isEmpty()) -1 else registrationOrder.first()
    }
}

/**
 * The intended walk order across the whole tutorial, regardless of when each
 * target actually registers. Index 0 = Numpad, 1 = category/comment, 2 = BudgetPill,
 * 3 = Settings, 4 = Recurrent toggle. Indices 1 and 4 are gated on the editor being
 * in EDITING mode, so they only get bounds once the user taps a number; the walk
 * should still visit them in this order when they become available.
 */
private val DefaultWalkOrder: List<Int> = listOf(0, 1, 4, 2, 3)

/**
 * Remembers a [TutorialBoxState] for the lifetime of the current composition. Pass the
 * returned state into [TutorialBox] and any [markForTutorial] calls in the content.
 *
 * The returned state has its [TutorialBoxState.registrationOrder] pre-populated with
 * [DefaultWalkOrder] so the walk sequence is fixed even before every target has
 * measured. Targets that haven't been laid out yet (e.g. category tag when the
 * editor is in IDLE mode) sit in the walk but have no bounds; [TutorialBoxState.advance]
 * skips them so the user is never stuck on an unrenderable step.
 */
@Composable
fun rememberTutorialBoxState(): TutorialBoxState = remember {
    TutorialBoxState().also { it.registrationOrder.addAll(DefaultWalkOrder) }
}

/**
 * Modifier that registers the element's on-screen bounds as a coachmark target for the
 * given [index]. Multiple elements can register themselves with the same [state] using
 * distinct [index] values; [TutorialBox] will walk through them in registration order.
 *
 * Safe to use inside deeply nested composables — only the actively composed and laid out
 * elements will report bounds, so layouts that aren't currently visible (e.g. the
 * non-active layout branch in `MainScreenContent`) won't contribute stale data.
 */
fun Modifier.markForTutorial(
    state: TutorialBoxState,
    index: Int,
): Modifier = this.onGloballyPositioned { coordinates ->
    val bounds = coordinates.boundsInWindow()
    if (bounds.isFinite && !bounds.isEmpty) {
        val wasRegistered = index in state.registrationOrder
        if (!wasRegistered) {
            state.registrationOrder.add(index)
        }
        // Track FIRST measurement with valid bounds separately from
        // registrationOrder membership. `wasRegistered` will be `true` on every
        // call after the first because the walk order is pre-populated, so we need
        // a separate signal to know "this is the first time we've actually seen
        // this target" — otherwise the rewind never fires.
        val isFirstMeasurement = index !in state.measuredIndices
        state.measuredIndices.add(index)
        state.targetBounds[index] = bounds
        // First target to register becomes the current highlight on the very first frame
        // the overlay shows.
        if (state.currentIndexState.value == -1) {
            state.currentIndexState.value = state.registrationOrder.first()
        }
        // Rewind logic: if a target is being measured for the first time AND the
        // walk has already moved past its position (or completed), rewind the walk
        // to it. This handles the "gated" targets (1 = category tag, 4 = recurrent
        // toggle) which only become measurable once the editor enters EDITING state
        // — without this, the user could tap fast through the walk before entering
        // edit mode, the walk would complete skipping the gated targets, and the
        // gated targets would never be shown. With the rewind, the walk rewinds
        // whenever a gated target becomes available for the first time AND the
        // user hasn't seen it yet (i.e. it's not in visitedIndices), so the
        // gated tutorials only fire on the first edit-mode entry, not on every
        // subsequent one.
        if (isFirstMeasurement && index !in state.visitedIndices) {
            val targetPos = state.registrationOrder.indexOf(index)
            val currentPos = state.registrationOrder
                .indexOf(state.currentIndexState.value)
                .coerceAtLeast(0)
            if (state.isCompleted) {
                state.isCompleted = false
                state.currentIndexState.value = index
                logcat(TUTORIAL_LOG_TAG) {
                    "rewind: index=$index first-measured AFTER walk completed, " +
                        "rewinding walk to it (position=$targetPos)"
                }
            } else if (currentPos > targetPos) {
                state.currentIndexState.value = index
                logcat(TUTORIAL_LOG_TAG) {
                    "rewind: index=$index first-measured late, " +
                        "walk was at position=$currentPos, rewinding to position=$targetPos"
                }
            }
        }
        logcat(TUTORIAL_LOG_TAG) {
            "markForTutorial: index=$index bounds=$bounds " +
                "firstMeasure=$isFirstMeasurement " +
                "firstRegister=$wasRegistered " +
                "registrationOrder=${state.registrationOrder.toList()} " +
                "currentIndex=${state.currentIndexState.value} " +
                "isCompleted=${state.isCompleted}"
        }
    } else {
        logcat(TUTORIAL_LOG_TAG) {
            "markForTutorial: index=$index SKIPPED (empty/inf bounds=$bounds)"
        }
    }
}
