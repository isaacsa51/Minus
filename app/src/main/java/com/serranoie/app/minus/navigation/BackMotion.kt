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
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object BackMotionTokens {
    val PageScaleTarget = 0.90f
    val ScrubAlphaFloor = 0f
    val ScrubAlphaFloorAt = 0.8f
    val PageCornerRadius = 36.dp
    val RevealDimAlpha = 0.4f
    val DismissAlphaFloor = 0.7f
    val EnteringStartOffset = 96.dp
    val ExitSlideDivisor = 4
    val ClosingDepartureFraction = 0.3f
    val TransitionDurationMillis = 300
    val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val GestureEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
}

private fun <T> pageSpec() = tween<T>(
    durationMillis = BackMotionTokens.TransitionDurationMillis,
    easing = BackMotionTokens.Emphasized,
)

private typealias PageScope = AnimatedContentTransitionScope<NavBackStackEntry>

fun PageScope.screenEnter(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = pageSpec(),
    ) + fadeIn(animationSpec = pageSpec())

fun PageScope.screenExit(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Left,
        animationSpec = pageSpec(),
        targetOffset = { it / BackMotionTokens.ExitSlideDivisor },
    ) + fadeOut(animationSpec = pageSpec())

fun PageScope.screenPopEnter(offsetPx: Int): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Right,
        animationSpec = pageSpec(),
        initialOffset = { -offsetPx },
    ) + fadeIn(animationSpec = pageSpec())

private fun scrubThenRemoveFadeOut() = keyframes {
    durationMillis = BackMotionTokens.TransitionDurationMillis
    1f at 0
    BackMotionTokens.ScrubAlphaFloor at
        (BackMotionTokens.TransitionDurationMillis * BackMotionTokens.ScrubAlphaFloorAt).toInt()
    0f at BackMotionTokens.TransitionDurationMillis
}

fun PageScope.screenPopExit(): ExitTransition =
    scaleOut(
        animationSpec = pageSpec(),
        targetScale = BackMotionTokens.PageScaleTarget,
        transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0.5f),
    ) + fadeOut(animationSpec = scrubThenRemoveFadeOut())

@Stable
class BackGesture internal constructor() {
    var release by mutableFloatStateOf(0f)
        private set
    var engaged by mutableStateOf(false)
        private set

    internal fun onEvent() {
        release = 1f
        engaged = true
    }

    internal suspend fun settle() {
        animate(
            initialValue = release,
            targetValue = 0f,
            animationSpec = pageSpec(),
        ) { value, _ -> release = value }
        engaged = false
    }
}

@Composable
fun rememberBackGesture(): BackGesture {
    val gesture = remember { BackGesture() }
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher

    LaunchedEffect(dispatcher) {
        var settleJob: Job? = null
        dispatcher?.transitionState?.collect { state ->
            val isBackGesture = (state as? NavigationEventTransitionState.InProgress)
                ?.direction == NavigationEventTransitionState.TRANSITIONING_BACK

            if (isBackGesture) {
                settleJob?.cancel()
                settleJob = null
                gesture.onEvent()
            } else {
                if (gesture.release > 0f && settleJob?.isActive != true) {
                    settleJob = launch { gesture.settle() }
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
fun rememberAnimationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(
            resolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) != 0f
    }
}

internal fun closingDeparture(width: Float, release: Float): Float =
    (1f - release.coerceIn(0f, 1f)) * width * BackMotionTokens.ClosingDepartureFraction

fun Modifier.predictiveBackPage(
    gesture: BackGesture,
    transition: Transition<EnterExitState>,
    dimAlpha: () -> Float,
): Modifier = this
    .graphicsLayer {
        val release = gesture.release
        val leaving = transition.targetState == EnterExitState.PostExit
        val moving = transition.currentState != transition.targetState

        clip = leaving && (moving || release > 0f)
        shape = RoundedCornerShape(BackMotionTokens.PageCornerRadius)

        translationX = if (leaving && gesture.engaged) {
            closingDeparture(size.width, release)
        } else {
            0f
        }
    }
    .drawWithContent {
        drawContent()

        if (transition.targetState == EnterExitState.PostExit) return@drawWithContent

        val dim = dimAlpha()
        if (dim > 0f) drawRect(color = Color.Black, alpha = dim)
    }
