package com.serranoie.app.minus.presentation.ui.editor.receipt

import androidx.compose.ui.unit.IntRect
import kotlin.math.ceil
import kotlin.math.floor

const val RECEIPT_FRAME_FRACTION = 0.72f

fun receiptFrameCrop(imageWidth: Int, imageHeight: Int, viewWidth: Int, viewHeight: Int): IntRect? {
    if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return null

    val fillScale = maxOf(viewWidth.toFloat() / imageWidth, viewHeight.toFloat() / imageHeight)
    val half = minOf(viewWidth, viewHeight) * RECEIPT_FRAME_FRACTION / fillScale / 2f
    val centerX = imageWidth / 2f
    val centerY = imageHeight / 2f

    return IntRect(
        left = floor(centerX - half).toInt().coerceAtLeast(0),
        top = floor(centerY - half).toInt().coerceAtLeast(0),
        right = ceil(centerX + half).toInt().coerceAtMost(imageWidth),
        bottom = ceil(centerY + half).toInt().coerceAtMost(imageHeight),
    )
}
