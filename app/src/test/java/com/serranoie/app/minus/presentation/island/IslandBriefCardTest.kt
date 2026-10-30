package com.serranoie.app.minus.presentation.island

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import org.junit.Test

class IslandBriefCardTest {

    private val labels = mapOf(
        R.string.budget_period_label_today to "For today",
        R.string.budget_period_label_this_week to "This week",
        R.string.island_brief_stale to "left in period - open Minus to update",
    )
    private val text: (Int, Array<out Any>) -> String = { res, args -> labels[res] ?: "days:${args.joinToString()}" }

    @Test
    fun `brief subtitle is only the split label without the period range`() {
        val card = islandBriefCard(PreviewIslandSnapshot, PreviewIslandSnapshot.date, text)

        assertThat(card.subtitle).isEqualTo("For today")
    }

    @Test
    fun `brief subtitle follows the user's split view`() {
        val weekly = PreviewIslandSnapshot.copy(viewPeriod = BudgetPeriod.WEEKLY)

        assertThat(islandBriefCard(weekly, weekly.date, text).subtitle).isEqualTo("This week")
    }

    @Test
    fun `stale brief subtitle drops the period range too`() {
        val card = islandBriefCard(PreviewIslandSnapshot, PreviewIslandSnapshot.date.plusDays(1), text)

        assertThat(card.subtitle).isEqualTo("left in period - open Minus to update")
    }
}
