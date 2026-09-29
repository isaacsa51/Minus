package com.serranoie.app.minus.navigation

import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object BackMotionTokens {
    val PageScaleTarget = 0.94f
    val ScrubAlphaFloor = 0f
    val ScrubAlphaFloorAt = 0.65f
    val PageCornerRadius = 36.dp
    val RevealDimAlpha = 0.8f
    val DismissAlphaFloor = 1.0f
    val EnteringStartOffset = 96.dp
    val GestureMaxOffset = 32.dp
    val ExitSlideDivisor = 3
    val ClosingDepartureFraction = 0.3f
    val TransitionDurationMillis = 325
    val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val GestureEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
}

private val PageCornerShape = RoundedCornerShape(BackMotionTokens.PageCornerRadius)

private fun <T> pageSpec(durationMillis: Int = BackMotionTokens.TransitionDurationMillis) = tween<T>(
    durationMillis = durationMillis,
    easing = BackMotionTokens.Emphasized,
)

private typealias PageScope = AnimatedContentTransitionScope<NavBackStackEntry>

fun PageScope.screenEnter(
    durationMillis: Int = BackMotionTokens.TransitionDurationMillis,
): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = pageSpec(durationMillis),
    ) + fadeIn(animationSpec = pageSpec(durationMillis))

fun PageScope.screenExit(
    durationMillis: Int = BackMotionTokens.TransitionDurationMillis,
): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = pageSpec(durationMillis),
        targetOffset = { it / BackMotionTokens.ExitSlideDivisor },
    ) + fadeOut(animationSpec = pageSpec(durationMillis))

fun PageScope.screenPopEnter(
    offsetPx: Int,
    durationMillis: Int = BackMotionTokens.TransitionDurationMillis,
): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = pageSpec(durationMillis),
        initialOffset = { -offsetPx },
    ) + fadeIn(animationSpec = pageSpec(durationMillis))

private fun scrubThenRemoveFadeOut(durationMillis: Int) = keyframes {
    this.durationMillis = durationMillis
    1f at 0
    BackMotionTokens.ScrubAlphaFloor at
        (durationMillis * BackMotionTokens.ScrubAlphaFloorAt).toInt()
    0f at durationMillis
}

fun PageScope.screenPopExit(
    durationMillis: Int = BackMotionTokens.TransitionDurationMillis,
): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = pageSpec(durationMillis),
        targetOffset = { it / BackMotionTokens.ExitSlideDivisor },
    ) + scaleOut(
        animationSpec = pageSpec(durationMillis),
        targetScale = BackMotionTokens.PageScaleTarget,
        transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f),
    ) + fadeOut(animationSpec = scrubThenRemoveFadeOut(durationMillis))

@Stable
class BackGesture internal constructor() {
    var scale by mutableFloatStateOf(1f)
        private set
    var xOffset by mutableFloatStateOf(0f)
        private set
    var alpha by mutableFloatStateOf(1f)
        private set
    var release by mutableFloatStateOf(0f)
        private set
    var engaged by mutableStateOf(false)
        private set

    internal fun onProgress(progress: Float, swipeEdge: Int) {
        val directionMultiplier = if (swipeEdge == NavigationEvent.EDGE_RIGHT) -1f else 1f
        val eased = easedProgress(progress)
        this.scale = 1f - (eased * (1f - BackMotionTokens.PageScaleTarget))
        this.xOffset = eased * BackMotionTokens.GestureMaxOffset.value * directionMultiplier
        this.alpha = 1f // full opacity (1.0) on exiting screen during gesture
        this.release = eased
        this.engaged = true
    }

    internal suspend fun settle(durationMillis: Int = BackMotionTokens.TransitionDurationMillis) {
        val startScale = scale
        val startXOffset = xOffset
        val startAlpha = alpha
        val startRelease = release

        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = pageSpec(durationMillis),
        ) { progress, _ ->
            val inverseProgress = 1f - progress
            scale = 1f - (1f - startScale) * inverseProgress
            xOffset = startXOffset * inverseProgress
            alpha = 1f - (1f - startAlpha) * inverseProgress
            release = startRelease * inverseProgress
        }
        scale = 1f
        xOffset = 0f
        alpha = 1f
        release = 0f
        engaged = false
    }
}

@Composable
fun rememberBackGesture(): BackGesture {
    val gesture = remember { BackGesture() }
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    val animatorScale = rememberAnimatorDurationScale()
    val durationMillis = (BackMotionTokens.TransitionDurationMillis * (if (animatorScale <= 0f) 1f else animatorScale)).toInt()

    LaunchedEffect(dispatcher, durationMillis) {
        var settleJob: Job? = null
        dispatcher?.transitionState?.collect { state ->
            val inProgress = state as? NavigationEventTransitionState.InProgress

            if (inProgress != null && inProgress.direction == NavigationEventTransitionState.TRANSITIONING_BACK) {
                settleJob?.cancel()
                settleJob = null
                gesture.onProgress(inProgress.latestEvent.progress, inProgress.latestEvent.swipeEdge)
            } else {
                if (gesture.engaged && settleJob?.isActive != true) {
                    settleJob = launch { gesture.settle(durationMillis) }
                }
            }
        }
    }

    return gesture
}

internal fun easedProgress(progress: Float): Float =
    BackMotionTokens.GestureEasing.transform(progress.coerceIn(0f, 1f))

internal fun revealDim(revealFraction: Float): Float =
    BackMotionTokens.RevealDimAlpha * (1f - revealFraction.coerceIn(0f, 1f))

class PopDirection {
    var isPop: Boolean = false
        internal set

    internal fun record(pop: Boolean) {
        isPop = pop
    }
}

@Composable
fun rememberAnimatorDurationScale(): Float {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(
            resolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }
}

@Composable
fun rememberAnimationsEnabled(): Boolean {
    val scale = rememberAnimatorDurationScale()
    return scale > 0f
}

internal fun closingDeparture(width: Float, release: Float): Float =
    (1f - release.coerceIn(0f, 1f)) * width * BackMotionTokens.ClosingDepartureFraction

fun Modifier.predictiveBackPage(
    gesture: BackGesture,
    transition: Transition<EnterExitState>,
    dimAlpha: () -> Float,
): Modifier = this
    .graphicsLayer {
        val leaving = transition.targetState == EnterExitState.PostExit
        val moving = transition.currentState != transition.targetState

        clip = leaving && (moving || gesture.engaged || gesture.release > 0f)
        shape = PageCornerShape

        if (leaving && (gesture.engaged || gesture.scale < 1f || gesture.xOffset != 0f)) {
            scaleX = gesture.scale
            scaleY = gesture.scale
            translationX = gesture.xOffset.dp.toPx()
            this.alpha = 1f
        } else if (leaving && gesture.release > 0f) {
            translationX = closingDeparture(size.width, gesture.release)
        } else if (!leaving && gesture.engaged) {
            val enteringScale = 0.85f + (0.15f * gesture.release)
            scaleX = enteringScale
            scaleY = enteringScale

            val startOffsetPx = BackMotionTokens.EnteringStartOffset.toPx()
            translationX = -startOffsetPx * (1f - gesture.release)

            this.alpha = 0.5f + (0.5f * gesture.release)
        }
    }
    .drawWithContent {
        drawContent()

        if (transition.targetState == EnterExitState.PostExit) return@drawWithContent

        val dim = if (gesture.engaged) revealDim(gesture.release) else dimAlpha()
        if (dim > 0f) drawRect(color = Color.Black, alpha = dim)
    }
