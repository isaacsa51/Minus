package com.serranoie.app.minus.presentation.ui.theme.component.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.serranoie.app.minus.LocalWindowInsets

@Composable
fun WalletStatusBarStub() {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.requiredHeight(
				LocalWindowInsets.current.calculateTopPadding()
			)
			.background(androidx.compose.ui.graphics.Color.Transparent)
	)
}