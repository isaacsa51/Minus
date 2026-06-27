package com.serranoie.app.minus.presentation.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.serranoie.app.minus.presentation.ui.theme.harmonize.blend.Blend
import com.serranoie.app.minus.presentation.ui.theme.harmonize.hct.Hct
import com.serranoie.app.minus.presentation.ui.theme.harmonize.palette.CorePalette
import com.serranoie.app.minus.presentation.ui.theme.isNightMode
import kotlin.math.ceil
import kotlin.math.floor

fun combineColors(colorA: Color, colorB: Color, angle: Float = 0.5F): Color {
	val colorAPart = (1F - angle) * 2
	val colorBPart = angle * 2

	return Color(
		red = (colorA.red * colorAPart + colorB.red * colorBPart) / 2,
		green = (colorA.green * colorAPart + colorB.green * colorBPart) / 2,
		blue = (colorA.blue * colorAPart + colorB.blue * colorBPart) / 2,
	)
}

fun combineColors(colors: List<Color>, angle: Float = 0.5F): Color {
	val approximateIndex = (colors.size - 1) * angle
	val colorA = colors[floor(approximateIndex).toInt()]
	val colorB = colors[ceil(approximateIndex).toInt()]

	return combineColors(colorA, colorB, approximateIndex - floor(approximateIndex))
}

@Composable
fun harmonize(
	designColor: Color,
	sourceColor: Color = MaterialTheme.colorScheme.primary,
	chromaFactor: Float = 1.0f
): Color {
	return harmonizeWithColor(designColor, sourceColor, chromaFactor)
}

fun harmonizeWithColor(designColor: Color, sourceColor: Color, chromaFactor: Float = 1.0f): Color {
	val harmonized = Blend.harmonize(designColor.toArgb(), sourceColor.toArgb())
	if (chromaFactor == 1.0f) return Color(harmonized)

	val hct = Hct.fromInt(harmonized)
	return Color(Hct.from(hct.hue, hct.chroma * chromaFactor, hct.tone).toInt())
}

@Composable
fun toPalette(color: Color, darkTheme: Boolean = isNightMode()): HarmonizedColorPalette {
	return toPaletteWithTheme(color, darkTheme)
}

fun toPaletteWithTheme(color: Color, darkTheme: Boolean): HarmonizedColorPalette {
	return if (darkTheme) {
		darkFromCorePalette(color)
	} else {
		lightFromCorePalette(color)
	}
}

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

fun lightFromCorePalette(color: Color): HarmonizedColorPalette {
	val contentOfPalette = CorePalette.contentOf(color.toArgb())
	val ofPalette = CorePalette.of(color.toArgb())

	return HarmonizedColorPalette(
		main = Color(ofPalette.seed.toInt()),
		onMain = Color(contentOfPalette.a1.tone(10)),
		container = Color(ofPalette.a1.tone(90)),
		onContainer = Color(contentOfPalette.a1.tone(10)),
		surface = Color(ofPalette.n1.tone(99)),
		onSurface = Color(contentOfPalette.n1.tone(10)),
		surfaceVariant = Color(ofPalette.n1.tone(90)),
		onSurfaceVariant = Color(contentOfPalette.n1.tone(30)),
	)
}

private fun darkFromCorePalette(color: Color): HarmonizedColorPalette {
	val contentOfPalette = CorePalette.contentOf(color.toArgb())
	val ofPalette = CorePalette.of(color.toArgb())

	return HarmonizedColorPalette(
		main = Color(ofPalette.a1.tone(40)),
		onMain = Color(contentOfPalette.a1.tone(30)),
		container = Color(ofPalette.a1.tone(30)),
		onContainer = Color(contentOfPalette.a1.tone(90)),
		surface = Color(ofPalette.n1.tone(10)),
		onSurface = Color(contentOfPalette.n1.tone(90)),
		surfaceVariant = Color(ofPalette.n1.tone(30)),
		onSurfaceVariant = Color(contentOfPalette.n1.tone(80)),
	)
}
