package com.serranoie.app.minus.presentation.ui.editor.receipt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReceiptScanGenerationTest {

    @Test
    fun result_from_the_open_session_is_current() {
        val generation = ReceiptScanGeneration()
        val token = generation.open()

        assertThat(generation.isCurrent(token)).isTrue()
    }

    @Test
    fun closing_invalidates_an_in_flight_result() {
        val generation = ReceiptScanGeneration()
        val token = generation.open()

        generation.close()

        assertThat(generation.isCurrent(token)).isFalse()
    }

    @Test
    fun reopening_before_the_old_result_lands_keeps_it_stale() {
        val generation = ReceiptScanGeneration()
        val stale = generation.open()

        generation.close()
        val fresh = generation.open()

        assertThat(generation.isCurrent(stale)).isFalse()
        assertThat(generation.isCurrent(fresh)).isTrue()
    }

    @Test
    fun token_taken_mid_session_matches_the_session() {
        val generation = ReceiptScanGeneration()
        generation.open()

        assertThat(generation.isCurrent(generation.current)).isTrue()
    }
}
