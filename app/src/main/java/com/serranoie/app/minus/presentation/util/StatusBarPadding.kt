package com.serranoie.app.minus.presentation.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.serranoie.app.minus.presentation.LocalWindowInsets

@Composable
fun StatusBarPadding() {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.requiredHeight(
				LocalWindowInsets.current.calculateTopPadding()
			)
	)
}
