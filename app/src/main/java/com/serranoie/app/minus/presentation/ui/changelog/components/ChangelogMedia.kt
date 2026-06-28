package com.serranoie.app.minus.presentation.ui.changelog.components

import android.content.Context
import android.util.TypedValue
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import logcat.logcat

/**
 * Renders a changelog item's image (WebP / GIF / PNG) at the top of its card.
 *
 * Resolves the resource by name (`getIdentifier`) and loads it with Coil. The
 * `coil-gif` artifact auto-registers `GifDecoder.Factory()` so animated GIFs
 * animate on every API level supported by the app (no explicit decoder
 * selection needed).
 *
 * Returns nothing when the drawable cannot be resolved — caller (typically
 * `ChangelogItemCard`) is expected to skip the media slot entirely when
 * `imageName` is null so the card collapses to just title + description
 * without leaving blank space behind.
 */
@Composable
internal fun ChangelogMedia(
    imageName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val info = remember(imageName) { resolveChangelogDrawableInfo(context, imageName) }
    if (info.resId == 0) return

    if (info.isAnimated) {
        AnimatedChangelogMedia(resId = info.resId, modifier = modifier)
    } else {
        StaticChangelogMedia(resId = info.resId, modifier = modifier)
    }
}

@Composable
private fun AnimatedChangelogMedia(resId: Int, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(resId)
            }
        },
        modifier = modifier
    )
}

@Composable
private fun StaticChangelogMedia(resId: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(resId),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

private data class ChangelogDrawableInfo(
    val resId: Int,
    val isAnimated: Boolean,
)

private fun resolveChangelogDrawableInfo(context: Context, name: String): ChangelogDrawableInfo {
    val resId = try {
        context.resources.getIdentifier(name, "drawable", context.packageName).also {
            if (it == 0) logcat("Changelog") { "ChangelogMedia: drawable '$name' not found in res/drawable/" }
        }
    } catch (cancellation: kotlinx.coroutines.CancellationException) {
        throw cancellation
    } catch (t: Throwable) {
        logcat("Changelog") { "ChangelogMedia: failed to resolve '$name': ${t.message}" }
        0
    }

    if (resId == 0) return ChangelogDrawableInfo(0, false)

    val isAnimated = runCatching {
        val typedValue = TypedValue()
        context.resources.getValue(resId, typedValue, true)
        val path = typedValue.string?.toString() ?: ""
        val extension = path.substringAfterLast('.', "").lowercase()
        extension == "gif" || extension == "webp"
    }.getOrDefault(false)

    return ChangelogDrawableInfo(resId, isAnimated)
}