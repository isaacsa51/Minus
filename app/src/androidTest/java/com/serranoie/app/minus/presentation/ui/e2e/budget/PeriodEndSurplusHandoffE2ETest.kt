package com.serranoie.app.minus.presentation.ui.e2e.budget

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.time.MidnightPeriodChecker
import com.serranoie.app.minus.domain.time.MidnightTransitionManager
import com.serranoie.app.minus.domain.time.TimeProvider
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.ui.budget.BudgetPeriodManager
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.RolloverDialog
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/**
 * The end of one period and the start of the next, from the user's side: the dialog that appears
 * when money is left over, the answer given there, and what the pill shows on day one of the new
 * period once that answer has been folded in.
 *
 * Everything below the UI is the real thing — [MidnightPeriodChecker] decides what to ask,
 * [BudgetPeriodManager] creates the next period, [BudgetStateCalculator] reads it back. Only the
 * repositories are faked, and they keep their state so a write is visible to the next read.
 *
 * Scenario: a 30-day period of 1000 that ended yesterday having spent 700, so 300 is left over.
 */
class PeriodEndSurplusHandoffE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val timeProvider: TimeProvider = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)

    private val checker = MidnightPeriodChecker(budgetRepository, settingsRepository)
    private val transitionManager = MidnightTransitionManager(checker)
    private val calculator = BudgetStateCalculator()
    private lateinit var periodManager: BudgetPeriodManager

    private var pendingRollover: Pair<BigDecimal, RemainingBudgetStrategy?> = BigDecimal.ZERO to null
    private var userSettings = UserSettings.DEFAULT
    private var liveSettings: BudgetSettings? = null
    private val savedSettings = mutableListOf<BudgetSettings>()

    private val today: LocalDate = LocalDate.now()
    private val lastPeriodEnd: LocalDate = today.minusDays(1)
    private val lastPeriodStart: LocalDate = lastPeriodEnd.minusDays(29)
    private val lastPeriodId = 100L
    private val lastPeriodBudget = BigDecimal("1000.00")
    private val surplus = BigDecimal("300.00")
    private val nextPeriodBudget = BigDecimal("1200.00")

    @Before
    fun setUp() {
        periodManager = BudgetPeriodManager(
            budgetRepository = budgetRepository,
            settingsRepository = settingsRepository,
            timeProvider = timeProvider,
            notificationScheduler = notificationScheduler,
            midnightPeriodChecker = checker,
        )

        coEvery { settingsRepository.getPendingRollover() } answers { pendingRollover }
        every { settingsRepository.observePendingRollover() } answers { flowOf(pendingRollover) }
        coEvery { settingsRepository.setPendingRollover(any(), any()) } answers {
            pendingRollover = firstArg<BigDecimal>() to secondArg()
        }
        coEvery { settingsRepository.clearPendingRollover() } answers {
            pendingRollover = BigDecimal.ZERO to null
        }
        coEvery { settingsRepository.markSurplusUnresolved(any()) } answers {
            pendingRollover = firstArg<BigDecimal>() to null
        }
        coEvery { settingsRepository.getSettings() } answers { userSettings }
        coEvery { settingsRepository.setPeriodEndAlreadyHandled(any()) } answers {
            userSettings = userSettings.copy(periodEndAlreadyHandled = firstArg())
        }
        coEvery { settingsRepository.setCurrentPeriod(any(), any()) } answers {
            userSettings = userSettings.copy(
                currentPeriodId = firstArg(),
                currentPeriodStartedAt = secondArg(),
            )
        }
        coEvery { settingsRepository.clearEarlyFinish() } answers {
            userSettings = userSettings.copy(
                earlyFinishActive = false,
                earlyFinishActualDate = 0L,
                earlyFinishOriginalEndDate = 0L,
            )
        }
        coEvery { budgetRepository.getBudgetSettingsSync() } answers { liveSettings }
        coEvery { budgetRepository.saveBudgetSettings(any()) } answers {
            val saved = firstArg<BudgetSettings>()
            savedSettings += saved
            liveSettings = saved
        }
        every { timeProvider.nowEpochMillis() } returns 1_700_000_000_000L
    }

    private fun usd(amount: BigDecimal): String = symbolOnlyCurrencyFormat("USD").format(amount)
    private fun label(resId: Int): String = composeTestRule.activity.getString(resId)

    private fun periodSettings(
        startDate: LocalDate,
        endDate: LocalDate,
        totalBudget: BigDecimal,
        strategy: RemainingBudgetStrategy,
        splitMode: BudgetSplitMode = BudgetSplitMode.STATIC,
    ) = BudgetSettings(
        totalBudget = totalBudget,
        period = BudgetPeriod.MONTHLY,
        startDate = startDate,
        endDate = endDate,
        currencyCode = "USD",
        daysInPeriod = 30,
        remainingBudgetStrategy = strategy,
        splitMode = splitMode,
    )

    private fun endLastPeriod(
        strategy: RemainingBudgetStrategy,
        spent: BigDecimal = lastPeriodBudget.subtract(surplus),
    ) {
        liveSettings = periodSettings(
            startDate = lastPeriodStart,
            endDate = lastPeriodEnd,
            totalBudget = lastPeriodBudget,
            strategy = strategy,
        )
        userSettings = UserSettings.DEFAULT.copy(
            currentPeriodId = lastPeriodId,
            currentPeriodStartedAt = 1L,
        )
        coEvery { settingsRepository.observeBudgetEndDate() } returns flowOf(
            lastPeriodEnd.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(false)
        coEvery { budgetRepository.getTransactions() } returns flowOf(
            listOf(
                Transaction(
                    id = 1L,
                    amount = spent,
                    date = lastPeriodStart.plusDays(3).atStartOfDay(),
                    periodId = lastPeriodId,
                ),
            ),
        )

        runBlocking { transitionManager.handleAppStart() }
    }

    private fun showRolloverDialog() {
        composeTestRule.setContent {
            MinusTheme {
                val show by transitionManager.shouldShowTransitionDialog.collectAsState()
                val data by transitionManager.midnightTransitionData.collectAsState()
                val scope = rememberCoroutineScope()
                val transition = data
                if (show && transition != null && !transition.shouldNavigateToAnalyticsOnly) {
                    RolloverDialog(
                        remainingAmount = transition.remainingAmount,
                        currencyCode = transition.currencyCode,
                        periodLabel = "${transition.periodStartDate} - ${transition.periodEndDate}",
                        spentAmount = if (transition.isPersistedReopen) null else transition.totalSpent,
                        onSplitEqually = {
                            scope.launch {
                                transitionManager.resolveUnresolvedSurplus(RemainingBudgetStrategy.SPLIT_EQUALLY)
                            }
                        },
                        onCarryToNextDay = {
                            scope.launch {
                                transitionManager.resolveUnresolvedSurplus(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)
                            }
                        },
                        onViewAnalytics = {
                            scope.launch { transitionManager.resolveUnresolvedSurplus(null) }
                        },
                        onDismiss = { transitionManager.onTransitionDialogDismissed() },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun answerDialog(labelResId: Int) {
        composeTestRule.onNodeWithText(label(labelResId)).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            !transitionManager.shouldShowTransitionDialog.value
        }
    }

    private fun startNextPeriod(
        totalBudget: BigDecimal = nextPeriodBudget,
        splitMode: BudgetSplitMode = BudgetSplitMode.STATIC,
    ): BudgetSettings {
        savedSettings.clear()
        runBlocking {
            periodManager.persistBudgetSettings(
                settings = periodSettings(
                    startDate = today,
                    endDate = today.plusDays(29),
                    totalBudget = totalBudget,
                    strategy = RemainingBudgetStrategy.ASK_ALWAYS,
                    splitMode = splitMode,
                ),
                forceNewPeriodBoundary = false,
            )
        }
        return savedSettings.single()
    }

    private fun dayOneOf(settings: BudgetSettings): BudgetState = calculator.calculateBudgetState(
        settings = settings,
        transactions = emptyList(),
        currentDate = settings.startDate,
    )

    @Test
    fun when_a_period_ends_with_money_left_then_the_dialog_shows_the_surplus_and_what_was_spent() {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
        showRolloverDialog()

        composeTestRule.onNodeWithText(usd(surplus)).assertIsDisplayed()
        composeTestRule.onNodeWithText(label(R.string.rollover_dialog_split_equally_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(label(R.string.rollover_dialog_carry_to_tomorrow_title))
            .assertIsDisplayed()
    }

    @Test
    fun when_a_period_ends_over_budget_then_no_question_is_asked() {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS, spent = BigDecimal("1300.00"))
        showRolloverDialog()

        composeTestRule.onNodeWithText(label(R.string.rollover_dialog_split_equally_title))
            .assertDoesNotExist()
        assertThat(transitionManager.midnightTransitionData.value?.shouldNavigateToAnalyticsOnly)
            .isTrue()
    }

    @Test
    fun when_the_surplus_strategy_is_already_decided_then_no_question_is_asked() {
        endLastPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY)
        showRolloverDialog()

        composeTestRule.onNodeWithText(label(R.string.rollover_dialog_split_equally_title))
            .assertDoesNotExist()
        assertThat(pendingRollover.second).isEqualTo(RemainingBudgetStrategy.SPLIT_EQUALLY)
    }

    @Test
    fun when_answering_spread_it_then_day_one_of_the_next_period_has_a_bigger_daily_budget() {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
        showRolloverDialog()

        answerDialog(R.string.rollover_dialog_split_equally_title)
        val next = startNextPeriod()
        val dayOne = dayOneOf(next)

        assertThat(next.totalBudget).isEqualTo(BigDecimal("1500.00"))
        assertThat(next.rollOverCarryForward).isFalse()
        assertThat(dayOne.dailyBudget).isEqualTo(BigDecimal("50.00"))
        assertThat(dayOne.remainingToday).isEqualTo(BigDecimal("50.00"))
    }

    @Test
    fun when_answering_carry_it_then_day_one_of_the_next_period_holds_the_whole_surplus() {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
        showRolloverDialog()

        answerDialog(R.string.rollover_dialog_carry_to_tomorrow_title)
        val next = startNextPeriod()
        val dayOne = dayOneOf(next)

        assertThat(next.totalBudget).isEqualTo(BigDecimal("1500.00"))
        assertThat(next.rollOverCarryForward).isTrue()
        assertThat(next.rollOverAppliedDate).isEqualTo(today)
        assertThat(dayOne.dailyBudget).isEqualTo(BigDecimal("40.00"))
        assertThat(dayOne.remainingToday).isEqualTo(BigDecimal("340.00"))
    }

    @Test
    fun when_answering_carry_it_then_the_lump_is_gone_again_on_day_two() {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
        showRolloverDialog()

        answerDialog(R.string.rollover_dialog_carry_to_tomorrow_title)
        val next = startNextPeriod()
        val dayTwo = calculator.calculateBudgetState(
            settings = next,
            transactions = emptyList(),
            currentDate = next.startDate.plusDays(1),
        )

        assertThat(dayTwo.remainingToday).isEqualTo(BigDecimal("40.00"))
    }

    @Test
    fun when_discarding_the_surplus_then_the_next_period_is_exactly_what_the_user_typed() {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
        showRolloverDialog()

        answerDialog(R.string.rollover_dialog_view_analytics_title)
        val next = startNextPeriod()

        assertThat(next.totalBudget).isEqualTo(nextPeriodBudget)
        assertThat(dayOneOf(next).dailyBudget).isEqualTo(BigDecimal("40.00"))
        assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun when_cancelling_the_dialog_then_the_next_period_starts_without_the_surplus_but_it_is_not_lost() {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
        showRolloverDialog()

        answerDialog(R.string.cancel)
        val next = startNextPeriod()

        assertThat(next.totalBudget).isEqualTo(nextPeriodBudget)
        assertThat(pendingRollover.first).isEqualTo(surplus)
        assertThat(pendingRollover.second).isNull()
    }

    @Test
    fun when_the_surplus_was_left_unanswered_then_it_can_still_be_claimed_into_the_running_period() {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
        showRolloverDialog()
        answerDialog(R.string.cancel)
        startNextPeriod()

        savedSettings.clear()
        runBlocking {
            transitionManager.reopenUnresolvedSurplusDialog()
            transitionManager.resolveUnresolvedSurplus(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)
        }

        val claimed = savedSettings.single()
        assertThat(claimed.totalBudget).isEqualTo(BigDecimal("1500.00"))
        assertThat(dayOneOf(claimed).remainingToday).isEqualTo(BigDecimal("340.00"))
        assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun when_spread_it_was_chosen_up_front_then_the_next_period_picks_the_surplus_up_silently() {
        endLastPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY)

        val next = startNextPeriod()

        assertThat(next.totalBudget).isEqualTo(BigDecimal("1500.00"))
        assertThat(dayOneOf(next).dailyBudget).isEqualTo(BigDecimal("50.00"))
    }

    @Test
    fun when_day_one_lump_was_chosen_up_front_then_the_next_period_opens_with_the_lump() {
        endLastPeriod(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        val next = startNextPeriod()
        val dayOne = dayOneOf(next)

        assertThat(dayOne.dailyBudget).isEqualTo(BigDecimal("40.00"))
        assertThat(dayOne.remainingToday).isEqualTo(BigDecimal("340.00"))
    }

    @Test
    fun when_the_period_ended_over_budget_then_nothing_is_handed_to_the_next_one() {
        endLastPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY, spent = BigDecimal("1300.00"))

        val next = startNextPeriod()

        assertThat(next.totalBudget).isEqualTo(nextPeriodBudget)
        assertThat(dayOneOf(next).isOverBudget).isFalse()
    }

    @Test
    fun when_the_new_period_runs_on_carry_over_then_an_unspent_lump_is_banked() {
        endLastPeriod(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        val next = startNextPeriod(splitMode = BudgetSplitMode.CARRY_OVER)
        val dayTwo = calculator.calculateBudgetState(
            settings = next,
            transactions = emptyList(),
            currentDate = next.startDate.plusDays(1),
        )

        assertThat(dayOneOf(next).remainingToday).isEqualTo(BigDecimal("340.00"))
        assertThat(dayTwo.remainingToday).isEqualTo(BigDecimal("380.00"))
    }

    @Test
    fun when_the_new_period_runs_on_ask_me_then_an_unspent_lump_becomes_the_first_prompt() {
        endLastPeriod(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        val next = startNextPeriod(splitMode = BudgetSplitMode.ASK_ME)
        val dayTwo = calculator.calculateBudgetState(
            settings = next,
            transactions = emptyList(),
            currentDate = next.startDate.plusDays(1),
        )

        assertThat(dayTwo.remainingToday).isEqualTo(BigDecimal("40.00"))
        assertThat(dayTwo.pendingLeftover).isEqualTo(BigDecimal("340.00"))
    }

    @Test
    fun when_the_new_period_runs_on_ask_me_and_the_surplus_was_spread_then_the_rate_itself_is_higher() {
        endLastPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY)

        val next = startNextPeriod(splitMode = BudgetSplitMode.ASK_ME)
        val dayOne = dayOneOf(next)

        assertThat(dayOne.dailyBudget).isEqualTo(BigDecimal("50.00"))
        assertThat(dayOne.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }
}
