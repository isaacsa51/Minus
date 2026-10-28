package com.serranoie.app.minus.domain.receipt

import com.serranoie.app.minus.domain.model.SupportedCurrency
import java.text.Normalizer
import java.util.Locale

internal object ReceiptVocabulary {

    private val TOTAL_WORDS = listOf(
        "total", "grand total", "amount due", "balance due", "to pay", "amount payable",
        "importe", "a pagar", "monto", "total a pagar",
        "summe", "gesamt", "gesamtsumme", "gesamtbetrag", "endsumme", "endbetrag", "zu zahlen", "betrag",
        "montant", "à payer", "net à payer",
        "totale", "importo", "da pagare", "totale complessivo",
        "valor total", "valor a pagar",
        "итог", "итого", "всего", "к оплате", "сумма",
        "общо", "сума", "обща сума", "за плащане",
        "σύνολο", "γενικό σύνολο", "πληρωτέο",
        "جمع کل", "جمع", "مبلغ کل", "قابل پرداخت", "مبلغ قابل پرداخت",
        "कुल", "कुल राशि", "देय राशि", "योग",
        "合計", "総合計", "総額", "お会計", "ご請求額",
        "합계", "총액", "총계", "결제금액", "받을금액",
        "合计", "总计", "總計", "总额", "總額", "应付", "應付", "实付", "實付", "应收", "應收",
    )

    private val SUBTOTAL_WORDS = listOf(
        "subtotal", "sub total", "sub-total",
        "zwischensumme",
        "sous-total", "sous total",
        "subtotale",
        "подытог", "промежуточный итог",
        "междинна сума",
        "μερικό σύνολο", "υποσύνολο",
        "جمع جزء", "جمع فرعی",
        "उप योग", "उप-योग", "उप कुल", "उपकुल",
        "小計", "小计",
        "소계", "공급가액",
    )

    private val PAYMENT_WORDS = listOf(
        "cash", "change", "tendered",
        "efectivo", "cambio", "pago con", "su cambio",
        "bar", "gegeben", "rückgeld",
        "espèces", "rendu", "monnaie",
        "contanti", "contante", "resto",
        "dinheiro", "troco",
        "наличные", "сдача", "получено",
        "в брой", "ресто",
        "μετρητά", "ρέστα",
        "نقد", "نقدی", "باقیمانده",
        "नकद", "वापसी", "शेष",
        "お預り", "お預かり", "お釣り", "おつり", "釣銭",
        "현금", "거스름돈", "받은금액",
        "现金", "現金", "找零", "找贖", "实收", "實收",
    )

    private val TAX_OR_COUNT_WORDS = listOf(
        "tax", "vat", "gst", "cgst", "sgst", "items", "qty",
        "iva", "impuesto", "artículos", "cantidad",
        "mwst", "ust", "steuer", "artikel",
        "tva", "articles",
        "articoli",
        "icms", "itens",
        "ндс", "позиций",
        "ддс",
        "φπα",
        "مالیات", "ارزش افزوده",
        "कर", "जीएसटी",
        "消費税", "点数",
        "부가세", "과세", "수량",
        "税额", "稅額", "税金", "数量",
    )

    private val NOISE_WORDS = listOf(
        "ticket", "receipt", "welcome", "invoice",
        "factura", "rfc", "recibo", "bienvenido", "bienvenida",
        "quittung", "kassenbon", "beleg", "rechnung",
        "reçu", "facture",
        "ricevuta", "scontrino",
        "cupom", "nota fiscal",
        "чек", "кассовый чек", "квитанция",
        "касов бон", "фискален бон",
        "απόδειξη",
        "رسید", "فاکتور",
        "रसीद", "बिल",
        "レシート", "領収書",
        "영수증",
        "收据", "收據", "小票", "发票", "發票",
    )

    private val CURRENCY_WORDS = SupportedCurrency.ALL.flatMap { listOf(it.code, it.symbol) } +
        listOf("¥", "￥", "円", "元", "₩", "원", "₹", "₽", "руб", "лв", "ریال", "تومان")

    private val totals = TOTAL_WORDS.map(::normalize)
    private val subtotals = SUBTOTAL_WORDS.map(::normalize)
    private val payments = PAYMENT_WORDS.map(::normalize)
    private val taxesOrCounts = TAX_OR_COUNT_WORDS.map(::normalize)
    private val noise = NOISE_WORDS.map(::normalize)
    private val currencies = CURRENCY_WORDS.map(::normalize).filter { it.isNotBlank() }.distinct()

    fun isTotal(line: String) = containsAny(line, totals)

    fun isSubtotal(line: String) = containsAny(line, subtotals)

    fun isPayment(line: String) = containsAny(line, payments)

    fun isTaxOrCount(line: String) = containsAny(line, taxesOrCounts)

    fun isNoise(line: String) = containsAny(line, noise)

    fun hasCurrency(line: String) = containsAny(line, currencies)

    fun asciiDigits(text: String): String = buildString(text.length) {
        text.forEach { c ->
            append(
                when (c) {
                    in '٠'..'٩' -> '0' + (c - '٠')
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '०'..'९' -> '0' + (c - '०')
                    in '０'..'９' -> '0' + (c - '０')
                    '٫', '．' -> '.'
                    '٬', '，' -> ','
                    else -> c
                },
            )
        }
    }

    private fun containsAny(line: String, words: List<String>): Boolean {
        val haystack = normalize(line)
        return words.any { word -> occursAsWord(haystack, word) }
    }

    private fun occursAsWord(haystack: String, word: String): Boolean {
        var index = haystack.indexOf(word)
        while (index >= 0) {
            val end = index + word.length
            val startOk = !needsBoundary(word.first()) || !haystack.isWordCharAt(index - 1)
            val endOk = !needsBoundary(word.last()) || !haystack.isWordCharAt(end)
            if (startOk && endOk) return true
            index = haystack.indexOf(word, index + 1)
        }
        return false
    }

    private fun needsBoundary(c: Char): Boolean {
        if (!c.isLetterOrMark()) return false
        return when (Character.UnicodeScript.of(c.code)) {
            Character.UnicodeScript.HAN,
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.HANGUL -> false
            else -> true
        }
    }

    private fun String.isWordCharAt(index: Int): Boolean = getOrNull(index)?.isLetterOrMark() == true

    private fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        val stripped = buildString(decomposed.length) {
            decomposed.forEach { c ->
                when (c) {
                    in '̀'..'ͯ' -> Unit
                    'ς' -> append('σ')
                    'ك' -> append('ک')
                    'ي', 'ى' -> append('ی')
                    else -> append(c)
                }
            }
        }
        return Normalizer.normalize(stripped, Normalizer.Form.NFC)
    }
}

internal fun Char.isLetterOrMark(): Boolean = isLetter() ||
    Character.getType(this).let {
        it == Character.NON_SPACING_MARK.toInt() || it == Character.COMBINING_SPACING_MARK.toInt()
    }
