package com.serranoie.app.minus.domain.calculator

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.math.BigDecimal

class SplitExpenseTest {

    @Test
    fun `each typed amount goes to the category in the same position`() {
        val outcome = splitExpense("15+125+66", "uber+food+coffee")

        assertThat(outcome).isEqualTo(
            SplitOutcome.Split(
                listOf(
                    ExpenseSplit("uber", BigDecimal("15")),
                    ExpenseSplit("food", BigDecimal("125")),
                    ExpenseSplit("coffee", BigDecimal("66")),
                ),
            ),
        )
    }

    @Test
    fun `multiplication division and minus stay inside their own amount`() {
        val outcome = splitExpense("2×15+125-25+9÷3", "uber + food + coffee")

        assertThat(outcome).isEqualTo(
            SplitOutcome.Split(
                listOf(
                    ExpenseSplit("uber", BigDecimal("30")),
                    ExpenseSplit("food", BigDecimal("100")),
                    ExpenseSplit("coffee", BigDecimal("3")),
                ),
            ),
        )
    }

    @Test
    fun `a single category keeps the old single transaction`() {
        assertThat(splitExpense("15+125+66", "food")).isEqualTo(SplitOutcome.NotSplit)
        assertThat(splitExpense("15+125+66", "")).isEqualTo(SplitOutcome.NotSplit)
    }

    @Test
    fun `different counts of amounts and categories are a mismatch`() {
        assertThat(splitExpense("15+125+66", "uber+food")).isEqualTo(SplitOutcome.Mismatch(amounts = 3, categories = 2))
        assertThat(splitExpense("206", "uber+food+coffee")).isEqualTo(SplitOutcome.Mismatch(amounts = 1, categories = 3))
    }

    @Test
    fun `an amount that is zero negative or unfinished is invalid`() {
        assertThat(splitExpense("15+0", "uber+food")).isEqualTo(SplitOutcome.InvalidAmount)
        assertThat(splitExpense("15+10-20", "uber+food")).isEqualTo(SplitOutcome.InvalidAmount)
        assertThat(splitExpense("15+", "uber+food")).isEqualTo(SplitOutcome.InvalidAmount)
    }

    @Test
    fun `income and adjustment inputs never split`() {
        assertThat(splitExpense("+15+125", "uber+food")).isEqualTo(SplitOutcome.NotSplit)
        assertThat(splitExpense("-15+125", "uber+food")).isEqualTo(SplitOutcome.NotSplit)
    }

    @Test
    fun `tapping a category appends while amounts outnumber picked categories`() {
        assertThat(pickCategory(comment = "", tag = "uber", input = "15+")).isEqualTo("uber")
        assertThat(pickCategory(comment = "uber", tag = "food", input = "15+125")).isEqualTo("uber+food")
        assertThat(pickCategory(comment = "uber+food", tag = "coffee", input = "15+125+66")).isEqualTo("uber+food+coffee")
    }

    @Test
    fun `tapping a category replaces when there is nothing left to pair`() {
        assertThat(pickCategory(comment = "uber", tag = "food", input = "15")).isEqualTo("food")
        assertThat(pickCategory(comment = "uber+food", tag = "coffee", input = "15+125")).isEqualTo("coffee")
        assertThat(pickCategory(comment = "uber", tag = "food", input = "+15+125")).isEqualTo("food")
        assertThat(pickCategory(comment = "uber", tag = "food", input = "")).isEqualTo("food")
    }

    @Test
    fun `picked categories are read back from the comment`() {
        assertThat(splitCategories("uber + food+coffee")).containsExactly("uber", "food", "coffee").inOrder()
        assertThat(splitCategories("")).isEmpty()
    }

    @Test
    fun `preview pairs each category with its amount`() {
        assertThat(previewSplit("15+125+66", "uber+food+coffee")).containsExactly(
            SplitPreview("uber", BigDecimal("15")),
            SplitPreview("food", BigDecimal("125")),
            SplitPreview("coffee", BigDecimal("66")),
        ).inOrder()
    }

    @Test
    fun `preview leaves the unpaired side empty when counts differ`() {
        assertThat(previewSplit("15+125+66", "uber+food")).containsExactly(
            SplitPreview("uber", BigDecimal("15")),
            SplitPreview("food", BigDecimal("125")),
            SplitPreview(null, BigDecimal("66")),
        ).inOrder()
        assertThat(previewSplit("15+", "uber+food")).containsExactly(
            SplitPreview("uber", BigDecimal("15")),
            SplitPreview("food", null),
        ).inOrder()
        assertThat(previewSplit("", "uber+food")).containsExactly(
            SplitPreview("uber", null),
            SplitPreview("food", null),
        ).inOrder()
    }

    @Test
    fun `no preview without a split`() {
        assertThat(previewSplit("15+125", "food")).isEmpty()
        assertThat(previewSplit("+15+125", "uber+food")).isEmpty()
    }

    @Test
    fun `removing a picked category keeps the others in order`() {
        assertThat(removeCategoryAt("uber+food+coffee", 1)).isEqualTo("uber+coffee")
        assertThat(removeCategoryAt("uber+food", 0)).isEqualTo("food")
    }

    @Test
    fun `suggestions search only the category being typed after the last plus`() {
        assertThat(lastCategoryQuery("Uber+fo")).isEqualTo("fo")
        assertThat(lastCategoryQuery("Uber + ")).isEmpty()
        assertThat(lastCategoryQuery("Gro")).isEqualTo("Gro")
    }

    @Test
    fun `picking a suggestion replaces only the category being typed`() {
        assertThat(completeLastCategory("Uber+fo", "Food")).isEqualTo("Uber+Food")
        assertThat(completeLastCategory("Uber + Food+co", "Coffee")).isEqualTo("Uber + Food+Coffee")
        assertThat(completeLastCategory("Gro", "Groceries")).isEqualTo("Groceries")
    }

    @Test
    fun `only names that match a known category are detected`() {
        val tags = listOf("Uber", "Food", "Coffee")

        assertThat(detectedCategoryRanges("Uber+Fo+ Coffee ", tags)).containsExactly(0 until 4, 9 until 15).inOrder()
        assertThat(detectedCategoryRanges("Food", tags)).containsExactly(0 until 4)
        assertThat(detectedCategoryRanges("Taxi+food", tags)).isEmpty()
    }

    @Test
    fun `typed pluses collapse to one and never lead`() {
        assertThat(singlePlus("Uber++")).isEqualTo("Uber+")
        assertThat(singlePlus("Uber+ +Food")).isEqualTo("Uber+Food")
        assertThat(singlePlus("+Uber")).isEqualTo("Uber")
        assertThat(singlePlus("Uber+Food")).isEqualTo("Uber+Food")
    }

    @Test
    fun `picking a suggestion adds the next plus while amounts still need categories`() {
        assertThat(pickSuggestion(text = "Ub", category = "Uber", input = "15+125+66")).isEqualTo("Uber+")
        assertThat(pickSuggestion(text = "Uber+fo", category = "Food", input = "15+125+66")).isEqualTo("Uber+Food+")
    }

    @Test
    fun `picking the last needed suggestion adds no plus`() {
        assertThat(pickSuggestion(text = "Uber+Food+co", category = "Coffee", input = "15+125+66")).isEqualTo("Uber+Food+Coffee")
        assertThat(pickSuggestion(text = "fo", category = "Food", input = "206")).isEqualTo("Food")
        assertThat(pickSuggestion(text = "fo", category = "Food", input = "+15+125")).isEqualTo("Food")
        assertThat(pickSuggestion(text = "fo", category = "Food", input = "")).isEqualTo("Food")
    }
}
