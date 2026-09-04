package com.serranoie.app.minus.presentation.ui.theme.component.numpad

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

private const val DEFAULT_PRESSED_MULTIPLIER = 1.1f
private const val MIN_WEIGHT = 0.001f

class ExpandableRowState(
    val interactionSources: List<MutableInteractionSource>,
    val animatedWeights: List<Float>,
) {
    fun getInteractionSource(index: Int): MutableInteractionSource =
        interactionSources.getOrElse(index) { MutableInteractionSource() }

    fun getWeight(index: Int): Float =
        animatedWeights.getOrElse(index) { 1f }
}

@Composable
fun rememberExpandableRowState(
    itemCount: Int,
    baseWeights: List<Float> = List(itemCount) { 1f },
    pressedMultiplier: Float = DEFAULT_PRESSED_MULTIPLIER,
    animationSpec: AnimationSpec<Float>? = null,
): ExpandableRowState {
    val sources = remember(itemCount) {
        List(itemCount) { MutableInteractionSource() }
    }

    val animatedWeights = List(itemCount) { index ->
        val isPressed by sources[index].collectIsPressedAsState()
        val baseWeight = baseWeights.getOrElse(index) { 1f }
        val targetWeight = if (isPressed) baseWeight * pressedMultiplier else baseWeight
        val spec = animationSpec ?: if (isPressed) {
            tween(durationMillis = 60, easing = LinearOutSlowInEasing)
        } else {
            tween(durationMillis = 300, easing = LinearOutSlowInEasing)
        }
        val animatedWeight by animateFloatAsState(
            targetValue = targetWeight,
            animationSpec = spec,
            label = "ExpandableWeight_$index",
        )
        animatedWeight
    }

    return remember(sources, animatedWeights) {
        ExpandableRowState(sources, animatedWeights)
    }
}

@Composable
fun <T> ExpandableRow(
    items: List<T>,
    modifier: Modifier = Modifier,
    baseWeights: List<Float>? = null,
    pressedMultiplier: Float = DEFAULT_PRESSED_MULTIPLIER,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    animationSpec: AnimationSpec<Float>? = null,
    content: @Composable RowScope.(index: Int, item: T, interactionSource: MutableInteractionSource) -> Unit,
) {
    val state = rememberExpandableRowState(
        itemCount = items.size,
        baseWeights = baseWeights ?: List(items.size) { 1f },
        pressedMultiplier = pressedMultiplier,
        animationSpec = animationSpec,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
    ) {
        val rowScope = this
        items.forEachIndexed { index, item ->
            Box(
                modifier = Modifier.weight(state.getWeight(index).coerceAtLeast(MIN_WEIGHT)),
            ) {
                rowScope.content(index, item, state.getInteractionSource(index))
            }
        }
    }
}

@Composable
fun ExpandableRow(
    itemCount: Int,
    modifier: Modifier = Modifier,
    baseWeights: List<Float> = List(itemCount) { 1f },
    pressedMultiplier: Float = DEFAULT_PRESSED_MULTIPLIER,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    animationSpec: AnimationSpec<Float>? = null,
    content: @Composable RowScope.(index: Int, interactionSource: MutableInteractionSource) -> Unit,
) {
    val state = rememberExpandableRowState(
        itemCount = itemCount,
        baseWeights = baseWeights,
        pressedMultiplier = pressedMultiplier,
        animationSpec = animationSpec,
    )

    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
    ) {
        val rowScope = this
        for (i in 0 until itemCount) {
            Box(
                modifier = Modifier.weight(state.getWeight(i).coerceAtLeast(MIN_WEIGHT)),
            ) {
                rowScope.content(i, state.getInteractionSource(i))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpandableRowPreview() {
    MinusTheme {
        Box(Modifier.padding(16.dp)) {
            ExpandableRow(
                items = listOf("7", "8", "9"),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) { _, text, interactionSource ->
                NumpadButton(
                    modifier = Modifier.padding(BUTTON_GAP),
                    type = NumpadButtonType.DEFAULT,
                    text = text,
                    interactionSource = interactionSource,
                )
            }
        }
    }
}
