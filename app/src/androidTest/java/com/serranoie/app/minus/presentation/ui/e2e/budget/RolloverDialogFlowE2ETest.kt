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
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.time.MidnightPeriodChecker
import com.serranoie.app.minus.domain.time.MidnightTransitionManager
import com.serranoie.app.minus.domain.time.TimeProvider
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.ui.budget.BudgetPeriodManager
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

class RolloverDialogFlowE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val timeProvider: TimeProvider = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)

    private val checker = MidnightPeriodChecker(budgetRepository, settingsRepository)
    private val transitionManager = MidnightTransitionManager(checker)
    private lateinit var periodManager: BudgetPeriodManager

    private var pendingRollover: Pair<BigDecimal, RemainingBudgetStrategy?> =
        BigDecimal.ZERO to null
    private var userSettings = UserSettings.DEFAULT.copy(
        earlyFinishActive = false,
        periodEndAlreadyHandled = false,
    )

    private val periodEnd: LocalDate = LocalDate.now().minusDays(1)
    private val baseBudget = BigDecimal("1000.00")
    private val surplus = BigDecimal("300.00")

    private fun usd(amount: BigDecimal): String = symbolOnlyCurrencyFormat("USD").format(amount)
    private fun label(resId: Int): String = composeTestRule.activity.getString(resId)

    private fun monthlySettings(
        startDate: LocalDate,
        endDate: LocalDate,
        totalBudget: BigDecimal = baseBudget,
    ) = BudgetSettings(
        totalBudget = totalBudget,
        period = BudgetPeriod.MONTHLY,
        startDate = startDate,
        endDate = endDate,
        currencyCode = "USD",
        remainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS,
    )

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
        coEvery { settingsRepository.setPendingRollover(any(), any()) } answers {
            pendingRollover = firstArg<BigDecimal>() to secondArg()
        }
        coEvery { settingsRepository.clearPendingRollover() } answers {
            pendingRollover = BigDecimal.ZERO to null
        }
        coEvery { settingsRepository.getSettings() } answers { userSettings }
        coEvery { settingsRepository.setPeriodEndAlreadyHandled(any()) } answers {
            userSettings = userSettings.copy(periodEndAlreadyHandled = firstArg())
        }
        coEvery { settingsRepository.setEarlyFinishActive(any(), any(), any()) } answers {
            userSettings = userSettings.copy(
                earlyFinishActive = firstArg(),
                earlyFinishActualDate = secondArg(),
                earlyFinishOriginalEndDate = thirdArg(),
            )
        }
        coEvery { settingsRepository.clearEarlyFinish() } answers {
            userSettings = userSettings.copy(
                earlyFinishActive = false,
                earlyFinishActualDate = 0L,
                earlyFinishOriginalEndDate = 0L,
            )
        }
        every { timeProvider.nowEpochMillis() } returns 1_000_000L

        coEvery { budgetRepository.getBudgetSettingsSync() } returns monthlySettings(
            startDate = periodEnd.minusDays(29),
            endDate = periodEnd,
        )
        coEvery { settingsRepository.observeBudgetEndDate() } returns flowOf(
            periodEnd.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(false)
        coEvery { budgetRepository.getTransactions() } returns flowOf(
            listOf(
                Transaction.create(
                    amount = baseBudget.subtract(surplus),
                    date = periodEnd.minusDays(5).atStartOfDay(),
                )
            )
        )
    }

    private fun launchRolloverFlow() {
        runBlocking { transitionManager.handleAppStart() }

        composeTestRule.setContent {
            MinusTheme {
                val show by transitionManager.shouldShowTransitionDialog.collectAsState()
                val data by transitionManager.midnightTransitionData.collectAsState()
                val scope = rememberCoroutineScope()
                val d = data
                if (show && d != null && !d.shouldNavigateToAnalyticsOnly) {
                    RolloverDialog(
                        remainingAmount = d.remainingAmount,
                        currencyCode = d.currencyCode,
                        periodLabel = "${d.periodStartDate} - ${d.periodEndDate}",
                        spentAmount = d.totalSpent,
                        onSplitEqually = {
                            scope.launch { transitionManager.rollRemainingSplitEqually() }
                        },
                        onCarryToNextDay = {
                            scope.launch { transitionManager.rollRemainingToFirstDay() }
                        },
                        onViewAnalytics = { transitionManager.onTransitionDialogConfirmed() },
                        onDismiss = { transitionManager.onTransitionDialogDismissed() },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun tapAndAwaitDismiss(labelResId: Int) {
        composeTestRule.onNodeWithText(label(labelResId)).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            !transitionManager.shouldShowTransitionDialog.value
        }
    }

    private fun startNextPeriod(newIncome: BigDecimal): BudgetSettings {
        val saved = mutableListOf<BudgetSettings>()
        coEvery { budgetRepository.saveBudgetSettings(any()) } answers { saved.add(firstArg()) }
        runBlocking {
            periodManager.persistBudgetSettings(
                monthlySettings(
                    startDate = LocalDate.now(),
                    endDate = LocalDate.now().plusDays(29),
                    totalBudget = newIncome,
                ),
                forceNewPeriodBoundary = true,
            )
        }
        return saved.single()
    }

    @Test
    fun when_a_period_ends_then_the_rollover_dialog_is_shown_with_the_surplus() {
        launchRolloverFlow()

        composeTestRule.onNodeWithText(label(R.string.rollover_dialog_split_equally_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(usd(surplus)).assertIsDisplayed()
    }

    @Test
    fun choosing_split_equally_queues_a_split_rollover_and_dismisses_the_dialog() {
        launchRolloverFlow()

        tapAndAwaitDismiss(R.string.rollover_dialog_split_equally_title)

        assertThat(pendingRollover.first).isEqualTo(surplus)
        assertThat(pendingRollover.second).isEqualTo(RemainingBudgetStrategy.SPLIT_EQUALLY)
        assertThat(transitionManager.midnightTransitionData.value).isNull()
    }

    @Test
    fun choosing_split_equally_then_the_next_period_total_grows_by_the_surplus() {
        launchRolloverFlow()
        tapAndAwaitDismiss(R.string.rollover_dialog_split_equally_title)

        val next = startNextPeriod(newIncome = BigDecimal("1200.00"))

        assertThat(next.totalBudget).isEqualTo(BigDecimal("1500.00"))
        assertThat(next.rollOverCarryForward).isFalse()
    }

    @Test
    fun choosing_carry_to_tomorrow_queues_an_add_to_first_day_rollover_and_dismisses_the_dialog() {
        launchRolloverFlow()

        tapAndAwaitDismiss(R.string.rollover_dialog_carry_to_tomorrow_title)

        assertThat(pendingRollover.first).isEqualTo(surplus)
        assertThat(pendingRollover.second).isEqualTo(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)
        assertThat(transitionManager.midnightTransitionData.value).isNull()
    }

    @Test
    fun choosing_carry_to_tomorrow_then_the_next_period_carries_it_as_a_first_day_allowance() {
        launchRolloverFlow()
        tapAndAwaitDismiss(R.string.rollover_dialog_carry_to_tomorrow_title)

        val next = startNextPeriod(newIncome = baseBudget)

        assertThat(next.totalBudget).isEqualTo(baseBudget)
        assertThat(next.rollOverCarryForward).isTrue()
        assertThat(next.rollOverLimit).isEqualTo(surplus)
    }

    @Test
    fun discarding_the_remaining_budget_queues_nothing_and_dismisses_the_dialog() {
        launchRolloverFlow()

        tapAndAwaitDismiss(R.string.rollover_dialog_view_analytics_title)

        assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)
        assertThat(pendingRollover.second).isNull()
        assertThat(transitionManager.midnightTransitionData.value).isNull()

        assertThat(startNextPeriod(newIncome = baseBudget).totalBudget).isEqualTo(baseBudget)
    }

    @Test
    fun cancelling_the_dialog_queues_nothing_and_dismisses_the_dialog() {
        launchRolloverFlow()

        tapAndAwaitDismiss(R.string.cancel)

        assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)
        assertThat(transitionManager.midnightTransitionData.value).isNull()

        assertThat(startNextPeriod(newIncome = baseBudget).totalBudget).isEqualTo(baseBudget)
    }
}
