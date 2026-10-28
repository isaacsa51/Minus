package com.serranoie.app.minus.domain.receipt

import java.util.Locale

private const val ENGLISH_MODEL = "eng"
private const val MAX_EXTRA_MODELS = 2

private val MODEL_BY_LANGUAGE = mapOf(
    "en" to ENGLISH_MODEL,
    "es" to "spa",
    "de" to "deu",
    "fr" to "fra",
    "it" to "ita",
    "pt" to "por",
    "ru" to "rus",
    "bg" to "bul",
    "el" to "ell",
    "fa" to "fas",
    "hi" to "hin",
    "ja" to "jpn",
    "ko" to "kor",
    "zh" to "chi_sim",
)

val BUNDLED_OCR_MODELS: Set<String> = MODEL_BY_LANGUAGE.values.toSet()

fun receiptOcrLanguages(locales: List<Locale>): List<String> =
    locales.mapNotNull { MODEL_BY_LANGUAGE[it.language] }
        .filter { it != ENGLISH_MODEL }
        .distinct()
        .take(MAX_EXTRA_MODELS) + ENGLISH_MODEL
