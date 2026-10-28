package com.serranoie.app.minus.domain.receipt

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.util.Locale

class ReceiptOcrLanguagesTest {

    private fun languages(vararg tags: String) = receiptOcrLanguages(tags.map(Locale::forLanguageTag))

    @Test
    fun spanish_phone_reads_spanish_then_english() {
        assertThat(languages("es-MX")).containsExactly("spa", "eng").inOrder()
    }

    @Test
    fun english_phone_reads_english_only() {
        assertThat(languages("en-US")).containsExactly("eng")
    }

    @Test
    fun non_latin_language_gets_its_script_model() {
        assertThat(languages("ru-RU", "en-US")).containsExactly("rus", "eng").inOrder()
    }

    @Test
    fun at_most_two_languages_plus_english() {
        assertThat(languages("ja-JP", "de-DE", "fr-FR")).containsExactly("jpn", "deu", "eng").inOrder()
    }

    @Test
    fun english_listed_early_does_not_use_up_a_slot() {
        assertThat(languages("en-GB", "ko-KR", "fa-IR")).containsExactly("kor", "fas", "eng").inOrder()
    }

    @Test
    fun every_app_language_maps_to_a_bundled_model() {
        val appLanguages = listOf("en", "de", "el", "es", "fa", "fr", "hi", "it", "ja", "ko", "pt", "ru", "zh", "bg")
        appLanguages.forEach { tag ->
            assertThat(languages(tag).first()).isIn(BUNDLED_OCR_MODELS)
        }
    }

    @Test
    fun every_bundled_model_ships_in_assets() {
        val shipped = File("src/main/assets/tessdata").list().orEmpty()
            .map { it.removeSuffix(".traineddata") }
            .toSet()
        assertThat(shipped).containsAtLeastElementsIn(BUNDLED_OCR_MODELS)
    }

    @Test
    fun traditional_chinese_falls_back_to_the_bundled_chinese_model() {
        assertThat(languages("zh-TW")).containsExactly("chi_sim", "eng").inOrder()
    }

    @Test
    fun unsupported_languages_are_skipped() {
        assertThat(languages("sw-KE", "es-ES")).containsExactly("spa", "eng").inOrder()
    }

    @Test
    fun duplicate_regions_count_once() {
        assertThat(languages("es-ES", "es-MX", "pt-BR")).containsExactly("spa", "por", "eng").inOrder()
    }

    @Test
    fun no_locales_reads_english() {
        assertThat(receiptOcrLanguages(emptyList())).containsExactly("eng")
    }
}
