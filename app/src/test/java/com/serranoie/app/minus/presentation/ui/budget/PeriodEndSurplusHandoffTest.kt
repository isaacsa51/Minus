package com.serranoie.app.minus.presentation.ui.budget

import com.google.common.truth.Truth.assertThat
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
import com.serranoie.app.minus.domain.time.TimeProvider
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/**
 * What happens to the money left over when a period finishes and the next one is created.
 *
 * This walks the whole handoff rather than a single class: [MidnightPeriodChecker] decides whether
 * to ask the user or to queue the surplus silently, [BudgetPeriodManager] folds whatever was queued
 * into the settings of the new period, and [BudgetStateCalculator] is then asked what day one of
 * that new period looks like — which is the part the user actually sees.
 *
 * Scenario: a 30-day period of 1000 that ended yesterday with 700 spent, so 300 is left over.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PeriodEndSurplusHandoffTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val timeProvider: TimeProvider = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)

    private val checker = MidnightPeriodChecker(budgetRepository, settingsRepository)
    private val calculator = BudgetStateCalculator()
    private lateinit var periodManager: BudgetPeriodManager

    private var pendingRollover: Pair<BigDecimal, RemainingBudgetStrategy?> = BigDecimal.ZERO to null
    private var userSettings = UserSettings.DEFAULT
    private var currentPeriodRollover: Pair<BigDecimal, Boolean> = BigDecimal.ZERO to false
    private var liveSettings: BudgetSettings? = null
    private val savedSettings = mutableListOf<BudgetSettings>()

    private val today: LocalDate = LocalDate.now()
    private val lastPeriodEnd: LocalDate = today.minusDays(1)
    private val lastPeriodStart: LocalDate = lastPeriodEnd.minusDays(29)
    private val lastPeriodId = 100L
    private val lastPeriodBudget = BigDecimal("1000.00")

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
        coEvery { settingsRepository.markSurplusUnresolved(any()) } answers {
            pendingRollover = firstArg<BigDecimal>() to null
        }
        coEvery { settingsRepository.setCurrentPeriodRollover(any(), any()) } answers {
            currentPeriodRollover = firstArg<BigDecimal>() to secondArg()
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

    /**
     * Arranges a period that ran out yesterday having spent [spent], then lets the app notice it
     * the way a cold start would.
     */
    private suspend fun endLastPeriod(
        strategy: RemainingBudgetStrategy,
        spent: BigDecimal = BigDecimal("700.00"),
        splitMode: BudgetSplitMode = BudgetSplitMode.STATIC,
    ) {
        liveSettings = periodSettings(
            startDate = lastPeriodStart,
            endDate = lastPeriodEnd,
            totalBudget = lastPeriodBudget,
            strategy = strategy,
            splitMode = splitMode,
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

        checker.handleEndingPeriod()
    }

    private suspend fun startNextPeriod(
        totalBudget: BigDecimal = BigDecimal("1200.00"),
        splitMode: BudgetSplitMode = BudgetSplitMode.STATIC,
        strategy: RemainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS,
    ): BudgetSettings {
        savedSettings.clear()
        periodManager.persistBudgetSettings(
            settings = periodSettings(
                startDate = today,
                endDate = today.plusDays(29),
                totalBudget = totalBudget,
                strategy = strategy,
                splitMode = splitMode,
            ),
            forceNewPeriodBoundary = false,
        )
        return savedSettings.single()
    }

    private fun dayOneOf(settings: BudgetSettings): BudgetState = calculator.calculateBudgetState(
        settings = settings,
        transactions = emptyList(),
        currentDate = settings.startDate,
    )

    private fun dayTwoOf(settings: BudgetSettings): BudgetState = calculator.calculateBudgetState(
        settings = settings,
        transactions = emptyList(),
        currentDate = settings.startDate.plusDays(1),
    )

    @Test
    fun `a period that ends with money left asks the user, and queues nothing on its own`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)

        assertThat(checker.shouldShowTransitionDialog.value).isTrue()
        assertThat(checker.midnightTransitionData.value?.remainingAmount).isEqualTo(BigDecimal("300.00"))
        assertThat(checker.midnightTransitionData.value?.totalSpent).isEqualTo(BigDecimal("700.00"))
        assertThat(checker.midnightTransitionData.value?.shouldNavigateToAnalyticsOnly).isFalse()
        assertThat(pendingRollover.second).isNull()
    }

    @Test
    fun `answering spread it makes the next period's daily budget bigger from day one`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
        checker.resolveUnresolvedSurplus(RemainingBudgetStrategy.SPLIT_EQUALLY)

        val next = startNextPeriod(totalBudget = BigDecimal("1200.00"))
        val dayOne = dayOneOf(next)

        assertThat(next.totalBudget).isEqualTo(BigDecimal("1500.00"))
        assertThat(next.rollOverCarryForward).isFalse()
        assertThat(dayOne.dailyBudget).isEqualTo(BigDecimal("50.00"))
        assertThat(dayOne.remainingToday).isEqualTo(BigDecimal("50.00"))
    }

    @Test
    fun `answering give it to me now puts the whole surplus on day one without raising the daily budget`() =
        runTest {
            endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
            checker.resolveUnresolvedSurplus(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

            val next = startNextPeriod(totalBudget = BigDecimal("1200.00"))
            val dayOne = dayOneOf(next)

            assertThat(next.totalBudget).isEqualTo(BigDecimal("1500.00"))
            assertThat(next.rollOverCarryForward).isTrue()
            assertThat(next.rollOverLimit).isEqualTo(BigDecimal("300.00"))
            assertThat(next.rollOverAppliedDate).isEqualTo(today)
            assertThat(dayOne.dailyBudget).isEqualTo(BigDecimal("40.00"))
            assertThat(dayOne.remainingToday).isEqualTo(BigDecimal("340.00"))
        }

    @Test
    fun `the lump sum is only offered on day one, it does not repeat every day`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
        checker.resolveUnresolvedSurplus(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        val next = startNextPeriod(totalBudget = BigDecimal("1200.00"))

        assertThat(dayTwoOf(next).remainingToday).isEqualTo(BigDecimal("40.00"))
    }

    @Test
    fun `discarding the surplus leaves the next period with exactly what the user typed`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
        checker.resolveUnresolvedSurplus(null)

        val next = startNextPeriod(totalBudget = BigDecimal("1200.00"))

        assertThat(next.totalBudget).isEqualTo(BigDecimal("1200.00"))
        assertThat(next.rollOverLimit).isNull()
        assertThat(dayOneOf(next).dailyBudget).isEqualTo(BigDecimal("40.00"))
    }

    @Test
    fun `never answering leaves the surplus out of the new period, but does not throw it away`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
        checker.onTransitionDialogDismissed()

        val next = startNextPeriod(totalBudget = BigDecimal("1200.00"))

        assertThat(next.totalBudget).isEqualTo(BigDecimal("1200.00"))
        assertThat(pendingRollover.first).isEqualTo(BigDecimal("300.00"))
        assertThat(pendingRollover.second).isNull()
    }

    @Test
    fun `a surplus left unanswered can still be claimed once the new period is running`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS)
        checker.onTransitionDialogDismissed()
        val next = startNextPeriod(totalBudget = BigDecimal("1200.00"))
        assertThat(next.totalBudget).isEqualTo(BigDecimal("1200.00"))

        savedSettings.clear()
        checker.resolveUnresolvedSurplus(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        val claimed = savedSettings.single()
        assertThat(claimed.totalBudget).isEqualTo(BigDecimal("1500.00"))
        assertThat(claimed.rollOverAppliedDate).isEqualTo(today)
        assertThat(dayOneOf(claimed).remainingToday).isEqualTo(BigDecimal("340.00"))
        assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `with spread it chosen up front the surplus is queued silently and the user is only shown analytics`() =
        runTest {
            endLastPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY)

            assertThat(pendingRollover).isEqualTo(BigDecimal("300.00") to RemainingBudgetStrategy.SPLIT_EQUALLY)
            assertThat(checker.midnightTransitionData.value?.shouldNavigateToAnalyticsOnly).isTrue()

            val next = startNextPeriod(totalBudget = BigDecimal("1200.00"))

            assertThat(next.totalBudget).isEqualTo(BigDecimal("1500.00"))
            assertThat(dayOneOf(next).dailyBudget).isEqualTo(BigDecimal("50.00"))
        }

    @Test
    fun `with day one lump chosen up front the surplus lands on the first day of the next period`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        assertThat(pendingRollover).isEqualTo(BigDecimal("300.00") to RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        val next = startNextPeriod(totalBudget = BigDecimal("1200.00"))
        val dayOne = dayOneOf(next)

        assertThat(next.rollOverCarryForward).isTrue()
        assertThat(dayOne.dailyBudget).isEqualTo(BigDecimal("40.00"))
        assertThat(dayOne.remainingToday).isEqualTo(BigDecimal("340.00"))
    }

    @Test
    fun `the queued surplus is consumed once, a third period starts clean`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY)
        val second = startNextPeriod(totalBudget = BigDecimal("1200.00"))
        assertThat(second.totalBudget).isEqualTo(BigDecimal("1500.00"))

        userSettings = userSettings.copy(periodEndAlreadyHandled = true)
        savedSettings.clear()
        periodManager.persistBudgetSettings(
            settings = periodSettings(
                startDate = today.plusDays(30),
                endDate = today.plusDays(59),
                totalBudget = BigDecimal("1200.00"),
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
            ),
            forceNewPeriodBoundary = false,
        )

        assertThat(savedSettings.single().totalBudget).isEqualTo(BigDecimal("1200.00"))
        assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `a period that ended over budget hands nothing over and routes the user to analytics`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.ASK_ALWAYS, spent = BigDecimal("1300.00"))

        assertThat(checker.midnightTransitionData.value?.remainingAmount).isEqualTo(BigDecimal("-300.00"))
        assertThat(checker.midnightTransitionData.value?.shouldNavigateToAnalyticsOnly).isTrue()
        assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)

        val next = startNextPeriod(totalBudget = BigDecimal("1200.00"))

        assertThat(next.totalBudget).isEqualTo(BigDecimal("1200.00"))
        assertThat(dayOneOf(next).dailyBudget).isEqualTo(BigDecimal("40.00"))
    }

    @Test
    fun `a period that ended exactly on budget hands nothing over`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.ADD_TO_FIRST_DAY, spent = lastPeriodBudget)

        assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)
        assertThat(startNextPeriod(totalBudget = BigDecimal("1200.00")).totalBudget)
            .isEqualTo(BigDecimal("1200.00"))
    }

    @Test
    fun `the debt of an overspent period is not carried into the next one`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY, spent = BigDecimal("1300.00"))

        val next = startNextPeriod(totalBudget = BigDecimal("1200.00"))

        assertThat(next.totalBudget).isEqualTo(BigDecimal("1200.00"))
        assertThat(dayOneOf(next).isOverBudget).isFalse()
    }

    @Test
    fun `the surplus is what the period had left, whatever split mode it ran on`() = runTest {
        BudgetSplitMode.entries.forEach { mode ->
            pendingRollover = BigDecimal.ZERO to null
            checker.onTransitionDialogConfirmed()

            endLastPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY, splitMode = mode)

            assertThat(pendingRollover.first).isEqualTo(BigDecimal("300.00"))
        }
    }

    @Test
    fun `the amount folded into the new period is recomputed at the boundary, not the number shown in the dialog`() =
        runTest {
            endLastPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY)
            assertThat(pendingRollover.first).isEqualTo(BigDecimal("300.00"))

            // a forgotten expense is filed against the closed period before the new one is created
            coEvery { budgetRepository.getTransactions() } returns flowOf(
                listOf(
                    Transaction(
                        id = 1L,
                        amount = BigDecimal("700.00"),
                        date = lastPeriodStart.plusDays(3).atStartOfDay(),
                        periodId = lastPeriodId,
                    ),
                    Transaction(
                        id = 2L,
                        amount = BigDecimal("120.00"),
                        date = lastPeriodEnd.atStartOfDay(),
                        periodId = lastPeriodId,
                    ),
                ),
            )

            val next = startNextPeriod(totalBudget = BigDecimal("1200.00"))

            assertThat(next.totalBudget).isEqualTo(BigDecimal("1380.00"))
        }

    @Test
    fun `ending a period twice in a row does not hand the surplus over twice`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY)
        checker.handleEndingPeriod()

        assertThat(pendingRollover.first).isEqualTo(BigDecimal("300.00"))
        assertThat(startNextPeriod(totalBudget = BigDecimal("1200.00")).totalBudget)
            .isEqualTo(BigDecimal("1500.00"))
    }

    @Test
    fun `a day one lump left unspent is banked by a carry over period`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        val next = startNextPeriod(
            totalBudget = BigDecimal("1200.00"),
            splitMode = BudgetSplitMode.CARRY_OVER,
        )

        assertThat(dayOneOf(next).remainingToday).isEqualTo(BigDecimal("340.00"))
        assertThat(dayTwoOf(next).remainingToday).isEqualTo(BigDecimal("380.00"))
    }

    @Test
    fun `a day one lump left unspent becomes the first ask me prompt`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        val next = startNextPeriod(
            totalBudget = BigDecimal("1200.00"),
            splitMode = BudgetSplitMode.ASK_ME,
        )
        val dayTwo = dayTwoOf(next)

        assertThat(dayOneOf(next).remainingToday).isEqualTo(BigDecimal("340.00"))
        assertThat(dayTwo.remainingToday).isEqualTo(BigDecimal("40.00"))
        assertThat(dayTwo.pendingLeftover).isEqualTo(BigDecimal("340.00"))
    }

    @Test
    fun `a spread surplus raises the rate of an ask me period instead of waiting behind a prompt`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY)

        val next = startNextPeriod(
            totalBudget = BigDecimal("1200.00"),
            splitMode = BudgetSplitMode.ASK_ME,
        )
        val dayOne = dayOneOf(next)

        assertThat(dayOne.dailyBudget).isEqualTo(BigDecimal("50.00"))
        assertThat(dayOne.pendingLeftover).isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `a dynamic period simply starts from the bigger total`() = runTest {
        endLastPeriod(RemainingBudgetStrategy.SPLIT_EQUALLY)

        val next = startNextPeriod(
            totalBudget = BigDecimal("1200.00"),
            splitMode = BudgetSplitMode.DYNAMIC,
        )

        assertThat(dayOneOf(next).dailyBudget).isEqualTo(BigDecimal("50.00"))
        assertThat(dayOneOf(next).totalBudget).isEqualTo(BigDecimal("1500.00"))
    }

    @Test
    fun `finishing early with ask me chosen asks about the balance left on the day it was stopped`() = runTest {
        liveSettings = periodSettings(
            startDate = today.minusDays(10),
            endDate = today.plusDays(19),
            totalBudget = lastPeriodBudget,
            strategy = RemainingBudgetStrategy.ASK_ALWAYS,
        )
        userSettings = UserSettings.DEFAULT.copy(currentPeriodId = lastPeriodId, currentPeriodStartedAt = 1L)
        coEvery { budgetRepository.getTransactions() } returns flowOf(
            listOf(
                Transaction(
                    id = 1L,
                    amount = BigDecimal("250.00"),
                    date = today.minusDays(5).atStartOfDay(),
                    periodId = lastPeriodId,
                ),
            ),
        )

        periodManager.finishBudgetEarly()

        assertThat(checker.shouldShowTransitionDialog.value).isTrue()
        assertThat(checker.midnightTransitionData.value?.remainingAmount).isEqualTo(BigDecimal("750.00"))
        assertThat(pendingRollover).isEqualTo(BigDecimal("750.00") to null)
    }

    @Test
    fun `finishing early and answering give it to me now starts the next period with the lump on day one`() =
        runTest {
            liveSettings = periodSettings(
                startDate = today.minusDays(10),
                endDate = today.plusDays(19),
                totalBudget = lastPeriodBudget,
                strategy = RemainingBudgetStrategy.ASK_ALWAYS,
            )
            userSettings = UserSettings.DEFAULT.copy(currentPeriodId = lastPeriodId, currentPeriodStartedAt = 1L)
            coEvery { budgetRepository.getTransactions() } returns flowOf(
                listOf(
                    Transaction(
                        id = 1L,
                        amount = BigDecimal("250.00"),
                        date = today.minusDays(5).atStartOfDay(),
                        periodId = lastPeriodId,
                    ),
                ),
            )

            periodManager.finishBudgetEarly()
            checker.resolveUnresolvedSurplus(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)
            val next = startNextPeriod(totalBudget = BigDecimal("1200.00"))

            assertThat(next.totalBudget).isEqualTo(BigDecimal("1950.00"))
            assertThat(dayOneOf(next).dailyBudget).isEqualTo(BigDecimal("40.00"))
            assertThat(dayOneOf(next).remainingToday).isEqualTo(BigDecimal("790.00"))
        }
}
