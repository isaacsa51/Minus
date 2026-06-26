package com.serranoie.app.minus.presentation.ui.changelog.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    val resolvedResId = remember(imageName) { resolveChangelogDrawableId(context, imageName) }
    if (resolvedResId == 0) return

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(resolvedResId)
            // coil-gif auto-registers GifDecoder for animated GIFs; static
            // images use Coil's default decoder. No explicit decoderFactory
            // needed — let Coil pick by content type.
            .crossfade(true)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

private fun resolveChangelogDrawableId(context: Context, name: String): Int {
    return try {
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (id == 0) {
            logcat("Changelog") { "ChangelogMedia: drawable '$name' not found in res/drawable/" }
        }
        id
    } catch (cancellation: kotlinx.coroutines.CancellationException) {
        throw cancellation
    } catch (t: Throwable) {
        logcat("Changelog") { "ChangelogMedia: failed to resolve '$name': ${t.message}" }
        0
    }
}