package com.serranoie.app.minus.presentation.ui.theme.component

import android.util.Log
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.SwipeableState
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.serranoie.app.minus.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.theme.colorButton
import com.serranoie.app.minus.presentation.ui.theme.colorEditor
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt

private val ScrimColor: Color
    @Composable
    get() = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)

enum class TopSheetValue {
    Expanded,
    HalfExpanded,
    Dismissed
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWearMaterialApi::class)
@Composable
fun TopSheetLayout(
    modifier: Modifier = Modifier,
    swipeableState: SwipeableState<TopSheetValue> = rememberSwipeableState(TopSheetValue.HalfExpanded),
    customHalfHeight: Float? = null,
    isLockSwipeable: () -> Boolean = { false },
    isLockDraggable: () -> Boolean = { false },
    canDismissBySwipeUp: () -> Boolean = { false },
    onTopSheetDownChanged: (Boolean) -> Unit = {},
    onDismiss: (() -> Unit)? = null,
    sheetContentHalfExpand: @Composable () -> Unit,
    sheetContentExpand: @Composable () -> Unit,
) {
    val tag = "TopSheetLayout - ISAAC"
    val localDensity = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var predictiveBackProgress by remember {
        mutableFloatStateOf(0f)
    }

    val navigationBarHeight = LocalWindowInsets.current.calculateBottomPadding()
        .coerceAtLeast(16.dp)

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        val fullHeight = constraints.maxHeight.toFloat()
        val halfHeight = customHalfHeight ?: (fullHeight / 2)
        val expandHeight =
            with(localDensity) { (fullHeight - navigationBarHeight.toPx() - 16.dp.toPx()) }
        val currOffset = swipeableState.offset.value
        val maxOffset = (-(expandHeight - halfHeight)).coerceAtMost(0f)

        val prevHalfHeight = remember { mutableFloatStateOf(halfHeight) }
        val isLockProgress = remember(swipeableState.isAnimationRunning) {
            mutableStateOf(prevHalfHeight.value != halfHeight && swipeableState.isAnimationRunning)
        }

        val progress = if (isLockProgress.value) {
            if (swipeableState.currentValue === TopSheetValue.HalfExpanded) 0f else 1f
        } else {
            (1f - (currOffset / maxOffset)).coerceIn(0f, 1f)
        }

        prevHalfHeight.value = halfHeight

        Box(Modifier.fillMaxSize()) {
            Scrim(
                color = ScrimColor,
                targetValue = (progress * 5).coerceIn(0f, 1f) * (1f - predictiveBackProgress * 0.7f),
            )
        }

        val halfExpanedOffset = (-(expandHeight - halfHeight)).coerceAtMost(0f)
        // Dismiss offset for swiping UP from Expanded (above the expanded position)
        val dismissOffsetAbove = -expandHeight * 0.5f
        // Keep the below offset for backward compatibility if needed
        val dismissOffsetBelow = expandHeight * 0.8f

        Card(
            shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp),
            colors = CardDefaults.cardColors(
                containerColor = colorButton,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = modifier
                .fillMaxWidth()
                .height(with(localDensity) {
                    (fullHeight - navigationBarHeight.toPx() - 16.dp.toPx()).toDp()
                })
                .offset {
                    val swipeOffset = swipeableState.offset.value
                    val predictiveOffset = halfExpanedOffset * predictiveBackProgress * 0.3f

                    IntOffset(
                        x = 0,
                        y = (swipeOffset + predictiveOffset)
                            .coerceIn(halfExpanedOffset, if (onDismiss != null) dismissOffsetBelow else 0f)
                            .roundToInt(),
                    )
                }
                .swipeable(
                    enabled = !isLockDraggable() && onDismiss != null,
                    state = swipeableState,
                    orientation = Orientation.Vertical,
                    anchors = if (onDismiss != null) {
                        val baseAnchors = mutableMapOf(
                            halfExpanedOffset to TopSheetValue.HalfExpanded,
                            0f to TopSheetValue.Expanded
                        )
                        // Add dismiss anchor above when swiping up is allowed
                        if (canDismissBySwipeUp()) {
                            baseAnchors[dismissOffsetAbove] = TopSheetValue.Dismissed
                        }
                        baseAnchors
                    } else {
                        mapOf(
                            halfExpanedOffset to TopSheetValue.HalfExpanded,
                            0f to TopSheetValue.Expanded
                        )
                    },
                    resistance = null
                )
        ) {
            Box(modifier = modifier.fillMaxSize()) {
                if (progress != 0f) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(top = 24.dp) // Status bar padding
                            .alpha(max(progress * 2f - 1f, 0f))
                    ) {
                        sheetContentExpand()
                    }

                    DisposableEffect(Unit) {
                        onTopSheetDownChanged(true)

                        onDispose {
                            onTopSheetDownChanged(false)
                        }
                    }
                }

                if (progress != 1f) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .alpha(max(1f - progress * 2, 0f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1F)
                                .background(colorEditor)
                        )
                        sheetContentHalfExpand()
                    }
                }

                // Drag handle at bottom
                Box(
                    Modifier
                        .padding(bottom = 10.dp, top = 32.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Box(
                        Modifier
                            .height(4.dp)
                            .width(30.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                    .copy(alpha = if (isLockDraggable()) 0f else 0.3f),
                                shape = CircleShape
                            )
                            .align(Alignment.Center)
                    )
                }
            }
        }
    }

    val scope = rememberCoroutineScope()

    // Handle dismiss state
    LaunchedEffect(swipeableState.currentValue) {
        if (swipeableState.currentValue == TopSheetValue.Dismissed && onDismiss != null) {
            Log.d(tag, "TopSheet dismissed via swipe, calling onDismiss")
            // Small delay to let the animation complete
            kotlinx.coroutines.delay(300)
            onDismiss()
            // Reset state after dismiss
            scope.launch {
                swipeableState.animateTo(TopSheetValue.HalfExpanded)
            }
        }
    }

    // Predictive back handler at TopSheetLayout level
    PredictiveBackHandler(swipeableState.currentValue === TopSheetValue.Expanded) { progress ->
        try {
            progress.collect { backEvent ->
                predictiveBackProgress = backEvent.progress
            }

            coroutineScope.launch {
                swipeableState.animateTo(TopSheetValue.HalfExpanded)
                predictiveBackProgress = 0f
            }
        } catch (e: CancellationException) {
            predictiveBackProgress = 0f
        }
    }
}

@Composable
fun Scrim(
    color: Color,
    targetValue: Float
) {
    if (color.isSpecified) {
        Canvas(
            Modifier.fillMaxSize()
        ) {
            drawRect(color = color, alpha = targetValue)
        }
    }
}
