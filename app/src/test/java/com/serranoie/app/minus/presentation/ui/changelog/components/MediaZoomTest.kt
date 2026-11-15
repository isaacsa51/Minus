package com.serranoie.app.minus.presentation.ui.changelog.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MediaZoomTest {

    private val size = Size(100f, 100f)

    @Test
    fun `pinching in below the original size snaps back to fit`() {
        val zoom = MediaZoom(2f, Offset(30f, 0f)).transform(size, Offset(50f, 50f), Offset.Zero, 0.2f)

        assertThat(zoom).isEqualTo(MediaZoom(1f, Offset.Zero))
    }

    @Test
    fun `pinching out stops at the max zoom`() {
        val zoom = MediaZoom().transform(size, Offset(50f, 50f), Offset.Zero, 20f)

        assertThat(zoom.scale).isEqualTo(MAX_MEDIA_ZOOM)
    }

    @Test
    fun `panning stops at the image edge`() {
        val zoom = MediaZoom(2f, Offset.Zero).transform(size, Offset(50f, 50f), Offset(80f, -80f), 1f)

        assertThat(zoom.offset).isEqualTo(Offset(50f, -50f))
    }

    @Test
    fun `the point under the fingers stays put while zooming`() {
        val zoom = MediaZoom().transform(size, Offset(75f, 50f), Offset.Zero, 2f)

        assertThat(zoom).isEqualTo(MediaZoom(2f, Offset(-25f, 0f)))
    }

    @Test
    fun `double tap toggles between fit and zoomed at the tap point`() {
        val zoomed = MediaZoom().toggle(size, Offset(75f, 50f))

        assertThat(zoomed).isEqualTo(MediaZoom(DOUBLE_TAP_ZOOM, Offset(-37.5f, 0f)))
        assertThat(zoomed.toggle(size, Offset(10f, 10f))).isEqualTo(MediaZoom())
    }
}
