package com.serranoie.app.minus.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.Transaction
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class CategorySuggestionTest {

    private fun tx(
        comment: String,
        amount: String,
        hour: Int,
        minute: Int = 0,
        isDeleted: Boolean = false,
        isAdjustment: Boolean = false,
    ) = Transaction(
        amount = BigDecimal(amount),
        comment = comment,
        date = LocalDateTime.of(2026, 10, 1, hour, minute),
        isDeleted = isDeleted,
        isAdjustment = isAdjustment,
    )

    private fun at(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 10, 29, hour, minute)

    @Test
    fun without_history_the_usage_order_is_kept() {
        val ranked = rankCategoriesForEntry(listOf("Food", "Transport", "Rent"), emptyList(), null, at(9))

        assertThat(ranked).containsExactly("Food", "Transport", "Rent").inOrder()
    }

    @Test
    fun category_used_around_this_hour_moves_first() {
        val history = listOf(
            tx("Coffee", "4", 8, 30),
            tx("Coffee", "4", 9, 0),
            tx("Dinner", "25", 20),
            tx("Dinner", "25", 21),
            tx("Dinner", "25", 20, 30),
        )

        val ranked = rankCategoriesForEntry(listOf("Dinner", "Coffee"), history, null, at(8, 45))

        assertThat(ranked).containsExactly("Coffee", "Dinner").inOrder()
    }

    @Test
    fun similar_amount_picks_the_matching_category() {
        val history = listOf(
            tx("Rent", "1000", 12),
            tx("Snacks", "5", 12),
            tx("Snacks", "6", 12),
        )
        val categories = listOf("Snacks", "Rent")

        assertThat(rankCategoriesForEntry(categories, history, BigDecimal("980"), at(3)))
            .containsExactly("Rent", "Snacks").inOrder()
        assertThat(rankCategoriesForEntry(categories, history, BigDecimal("5"), at(3)))
            .containsExactly("Snacks", "Rent").inOrder()
    }

    @Test
    fun hour_window_wraps_around_midnight() {
        val history = listOf(tx("Taxi", "12", 23, 30))

        val ranked = rankCategoriesForEntry(listOf("Food", "Taxi"), history, null, at(0, 30))

        assertThat(ranked.first()).isEqualTo("Taxi")
    }

    @Test
    fun deleted_and_adjustment_rows_are_ignored() {
        val history = listOf(
            tx("Bonus", "500", 9, isAdjustment = true),
            tx("Gone", "500", 9, isDeleted = true),
        )

        val ranked = rankCategoriesForEntry(listOf("Food", "Bonus", "Gone"), history, BigDecimal("500"), at(9))

        assertThat(ranked).containsExactly("Food", "Bonus", "Gone").inOrder()
    }

    @Test
    fun unscored_categories_keep_their_relative_usage_order_after_matches() {
        val history = listOf(tx("Gym", "30", 7))

        val ranked = rankCategoriesForEntry(listOf("Food", "Transport", "Gym", "Rent"), history, null, at(7))

        assertThat(ranked).containsExactly("Gym", "Food", "Transport", "Rent").inOrder()
    }
}
