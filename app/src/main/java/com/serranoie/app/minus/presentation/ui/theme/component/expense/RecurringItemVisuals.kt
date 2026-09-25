package com.serranoie.app.minus.presentation.ui.theme.component.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.charts.baseColors
import com.serranoie.app.minus.presentation.ui.theme.isNightMode
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.util.HarmonizedColorPalette
import com.serranoie.app.minus.presentation.util.harmonizeWithColor
import com.serranoie.app.minus.presentation.util.toPaletteWithTheme

/**
 * Distinct per-transaction color: reuses [baseColors] and the hue-rotation + theme-harmonization
 * approach from [com.serranoie.app.minus.presentation.ui.theme.component.charts.CategoriesChartCard]
 * so recurring items landing on the same calendar day (and their matching avatar) are visually
 * distinguishable instead of cycling through a handful of fixed Material roles. Shared between
 * the subscriptions screen and the recurring-item rows used elsewhere (e.g. History).
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

/**
 * Lettered avatar chip for a recurring-item row — the [subscriptionPalette]-derived
 * container/onContainer pair keeps it legible in both themes, unlike a bare hue on white text.
 */
@Composable
internal fun SubscriptionAvatar(
    label: String,
    transactionId: Long,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
) {
    val palette = subscriptionPalette(transactionId)
    Box(
        modifier = modifier
            .size(size)
            .background(palette.container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMediumEmphasized,
            color = palette.onContainer,
        )
    }
}

@Composable
internal fun StatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmallCondensed,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun RecurringItemVisualsPreview() {
    MinusTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Netflix", "Spotify", "Gym", "iCloud+", "Disney+").forEachIndexed { index, name ->
                SubscriptionAvatar(label = name.first().uppercaseChar().toString(), transactionId = index.toLong())
            }
        }
    }
}
