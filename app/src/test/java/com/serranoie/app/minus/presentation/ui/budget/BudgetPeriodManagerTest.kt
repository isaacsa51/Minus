package com.serranoie.app.minus.presentation.ui.budget

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
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

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetPeriodManagerTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val timeProvider: TimeProvider = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)

    private val midnightPeriodChecker = MidnightPeriodChecker(budgetRepository, settingsRepository)

    private lateinit var periodManager: BudgetPeriodManager

    private var pendingRollover: Pair<BigDecimal, RemainingBudgetStrategy?> =
        BigDecimal.ZERO to null
    private var userSettings = UserSettings.DEFAULT

    @Before
    fun setUp() {
        periodManager = BudgetPeriodManager(
            budgetRepository = budgetRepository,
            settingsRepository = settingsRepository,
            timeProvider = timeProvider,
            notificationScheduler = notificationScheduler,
            midnightPeriodChecker = midnightPeriodChecker,
        )

        coEvery { settingsRepository.getPendingRollover() } answers { pendingRollover }
        coEvery { settingsRepository.setPendingRollover(any(), any()) } answers {
            pendingRollover = firstArg<BigDecimal>() to secondArg()
        }
        coEvery { settingsRepository.clearPendingRollover() } answers {
            pendingRollover = BigDecimal.ZERO to null
        }
        coEvery { settingsRepository.getSettings() } answers { userSettings }
        coEvery {
            settingsRepository.setEarlyFinishActive(any(), any(), any())
        } answers {
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
    }

    private fun budget(
        strategy: RemainingBudgetStrategy,
        startDate: LocalDate,
        endDate: LocalDate,
        totalBudget: BigDecimal = BigDecimal("1000.00"),
    ) = BudgetSettings(
        totalBudget = totalBudget,
        period = BudgetPeriod.MONTHLY,
        startDate = startDate,
        endDate = endDate,
        currencyCode = "USD",
        remainingBudgetStrategy = strategy,
    )

    @Test
    fun `when finishing early with split-equally strategy then remaining balance is queued as a pending rollover`() =
        runTest {
            val today = LocalDate.now()
            val settings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.minusDays(20),
                endDate = today.plusDays(10),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns settings
            coEvery { budgetRepository.getTransactions() } returns flowOf(
                listOf(
                    Transaction.create(
                        amount = BigDecimal("200.00"),
                        date = today.minusDays(15).atStartOfDay()
                    ),
                    Transaction.create(
                        amount = BigDecimal("100.00"),
                        date = today.minusDays(5).atStartOfDay()
                    ),
                )
            )

            periodManager.finishBudgetEarly()

            assertThat(pendingRollover.first).isEqualTo(BigDecimal("700.00"))
            assertThat(pendingRollover.second).isEqualTo(RemainingBudgetStrategy.SPLIT_EQUALLY)
        }

    @Test
    fun `when finishing early with ask-always strategy then nothing is queued and the rollover dialog is requested instead`() =
        runTest {
            val today = LocalDate.now()
            val settings = budget(
                strategy = RemainingBudgetStrategy.ASK_ALWAYS,
                startDate = today.minusDays(20),
                endDate = today.plusDays(10),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns settings
            coEvery { budgetRepository.getTransactions() } returns flowOf(
                listOf(
                    Transaction.create(
                        amount = BigDecimal("300.00"),
                        date = today.minusDays(10).atStartOfDay()
                    )
                )
            )

            periodManager.finishBudgetEarly()

            assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)
            assertThat(midnightPeriodChecker.shouldShowTransitionDialog.value).isTrue()
            assertThat(midnightPeriodChecker.midnightTransitionData.value?.remainingAmount)
                .isEqualTo(BigDecimal("700.00"))
        }

    @Test
    fun `when finishing early with nothing left then no rollover is queued and no dialog is shown`() =
        runTest {
            val today = LocalDate.now()
            val settings = budget(
                strategy = RemainingBudgetStrategy.ASK_ALWAYS,
                startDate = today.minusDays(20),
                endDate = today.plusDays(10),
                totalBudget = BigDecimal("100.00"),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns settings
            coEvery { budgetRepository.getTransactions() } returns flowOf(
                listOf(
                    Transaction.create(
                        amount = BigDecimal("150.00"),
                        date = today.minusDays(10).atStartOfDay()
                    )
                )
            )

            periodManager.finishBudgetEarly()

            assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)
            assertThat(midnightPeriodChecker.shouldShowTransitionDialog.value).isFalse()
        }

    @Test
    fun `when starting a new period after finishing early then the surplus is carried into the new total budget`() =
        runTest {
            val today = LocalDate.now()
            val oldSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.minusDays(20),
                endDate = today.plusDays(10),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns oldSettings
            coEvery { budgetRepository.getTransactions() } returns flowOf(
                listOf(
                    Transaction.create(
                        amount = BigDecimal("400.00"),
                        date = today.minusDays(10).atStartOfDay(),
                        periodId = 500L,
                    )
                )
            )
            userSettings = userSettings.copy(currentPeriodId = 500L, currentPeriodStartedAt = 1L)

            periodManager.finishBudgetEarly()
            assertThat(pendingRollover.first).isEqualTo(BigDecimal("600.00"))

            val newSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today,
                endDate = today.plusDays(27),
                totalBudget = BigDecimal("1200.00"), // "only the new income"
            )
            val savedSlot = mutableListOf<BudgetSettings>()
            coEvery { budgetRepository.saveBudgetSettings(any()) } answers { savedSlot.add(firstArg()) }

            periodManager.persistBudgetSettings(newSettings, forceNewPeriodBoundary = true)

            assertThat(savedSlot.single().totalBudget).isEqualTo(BigDecimal("1800.00"))
        }

    @Test
    fun `when starting a new period the way the real UI does it (no explicit force, only a changed start date) then the surplus still carries over`() =
        runTest {
            val today = LocalDate.now()
            val oldSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.minusDays(20),
                endDate = today.plusDays(10),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns oldSettings
            coEvery { budgetRepository.getTransactions() } returns flowOf(
                listOf(
                    Transaction.create(
                        amount = BigDecimal("400.00"),
                        date = today.minusDays(10).atStartOfDay(),
                        periodId = 500L,
                    )
                )
            )
            userSettings = userSettings.copy(currentPeriodId = 500L, currentPeriodStartedAt = 1L)

            periodManager.finishBudgetEarly()
            assertThat(pendingRollover.first).isEqualTo(BigDecimal("600.00"))

            val newSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today,
                endDate = today.plusDays(27),
                totalBudget = BigDecimal("1200.00"),
            )
            val savedSlot = mutableListOf<BudgetSettings>()
            coEvery { budgetRepository.saveBudgetSettings(any()) } answers { savedSlot.add(firstArg()) }

            periodManager.persistBudgetSettings(newSettings, forceNewPeriodBoundary = false)

            assertThat(savedSlot.single().totalBudget).isEqualTo(BigDecimal("1800.00"))
            assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)
        }

    @Test
    fun `when the user just edits the active budget mid-period (same start date, no force) then nothing is archived and no rollover applies`() =
        runTest {
            val today = LocalDate.now()
            val activeSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.minusDays(5),
                endDate = today.plusDays(25),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns activeSettings
            coEvery { budgetRepository.getTransactions() } returns flowOf(emptyList())
            userSettings = userSettings.copy(currentPeriodId = 999L, currentPeriodStartedAt = 1L)
            pendingRollover = BigDecimal("50.00") to RemainingBudgetStrategy.SPLIT_EQUALLY

            val archivedCalls = mutableListOf<Long>()
            coEvery {
                budgetRepository.archiveCurrentPeriod(any(), any(), any())
            } answers { archivedCalls.add(firstArg()) }

            val editedSettings = activeSettings.copy(totalBudget = BigDecimal("1500.00"))
            periodManager.persistBudgetSettings(editedSettings, forceNewPeriodBoundary = false)

            assertThat(archivedCalls).isEmpty()
            assertThat(pendingRollover.first).isEqualTo(BigDecimal("50.00"))
        }

    @Test
    fun `when archiving a period finished early then the snapshot uses the actual finish date not the original schedule`() =
        runTest {
            val today = LocalDate.now()
            val oldSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.minusDays(20),
                endDate = today.plusDays(10),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns oldSettings
            coEvery { budgetRepository.getTransactions() } returns flowOf(emptyList())
            userSettings = userSettings.copy(currentPeriodId = 42L, currentPeriodStartedAt = 1L)

            periodManager.finishBudgetEarly()

            val archivedSettingsSlot = mutableListOf<BudgetSettings>()
            coEvery {
                budgetRepository.archiveCurrentPeriod(any(), any(), any())
            } answers { archivedSettingsSlot.add(secondArg()) }

            val newSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today,
                endDate = today.plusDays(27),
            )
            periodManager.persistBudgetSettings(newSettings, forceNewPeriodBoundary = true)

            assertThat(archivedSettingsSlot.single().endDate).isEqualTo(today)
        }
}
