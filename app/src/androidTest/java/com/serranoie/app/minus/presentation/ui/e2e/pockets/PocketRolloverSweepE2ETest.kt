package com.serranoie.app.minus.presentation.ui.e2e.pockets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class PocketRolloverSweepE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var harness: PocketE2EHarness
    private var endedPeriodId = 0L

    private val periodBudget = BigDecimal("1000.00")
    private val leftover = BigDecimal("300.00")

    @Before
    fun setUp() {
        harness = PocketE2EHarness()
    }

    @After
    fun tearDown() {
        harness.close()
    }

    private fun label(resId: Int, vararg args: Any): String = composeTestRule.activity.getString(resId, *args)

    private fun endPeriodWithLeftover(beforeEnd: () -> Unit = { harness.spend("700.00", harness.today.minusDays(27).atTime(10, 0)) }) {
        harness.startPeriod(harness.today.minusDays(30), harness.today.minusDays(1), periodBudget)
        endedPeriodId = harness.currentPeriodId()
        beforeEnd()
        harness.appStart()
    }

    private fun showDialog() {
        composeTestRule.setContent { PocketRolloverHost(harness) }
        composeTestRule.waitForIdle()
    }

    private fun tapAndAwaitDismiss(text: String) {
        composeTestRule.onNodeWithText(text).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            !harness.transitionManager.shouldShowTransitionDialog.value
        }
        composeTestRule.waitForIdle()
    }

    private fun awaitBalance(name: String, expected: BigDecimal) {
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            harness.balanceOf(harness.pockets().single { it.name == name }).compareTo(expected) == 0
        }
    }

    @Test
    fun a_period_ending_with_money_left_moves_the_whole_leftover_into_the_pocket_picked_in_the_dialog() {
        val trip = harness.createPocket("Trip")
        endPeriodWithLeftover()
        showDialog()

        tapAndAwaitDismiss(label(R.string.rollover_dialog_save_to_pocket_title, "Trip"))

        awaitBalance("Trip", leftover)
        val deposit = harness.transactions().single { it.pocketId == trip.id }
        assertThat(deposit.isAdjustment).isTrue()
        assertThat(deposit.periodId).isEqualTo(endedPeriodId)
        assertThat(deposit.date!!.toLocalDate()).isEqualTo(harness.today.minusDays(1))
        assertThat(harness.pendingRollover().first).isEqualToIgnoringScale(BigDecimal.ZERO)
    }

    @Test
    fun with_several_pockets_the_leftover_goes_only_to_the_one_picked() {
        val trip = harness.createPocket("Trip")
        val house = harness.createPocket("House")
        endPeriodWithLeftover()
        showDialog()

        composeTestRule.onNodeWithText("House").performClick()
        composeTestRule.waitForIdle()
        tapAndAwaitDismiss(label(R.string.rollover_dialog_save_to_pocket_title, "House"))

        awaitBalance("House", leftover)
        assertThat(harness.balanceOf(trip)).isEqualToIgnoringScale(BigDecimal.ZERO)
        assertThat(harness.transactions().count { it.pocketId == house.id }).isEqualTo(1)
    }

    @Test
    fun after_saving_the_leftover_the_next_period_starts_with_exactly_what_was_typed() {
        val trip = harness.createPocket("Trip")
        endPeriodWithLeftover()
        showDialog()
        tapAndAwaitDismiss(label(R.string.rollover_dialog_save_to_pocket_title, "Trip"))
        awaitBalance("Trip", leftover)

        val next = harness.startPeriod(harness.today, harness.today.plusDays(29), BigDecimal("1200.00"))

        assertThat(next.totalBudget).isEqualToIgnoringScale(BigDecimal("1200.00"))
        val calculator = BudgetStateCalculator()
        val user = runBlocking { harness.settingsRepository.getSettings() }
        val periodRows = calculator.filterPeriodTransactions(
            transactions = harness.transactions(),
            settings = next,
            currentPeriodId = user.currentPeriodId,
            currentPeriodStartedAtMillis = user.currentPeriodStartedAt,
        )
        assertThat(periodRows.map { it.pocketId }).doesNotContain(trip.id)
        val state = calculator.calculateBudgetState(next, periodRows, harness.today, allTransactions = harness.transactions())
        assertThat(state.totalSpentInPeriod).isEqualToIgnoringScale(BigDecimal.ZERO)
        assertThat(state.totalBudget).isEqualToIgnoringScale(BigDecimal("1200.00"))
        assertThat(harness.balanceOf(trip)).isEqualToIgnoringScale(leftover)
    }

    @Test
    fun reopening_the_app_after_saving_the_leftover_neither_asks_again_nor_credits_twice() {
        val trip = harness.createPocket("Trip")
        endPeriodWithLeftover()
        showDialog()
        tapAndAwaitDismiss(label(R.string.rollover_dialog_save_to_pocket_title, "Trip"))
        awaitBalance("Trip", leftover)

        harness.appStart()
        composeTestRule.waitForIdle()

        assertThat(harness.transitionManager.shouldShowTransitionDialog.value).isFalse()
        assertThat(harness.transactions().count { it.pocketId == trip.id }).isEqualTo(1)
        assertThat(harness.balanceOf(trip)).isEqualToIgnoringScale(leftover)
    }

    @Test
    fun money_moved_into_and_out_of_a_pocket_during_the_period_is_not_offered_again_as_leftover() {
        val trip = harness.createPocket("Trip")
        endPeriodWithLeftover {
            harness.spend("500.00", harness.today.minusDays(27).atTime(10, 0))
            assertThat(harness.moveMoney(trip, "200.00", harness.today.minusDays(20).atTime(9, 0))).isTrue()
            assertThat(harness.moveMoney(trip, "50.00", harness.today.minusDays(10).atTime(9, 0), withdraw = true)).isTrue()
        }

        assertThat(harness.transitionManager.midnightTransitionData.value!!.remainingAmount)
            .isEqualToIgnoringScale(BigDecimal("350.00"))

        showDialog()
        tapAndAwaitDismiss(label(R.string.rollover_dialog_save_to_pocket_title, "Trip"))

        awaitBalance("Trip", BigDecimal("500.00"))
    }

    @Test
    fun a_leftover_left_unanswered_can_still_be_saved_to_a_pocket_after_the_next_period_started() {
        val trip = harness.createPocket("Trip")
        endPeriodWithLeftover()
        showDialog()
        tapAndAwaitDismiss(label(R.string.cancel))
        harness.startPeriod(harness.today, harness.today.plusDays(29), BigDecimal("1200.00"))

        runBlocking { harness.transitionManager.reopenUnresolvedSurplusDialog() }
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            harness.transitionManager.shouldShowTransitionDialog.value
        }
        tapAndAwaitDismiss(label(R.string.rollover_dialog_save_to_pocket_title, "Trip"))

        awaitBalance("Trip", leftover)
        assertThat(harness.transactions().single { it.pocketId == trip.id }.periodId).isEqualTo(endedPeriodId)
        assertThat(runBlocking { harness.budgetRepository.getBudgetSettingsSync() }!!.totalBudget)
            .isEqualToIgnoringScale(BigDecimal("1200.00"))
        assertThat(harness.pendingRollover().first).isEqualToIgnoringScale(BigDecimal.ZERO)
    }

    @Test
    fun without_any_pocket_the_dialog_offers_no_pocket_option() {
        endPeriodWithLeftover()
        showDialog()

        composeTestRule.onNodeWithText(label(R.string.rollover_dialog_split_equally_title)).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText(label(R.string.rollover_dialog_save_to_pocket_title, "").trim(), substring = true)
            .assertCountEquals(0)
    }

    @Test
    fun a_pocket_deleted_before_the_answer_lands_keeps_the_leftover_pending() {
        val trip = harness.createPocket("Trip")
        endPeriodWithLeftover()
        assertThat(runBlocking { harness.budgetRepository.deleteSavingsPocket(trip.id) }).isTrue()

        val result = runCatching { runBlocking { harness.transitionManager.sweepSurplusToPocket(trip) } }

        assertThat(result.isFailure).isTrue()
        assertThat(harness.pendingRollover().first).isEqualToIgnoringScale(leftover)
        assertThat(harness.transactions().none { it.pocketId == trip.id }).isTrue()
        assertThat(harness.transitionManager.shouldShowTransitionDialog.value).isTrue()
    }
}
