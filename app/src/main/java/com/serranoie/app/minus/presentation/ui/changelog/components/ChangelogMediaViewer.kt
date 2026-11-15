package com.serranoie.app.minus.presentation.ui.changelog.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import com.serranoie.app.minus.R

internal const val MAX_MEDIA_ZOOM = 5f
internal const val DOUBLE_TAP_ZOOM = 2.5f
private const val SCRIM_ALPHA = 0.92f
private const val ENTER_SCALE = 0.9f

internal data class MediaZoom(val scale: Float = 1f, val offset: Offset = Offset.Zero) {

    fun transform(size: Size, centroid: Offset, pan: Offset, zoom: Float): MediaZoom {
        val newScale = (scale * zoom).coerceIn(1f, MAX_MEDIA_ZOOM)
        if (newScale == 1f) return MediaZoom()
        val focus = centroid - size.center
        val moved = focus - (focus - offset) * (newScale / scale) + pan
        val maxX = size.width * (newScale - 1f) / 2f
        val maxY = size.height * (newScale - 1f) / 2f
        return MediaZoom(newScale, Offset(moved.x.coerceIn(-maxX, maxX), moved.y.coerceIn(-maxY, maxY)))
    }

    fun toggle(size: Size, tap: Offset): MediaZoom =
        if (scale > 1f) MediaZoom() else transform(size, tap, Offset.Zero, DOUBLE_TAP_ZOOM / scale)
}

@Composable
internal fun ChangelogMediaViewer(
    model: String,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        var zoom by remember { mutableStateOf(MediaZoom()) }
        var size by remember { mutableStateOf(Size.Zero) }
        var animateZoom by remember { mutableStateOf(false) }
        val appear = remember { Animatable(0f) }
        LaunchedEffect(appear) {
            appear.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
        }
        val scale by animateFloatAsState(zoom.scale, if (animateZoom) spring() else snap(), label = "media_scale")
        val offset by animateOffsetAsState(zoom.offset, if (animateZoom) spring() else snap(), label = "media_offset")

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .onSizeChanged { size = it.toSize() }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            animateZoom = true
                            zoom = zoom.toggle(size, it)
                        },
                        onTap = { onDismiss() },
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        animateZoom = false
                        zoom = zoom.transform(size, centroid, pan, gestureZoom)
                    }
                },
        ) {
            AsyncImage(
                model = model,
                imageLoader = imageLoader,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val enter = ENTER_SCALE + (1f - ENTER_SCALE) * appear.value
                        scaleX = scale * enter
                        scaleY = scale * enter
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(8.dp),
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
            }
        }
    }
}
