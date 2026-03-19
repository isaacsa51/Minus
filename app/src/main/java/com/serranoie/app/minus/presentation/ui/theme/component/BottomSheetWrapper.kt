package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.LocalWindowInsets
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class BottomSheetScrollState(
	val topPadding: Dp,
)

val LocalBottomSheetScrollState = compositionLocalOf { BottomSheetScrollState(0.dp) }

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetWrapper(
	name: String,
	cancelable: Boolean = true,
	state: ModalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
	content: @Composable (state: ModalBottomSheetState) -> Unit
) {
	val coroutineScope = rememberCoroutineScope()
	val localDensity = LocalDensity.current
	val statusBarHeight = LocalWindowInsets.current.calculateTopPadding()
	val statusBarFillProgress = if (statusBarHeight == 0.dp) {
		0F
	} else {
		with(localDensity) {
			0.toDp()
		} / statusBarHeight
	}.coerceIn(0f, 1f)

	val focusManager = LocalFocusManager.current

	LaunchedEffect(Unit) {
		focusManager.clearFocus()
	}

	var predictiveBackProgress by remember {
		mutableFloatStateOf(0f)
	}

	ModalBottomSheetLayout(
		sheetBackgroundColor = MaterialTheme.colorScheme.surface,
		sheetState = state,
		sheetShape = MaterialTheme.shapes.extraLarge.copy(
			bottomStart = CornerSize(0.dp),
			bottomEnd = CornerSize(0.dp),
			topStart = CornerSize(28.dp * (1F - statusBarFillProgress)),
			topEnd = CornerSize(28.dp * (1F - statusBarFillProgress)),
		),
		sheetContent = {
			Box {
				CompositionLocalProvider(
					LocalBottomSheetScrollState provides BottomSheetScrollState(
						topPadding = statusBarHeight * statusBarFillProgress,
					)
				) {
					content(state)
				}
			}

			if (cancelable) {
				Box(
					Modifier
						.padding(8.dp)
						.fillMaxWidth()
				) {
					Box(
						Modifier
							.height(4.dp)
							.width(30.dp)
							.background(
								color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
								shape = CircleShape
							)
							.align(Alignment.Center)
					)
				}
			}
		}) {}

	PredictiveBackHandler(state.isVisible) { progress ->
		try {
			progress.collect { backEvent ->
				predictiveBackProgress = backEvent.progress
			}

			coroutineScope.launch {
				// TODO: Close sheet correctly
			}
		} catch (e: CancellationException) {
			predictiveBackProgress = 0f
		}
	}
}
