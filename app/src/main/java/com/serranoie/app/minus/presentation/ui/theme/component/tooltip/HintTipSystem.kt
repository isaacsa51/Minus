package com.serranoie.app.minus.presentation.ui.theme.component.tooltip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import java.util.UUID

internal data class HintTipPayload(
    val id: String,
    val state: HintTipState,
    val content: @Composable () -> Unit,
    val onClose: (() -> Unit)?
)

@Stable
class HintTipController {
    private val _activeTips = mutableStateListOf<HintTipPayload>()

    internal val activeTips: List<HintTipPayload>
        get() = _activeTips

    internal fun spawn(
        state: HintTipState,
        content: @Composable () -> Unit,
        onClose: (() -> Unit)? = null
    ) {
        _activeTips.removeAll { it.id == state.id }
        _activeTips.add(
            HintTipPayload(
                id = state.id,
                state = state,
                content = content,
                onClose = onClose
            )
        )
    }

    fun hide(id: String) {
        val target = _activeTips.firstOrNull { it.id == id } ?: return
        target.state.isVisible = false
        target.onClose?.invoke()
        destroy(id)
    }

    fun hideAll() {
        val ids = _activeTips.map { it.id }
        ids.forEach(::hide)
    }

    fun destroy(id: String) {
        _activeTips.removeAll { it.id == id }
    }
}

@Stable
class HintTipState internal constructor(
    internal val id: String,
    private val controller: HintTipController
) {
    internal var anchor by mutableStateOf(Offset.Unspecified)
    internal var isVisible by mutableStateOf(false)

    fun updateAnchor(anchorInWindow: Offset, anchorSize: IntSize) {
        anchor = Offset(
            x = anchorInWindow.x + anchorSize.width / 2f,
            y = anchorInWindow.y + anchorSize.height
        )
    }

    fun show(
        onClose: (() -> Unit)? = null,
        content: @Composable () -> Unit
    ) {
        controller.spawn(state = this, content = content, onClose = onClose)
        isVisible = true
    }

    fun hide() {
        controller.hide(id)
    }
}

private val LocalHintTipController = staticCompositionLocalOf<HintTipController> {
    error("HintTipController not found. Wrap your UI with HintTipProvider.")
}

@Composable
fun rememberHintTipState(
    key: String = UUID.randomUUID().toString()
): HintTipState {
    val controller = LocalHintTipController.current
    return remember(key, controller) {
        HintTipState(id = key, controller = controller)
    }
}

@Composable
fun HintTipScope(
    state: HintTipState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.hintTipAnchor(state),
        content = content
    )
}

fun Modifier.hintTipAnchor(state: HintTipState): Modifier {
    return this.onGloballyPositioned { coordinates ->
        state.updateAnchor(
            anchorInWindow = coordinates.positionInWindow(),
            anchorSize = coordinates.size
        )
    }
}

@Composable
fun HintTipProvider(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val controller = remember { HintTipController() }
    CompositionLocalProvider(LocalHintTipController provides controller) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            HintTipOverlay(controller)
        }
    }
}

@Composable
private fun HintTipOverlay(controller: HintTipController) {
    val density = LocalDensity.current
    val yGapPx = with(density) { 4.dp.toPx() }
    val screenPaddingPx = with(density) { 8.dp.toPx() }

    val dismissTapModifier = if (controller.activeTips.isNotEmpty()) {
        Modifier.pointerInput(controller.activeTips.size) {
            detectTapGestures(onTap = { controller.hideAll() })
        }
    } else {
        Modifier
    }

    var overlaySize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(dismissTapModifier)
            .onGloballyPositioned { overlaySize = it.size }
    ) {
        controller.activeTips.forEach { payload ->
            val currentContent by rememberUpdatedState(payload.content)
            var tipSize by remember(payload.id) { mutableStateOf(IntSize.Zero) }
            val rawOffsetX = payload.state.anchor.x - tipSize.width / 2f
            val maxOffsetX = (overlaySize.width - tipSize.width - screenPaddingPx)
                .coerceAtLeast(screenPaddingPx)
            val offsetX = rawOffsetX.coerceIn(screenPaddingPx, maxOffsetX)

            val rawOffsetY = payload.state.anchor.y + yGapPx
            val maxOffsetY = (overlaySize.height - tipSize.height - screenPaddingPx)
                .coerceAtLeast(screenPaddingPx)
            val offsetY = rawOffsetY.coerceIn(screenPaddingPx, maxOffsetY)

            AnimatedVisibility(
                visible = payload.state.isVisible,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .onGloballyPositioned { tipSize = it.size }
                    .offset {
                        IntOffset(
                            x = offsetX.roundToInt(),
                            y = offsetY.roundToInt()
                        )
                    }
            ) {
                currentContent()
            }
        }
    }
}
