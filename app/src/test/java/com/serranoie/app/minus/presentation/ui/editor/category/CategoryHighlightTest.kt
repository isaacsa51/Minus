package com.serranoie.app.minus.presentation.ui.editor.category

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryHighlightTest {

    @Test
    fun `each plus is shown with a space on both sides`() {
        assertThat(spacedPluses("Uber+Food+Cof")).isEqualTo("Uber + Food + Cof")
        assertThat(spacedPluses("Food")).isEqualTo("Food")
    }

    @Test
    fun `cursor positions map around the added spaces`() {
        val text = "Uber+Food"

        assertThat(displayOffset(text, 4)).isEqualTo(4)
        assertThat(displayOffset(text, 5)).isEqualTo(7)
        assertThat(displayOffset(text, 9)).isEqualTo(11)
        assertThat(originalOffset(text, 4)).isEqualTo(4)
        assertThat(originalOffset(text, 6)).isEqualTo(4)
        assertThat(originalOffset(text, 7)).isEqualTo(5)
        assertThat(originalOffset(text, 11)).isEqualTo(9)
    }

    @Test
    fun `every original offset maps back to itself`() {
        val text = "Uber+Food+Cof"

        (0..text.length).forEach { assertThat(originalOffset(text, displayOffset(text, it))).isEqualTo(it) }
    }
}
