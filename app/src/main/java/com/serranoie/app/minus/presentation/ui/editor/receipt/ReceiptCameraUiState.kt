package com.serranoie.app.minus.presentation.ui.editor.receipt

import com.serranoie.app.minus.domain.receipt.ReceiptScan

data class ReceiptCameraUiState(
    val detected: ReceiptScan? = null,
    val isCapturing: Boolean = false,
    val captureFailed: Boolean = false,
    val usingFrontCamera: Boolean = false,
    val torchOn: Boolean = false,
    val hasFlash: Boolean = false,
    val canSwitchCamera: Boolean = false,
) {
    fun cameraReady(hasFlash: Boolean, canSwitchCamera: Boolean): ReceiptCameraUiState =
        copy(hasFlash = hasFlash, canSwitchCamera = canSwitchCamera, torchOn = torchOn && hasFlash)

    fun toggleTorch(): ReceiptCameraUiState = if (hasFlash) copy(torchOn = !torchOn) else this

    fun switchCamera(): ReceiptCameraUiState =
        if (canSwitchCamera) copy(usingFrontCamera = !usingFrontCamera, torchOn = false, hasFlash = false) else this

    fun pickedImage(scan: ReceiptScan?): Pair<ReceiptCameraUiState, ReceiptScan?> =
        copy(isCapturing = false, captureFailed = scan == null) to scan

    fun withFrame(scan: ReceiptScan?): ReceiptCameraUiState =
        if (scan == null || isCapturing) this else copy(detected = scan, captureFailed = false)

    fun capturing(): ReceiptCameraUiState = copy(isCapturing = true, captureFailed = false)

    fun captured(scan: ReceiptScan?): Pair<ReceiptCameraUiState, ReceiptScan?> {
        val result = scan ?: detected
        return copy(isCapturing = false, captureFailed = result == null) to result
    }
}
