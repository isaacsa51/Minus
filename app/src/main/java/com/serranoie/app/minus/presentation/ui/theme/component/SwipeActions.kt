package com.serranoie.app.minus.presentation.ui.theme.component

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/**
 * Configuration for swipe actions.
 */
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeActions(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    enabled: Boolean = true,
    startActionsConfig: SwipeActionsConfig = DefaultSwipeActionsConfig,
    endActionsConfig: SwipeActionsConfig = DefaultSwipeActionsConfig,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    
    // Track if user has crossed threshold for haptic
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    
    // Track if action will trigger based on threshold
    var willDismiss by remember { mutableStateOf(false) }

    val state = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> 
            // Use the larger threshold of the two configs
            val threshold = maxOf(startActionsConfig.threshold, endActionsConfig.threshold)
            distance * threshold 
        }
    )
    
    // Reset swipe state when disabled (e.g., when item is being deleted)
    LaunchedEffect(enabled) {
        if (!enabled) {
            state.snapTo(SwipeToDismissBoxValue.Settled)
            hasTriggeredHaptic = false
            willDismiss = false
        }
    }
    
    val currentConfig = when (state.dismissDirection) {
        SwipeToDismissBoxValue.StartToEnd -> endActionsConfig
        SwipeToDismissBoxValue.EndToStart -> startActionsConfig
        else -> null
    }

    LaunchedEffect(state.progress, state.dismissDirection) {
        val threshold = currentConfig?.threshold ?: 0.32f
        val newWillDismiss = state.progress >= threshold
        
        // Trigger haptic when crossing threshold
        if (newWillDismiss && !hasTriggeredHaptic && state.dismissDirection != SwipeToDismissBoxValue.Settled) {
            @Suppress("DEPRECATION")
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            hasTriggeredHaptic = true
        } else if (!newWillDismiss) {
            hasTriggeredHaptic = false
        }
        
        willDismiss = newWillDismiss
    }

    LaunchedEffect(state.currentValue) {
        when (state.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> {
                startActionsConfig.onDismiss()
                hasTriggeredHaptic = false
                if (!startActionsConfig.stayDismissed) {
                    state.snapTo(SwipeToDismissBoxValue.Settled)
                }
            }
            SwipeToDismissBoxValue.StartToEnd -> {
                endActionsConfig.onDismiss()
                hasTriggeredHaptic = false
                if (!endActionsConfig.stayDismissed) {
                    state.snapTo(SwipeToDismissBoxValue.Settled)
                }
            }
            else -> {}
        }
    }

    val iconScale by animateFloatAsState(
        targetValue = if (willDismiss) 1.3f else 1f,
        animationSpec = if (willDismiss) {
            spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessMedium
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "iconScale"
    )

    SwipeToDismissBox(
        modifier = modifier,
        state = state,
        backgroundContent = {
            val direction = state.dismissDirection
            
            if (direction != SwipeToDismissBoxValue.Settled && state.progress > 0f) {
                val config = currentConfig ?: return@SwipeToDismissBox
                
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    else -> Alignment.Center
                }
                
                // Background color animation - invert when willDismiss
                val backgroundColor = if (willDismiss) {
                    config.backgroundActive
                } else {
                    config.background.copy(alpha = 0.6f)
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = backgroundColor,
                            shape = shape
                        ),
                    contentAlignment = alignment
                ) {
                    // Animate tint separately from scale
                    val iconTint by animateColorAsState(
                        targetValue = if (willDismiss) config.iconTint else config.iconTint.copy(alpha = 0.7f),
                        animationSpec = tween(150),
                        label = "iconTint"
                    )
                    
                    Icon(
                        imageVector = config.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 18.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            },
                        tint = iconTint
                    )
                }
            }
        }
    ) {
        content()
    }
}
