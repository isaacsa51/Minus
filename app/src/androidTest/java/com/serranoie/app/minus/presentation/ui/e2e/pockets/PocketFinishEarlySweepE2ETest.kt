package com.serranoie.app.minus.presentation.ui.e2e.pockets

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class PocketFinishEarlySweepE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var harness: PocketE2EHarness
    private lateinit var trip: SavingsPocket
    private var runningPeriodId = 0L

    @Before
    fun setUp() {
        harness = PocketE2EHarness()
    }

    @After
    fun tearDown() {
        harness.close()
    }

    private fun label(resId: Int, vararg args: Any): String = composeTestRule.activity.getString(resId, *args)

    private fun runningPeriod(strategy: RemainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS) {
        harness.startPeriod(harness.today.minusDays(10), harness.today.plusDays(19), BigDecimal("1000.00"), strategy)
        runningPeriodId = harness.currentPeriodId()
        trip = harness.createPocket("Trip")
        harness.spend("300.00", harness.today.minusDays(5).atTime(10, 0))
    }

    private fun finishEarlyAndSave() {
        harness.finishEarly()
        composeTestRule.setContent { PocketRolloverHost(harness) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(label(R.string.rollover_dialog_save_to_pocket_title, "Trip")).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            !harness.transitionManager.shouldShowTransitionDialog.value &&
                harness.transactions().any { it.pocketId == trip.id && it.periodId == runningPeriodId && it.clientGeneratedId != null }
        }
    }

    @Test
    fun finishing_early_with_money_left_saves_the_leftover_into_the_pocket_picked() {
        runningPeriod()

        finishEarlyAndSave()

        val deposit = harness.transactions().single { it.clientGeneratedId != null }
        assertThat(deposit.pocketId).isEqualTo(trip.id)
        assertThat(deposit.periodId).isEqualTo(runningPeriodId)
        assertThat(deposit.date!!.toLocalDate()).isEqualTo(harness.today)
        assertThat(harness.balanceOf(trip)).isEqualToIgnoringScale(BigDecimal("700.00"))
        assertThat(harness.pendingRollover().first).isEqualToIgnoringScale(BigDecimal.ZERO)
    }

    @Test
    fun a_new_period_started_the_same_day_after_saving_starts_clean_and_the_pocket_keeps_the_money() {
        runningPeriod()
        finishEarlyAndSave()

        val next = harness.startPeriod(harness.today, harness.today.plusDays(29), BigDecimal("1200.00"))

        assertThat(next.totalBudget).isEqualToIgnoringScale(BigDecimal("1200.00"))
        val user = runBlocking { harness.settingsRepository.getSettings() }
        assertThat(user.currentPeriodId).isNotEqualTo(runningPeriodId)
        val calculator = BudgetStateCalculator()
        val periodRows = calculator.filterPeriodTransactions(
            transactions = harness.transactions(),
            settings = next,
            currentPeriodId = user.currentPeriodId,
            currentPeriodStartedAtMillis = user.currentPeriodStartedAt,
        )
        assertThat(periodRows).isEmpty()
        val state = calculator.calculateBudgetState(next, periodRows, harness.today, allTransactions = harness.transactions())
        assertThat(state.totalBudget).isEqualToIgnoringScale(BigDecimal("1200.00"))
        assertThat(state.totalSpentInPeriod).isEqualToIgnoringScale(BigDecimal.ZERO)
        assertThat(harness.balanceOf(trip)).isEqualToIgnoringScale(BigDecimal("700.00"))
    }

    @Test
    fun money_set_aside_before_finishing_early_is_not_offered_again_as_leftover() {
        runningPeriod()
        assertThat(harness.moveMoney(trip, "200.00", harness.today.atStartOfDay().plusMinutes(1))).isTrue()

        harness.finishEarly()

        assertThat(harness.transitionManager.midnightTransitionData.value!!.remainingAmount)
            .isEqualToIgnoringScale(BigDecimal("500.00"))
    }

    @Test
    fun finishing_early_with_spread_chosen_up_front_never_asks_and_leaves_the_pocket_alone() {
        runningPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY)
        assertThat(harness.moveMoney(trip, "100.00", harness.today.atStartOfDay().plusMinutes(1))).isTrue()

        harness.finishEarly()

        assertThat(harness.transitionManager.shouldShowTransitionDialog.value).isFalse()
        val (amount, strategy) = harness.pendingRollover()
        assertThat(amount).isEqualToIgnoringScale(BigDecimal("600.00"))
        assertThat(strategy).isEqualTo(RemainingBudgetStrategy.SPLIT_EQUALLY)
        assertThat(harness.balanceOf(trip)).isEqualToIgnoringScale(BigDecimal("100.00"))
    }
}
