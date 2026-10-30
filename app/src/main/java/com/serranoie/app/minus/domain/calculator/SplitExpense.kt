package com.serranoie.app.minus.domain.calculator

import java.math.BigDecimal

data class ExpenseSplit(val category: String, val amount: BigDecimal)

data class SplitPreview(val category: String?, val amount: BigDecimal?)

sealed interface SplitOutcome {
    data object NotSplit : SplitOutcome
    data object InvalidAmount : SplitOutcome
    data class Mismatch(val amounts: Int, val categories: Int) : SplitOutcome
    data class Split(val parts: List<ExpenseSplit>) : SplitOutcome
}

internal fun splitCategories(comment: String): List<String> =
    comment.split('+').map { it.trim() }.filter { it.isNotEmpty() }

internal fun splitExpense(input: String, comment: String): SplitOutcome {
    val categories = splitCategories(comment)
    if (categories.size < 2 || isSignedInput(input)) return SplitOutcome.NotSplit
    val terms = amountTerms(input)
    if (terms.size != categories.size) return SplitOutcome.Mismatch(terms.size, categories.size)
    val amounts = terms.map { positiveAmount(it) ?: return SplitOutcome.InvalidAmount }
    return SplitOutcome.Split(categories.zip(amounts, ::ExpenseSplit))
}

internal fun pickCategory(comment: String, tag: String, input: String): String {
    if (isSignedInput(input)) return tag
    val picked = splitCategories(comment)
    return if (picked.isNotEmpty() && picked.size < amountTerms(input).size) "${picked.joinToString("+")}+$tag" else tag
}

private fun isSignedInput(input: String): Boolean = input.trim().let { it.startsWith("+") || it.startsWith("-") }

private fun positiveAmount(term: String): BigDecimal? =
    evaluateExpression(term)?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }

private fun amountTerms(input: String): List<String> = input.trim().split('+')

internal fun previewSplit(input: String, comment: String): List<SplitPreview> {
    val categories = splitCategories(comment)
    if (categories.size < 2 || isSignedInput(input)) return emptyList()
    val amounts = if (input.isBlank()) emptyList() else amountTerms(input).map(::positiveAmount)
    return List(maxOf(categories.size, amounts.size)) {
        SplitPreview(categories.getOrNull(it), amounts.getOrNull(it))
    }
}

internal fun removeCategoryAt(comment: String, index: Int): String =
    splitCategories(comment).filterIndexed { i, _ -> i != index }.joinToString("+")

internal fun lastCategoryQuery(text: String): String = text.substringAfterLast('+').trim()

internal fun completeLastCategory(text: String, category: String): String {
    val plus = text.lastIndexOf('+')
    return if (plus < 0) category else text.substring(0, plus + 1) + category
}

internal fun detectedCategoryRanges(text: String, tags: Collection<String>): List<IntRange> =
    Regex("[^+]+").findAll(text).mapNotNull { segment ->
        val name = segment.value.trim()
        if (name !in tags) return@mapNotNull null
        val start = segment.range.first + segment.value.indexOf(name)
        start until start + name.length
    }.toList()

internal fun singlePlus(text: String): String =
    text.replace(Regex("""\+(\s*\+)+"""), "+").replace(Regex("""^\s*\+"""), "")

internal fun pickSuggestion(text: String, category: String, input: String): String {
    val completed = completeLastCategory(text, category)
    val pending = input.isNotBlank() && !isSignedInput(input) &&
        splitCategories(completed).size < amountTerms(input).size
    return if (pending) "$completed+" else completed
}
