package com.serranoie.app.minus.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.NavigationEventState
import androidx.navigationevent.compose.rememberNavigationEventState

@Stable
class DismissBackState internal constructor(
    internal val events: NavigationEventState<NavigationEventInfo>,
    internal val animated: Boolean,
)

@Composable
fun rememberPredictiveDismiss(
    enabled: Boolean,
    onDismiss: () -> Unit,
): DismissBackState {
    val events = rememberNavigationEventState<NavigationEventInfo>(
        currentInfo = NavigationEventInfo.None,
    )
    val animated = rememberAnimationsEnabled()

    if (LocalNavigationEventDispatcherOwner.current != null) {
        NavigationBackHandler(
            state = events,
            isBackEnabled = enabled,
            onBackCompleted = onDismiss,
        )
    }

    return remember(events, animated) { DismissBackState(events, animated) }
}

internal fun DismissBackState.backProgress(): Float {
    if (!animated) return 0f
    val inProgress = events.transitionState as? NavigationEventTransitionState.InProgress
        ?: return 0f
    if (inProgress.direction != NavigationEventTransitionState.TRANSITIONING_BACK) return 0f
    return inProgress.latestEvent.progress
}

fun Modifier.predictiveDismiss(state: DismissBackState): Modifier = graphicsLayer {
    val eased = easedProgress(state.backProgress())

    scaleX = 1f - eased * (1f - BackMotionTokens.PageScaleTarget)
    scaleY = scaleX
    alpha = 1f - eased * (1f - BackMotionTokens.DismissAlphaFloor)
}
