package com.serranoie.app.minus.presentation.util

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val LocalCensorMode = compositionLocalOf { false }

@Composable
fun Modifier.censor(
	enabled: Boolean = true,
	radius: Dp = 12.dp
): Modifier {
	val isCensored = LocalCensorMode.current
	if (!enabled || !isCensored) return this

	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
		this.blur(radius)
	} else {
		// Fallback for older versions: use a background color that masks content
		this.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
	}
}
