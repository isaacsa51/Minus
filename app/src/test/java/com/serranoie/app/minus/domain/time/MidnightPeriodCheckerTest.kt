package com.serranoie.app.minus.domain.time

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
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

/**
 * Regression coverage for the gap where a period closed via "Finish early" was
 * still reachable by the natural/midnight period-end path once its original
 * (untouched) schedule elapsed -- silently overwriting the rollover already
 * queued from the early finish, or re-showing the rollover dialog for a period
 * the user already resolved.
 */
class MidnightPeriodCheckerTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val checker = MidnightPeriodChecker(budgetRepository, settingsRepository)

    private fun settingsEndingOn(endDate: LocalDate) = BudgetSettings(
        totalBudget = BigDecimal("1000.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = endDate.minusDays(29),
        endDate = endDate,
        currencyCode = "USD",
    )

    private fun stubEndDate(endDate: LocalDate) {
        coEvery { budgetRepository.getBudgetSettingsSync() } returns settingsEndingOn(endDate)
        coEvery { settingsRepository.observeBudgetEndDate() } returns flowOf(
            endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
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
}
