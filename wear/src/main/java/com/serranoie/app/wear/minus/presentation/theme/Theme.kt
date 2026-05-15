package com.serranoie.app.wear.minus.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.MotionScheme

private val primaryLight = Color(0xFF516526)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFD4EC9E)
private val onPrimaryContainerLight = Color(0xFF3A4D10)
private val secondaryLight = Color(0xFF5A6147)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFDEE6C5)
private val onSecondaryContainerLight = Color(0xFF424A31)
private val tertiaryLight = Color(0xFF656015)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFEDE68C)
private val onTertiaryContainerLight = Color(0xFF4D4800)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFFAFAEE)
private val onBackgroundLight = Color(0xFF1A1C15)
private val onSurfaceLight = Color(0xFF1A1C15)
private val onSurfaceVariantLight = Color(0xFF45483C)
private val outlineLight = Color(0xFF76786B)
private val outlineVariantLight = Color(0xFFC6C8B9)
private val surfaceContainerLowLight = Color(0xFFF4F4E8)
private val surfaceContainerLight = Color(0xFFEEEFE3)
private val surfaceContainerHighLight = Color(0xFFE9E9DD)

private val primaryDark = Color(0xFFB8CF84)
private val onPrimaryDark = Color(0xFF253500)
private val primaryContainerDark = Color(0xFF3A4D10)
private val onPrimaryContainerDark = Color(0xFFD4EC9E)
private val secondaryDark = Color(0xFFC2CAAA)
private val onSecondaryDark = Color(0xFF2C331D)
private val secondaryContainerDark = Color(0xFF424A31)
private val onSecondaryContainerDark = Color(0xFFDEE6C5)
private val tertiaryDark = Color(0xFFD0C973)
private val onTertiaryDark = Color(0xFF353200)
private val tertiaryContainerDark = Color(0xFF4D4800)
private val onTertiaryContainerDark = Color(0xFFEDE68C)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF12140D)
private val onBackgroundDark = Color(0xFFE3E3D7)
private val onSurfaceDark = Color(0xFFE3E3D7)
private val onSurfaceVariantDark = Color(0xFFC6C8B9)
private val outlineDark = Color(0xFF909284)
private val outlineVariantDark = Color(0xFF45483C)
private val surfaceContainerLowDark = Color(0xFF1A1C15)
private val surfaceContainerDark = Color(0xFF1E2019)
private val surfaceContainerHighDark = Color(0xFF292B23)

private val lightScheme = ColorScheme(
	primary = primaryLight,
	primaryDim = primaryLight,
	primaryContainer = primaryContainerLight,
	onPrimary = onPrimaryLight,
	onPrimaryContainer = onPrimaryContainerLight,
	secondary = secondaryLight,
	secondaryDim = secondaryLight,
	secondaryContainer = secondaryContainerLight,
	onSecondary = onSecondaryLight,
	onSecondaryContainer = onSecondaryContainerLight,
	tertiary = tertiaryLight,
	tertiaryDim = tertiaryLight,
	tertiaryContainer = tertiaryContainerLight,
	onTertiary = onTertiaryLight,
	onTertiaryContainer = onTertiaryContainerLight,
	surfaceContainerLow = surfaceContainerLowLight,
	surfaceContainer = surfaceContainerLight,
	surfaceContainerHigh = surfaceContainerHighLight,
	onSurface = onSurfaceLight,
	onSurfaceVariant = onSurfaceVariantLight,
	outline = outlineLight,
	outlineVariant = outlineVariantLight,
	background = backgroundLight,
	onBackground = onBackgroundLight,
	error = errorLight,
	errorDim = errorLight,
	errorContainer = errorContainerLight,
	onError = onErrorLight,
	onErrorContainer = onErrorContainerLight,
)

private val darkScheme = ColorScheme(
	primary = primaryDark,
	primaryDim = primaryDark,
	primaryContainer = primaryContainerDark,
	onPrimary = onPrimaryDark,
	onPrimaryContainer = onPrimaryContainerDark,
	secondary = secondaryDark,
	secondaryDim = secondaryDark,
	secondaryContainer = secondaryContainerDark,
	onSecondary = onSecondaryDark,
	onSecondaryContainer = onSecondaryContainerDark,
	tertiary = tertiaryDark,
	tertiaryDim = tertiaryDark,
	tertiaryContainer = tertiaryContainerDark,
	onTertiary = onTertiaryDark,
	onTertiaryContainer = onTertiaryContainerDark,
	surfaceContainerLow = surfaceContainerLowDark,
	surfaceContainer = surfaceContainerDark,
	surfaceContainerHigh = surfaceContainerHighDark,
	onSurface = onSurfaceDark,
	onSurfaceVariant = onSurfaceVariantDark,
	outline = outlineDark,
	outlineVariant = outlineVariantDark,
	background = backgroundDark,
	onBackground = onBackgroundDark,
	error = errorDark,
	errorDim = errorDark,
	errorContainer = errorContainerDark,
	onError = onErrorDark,
	onErrorContainer = onErrorContainerDark,
)

@Composable
fun MinusTheme(
	darkTheme: Boolean = isSystemInDarkTheme(),
	content: @Composable () -> Unit
) {
	MaterialTheme(
		colorScheme = darkScheme,
		motionScheme = MotionScheme.expressive(),
		content = content,
	)
}
