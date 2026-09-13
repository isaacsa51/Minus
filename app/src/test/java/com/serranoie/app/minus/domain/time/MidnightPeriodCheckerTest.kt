package com.serranoie.app.minus.domain.time

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class MidnightPeriodCheckerTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val checker = MidnightPeriodChecker(budgetRepository, settingsRepository)

    private fun settingsEndingOn(
        endDate: LocalDate,
        strategy: RemainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS,
    ) = BudgetSettings(
        totalBudget = BigDecimal("1000.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = endDate.minusDays(29),
        endDate = endDate,
        currencyCode = "USD",
        remainingBudgetStrategy = strategy,
    )

    private fun stubEndDate(
        endDate: LocalDate,
        strategy: RemainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS,
    ) {
        coEvery { budgetRepository.getBudgetSettingsSync() } returns settingsEndingOn(endDate, strategy)
        coEvery { settingsRepository.observeBudgetEndDate() } returns flowOf(
            endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
    }

    private fun arrangeNaturalEndAskAlways(
        remaining: BigDecimal = BigDecimal("300.00"),
        strategy: RemainingBudgetStrategy = RemainingBudgetStrategy.ASK_ALWAYS,
    ): LocalDate {
        val endDate = LocalDate.now().minusDays(1)
        stubEndDate(endDate, strategy)
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(false)
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT.copy(
            earlyFinishActive = false,
            periodEndAlreadyHandled = false,
        )
        val spent = BigDecimal("1000.00").subtract(remaining)
        coEvery { budgetRepository.getTransactions() } returns flowOf(
            listOf(
                Transaction.create(
                    amount = spent,
                    date = endDate.minusDays(5).atStartOfDay(),
                )
            )
        )
        return endDate
    }

    @Test
    fun `when a period was finished early then it is never flagged as ended, even past the original schedule`() = runTest {
        val originalEndDate = LocalDate.now().minusDays(5) // original schedule already elapsed
        stubEndDate(originalEndDate)
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(false)
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT.copy(earlyFinishActive = true)

        val state = checker.resolveEndingPeriodState()

        assertThat(state.shouldHandleEndingPeriod).isFalse()
    }

    @Test
    fun `when a period was finished early and a stale midnight-transition flag is set then it gets cleared`() = runTest {
        val originalEndDate = LocalDate.now().minusDays(5)
        stubEndDate(originalEndDate)
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(true)
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT.copy(earlyFinishActive = true)

        val state = checker.resolveEndingPeriodState()

        assertThat(state.shouldHandleEndingPeriod).isFalse()
        coVerify { settingsRepository.setMidnightTransitionOccurred(false) }
    }

    @Test
    fun `when a period elapsed naturally (no early finish) then it is still flagged as ended`() = runTest {
        val originalEndDate = LocalDate.now().minusDays(1)
        stubEndDate(originalEndDate)
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(false)
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT.copy(earlyFinishActive = false)
        coEvery { budgetRepository.getTransactions() } returns flowOf(emptyList())

        val state = checker.resolveEndingPeriodState()

        assertThat(state.shouldHandleEndingPeriod).isTrue()
    }

    @Test
    fun `when a period is still active (no early finish, end date in the future) then it is not flagged as ended`() = runTest {
        val futureEndDate = LocalDate.now().plusDays(10)
        stubEndDate(futureEndDate)
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(false)
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT.copy(earlyFinishActive = false)
        coEvery { budgetRepository.getTransactions() } returns flowOf(emptyList())

        val state = checker.resolveEndingPeriodState()

        assertThat(state.shouldHandleEndingPeriod).isFalse()
    }

    @Test
    fun `when a period ends naturally with a cached midnight snapshot then totalSpent reflects the actual spend`() = runTest {
        val originalEndDate = LocalDate.now().minusDays(1)
        stubEndDate(originalEndDate)
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(true)
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT.copy(earlyFinishActive = false)
        coEvery { settingsRepository.getLastPeriodEnd() } returns originalEndDate
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        coEvery { settingsRepository.getRemainingFromLastPeriod() } returns BigDecimal("200.00")

        checker.handleEndingPeriod()

        assertThat(checker.midnightTransitionData.value?.totalSpent).isEqualTo(BigDecimal("800.00"))
    }

    @Test
    fun `when a period ends naturally without a cached snapshot then totalSpent is computed fresh, not a flat totalBudget placeholder`() = runTest {
        val originalEndDate = LocalDate.now().minusDays(1)
        stubEndDate(originalEndDate)
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(false)
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT.copy(earlyFinishActive = false)
        coEvery { budgetRepository.getTransactions() } returns flowOf(
            listOf(
                Transaction.create(
                    amount = BigDecimal("300.00"),
                    date = originalEndDate.minusDays(5).atStartOfDay(),
                )
            )
        )

        checker.handleEndingPeriod()

        assertThat(checker.midnightTransitionData.value?.totalSpent).isEqualTo(BigDecimal("300.00"))
    }

    @Test
    fun `when the period ended over budget then totalSpent reflects the actual overspend, not a flat totalBudget placeholder`() = runTest {
        val originalEndDate = LocalDate.now().minusDays(1)
        stubEndDate(originalEndDate)
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(false)
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT.copy(earlyFinishActive = false)
        coEvery { budgetRepository.getTransactions() } returns flowOf(
            listOf(
                Transaction.create(
                    amount = BigDecimal("1050.00"),
                    date = originalEndDate.minusDays(5).atStartOfDay(),
                )
            )
        )

        checker.handleEndingPeriod()

        val data = checker.midnightTransitionData.value
        assertThat(data?.remainingAmount).isEqualTo(BigDecimal("-50.00"))
        assertThat(data?.totalSpent).isEqualTo(BigDecimal("1050.00"))
    }

    @Test
    fun `when a period end was already surfaced then it is never re-flagged as ended, even past the schedule`() = runTest {
        val originalEndDate = LocalDate.now().minusDays(5)
        stubEndDate(originalEndDate)
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(false)
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT.copy(
            earlyFinishActive = false,
            periodEndAlreadyHandled = true,
        )

        val state = checker.resolveEndingPeriodState()

        assertThat(state.shouldHandleEndingPeriod).isFalse()
    }

    @Test
    fun `when a natural end is processed then periodEndAlreadyHandled is marked so the next app foreground won't re-trigger it`() = runTest {
        val originalEndDate = LocalDate.now().minusDays(1)
        stubEndDate(originalEndDate)
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(false)
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT.copy(
            earlyFinishActive = false,
            periodEndAlreadyHandled = false,
        )
        coEvery { budgetRepository.getTransactions() } returns flowOf(emptyList())

        checker.handleEndingPeriod()

        coVerify { settingsRepository.setPeriodEndAlreadyHandled(true) }
    }

    @Test
    fun `when the period ended over budget then it is also marked as already handled`() = runTest {
        val originalEndDate = LocalDate.now().minusDays(1)
        stubEndDate(originalEndDate)
        coEvery { settingsRepository.observeMidnightTransitionOccurred() } returns flowOf(false)
        coEvery { settingsRepository.getSettings() } returns UserSettings.DEFAULT.copy(
            earlyFinishActive = false,
            periodEndAlreadyHandled = false,
        )
        coEvery { budgetRepository.getTransactions() } returns flowOf(
            listOf(
                Transaction.create(
                    amount = BigDecimal("1050.00"),
                    date = originalEndDate.minusDays(5).atStartOfDay(),
                )
            )
        )

        checker.handleEndingPeriod()

        coVerify { settingsRepository.setPeriodEndAlreadyHandled(true) }
    }

    @Test
    fun `when a period ends with ASK_ALWAYS then the dialog is shown, nothing is queued, and the surplus is marked unresolved`() = runTest {
        arrangeNaturalEndAskAlways()

        checker.handleEndingPeriod()

        assertThat(checker.shouldShowTransitionDialog.value).isTrue()
        val data = checker.midnightTransitionData.value
        assertThat(data).isNotNull()
        assertThat(data!!.shouldNavigateToAnalyticsOnly).isFalse()
        assertThat(data.remainingAmount).isEqualTo(BigDecimal("300.00"))
        coVerify(exactly = 0) { settingsRepository.setPendingRollover(any(), any()) }
        coVerify { settingsRepository.markSurplusUnresolved(BigDecimal("300.00")) }
    }

    @Test
    fun `when a period ends with SPLIT_EQUALLY then a rollover is queued and analytics is the only stop`() = runTest {
        arrangeNaturalEndAskAlways(strategy = RemainingBudgetStrategy.SPLIT_EQUALLY)

        checker.handleEndingPeriod()

        coVerify {
            settingsRepository.setPendingRollover(
                BigDecimal("300.00"),
                RemainingBudgetStrategy.SPLIT_EQUALLY,
            )
        }
        val data = checker.midnightTransitionData.value
        assertThat(data).isNotNull()
        assertThat(data!!.shouldNavigateToAnalyticsOnly).isTrue()
        assertThat(checker.shouldShowTransitionDialog.value).isTrue()
    }

    @Test
    fun `when a period ends with ADD_TO_FIRST_DAY then the surplus is queued for the first day`() = runTest {
        arrangeNaturalEndAskAlways(strategy = RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        checker.handleEndingPeriod()

        coVerify {
            settingsRepository.setPendingRollover(
                BigDecimal("300.00"),
                RemainingBudgetStrategy.ADD_TO_FIRST_DAY,
            )
        }
        val data = checker.midnightTransitionData.value
        assertThat(data).isNotNull()
        assertThat(data!!.shouldNavigateToAnalyticsOnly).isTrue()
    }

    @Test
    fun `when the period ended over budget then the user is routed to analytics only and nothing is queued`() = runTest {
        arrangeNaturalEndAskAlways(remaining = BigDecimal("-50.00"))

        checker.handleEndingPeriod()

        val data = checker.midnightTransitionData.value
        assertThat(data).isNotNull()
        assertThat(data!!.shouldNavigateToAnalyticsOnly).isTrue()
        assertThat(checker.shouldShowTransitionDialog.value).isTrue()
        coVerify(exactly = 0) { settingsRepository.setPendingRollover(any(), any()) }
    }

    @Test
    fun `the transition data exposes the finished period's window, budget and currency`() = runTest {
        val endDate = arrangeNaturalEndAskAlways()

        checker.handleEndingPeriod()

        val data = checker.midnightTransitionData.value
        assertThat(data).isNotNull()
        assertThat(data!!.periodEndDate).isEqualTo(endDate)
        assertThat(data.periodStartDate).isEqualTo(endDate.minusDays(29))
        assertThat(data.totalBudget).isEqualTo(BigDecimal("1000.00"))
        assertThat(data.totalSpent).isEqualTo(BigDecimal("700.00"))
        assertThat(data.currencyCode).isEqualTo("USD")
    }

    @Test
    fun `resolveUnresolvedSurplus queues SPLIT_EQUALLY for next boundary when no new period is active yet`() = runTest {
        arrangeNaturalEndAskAlways()
        checker.handleEndingPeriod()
        coEvery { settingsRepository.getPendingRollover() } returns (BigDecimal("300.00") to null)

        checker.resolveUnresolvedSurplus(RemainingBudgetStrategy.SPLIT_EQUALLY)

        coVerify {
            settingsRepository.setPendingRollover(
                BigDecimal("300.00"),
                RemainingBudgetStrategy.SPLIT_EQUALLY,
            )
        }
        coVerify(exactly = 0) { settingsRepository.clearPendingRollover() }
        assertThat(checker.shouldShowTransitionDialog.value).isFalse()
        assertThat(checker.midnightTransitionData.value).isNull()
    }

    @Test
    fun `resolveUnresolvedSurplus queues ADD_TO_FIRST_DAY for next boundary when no new period is active yet`() = runTest {
        arrangeNaturalEndAskAlways()
        checker.handleEndingPeriod()
        coEvery { settingsRepository.getPendingRollover() } returns (BigDecimal("300.00") to null)

        checker.resolveUnresolvedSurplus(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        coVerify {
            settingsRepository.setPendingRollover(
                BigDecimal("300.00"),
                RemainingBudgetStrategy.ADD_TO_FIRST_DAY,
            )
        }
        assertThat(checker.shouldShowTransitionDialog.value).isFalse()
        assertThat(checker.midnightTransitionData.value).isNull()
    }

    @Test
    fun `resolveUnresolvedSurplus applies SPLIT_EQUALLY immediately when a new period is already active`() = runTest {
        val activeSettings = settingsEndingOn(LocalDate.now().plusDays(10))
        coEvery { budgetRepository.getBudgetSettingsSync() } returns activeSettings
        coEvery { settingsRepository.getPendingRollover() } returns (BigDecimal("300.00") to null)

        checker.resolveUnresolvedSurplus(RemainingBudgetStrategy.SPLIT_EQUALLY)

        coVerify {
            budgetRepository.saveBudgetSettings(
                match { it.totalBudget == BigDecimal("1300.00") },
            )
        }
        coVerify(exactly = 0) { settingsRepository.setPendingRollover(any(), any()) }
        coVerify { settingsRepository.clearPendingRollover() }
    }

    @Test
    fun `resolveUnresolvedSurplus applies ADD_TO_FIRST_DAY immediately, dated today, when a new period is already active`() = runTest {
        val activeSettings = settingsEndingOn(LocalDate.now().plusDays(10))
        coEvery { budgetRepository.getBudgetSettingsSync() } returns activeSettings
        coEvery { settingsRepository.getPendingRollover() } returns (BigDecimal("300.00") to null)

        checker.resolveUnresolvedSurplus(RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        coVerify {
            budgetRepository.saveBudgetSettings(
                match {
                    it.rollOverCarryForward &&
                        it.rollOverLimit == BigDecimal("300.00") &&
                        it.rollOverAppliedDate == LocalDate.now()
                },
            )
        }
        coVerify { settingsRepository.clearPendingRollover() }
    }

    @Test
    fun `resolveUnresolvedSurplus with a null strategy discards without touching settings`() = runTest {
        val activeSettings = settingsEndingOn(LocalDate.now().plusDays(10))
        coEvery { budgetRepository.getBudgetSettingsSync() } returns activeSettings
        coEvery { settingsRepository.getPendingRollover() } returns (BigDecimal("300.00") to null)

        checker.resolveUnresolvedSurplus(null)

        coVerify(exactly = 0) { budgetRepository.saveBudgetSettings(any()) }
        coVerify { settingsRepository.clearPendingRollover() }
    }

    @Test
    fun `resolveUnresolvedSurplus is a no-op when there is nothing pending`() = runTest {
        coEvery { settingsRepository.getPendingRollover() } returns (BigDecimal.ZERO to null)

        checker.resolveUnresolvedSurplus(RemainingBudgetStrategy.SPLIT_EQUALLY)

        coVerify(exactly = 0) { settingsRepository.setPendingRollover(any(), any()) }
        coVerify(exactly = 0) { budgetRepository.saveBudgetSettings(any()) }
    }

    @Test
    fun `dismissing the dialog leaves the surplus persisted as unresolved, not discarded`() = runTest {
        arrangeNaturalEndAskAlways()
        checker.handleEndingPeriod()

        checker.onTransitionDialogDismissed()

        coVerify { settingsRepository.markSurplusUnresolved(BigDecimal("300.00")) }
        coVerify(exactly = 0) { settingsRepository.clearPendingRollover() }
        assertThat(checker.shouldShowTransitionDialog.value).isFalse()
    }

    @Test
    fun `reopenUnresolvedSurplusDialog re-shows the dialog leanly from the persisted amount`() = runTest {
        coEvery { settingsRepository.getPendingRollover() } returns (BigDecimal("300.00") to null)
        coEvery { budgetRepository.getBudgetSettingsSync() } returns settingsEndingOn(LocalDate.now().plusDays(10))

        checker.reopenUnresolvedSurplusDialog()

        assertThat(checker.shouldShowTransitionDialog.value).isTrue()
        val data = checker.midnightTransitionData.value
        assertThat(data).isNotNull()
        assertThat(data!!.remainingAmount).isEqualTo(BigDecimal("300.00"))
        assertThat(data.isPersistedReopen).isTrue()
    }

    @Test
    fun `reopenUnresolvedSurplusDialog does nothing when there is nothing pending`() = runTest {
        coEvery { settingsRepository.getPendingRollover() } returns (BigDecimal.ZERO to null)

        checker.reopenUnresolvedSurplusDialog()

        assertThat(checker.shouldShowTransitionDialog.value).isFalse()
    }

    @Test
    fun `reopenUnresolvedSurplusDialog does nothing once a strategy has already been decided`() = runTest {
        coEvery { settingsRepository.getPendingRollover() } returns
            (BigDecimal("300.00") to RemainingBudgetStrategy.SPLIT_EQUALLY)

        checker.reopenUnresolvedSurplusDialog()

        assertThat(checker.shouldShowTransitionDialog.value).isFalse()
    }

    @Test
    fun `confirming the dialog clears its state without queuing a rollover`() = runTest {
        arrangeNaturalEndAskAlways()
        checker.handleEndingPeriod()

        checker.onTransitionDialogConfirmed()

        coVerify(exactly = 0) { settingsRepository.setPendingRollover(any(), any()) }
        assertThat(checker.shouldShowTransitionDialog.value).isFalse()
        assertThat(checker.midnightTransitionData.value).isNull()
    }

    @Test
    fun `an early finish with ASK_ALWAYS shows the dialog for a period ending today`() = runTest {
        val settings = settingsEndingOn(
            LocalDate.now().plusDays(10),
            strategy = RemainingBudgetStrategy.ASK_ALWAYS,
        )

        checker.handleEarlyFinish(settings, remainingAmount = BigDecimal("300.00"))

        assertThat(checker.shouldShowTransitionDialog.value).isTrue()
        val data = checker.midnightTransitionData.value
        assertThat(data).isNotNull()
        assertThat(data!!.periodEndDate).isEqualTo(LocalDate.now())
        assertThat(data.remainingAmount).isEqualTo(BigDecimal("300.00"))
        assertThat(data.totalSpent).isEqualTo(BigDecimal("700.00"))
        coVerify(exactly = 0) { settingsRepository.setPendingRollover(any(), any()) }
    }

    @Test
    fun `an early finish with SPLIT_EQUALLY queues the surplus immediately without a dialog`() = runTest {
        val settings = settingsEndingOn(
            LocalDate.now().plusDays(10),
            strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
        )

        checker.handleEarlyFinish(settings, remainingAmount = BigDecimal("250.00"))

        coVerify {
            settingsRepository.setPendingRollover(
                BigDecimal("250.00"),
                RemainingBudgetStrategy.SPLIT_EQUALLY,
            )
        }
        assertThat(checker.shouldShowTransitionDialog.value).isFalse()
        assertThat(checker.midnightTransitionData.value).isNull()
    }

    @Test
    fun `an early finish with nothing remaining does nothing`() = runTest {
        val settings = settingsEndingOn(LocalDate.now().plusDays(10))

        checker.handleEarlyFinish(settings, remainingAmount = BigDecimal.ZERO)

        assertThat(checker.shouldShowTransitionDialog.value).isFalse()
        assertThat(checker.midnightTransitionData.value).isNull()
        coVerify(exactly = 0) { settingsRepository.setPendingRollover(any(), any()) }
    }
}
