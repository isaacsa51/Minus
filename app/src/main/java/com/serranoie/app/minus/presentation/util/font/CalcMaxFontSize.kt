package com.serranoie.app.minus.presentation.util.font

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@Composable
fun calcMaxFont(
    height: Float,
    text: String = "SAMPLE 1234567890",
    style: TextStyle = MaterialTheme.typography.displayLarge,
): TextUnit {
    val measureFontSize = 100.sp

    val intrinsics = ParagraphIntrinsics(
        text = text,
        style = style.copy(fontSize = measureFontSize),
        density = LocalDensity.current,
        fontFamilyResolver = createFontFamilyResolver(LocalContext.current)
    )

    val paragraph = Paragraph(
        paragraphIntrinsics = intrinsics,
        constraints = Constraints(maxWidth = ceil(1000f).toInt()),
        maxLines = 1,
        overflow = TextOverflow.Clip
    )

    return with(LocalDensity.current) {
        ((measureFontSize.toPx() / paragraph.firstBaseline) * height).toSp()
    }
}

@Composable
fun calcAdaptiveFont(
    height: Float,
    width: Float,
    minFontSize: TextUnit,
    maxFontSize: TextUnit,
    text: String = "SAMPLE 1234567890",
    style: TextStyle = MaterialTheme.typography.displayLarge,
): TextUnit {
    if (width <= 0f) return minFontSize
    if (text.isEmpty()) return maxFontSize

    val density = LocalDensity.current
    val resolver = createFontFamilyResolver(LocalContext.current)
    val minPx = with(density) { minFontSize.toPx() }
    val maxPx = with(density) { maxFontSize.toPx() }

    fun fits(fontPx: Float): Boolean {
        if (fontPx <= 0f) return true
        val fontSp = with(density) { fontPx.toSp() }
        val intrinsics = ParagraphIntrinsics(
            text = text,
            style = style.copy(fontSize = fontSp),
            density = density,
            fontFamilyResolver = resolver
        )
        return intrinsics.maxIntrinsicWidth <= width
    }

    var low = minPx
    var high = maxPx
    var best = minPx

    // Binary search for the largest font that fits width.
    repeat(14) {
        val mid = (low + high) / 2f
        if (fits(mid)) {
            best = mid
            low = mid
        } else {
            high = mid
        }
    }

    return with(density) { best.toSp() }
}

@Stable
fun min(a: TextUnit, b: TextUnit): TextUnit = min(a.value, b.value).sp

@Stable
fun max(a: TextUnit, b: TextUnit): TextUnit = max(a.value, b.value).sp
