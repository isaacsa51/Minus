package com.serranoie.app.minus.presentation.ui.theme.component

import android.view.MotionEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class SwipeActionsConfig(
    val threshold: Float,
    val icon: ImageVector,
    val iconTint: Color,
    val background: Color,
    val backgroundActive: Color,
    val stayDismissed: Boolean,
    val onDismiss: () -> Unit,
)

val DefaultSwipeActionsConfig = SwipeActionsConfig(
    threshold = 0.4f,
    icon = Icons.Default.Delete,
    iconTint = Color.Transparent,
    background = Color.Transparent,
    backgroundActive = Color.Transparent,
    stayDismissed = false,
    onDismiss = {},
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SwipeActions(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    enabled: Boolean = true,
    background: Color = MaterialTheme.colorScheme.surface,
    startActionsConfig: SwipeActionsConfig = DefaultSwipeActionsConfig,
    endActionsConfig: SwipeActionsConfig = DefaultSwipeActionsConfig,
    showTutorial: Boolean = false,
    content: @Composable (SwipeToDismissBoxState) -> Unit,
) {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentBackground by rememberUpdatedState(background)
    val currentStartActionsConfig by rememberUpdatedState(startActionsConfig)
    val currentEndActionsConfig by rememberUpdatedState(endActionsConfig)
    val currentShape by rememberUpdatedState(shape)
    val currentContent by rememberUpdatedState(content)

    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth.toFloat()
        val haptic = LocalHapticFeedback.current

        var willDismissDirection: SwipeToDismissBoxValue? by remember {
            mutableStateOf(null)
        }

        val state = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (!currentEnabled) return@rememberSwipeToDismissBoxState false
                when (value) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (currentStartActionsConfig == DefaultSwipeActionsConfig) return@rememberSwipeToDismissBoxState false
                        currentStartActionsConfig.onDismiss()
                        currentStartActionsConfig.stayDismissed
                    }

                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (currentEndActionsConfig == DefaultSwipeActionsConfig) return@rememberSwipeToDismissBoxState false
                        currentEndActionsConfig.onDismiss()
                        currentEndActionsConfig.stayDismissed
                    }

                    else -> false
                }
            }
        )

        var showingTutorial by remember { mutableStateOf(showTutorial) }

        LaunchedEffect(Unit) {
            snapshotFlow {
                runCatching { state.requireOffset() }.getOrDefault(0f)
            }.collect { offset ->
                willDismissDirection = when {
                    offset > width * currentEndActionsConfig.threshold -> SwipeToDismissBoxValue.StartToEnd
                    offset < -width * currentStartActionsConfig.threshold -> SwipeToDismissBoxValue.EndToStart
                    else -> null
                }
            }
        }

        LaunchedEffect(willDismissDirection) {
            if (willDismissDirection != null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

        SwipeToDismissBox(
            state = state,
            modifier = Modifier
                .clip(currentShape)
                .pointerInteropFilter {
                    if (it.action == MotionEvent.ACTION_DOWN) {
                        showingTutorial = false
                    }
                    false
                },
            enableDismissFromStartToEnd = currentEnabled && currentEndActionsConfig != DefaultSwipeActionsConfig,
            enableDismissFromEndToStart = currentEnabled && currentStartActionsConfig != DefaultSwipeActionsConfig,
            backgroundContent = {
                val direction = state.dismissDirection
                val isActivating = willDismissDirection != null

                AnimatedContent(
                    targetState = direction to isActivating,
                    transitionSpec = {
                        fadeIn(tween(0), initialAlpha = if (targetState.second) 1f else 0f) togetherWith
                                fadeOut(tween(0), targetAlpha = if (targetState.second) 0.7f else 0f)
                    },
                    label = "background_content"
                ) { (dir, activating) ->
                    val revealSize = remember { Animatable(if (activating) 0f else 1f) }
                    val iconSize = remember { Animatable(if (activating) 1f else 1.25f) }

                    LaunchedEffect(activating) {
                        if (activating) {
                            revealSize.snapTo(0f)
                            launch { revealSize.animateTo(1f, animationSpec = tween(400)) }
                            iconSize.snapTo(1f)
                            iconSize.animateTo(
                                1.6f,
                                spring(dampingRatio = Spring.DampingRatioHighBouncy)
                            )
                            iconSize.animateTo(
                                1.25f,
                                spring(dampingRatio = Spring.DampingRatioLowBouncy)
                            )
                        }
                    }

                    val config = when (dir) {
                        SwipeToDismissBoxValue.StartToEnd -> currentEndActionsConfig
                        SwipeToDismissBoxValue.EndToStart -> currentStartActionsConfig
                        else -> null
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                CirclePath(
                                    revealSize.value,
                                    dir == SwipeToDismissBoxValue.StartToEnd
                                )
                            )
                            .background(
                                color = when (dir) {
                                    SwipeToDismissBoxValue.StartToEnd -> if (activating) currentEndActionsConfig.backgroundActive else Color.Transparent
                                    SwipeToDismissBoxValue.EndToStart -> if (activating) currentStartActionsConfig.backgroundActive else Color.Transparent
                                    else -> Color.Transparent
                                }
                            )
                    ) {
                        if (config != null) {
                            Box(
                                modifier = Modifier
                                    .align(
                                        if (dir == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart
                                        else Alignment.CenterEnd
                                    )
                                    .fillMaxHeight()
                                    .aspectRatio(1f)
                                    .scale(iconSize.value)
                                    .offset {
                                        IntOffset(
                                            x = 0,
                                            y = (10 * (1.25f - iconSize.value)).roundToInt()
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = rememberVectorPainter(image = config.icon),
                                    colorFilter = ColorFilter.tint(if (activating) config.iconTint else config.background),
                                    contentDescription = null,
                                    modifier = Modifier.padding(horizontal = 20.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) {

            val currentOffset by remember {
                derivedStateOf { runCatching { state.requireOffset() }.getOrDefault(0f) }
            }

            val animateCorners by remember {
                derivedStateOf {
                    currentOffset.absoluteValue > 30
                }
            }

            val cornerAnim by animateDpAsState(
                targetValue = if (animateCorners) 16.dp else 0.dp,
                animationSpec = tween(200),
                label = "corners"
            )

            val swipingStart = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val swipingEnd = state.dismissDirection == SwipeToDismissBoxValue.EndToStart

            val currentShapeInner = remember(animateCorners, currentShape, cornerAnim, swipingStart, swipingEnd) {
                if (animateCorners) {
                    RoundedCornerShape(
                        topStart = if (swipingStart) cornerAnim else 16.dp,
                        bottomStart = if (swipingStart) cornerAnim else 16.dp,
                        topEnd = if (swipingEnd) cornerAnim else 16.dp,
                        bottomEnd = if (swipingEnd) cornerAnim else 16.dp
                    )
                } else {
                    currentShape
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = currentBackground,
                shape = currentShape
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            shadowElevation =
                                if (animateCorners) 6f * (currentOffset.absoluteValue / width).coerceIn(
                                    0f,
                                    1f
                                ) else 0f
                        }
                        .clip(currentShapeInner)) {
                    currentContent(state)
                }
            }
        }
    }

}

class CirclePath(private val progress: Float, private val start: Boolean) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val origin = Offset(
            x = if (start) 0f else size.width,
            y = size.center.y,
        )

        val radius = (sqrt(
            size.height * size.height + size.width * size.width
        ) * 1.5f) * progress

        return Outline.Generic(
            Path().apply {
                addOval(Rect(center = origin, radius = radius))
            }
        )
    }
}
