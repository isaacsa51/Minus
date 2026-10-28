package com.serranoie.app.minus.presentation.ui.editor.receipt

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.receipt.ReceiptScan
import org.junit.Test
import java.math.BigDecimal

class ReceiptCameraUiStateTest {

    private val oxxo = ReceiptScan(merchant = "OXXO", total = BigDecimal("39.50"))
    private val walmart = ReceiptScan(merchant = "Walmart", total = BigDecimal("1234.50"))

    @Test
    fun frame_with_total_becomes_the_detected_value() {
        val state = ReceiptCameraUiState().withFrame(oxxo)

        assertThat(state.detected).isEqualTo(oxxo)
    }

    @Test
    fun frame_without_total_keeps_the_last_detected_value() {
        val state = ReceiptCameraUiState().withFrame(oxxo).withFrame(null)

        assertThat(state.detected).isEqualTo(oxxo)
    }

    @Test
    fun frames_are_ignored_while_capturing() {
        val state = ReceiptCameraUiState().withFrame(oxxo).capturing().withFrame(walmart)

        assertThat(state.detected).isEqualTo(oxxo)
        assertThat(state.isCapturing).isTrue()
    }

    @Test
    fun new_frame_clears_a_previous_capture_failure() {
        val state = ReceiptCameraUiState().capturing().captured(null).first.withFrame(oxxo)

        assertThat(state.captureFailed).isFalse()
        assertThat(state.detected).isEqualTo(oxxo)
    }

    @Test
    fun capture_result_wins_over_the_live_value() {
        val (state, result) = ReceiptCameraUiState().withFrame(oxxo).capturing().captured(walmart)

        assertThat(result).isEqualTo(walmart)
        assertThat(state.isCapturing).isFalse()
        assertThat(state.captureFailed).isFalse()
    }

    @Test
    fun unreadable_capture_falls_back_to_the_live_value() {
        val (_, result) = ReceiptCameraUiState().withFrame(oxxo).capturing().captured(null)

        assertThat(result).isEqualTo(oxxo)
    }

    @Test
    fun camera_ready_reports_flash_and_switch_support() {
        val state = ReceiptCameraUiState().cameraReady(hasFlash = true, canSwitchCamera = true)

        assertThat(state.hasFlash).isTrue()
        assertThat(state.canSwitchCamera).isTrue()
    }

    @Test
    fun torch_toggles_only_when_the_camera_has_a_flash() {
        val withFlash = ReceiptCameraUiState().cameraReady(hasFlash = true, canSwitchCamera = false)
        val withoutFlash = ReceiptCameraUiState().cameraReady(hasFlash = false, canSwitchCamera = false)

        assertThat(withFlash.toggleTorch().torchOn).isTrue()
        assertThat(withFlash.toggleTorch().toggleTorch().torchOn).isFalse()
        assertThat(withoutFlash.toggleTorch().torchOn).isFalse()
    }

    @Test
    fun switching_camera_flips_lens_and_turns_torch_off() {
        val state = ReceiptCameraUiState()
            .cameraReady(hasFlash = true, canSwitchCamera = true)
            .toggleTorch()
            .switchCamera()

        assertThat(state.usingFrontCamera).isTrue()
        assertThat(state.torchOn).isFalse()
        assertThat(state.hasFlash).isFalse()
        assertThat(state.switchCamera().usingFrontCamera).isFalse()
    }

    @Test
    fun switching_is_ignored_without_a_second_camera() {
        val state = ReceiptCameraUiState().cameraReady(hasFlash = true, canSwitchCamera = false).switchCamera()

        assertThat(state.usingFrontCamera).isFalse()
    }

    @Test
    fun camera_without_flash_drops_a_pending_torch() {
        val state = ReceiptCameraUiState()
            .cameraReady(hasFlash = true, canSwitchCamera = true)
            .toggleTorch()
            .cameraReady(hasFlash = false, canSwitchCamera = true)

        assertThat(state.torchOn).isFalse()
    }

    @Test
    fun picked_image_result_is_used_as_is() {
        val (state, result) = ReceiptCameraUiState().withFrame(oxxo).capturing().pickedImage(walmart)

        assertThat(result).isEqualTo(walmart)
        assertThat(state.isCapturing).isFalse()
    }

    @Test
    fun unreadable_picked_image_fails_instead_of_using_the_live_value() {
        val (state, result) = ReceiptCameraUiState().withFrame(oxxo).capturing().pickedImage(null)

        assertThat(result).isNull()
        assertThat(state.captureFailed).isTrue()
        assertThat(state.isCapturing).isFalse()
    }

    @Test
    fun unreadable_capture_without_live_value_marks_failure_and_stays_open() {
        val (state, result) = ReceiptCameraUiState().capturing().captured(null)

        assertThat(result).isNull()
        assertThat(state.captureFailed).isTrue()
        assertThat(state.isCapturing).isFalse()
    }
}
