package com.serranoie.app.minus.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.serranoie.app.minus.presentation.util.combineColors

val seedColor = Color(0xFFAFD367)
val colorSuperBad = Color(0xFFC70909)
val colorMin = Color(0xFF185ED6)
val colorMax = Color(0xFFDD1414)

val colorPrimary
	@Composable
	@ReadOnlyComposable
	get() = MaterialTheme.colorScheme.primary

val colorBackground
	@Composable
	@ReadOnlyComposable
	get() = MaterialTheme.colorScheme.surfaceVariant

val colorButton
	@Composable
	@ReadOnlyComposable
	get() = combineColors(
		combineColors(
			MaterialTheme.colorScheme.secondaryContainer,
			MaterialTheme.colorScheme.surfaceVariant,
			0.76F
		),
		MaterialTheme.colorScheme.surface,
		0.68F
	)

val colorOnButton
	@Composable
	@ReadOnlyComposable
	get() = combineColors(
		MaterialTheme.colorScheme.onSurfaceVariant,
		MaterialTheme.colorScheme.onSurface,
		0.56F
	)
val colorEditor
	@Composable
	@ReadOnlyComposable
	get() = combineColors(
		MaterialTheme.colorScheme.surfaceVariant,
		MaterialTheme.colorScheme.surfaceVariant,
		0.56F,
	)

val colorOnEditor
	@Composable
	@ReadOnlyComposable
	get() = MaterialTheme.colorScheme.onSurface

// Budget status colors
val colorGood
	@Composable
	@ReadOnlyComposable
	get() = Color(0xFF81C784) // Green

val colorNotGood
	@Composable
	@ReadOnlyComposable
	get() = Color(0xFFFFB74D) // Orange

val colorBad
	@Composable
	@ReadOnlyComposable
	get() = Color(0xFFE57373) // Red

