package com.serranoie.app.minus.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class FoldCarryIntoTotalBudgetUseCaseTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val useCase = FoldCarryIntoTotalBudgetUseCase(budgetRepository, settingsRepository)

    private val unfolded = BudgetSettings(
        totalBudget = BigDecimal("1000.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = LocalDate.of(2026, 9, 1),
        endDate = LocalDate.of(2026, 9, 30),
        currencyCode = "EUR",
        rollOverCarryForward = true,
        rollOverLimit = BigDecimal("200.00"),
    )

    @Test
    fun `a live add-to-today period stored before the fold gets its carry added to the total, once`() = runTest {
        coEvery { settingsRepository.getString(FoldCarryIntoTotalBudgetUseCase.DONE_KEY) } returns null
        coEvery { budgetRepository.getBudgetSettingsSync() } returns unfolded
        val saved = mutableListOf<BudgetSettings>()
        coEvery { budgetRepository.saveBudgetSettings(any()) } answers { saved.add(firstArg()) }

        useCase()

        assertThat(saved.single().totalBudget).isEqualTo(BigDecimal("1200.00"))
        coVerify { settingsRepository.setString(FoldCarryIntoTotalBudgetUseCase.DONE_KEY, "true") }
    }

    @Test
    fun `once marked done it never touches the settings again`() = runTest {
        coEvery { settingsRepository.getString(FoldCarryIntoTotalBudgetUseCase.DONE_KEY) } returns "true"
        coEvery { budgetRepository.getBudgetSettingsSync() } returns unfolded

        useCase()

        coVerify(exactly = 0) { budgetRepository.saveBudgetSettings(any()) }
    }

    @Test
    fun `a spread-across-days period is already folded and is left alone`() = runTest {
        coEvery { settingsRepository.getString(FoldCarryIntoTotalBudgetUseCase.DONE_KEY) } returns null
        coEvery { budgetRepository.getBudgetSettingsSync() } returns unfolded.copy(rollOverCarryForward = false)

        useCase()

        coVerify(exactly = 0) { budgetRepository.saveBudgetSettings(any()) }
        coVerify { settingsRepository.setString(FoldCarryIntoTotalBudgetUseCase.DONE_KEY, "true") }
    }
}
