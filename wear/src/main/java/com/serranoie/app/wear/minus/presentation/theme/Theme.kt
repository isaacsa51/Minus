package com.serranoie.app.wear.minus.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.MotionScheme
import androidx.wear.compose.material3.Shapes

@Composable
fun MinusTheme(
	content: @Composable () -> Unit
) {
	/**
	 * Empty theme to customize for your app.
	 * See: https://developer.android.com/jetpack/compose/designsystems/custom
	 */
	MaterialTheme(
        colorScheme = ColorScheme(),
        shapes = Shapes(),
        motionScheme = MotionScheme.standard(),
        content = content
    )
}