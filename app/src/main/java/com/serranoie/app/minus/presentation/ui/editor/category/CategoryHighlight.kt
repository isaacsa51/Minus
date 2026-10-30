package com.serranoie.app.minus.presentation.ui.editor.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.domain.calculator.detectedCategoryRanges

private data class CategoryHighlightTransformation(
    val tags: List<String>,
    val category: SpanStyle,
    val separator: SpanStyle,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val decorated = buildAnnotatedString {
            append(spacedPluses(original))
            detectedCategoryRanges(original, tags).forEach {
                addStyle(category, displayOffset(original, it.first), displayOffset(original, it.last + 1))
            }
            original.forEachIndexed { index, char ->
                if (char == '+') addStyle(separator, displayOffset(original, index), displayOffset(original, index) + 3)
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = displayOffset(original, offset)
            override fun transformedToOriginal(offset: Int): Int = originalOffset(original, offset)
        }
        return TransformedText(decorated, mapping)
    }
}

@Composable
fun rememberCategoryHighlight(tags: List<String>): VisualTransformation {
    val colors = MaterialTheme.colorScheme
    return remember(tags, colors) {
        CategoryHighlightTransformation(
            tags = tags,
            category = SpanStyle(color = colors.onSecondaryContainer),
            separator = SpanStyle(color = colors.outline, letterSpacing = 3.sp),
        )
    }
}

fun Modifier.categoryPills(
    text: String,
    tags: List<String>,
    layout: () -> TextLayoutResult?,
    color: Color,
): Modifier = drawBehind {
    val result = layout() ?: return@drawBehind
    if (result.size.width > size.width + 1f) return@drawBehind
    val padX = 4.dp.toPx()
    val padY = 1.dp.toPx()
    detectedCategoryRanges(text, tags).forEach { range ->
        val start = displayOffset(text, range.first)
        val end = displayOffset(text, range.last + 1)
        if (end > result.layoutInput.text.length) return@forEach
        val bounds = result.getPathForRange(start, end).getBounds()
        val height = bounds.height + padY * 2
        drawRoundRect(
            color = color,
            topLeft = Offset(bounds.left - padX, bounds.top - padY),
            size = Size(bounds.width + padX * 2, height),
            cornerRadius = CornerRadius(height / 2),
        )
    }
}

internal fun spacedPluses(text: String): String = text.replace("+", " + ")

internal fun displayOffset(text: String, offset: Int): Int =
    offset + 2 * text.take(offset).count { it == '+' }

internal fun originalOffset(text: String, offset: Int): Int {
    var original = 0
    while (original < text.length && displayOffset(text, original + 1) <= offset) original++
    return original
}
