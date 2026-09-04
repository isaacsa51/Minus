package com.serranoie.app.minus.presentation.ui.theme.component.budget.pill

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.presentation.util.font.calcAdaptiveFont

/**
 * Render each number from budget amount as individual segmented text.
 */
@Composable
internal fun SegmentedAmountText(
    text: String,
    style: TextStyle,
    color: Color,
    minFontSize: TextUnit,
    currencySymbol: String,
    symbolAtEnd: Boolean,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    fillWidth: Boolean = true,
) {
    val sign = if (text.startsWith("-")) "-" else ""
    val unsigned = text.removePrefix("-")
    val body = when {
        currencySymbol.isNotEmpty() && !symbolAtEnd && unsigned.startsWith(currencySymbol) ->
            unsigned.removePrefix(currencySymbol)

        currencySymbol.isNotEmpty() && symbolAtEnd && unsigned.endsWith(currencySymbol) ->
            unsigned.removeSuffix(currencySymbol)

        else -> unsigned
    }
    val splitStyling = currencySymbol.length > 2

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val availableWidth = with(density) { maxWidth.toPx() }
        val maxFontSize = style.fontSize.takeIf { it != TextUnit.Unspecified }
            ?: MaterialTheme.typography.bodyLarge.fontSize

        val widthProbe = buildString {
            append(sign)
            if (currencySymbol.isNotEmpty() && !symbolAtEnd) append(currencySymbol)
            append(body)
            if (currencySymbol.isNotEmpty() && symbolAtEnd) append(currencySymbol)
        }
        val fontSize = calcAdaptiveFont(
            height = with(density) { maxFontSize.toPx() },
            width = availableWidth,
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
            text = widthProbe,
            style = style,
        )
        val bodyStyle = if (splitStyling) {
            style.copy(fontSize = fontSize, fontWeight = FontWeight.Light)
        } else {
            style.copy(fontSize = fontSize)
        }
        val glyphStyle = bodyStyle.copy(letterSpacing = 0.sp, fontFeatureSettings = "tnum")
        val symbolTextStyle = if (splitStyling) {
            style.copy(
                fontSize = fontSize * 0.75f,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
        } else {
            glyphStyle
        }

        val glyphGap = (-1).dp

        Row(
            modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier.wrapContentWidth())
                .wrapContentHeight()
                .animateContentSize(animationSpec = tween(180)),
            horizontalArrangement = when (textAlign) {
                TextAlign.End -> Arrangement.spacedBy(glyphGap, Alignment.End)
                TextAlign.Start -> Arrangement.spacedBy(glyphGap, Alignment.Start)
                else -> Arrangement.spacedBy(glyphGap, Alignment.CenterHorizontally)
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (sign.isNotEmpty()) {
                Text(sign, style = glyphStyle, color = color, maxLines = 1)
            }
            if (currencySymbol.isNotEmpty() && !symbolAtEnd) {
                Text(currencySymbol, style = symbolTextStyle, color = color, maxLines = 1)
            }

            val chars = body.toCharArray()
            chars.forEachIndexed { index, ch ->
                key(chars.size - index) {
                    AnimatedContent(
                        targetState = ch,
                        contentAlignment = Alignment.Center,
                        transitionSpec = {
                            (fadeIn(tween(200)) +
                                scaleIn(initialScale = 0.6f, animationSpec = tween(200))) togetherWith
                                (fadeOut(tween(140)) +
                                    scaleOut(targetScale = 0.6f, animationSpec = tween(140))) using
                                SizeTransform(clip = false)
                        },
                        label = "amountGlyph",
                    ) { glyph ->
                        Text(
                            text = glyph.toString(),
                            style = glyphStyle,
                            color = color,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (currencySymbol.isNotEmpty() && symbolAtEnd) {
                Text(currencySymbol, style = symbolTextStyle, color = color, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun AdaptiveSingleLineText(
    text: String,
    style: TextStyle,
    color: Color = LocalContentColor.current,
    minFontSize: TextUnit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    fillWidth: Boolean = true,
    annotatedText: AnnotatedString? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val availableWidth = with(density) { maxWidth.toPx() }
        val maxFontSize = style.fontSize.takeIf { it != TextUnit.Unspecified }
            ?: MaterialTheme.typography.bodyLarge.fontSize

        val adaptiveFontSize = calcAdaptiveFont(
            height = with(density) { maxFontSize.toPx() },
            width = availableWidth,
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
            text = text,
            style = style,
        )

        Text(
            text = annotatedText ?: AnnotatedString(text),
            style = style.copy(fontSize = adaptiveFontSize),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = textAlign,
            modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .basicMarquee()
                .wrapContentHeight()
                .align(Alignment.Center),
        )
    }
}
