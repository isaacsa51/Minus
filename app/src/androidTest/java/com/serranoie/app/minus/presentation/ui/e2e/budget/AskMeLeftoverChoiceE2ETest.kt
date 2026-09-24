package com.serranoie.app.minus.presentation.ui.e2e.budget

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.data.repository.SettingsRepositoryImpl
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.LeftoverChoice
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.LeftoverChoiceList
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.leftoverChoiceTag
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/**
 * Scenario: a 10-day period of 1000 (100/day) that started 3 days ago; 60 was spent on day one and
 * 40 on day two, so today — day four — opens with 200 waiting for an answer.
 */
class AskMeLeftoverChoiceE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private class FakePreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = transform(state.value).also { state.value = it }
    }

    private val calculator = BudgetStateCalculator()
    private lateinit var dataStore: FakePreferencesDataStore
    private lateinit var settingsRepository: SettingsRepositoryImpl
    private val choicesApplied = AtomicInteger(0)

    private val today: LocalDate = LocalDate.now()
    private val periodStart: LocalDate = today.minusDays(3)
    private val periodEnd: LocalDate = today.plusDays(6)

    private val askMeSettings = BudgetSettings(
        totalBudget = BigDecimal("1000.00"),
        period = BudgetPeriod.DAILY,
        startDate = periodStart,
        endDate = periodEnd,
        currencyCode = "USD",
        daysInPeriod = 10,
        splitMode = BudgetSplitMode.ASK_ME,
    )

    private val spentSoFar = listOf(
        Transaction(
            id = 1L,
            amount = BigDecimal("60.00"),
            date = periodStart.atTime(12, 0),
            periodId = 1L,
        ),
        Transaction(
            id = 2L,
            amount = BigDecimal("40.00"),
            date = periodStart.plusDays(1).atTime(12, 0),
            periodId = 1L,
        ),
    )

    @Before
    fun setUp() {
        dataStore = FakePreferencesDataStore()
        settingsRepository = SettingsRepositoryImpl(dataStore)
        choicesApplied.set(0)
    }

    private fun usd(amount: BigDecimal): String = symbolOnlyCurrencyFormat("USD").format(amount)
    private fun label(resId: Int): String = composeTestRule.activity.getString(resId)

    /** Re-reads the budget with whatever answers are stored right now. */
    private fun budgetToday(
        transactions: List<Transaction> = spentSoFar,
        on: LocalDate = today,
    ): BudgetState = runBlocking {
        calculator.calculateBudgetState(
            settings = askMeSettings,
            transactions = transactions,
            currentDate = on,
            leftoverChoices = settingsRepository.getSettings().leftoverChoices,
        )
    }

    /**
     * Renders the choice list the way the numpad does — a selection that starts on the last answer
     * and an apply button that writes it down for today.
     */
    private fun renderChoiceList(
        state: BudgetState = budgetToday(),
        initialSelection: LeftoverChoice? = null,
        onApplied: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            MinusTheme {
                var selection by remember { mutableStateOf(initialSelection ?: LeftoverChoice.SPREAD) }
                val scope = rememberCoroutineScope()
                Column(modifier = Modifier.fillMaxWidth()) {
                    LeftoverChoiceList(
                        amount = state.pendingLeftover,
                        remainingToday = state.remainingToday,
                        dailyBudget = state.dailyBudget,
                        daysRemaining = state.daysRemaining,
                        currencyCode = "USD",
                        selected = selection,
                        onSelect = { selection = it },
                        modifier = Modifier.height(320.dp),
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                settingsRepository.setLeftoverChoice(
                                    date = today,
                                    choice = selection,
                                    keepFrom = periodStart,
                                )
                                choicesApplied.incrementAndGet()
                                onApplied()
                            }
                        },
                        modifier = Modifier.testTag(APPLY_TAG),
                    ) { Text("apply") }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun pick(choice: LeftoverChoice) {
        composeTestRule.onNodeWithTag(leftoverChoiceTag(choice)).performClick()
        composeTestRule.waitForIdle()
    }

    private fun applyChoice() {
        val before = choicesApplied.get()
        composeTestRule.onNodeWithTag(APPLY_TAG).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { choicesApplied.get() > before }
    }

    // ---------------------------------------------------------------------------------------
    // The prompt itself
    // ---------------------------------------------------------------------------------------

    @Test
    fun when_days_went_unspent_then_the_prompt_offers_both_answers() {
        renderChoiceList()

        composeTestRule.onNodeWithTag(leftoverChoiceTag(LeftoverChoice.SPREAD)).assertIsDisplayed()
        composeTestRule.onNodeWithTag(leftoverChoiceTag(LeftoverChoice.CARRY)).assertIsDisplayed()
    }

    @Test
    fun when_the_prompt_opens_then_it_explains_what_each_answer_does() {
        renderChoiceList()

        composeTestRule.onNodeWithText(label(R.string.leftover_choice_spread_desc)).assertIsDisplayed()
        composeTestRule.onNodeWithText(label(R.string.leftover_choice_carry_desc)).assertIsDisplayed()
    }

    @Test
    fun when_the_prompt_opens_then_carry_previews_today_with_the_whole_leftover_added() {
        // 100 for today plus the 200 that is waiting
        renderChoiceList()

        composeTestRule.onNodeWithText(usd(BigDecimal("300.00"))).assertIsDisplayed()
    }

    @Test
    fun when_the_prompt_opens_then_spread_previews_today_with_only_its_share_added() {
        // 200 shared over the 7 days left is 28.57, on top of today's 100
        renderChoiceList()

        composeTestRule.onNodeWithText(usd(BigDecimal("128.57"))).assertIsDisplayed()
    }

    @Test
    fun when_the_prompt_opens_then_it_starts_on_the_last_answer_the_user_gave() {
        renderChoiceList(initialSelection = LeftoverChoice.CARRY)

        composeTestRule.onNodeWithTag(leftoverChoiceTag(LeftoverChoice.CARRY)).assertIsSelected()
        composeTestRule.onNodeWithTag(leftoverChoiceTag(LeftoverChoice.SPREAD)).assertIsNotSelected()
    }

    @Test
    fun when_picking_the_other_answer_then_the_selection_moves() {
        renderChoiceList(initialSelection = LeftoverChoice.SPREAD)

        pick(LeftoverChoice.CARRY)

        composeTestRule.onNodeWithTag(leftoverChoiceTag(LeftoverChoice.CARRY)).assertIsSelected()
        composeTestRule.onNodeWithTag(leftoverChoiceTag(LeftoverChoice.SPREAD)).assertIsNotSelected()
    }

    @Test
    fun when_picking_back_and_forth_then_only_one_answer_stays_selected() {
        renderChoiceList()

        pick(LeftoverChoice.CARRY)
        pick(LeftoverChoice.SPREAD)

        composeTestRule.onNodeWithTag(leftoverChoiceTag(LeftoverChoice.SPREAD)).assertIsSelected()
        composeTestRule.onNodeWithTag(leftoverChoiceTag(LeftoverChoice.CARRY)).assertIsNotSelected()
    }

    // ---------------------------------------------------------------------------------------
    // Answering carry
    // ---------------------------------------------------------------------------------------

    @Test
    fun when_answering_carry_then_it_is_written_down_for_today() {
        renderChoiceList()

        pick(LeftoverChoice.CARRY)
        applyChoice()

        val stored = runBlocking { settingsRepository.getSettings().leftoverChoices }
        assertThat(stored).containsExactly(today, LeftoverChoice.CARRY)
    }

    @Test
    fun when_answering_carry_then_the_whole_leftover_lands_on_today() {
        renderChoiceList()
        assertThat(budgetToday().remainingToday).isEqualTo(BigDecimal("100.00"))

        pick(LeftoverChoice.CARRY)
        applyChoice()

        val after = budgetToday()
        assertThat(after.remainingToday).isEqualTo(BigDecimal("300.00"))
        assertThat(after.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun when_answering_carry_then_the_daily_budget_is_left_alone() {
        renderChoiceList()

        pick(LeftoverChoice.CARRY)
        applyChoice()

        assertThat(budgetToday().dailyBudget).isEqualTo(BigDecimal("100.00"))
    }

    @Test
    fun when_answering_carry_and_spending_nothing_then_tomorrow_asks_again() {
        renderChoiceList()

        pick(LeftoverChoice.CARRY)
        applyChoice()

        val tomorrow = budgetToday(on = today.plusDays(1))
        assertThat(tomorrow.pendingLeftover).isEqualTo(BigDecimal("300.00"))
        assertThat(tomorrow.remainingToday).isEqualTo(BigDecimal("100.00"))
    }

    // ---------------------------------------------------------------------------------------
    // Answering spread
    // ---------------------------------------------------------------------------------------

    @Test
    fun when_answering_spread_then_it_is_written_down_for_today() {
        renderChoiceList()

        pick(LeftoverChoice.SPREAD)
        applyChoice()

        val stored = runBlocking { settingsRepository.getSettings().leftoverChoices }
        assertThat(stored).containsExactly(today, LeftoverChoice.SPREAD)
    }

    @Test
    fun when_answering_spread_then_the_daily_budget_itself_goes_up() {
        renderChoiceList()

        pick(LeftoverChoice.SPREAD)
        applyChoice()

        val after = budgetToday()
        assertThat(after.dailyBudget).isEqualTo(BigDecimal("128.57"))
        assertThat(after.remainingToday).isEqualTo(BigDecimal("128.57"))
        assertThat(after.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun when_answering_spread_then_the_raise_is_still_there_on_later_days() {
        renderChoiceList()

        pick(LeftoverChoice.SPREAD)
        applyChoice()

        val threeDaysLater = budgetToday(on = today.plusDays(3))
        assertThat(threeDaysLater.dailyBudget).isEqualTo(BigDecimal("128.57"))
    }

    @Test
    fun when_answering_spread_then_it_hands_out_less_today_than_carry_would_have() {
        renderChoiceList()
        pick(LeftoverChoice.SPREAD)
        applyChoice()
        val spread = budgetToday()

        runBlocking {
            settingsRepository.setLeftoverChoice(today, LeftoverChoice.CARRY, keepFrom = periodStart)
        }
        val carried = budgetToday()

        assertThat(spread.remainingToday).isLessThan(carried.remainingToday)
        assertThat(spread.dailyBudget).isGreaterThan(carried.dailyBudget)
    }

    // ---------------------------------------------------------------------------------------
    // Answering again, and answering across periods
    // ---------------------------------------------------------------------------------------

    @Test
    fun when_answering_twice_on_the_same_day_then_the_last_answer_wins() {
        renderChoiceList()

        pick(LeftoverChoice.CARRY)
        applyChoice()
        runBlocking {
            settingsRepository.setLeftoverChoice(today, LeftoverChoice.SPREAD, keepFrom = periodStart)
        }

        val stored = runBlocking { settingsRepository.getSettings().leftoverChoices }
        assertThat(stored).containsExactly(today, LeftoverChoice.SPREAD)
        assertThat(budgetToday().dailyBudget).isEqualTo(BigDecimal("128.57"))
    }

    @Test
    fun when_an_answer_from_this_period_exists_then_a_new_answer_keeps_it() {
        runBlocking {
            settingsRepository.setLeftoverChoice(
                date = periodStart.plusDays(1),
                choice = LeftoverChoice.CARRY,
                keepFrom = periodStart,
            )
        }
        renderChoiceList()

        pick(LeftoverChoice.CARRY)
        applyChoice()

        val stored = runBlocking { settingsRepository.getSettings().leftoverChoices }
        assertThat(stored.keys).containsExactly(periodStart.plusDays(1), today)
    }

    @Test
    fun when_an_answer_belongs_to_a_previous_period_then_a_new_answer_drops_it() {
        runBlocking {
            settingsRepository.setLeftoverChoice(
                date = periodStart.minusDays(5),
                choice = LeftoverChoice.SPREAD,
                keepFrom = periodStart.minusDays(20),
            )
        }
        renderChoiceList()

        pick(LeftoverChoice.CARRY)
        applyChoice()

        val stored = runBlocking { settingsRepository.getSettings().leftoverChoices }
        assertThat(stored).containsExactly(today, LeftoverChoice.CARRY)
    }

    @Test
    fun when_answers_survive_a_restart_then_the_budget_is_read_back_the_same_way() {
        renderChoiceList()
        pick(LeftoverChoice.SPREAD)
        applyChoice()

        // the same store read through a brand new repository instance, as a cold start would
        val reopened = SettingsRepositoryImpl(dataStore)
        val choices = runBlocking { reopened.getSettings().leftoverChoices }

        assertThat(choices).containsExactly(today, LeftoverChoice.SPREAD)
    }

    // ---------------------------------------------------------------------------------------
    // When there is nothing to ask about
    // ---------------------------------------------------------------------------------------

    @Test
    fun when_every_day_was_spent_to_the_cent_then_both_answers_preview_the_same_plain_day() {
        val spentExactly = listOf(
            Transaction(1L, BigDecimal("100.00"), date = periodStart.atTime(12, 0), periodId = 1L),
            Transaction(2L, BigDecimal("100.00"), date = periodStart.plusDays(1).atTime(12, 0), periodId = 1L),
            Transaction(3L, BigDecimal("100.00"), date = periodStart.plusDays(2).atTime(12, 0), periodId = 1L),
        )
        val state = budgetToday(transactions = spentExactly)
        assertThat(state.pendingLeftover).isEqualTo(BigDecimal("0.00"))

        renderChoiceList(state = state)

        composeTestRule.onAllNodesWithText(usd(BigDecimal("100.00"))).assertCountEquals(2)
    }

    @Test
    fun when_the_user_overspent_then_the_debt_is_already_taken_out_of_both_previews() {
        val overspent = listOf(
            Transaction(1L, BigDecimal("350.00"), date = periodStart.atTime(12, 0), periodId = 1L),
        )
        val state = budgetToday(transactions = overspent)
        assertThat(state.pendingLeftover).isEqualTo(BigDecimal("0.00"))

        renderChoiceList(state = state)

        composeTestRule.onAllNodesWithText(usd(BigDecimal("50.00"))).assertCountEquals(2)
    }

    @Test
    fun when_the_prompt_is_shown_on_the_last_day_then_both_answers_agree_and_promise_no_daily_rate() {
        val lastDay = periodEnd
        val state = budgetToday(on = lastDay)
        assertThat(state.daysRemaining).isEqualTo(1)

        renderChoiceList(state = state)

        // 100 for today plus the 800 nobody spent, whichever answer is picked
        composeTestRule.onAllNodesWithText(usd(BigDecimal("900.00"))).assertCountEquals(2)
        composeTestRule.onNodeWithText(
            label(R.string.leftover_choice_carry_outcome).format(usd(state.dailyBudget)),
        ).assertDoesNotExist()
    }

    private companion object {
        const val APPLY_TAG = "AskMeLeftoverChoiceE2ETest.Apply"
    }
}
