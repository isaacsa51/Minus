package com.serranoie.app.minus.presentation.ui.editor.receipt

import com.serranoie.app.minus.domain.receipt.ReceiptScan

data class ReceiptCameraUiState(
    val detected: ReceiptScan? = null,
    val isCapturing: Boolean = false,
    val captureFailed: Boolean = false,
) {
    fun withFrame(scan: ReceiptScan?): ReceiptCameraUiState =
        if (scan == null || isCapturing) this else copy(detected = scan, captureFailed = false)

    fun capturing(): ReceiptCameraUiState = copy(isCapturing = true, captureFailed = false)

    fun captured(scan: ReceiptScan?): Pair<ReceiptCameraUiState, ReceiptScan?> {
        val result = scan ?: detected
        return copy(isCapturing = false, captureFailed = result == null) to result
    }
}
