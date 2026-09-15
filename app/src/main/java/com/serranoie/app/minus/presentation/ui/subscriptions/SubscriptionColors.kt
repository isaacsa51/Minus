package com.serranoie.app.minus.presentation.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.charts.baseColors
import com.serranoie.app.minus.presentation.ui.theme.isNightMode
import com.serranoie.app.minus.presentation.util.HarmonizedColorPalette
import com.serranoie.app.minus.presentation.util.harmonizeWithColor
import com.serranoie.app.minus.presentation.util.toPaletteWithTheme

/**
 * Distinct per-subscription color: reuses [baseColors] and the hue-rotation +
 * theme-harmonization approach from
 * [com.serranoie.app.minus.presentation.ui.theme.component.charts.CategoriesChartCard] so charges
 * landing on the same calendar day (and their matching avatar) are visually distinguishable
 * instead of cycling through a handful of fixed Material roles.
 */
@Composable
internal fun subscriptionPalette(transactionId: Long): HarmonizedColorPalette {
    val primaryColor = MaterialTheme.colorScheme.primary
    val nightMode = isNightMode()
    return remember(transactionId, primaryColor, nightMode) {
        val baseSize = baseColors.size
        val index = Math.floorMod(transactionId, baseSize.toLong()).toInt()
        val iteration = Math.floorMod(transactionId / baseSize, baseSize.toLong())
        var seedColor = baseColors[index]
        if (iteration > 0) {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(seedColor.toArgb(), hsl)
            hsl[0] = (hsl[0] + (iteration * 137.5f)) % 360f
            seedColor = Color(ColorUtils.HSLToColor(hsl))
        }
        toPaletteWithTheme(
            color = harmonizeWithColor(
                designColor = seedColor,
                sourceColor = primaryColor,
                chromaMultiplier = if (nightMode) 2f else 1f,
            ),
            darkTheme = nightMode,
        )
    }
}

@Composable
internal fun RecurringItemIndicator(
    label: String,
    transactionId: Long,
    modifier: Modifier = Modifier,
    backgroundOverride: Color? = null,
    contentColorOverride: Color? = null,
) {
    val palette = subscriptionPalette(transactionId)
    Box(
        modifier = modifier
            .size(8.dp)
            .background(backgroundOverride ?: palette.main, CircleShape),
        contentAlignment = Alignment.Center,
    ) { }
}

/** Several transaction ids side by side — shows the hue-rotation kicking in past [baseColors]'s own size. */
@PreviewLightDark
@Composable
private fun RecurringItemIndicatorPreview() {
    MinusTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (0L until 8L).forEach { id ->
                RecurringItemIndicator(label = "", transactionId = id)
            }
        }
    }
}
