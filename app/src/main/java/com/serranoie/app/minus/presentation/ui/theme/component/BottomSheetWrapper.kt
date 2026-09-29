package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class BottomSheetScrollState(
    val topPadding: Dp,
)

val LocalBottomSheetScrollState = compositionLocalOf { BottomSheetScrollState(0.dp) }
