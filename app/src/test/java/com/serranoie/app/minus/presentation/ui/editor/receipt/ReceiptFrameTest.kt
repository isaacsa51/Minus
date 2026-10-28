package com.serranoie.app.minus.presentation.ui.editor.receipt

import androidx.compose.ui.unit.IntRect
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReceiptFrameTest {

    @Test
    fun portrait_view_maps_guide_to_centered_square_in_image() {
        val crop = receiptFrameCrop(imageWidth = 960, imageHeight = 1280, viewWidth = 1080, viewHeight = 1800)

        assertThat(crop).isEqualTo(IntRect(left = 203, top = 363, right = 757, bottom = 917))
    }

    @Test
    fun crop_side_follows_the_shorter_view_side() {
        val crop = receiptFrameCrop(imageWidth = 1280, imageHeight = 960, viewWidth = 1800, viewHeight = 1080)

        assertThat(crop).isEqualTo(IntRect(left = 363, top = 203, right = 917, bottom = 757))
    }

    @Test
    fun higher_resolution_capture_scales_the_same_region() {
        val crop = receiptFrameCrop(imageWidth = 1920, imageHeight = 2560, viewWidth = 1080, viewHeight = 1800)

        assertThat(crop).isEqualTo(IntRect(left = 407, top = 727, right = 1513, bottom = 1833))
    }

    @Test
    fun crop_never_leaves_the_image() {
        val crop = receiptFrameCrop(imageWidth = 400, imageHeight = 300, viewWidth = 300, viewHeight = 300)!!

        assertThat(crop.left).isAtLeast(0)
        assertThat(crop.top).isAtLeast(0)
        assertThat(crop.right).isAtMost(400)
        assertThat(crop.bottom).isAtMost(300)
    }

    @Test
    fun unknown_view_size_means_no_crop() {
        assertThat(receiptFrameCrop(imageWidth = 960, imageHeight = 1280, viewWidth = 0, viewHeight = 0)).isNull()
    }
}
