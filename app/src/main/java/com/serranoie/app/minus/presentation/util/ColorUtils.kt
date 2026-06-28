package com.serranoie.app.minus.presentation.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.serranoie.app.minus.presentation.ui.theme.harmonize.blend.Blend
import com.serranoie.app.minus.presentation.ui.theme.harmonize.hct.Hct
import com.serranoie.app.minus.presentation.ui.theme.harmonize.palette.CorePalette
import com.serranoie.app.minus.presentation.ui.theme.isNightMode

/**
 * Linearly interpolates between two colors in sRGB space.
 *
 * @param from Start color (returned when t = 0)
 * @param to   End color (returned when t = 1)
 * @param t    Blend weight in [0, 1] — 0.5 gives a 50/50 mix.
 */
fun combineColors(from: Color, to: Color, t: Float = 0.5f): Color = Color(
    red = from.red + (to.red - from.red) * t,
    green = from.green + (to.green - from.green) * t,
    blue = from.blue + (to.blue - from.blue) * t,
)

/**
 * Picks a position in [colors] using [t] in [0, 1] and LERPs between the two
 * adjacent entries. With 3 colors `[a, b, c]`, t = 0 returns `a`, t = 0.5
 * returns `b`, t = 1 returns `c`, with smooth blending in between.
 *
 * @param colors At least 2 entries.
 * @param t      Position in [0, 1] — 0 picks the first, 1 picks the last.
 */
fun combineColors(colors: List<Color>, t: Float = 0.5f): Color {
    require(colors.size >= 2) { "combineColors(colors) needs at least 2 entries, got ${colors.size}" }
    val lastIndex = colors.size - 1
    val position = (lastIndex * t).coerceIn(0f, lastIndex.toFloat())
    val lower = position.toInt()
    val upper = (lower + 1).coerceAtMost(lastIndex)
    val localT = position - lower
    return combineColors(colors[lower], colors[upper], localT)
}

/**
 * Shifts [designColor]'s hue towards [sourceColor]'s hue so the result feels
 * related to the app's primary palette. Used for chart bars, badge colors,
 * and other accent surfaces that should sit in the same family as the theme.
 *
 * @param designColor       The arbitrary color the caller wants to bring into the theme.
 * @param sourceColor       The hue target (defaults to `MaterialTheme.colorScheme.primary`).
 * @param chromaMultiplier  Scale the output chroma by this factor. 1.0 leaves
 *   the harmonized color as-is; values > 1.0 boost saturation (used by
 *   `CategoriesChartCard` to keep dark-mode chart bars vivid). Defaults to 1.0.
 */
@Composable
fun harmonize(
    designColor: Color,
    sourceColor: Color = MaterialTheme.colorScheme.primary,
    chromaMultiplier: Float = 1.0f,
): Color = harmonizeWithColor(designColor, sourceColor, chromaMultiplier)

fun harmonizeWithColor(
    designColor: Color,
    sourceColor: Color,
    chromaMultiplier: Float = 1.0f,
): Color {
    val harmonized = Blend.harmonize(designColor.toArgb(), sourceColor.toArgb())
    if (chromaMultiplier == 1.0f) return Color(harmonized)

    val hct = Hct.fromInt(harmonized)
    return Color(Hct.from(hct.hue, hct.chroma * chromaMultiplier, hct.tone).toInt())
}

@Composable
fun toPalette(color: Color, darkTheme: Boolean = isNightMode()): HarmonizedColorPalette =
    corePaletteFor(color, darkTheme)

fun toPaletteWithTheme(color: Color, darkTheme: Boolean): HarmonizedColorPalette =
    corePaletteFor(color, darkTheme)

/**
 * Material-3-style role bundle derived from a single seed color by running it
 * through the HCT → CorePalette pipeline. Mirrors a subset of
 * `androidx.compose.material3.ColorScheme` so chart/card composables can swap
 * themed colors in a Material-friendly way without going through the full
 * `colorScheme` object.
 */
data class HarmonizedColorPalette(
    val main: Color,
    val onMain: Color,
    val container: Color,
    val onContainer: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
)

/**
 * Derives an 8-role Material-like palette from a seed color. The seed flows
 * through the HCT tonal-palette pipeline twice — `contentOf` gives the
 * neutral/contrast tones, `of` gives the accent tones — so the generated roles
 * have a coherent hue family.
 *
 * The light/dark switch is just a tone remap: same code path, opposite
 * brightness per role.
 */
private fun corePaletteFor(color: Color, dark: Boolean): HarmonizedColorPalette {
    val content = CorePalette.contentOf(color.toArgb())
    val accent = CorePalette.of(color.toArgb())
    return if (dark) {
        HarmonizedColorPalette(
            main = Color(accent.a1.tone(40)),
            onMain = Color(content.a1.tone(30)),
            container = Color(accent.a1.tone(30)),
            onContainer = Color(content.a1.tone(90)),
            surface = Color(accent.n1.tone(10)),
            onSurface = Color(content.n1.tone(90)),
            surfaceVariant = Color(accent.n1.tone(30)),
            onSurfaceVariant = Color(content.n1.tone(80)),
        )
    } else {
        HarmonizedColorPalette(
            main = Color(accent.seed.toInt()),
            onMain = Color(content.a1.tone(10)),
            container = Color(accent.a1.tone(90)),
            onContainer = Color(content.a1.tone(10)),
            surface = Color(accent.n1.tone(99)),
            onSurface = Color(content.n1.tone(10)),
            surfaceVariant = Color(accent.n1.tone(90)),
            onSurfaceVariant = Color(content.n1.tone(30)),
        )
    }
}
