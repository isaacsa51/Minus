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
import com.serranoie.app.minus.domain.calculator.RecurringExpenseCalculator
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.time.MidnightPeriodChecker
import com.serranoie.app.minus.domain.time.MidnightTransitionManager
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.DailySurplusDialog
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import io.mockk.coEvery
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

class DailySurplusDialogFlowE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private val checker = MidnightPeriodChecker(
        budgetRepository, settingsRepository, BudgetStateCalculator(RecurringExpenseCalculator()),
    )
    private val transitionManager = MidnightTransitionManager(checker)

    private val today: LocalDate = LocalDate.now()
    private val yesterday: LocalDate = today.minusDays(1)
    private val periodEnd: LocalDate = today.plusDays(10)
    private val baseBudget = BigDecimal("1000.00")
    private val spentYesterday = BigDecimal("30.00")
    // daysRemaining from yesterday's perspective = 12, so the baseline dailyBudget is
    // 1000/12 = 83.33, and the surplus is 83.33 - 30.00 = 53.33.
    private val surplus = BigDecimal("53.33")

    private var lastDailySurplusCheckMillis: Long? = null
    private var savedTransactions = mutableListOf<Transaction>()
    private var savedSettings = mutableListOf<BudgetSettings>()

    private fun usd(amount: BigDecimal): String = symbolOnlyCurrencyFormat("USD").format(amount)
    private fun label(resId: Int): String = composeTestRule.activity.getString(resId)

    private val settings = BudgetSettings(
        totalBudget = baseBudget,
        period = BudgetPeriod.MONTHLY,
        startDate = today.minusDays(10),
        endDate = periodEnd,
        currencyCode = "USD",
        remainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS,
        splitMode = BudgetSplitMode.DYNAMIC,
    )

    @Before
    fun setUp() {
        lastDailySurplusCheckMillis = yesterday
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        coEvery { budgetRepository.getBudgetSettingsSync() } returns settings
        coEvery { settingsRepository.observeBudgetEndDate() } returns flowOf(
            periodEnd.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(false)
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT.copy(
            earlyFinishActive = false,
            periodEndAlreadyHandled = false,
        )
        coEvery { budgetRepository.getTransactions() } returns flowOf(
            listOf(Transaction.create(amount = spentYesterday, date = yesterday.atTime(10, 0)))
        )
        coEvery { settingsRepository.getLastDailySurplusCheckDate() } answers {
            lastDailySurplusCheckMillis
        }
        coEvery { settingsRepository.setLastDailySurplusCheckDate(any()) } answers {
            lastDailySurplusCheckMillis = firstArg()
        }
        coEvery { settingsRepository.getCurrentPeriodId() } returns 7L
        coEvery { budgetRepository.addTransaction(any()) } answers { savedTransactions.add(firstArg()) }
        coEvery { budgetRepository.saveBudgetSettings(any()) } answers { savedSettings.add(firstArg()) }
    }

    private fun launchDailySurplusFlow() {
        runBlocking { transitionManager.handleAppStart() }

        composeTestRule.setContent {
            MinusTheme {
                val show by transitionManager.shouldShowDailySurplusDialog.collectAsState()
                val data by transitionManager.dailySurplusData.collectAsState()
                val scope = rememberCoroutineScope()
                val d = data
                if (show && d != null) {
                    DailySurplusDialog(
                        surplusAmount = d.surplusAmount,
                        currencyCode = d.currencyCode,
                        onAddToToday = {
                            scope.launch { transitionManager.onDailySurplusAddToToday() }
                        },
                        onSpread = { transitionManager.onDailySurplusDialogDismissed() },
                        onDismiss = { transitionManager.onDailySurplusDialogDismissed() },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun tapAndAwaitDismiss(labelResId: Int) {
        composeTestRule.onNodeWithText(label(labelResId)).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            !transitionManager.shouldShowDailySurplusDialog.value
        }
    }

    @Test
    fun when_yesterday_is_underspent_then_the_dialog_is_shown_with_the_surplus() {
        launchDailySurplusFlow()

        composeTestRule.onNodeWithText(label(R.string.daily_surplus_add_to_today_title))
            .assertIsDisplayed()
        // The surplus amount is only ever embedded in full sentences (question + both action
        // row descriptions), so assert on the interpolated question text rather than the bare
        // amount, which can otherwise match an off-screen node further down the dialog.
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(R.string.daily_surplus_dialog_question, usd(surplus)),
        ).assertIsDisplayed()
    }

    @Test
    fun choosing_add_to_today_inserts_a_yesterday_dated_adjustment_and_sets_todays_carry_forward() {
        launchDailySurplusFlow()

        tapAndAwaitDismiss(R.string.daily_surplus_add_to_today_title)

        assertThat(savedTransactions).hasSize(1)
        val adjustment = savedTransactions.single()
        assertThat(adjustment.isAdjustment).isTrue()
        assertThat(adjustment.amount).isEqualTo(surplus)
        assertThat(adjustment.date?.toLocalDate()).isEqualTo(yesterday)

        assertThat(savedSettings).hasSize(1)
        val updated = savedSettings.single()
        assertThat(updated.dailyCarryForwardDate).isEqualTo(today)
        assertThat(updated.dailyCarryForwardAmount).isEqualTo(surplus)

        assertThat(transitionManager.dailySurplusData.value).isNull()
    }

    @Test
    fun choosing_spread_changes_nothing_and_dismisses_the_dialog() {
        launchDailySurplusFlow()

        tapAndAwaitDismiss(R.string.daily_surplus_spread_title)

        assertThat(savedTransactions).isEmpty()
        assertThat(savedSettings).isEmpty()
        assertThat(transitionManager.dailySurplusData.value).isNull()
    }

    @Test
    fun cancelling_the_dialog_changes_nothing_and_dismisses_the_dialog() {
        launchDailySurplusFlow()

        tapAndAwaitDismiss(R.string.cancel)

        assertThat(savedTransactions).isEmpty()
        assertThat(savedSettings).isEmpty()
        assertThat(transitionManager.dailySurplusData.value).isNull()
    }
}
