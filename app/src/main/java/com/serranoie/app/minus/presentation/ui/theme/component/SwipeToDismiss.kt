package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Direction of the dismiss action
 */
enum class DismissDirection {
    StartToEnd,
    EndToStart
}

/**
 * State representing the dismiss state of an item
 */
class DismissState(
    val initialValue: DismissValue = DismissValue.Default,
    val confirmStateChange: (DismissValue) -> Boolean = { true }
) {
    var currentValue by mutableStateOf(initialValue)
        internal set
    
    var isDismissed: Boolean = false
        internal set
    
    var dismissDirection: DismissDirection? = null
        internal set
    
    var offset: Float by mutableFloatStateOf(0f)
        internal set
    
    suspend fun dismiss(direction: DismissDirection) {
        val targetValue = if (direction == DismissDirection.StartToEnd) {
            DismissValue.DismissedToEnd
        } else {
            DismissValue.DismissedToStart
        }
        
        if (confirmStateChange(targetValue)) {
            currentValue = targetValue
            isDismissed = true
        }
    }
    
    suspend fun reset() {
        currentValue = DismissValue.Default
        isDismissed = false
        offset = 0f
    }
}

/**
 * Value representing the dismiss state
 */
enum class DismissValue {
    Default,
    DismissedToEnd,
    DismissedToStart
}

/**
 * Creates and remembers a [DismissState]
 */
@Composable
fun rememberDismissState(
    initialValue: DismissValue = DismissValue.Default,
    confirmStateChange: (DismissValue) -> Boolean = { true }
): DismissState {
    return remember {
        DismissState(initialValue, confirmStateChange)
    }
}

/**
 * Custom SwipeToDismiss composable that works with Material3
 * 
 * @param state The state of the dismiss
 * @param modifier Modifier for the layout
 * @param directions Directions in which the item can be dismissed
 * @param background Background content shown during swipe
 * @param dismissContent Content that can be dismissed
 */
@Composable
fun SwipeToDismiss(
    state: DismissState,
    modifier: Modifier = Modifier,
    directions: Set<DismissDirection> = setOf(DismissDirection.EndToStart),
    background: @Composable RowScope.() -> Unit,
    dismissContent: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    
    // Threshold for dismissing (40% of width)
    val dismissThreshold = 0.4f
    
    // Animate offset
    val offsetAnim = remember { Animatable(0f) }
    
    // Track if we're currently dismissing
    var isDismissing by remember { mutableStateOf(false) }
    
    // Draggable state
    val draggableState = rememberDraggableState(
        onDelta = { delta ->
            scope.launch {
                val newOffset = offsetAnim.value + delta
                // Only allow swipe in configured directions
                val allowed = when {
                    directions.contains(DismissDirection.StartToEnd) && 
                            directions.contains(DismissDirection.EndToStart) -> true
                    directions.contains(DismissDirection.StartToEnd) && newOffset > 0 -> true
                    directions.contains(DismissDirection.EndToStart) && newOffset < 0 -> true
                    else -> false
                }
                
                if (allowed || newOffset == 0f) {
                    offsetAnim.snapTo(newOffset)
                    state.offset = newOffset
                    
                    // Calculate progress for haptic feedback
                    val progress = newOffset.absoluteValue / 1000f // approximate width
                    if (progress > dismissThreshold && !isDismissing) {
                        isDismissing = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else if (progress <= dismissThreshold) {
                        isDismissing = false
                    }
                }
            }
        }
    )
    
    Box(modifier = modifier) {
        // Background layer (revealed during swipe)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = (offsetAnim.value.absoluteValue / 500f).coerceIn(0f, 1f)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            background()
        }
        
        // Foreground content (draggable)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetAnim.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = draggableState,
                    onDragStopped = { velocity ->
                        scope.launch {
                            val offset = offsetAnim.value
                            val width = 1000f // approximate
                            val threshold = width * dismissThreshold
                            
                            when {
                                offset > threshold && directions.contains(DismissDirection.StartToEnd) -> {
                                    // Dismiss to end
                                    offsetAnim.animateTo(width, spring())
                                    state.dismiss(DismissDirection.StartToEnd)
                                    state.confirmStateChange(DismissValue.DismissedToEnd)
                                }
                                offset < -threshold && directions.contains(DismissDirection.EndToStart) -> {
                                    // Dismiss to start
                                    offsetAnim.animateTo(-width, spring())
                                    state.dismiss(DismissDirection.EndToStart)
                                    state.confirmStateChange(DismissValue.DismissedToStart)
                                }
                                else -> {
                                    // Reset
                                    offsetAnim.animateTo(0f, spring())
                                    state.reset()
                                }
                            }
                        }
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dismissContent()
        }
    }
}
