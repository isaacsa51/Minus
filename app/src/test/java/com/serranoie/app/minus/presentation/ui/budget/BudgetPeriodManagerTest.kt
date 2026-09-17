package com.serranoie.app.minus.presentation.ui.budget

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.time.MidnightPeriodChecker
import com.serranoie.app.minus.domain.time.TimeProvider
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import io.mockk.coEvery
import io.mockk.coVerify
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
        coEvery { settingsRepository.markSurplusUnresolved(any()) } answers {
            pendingRollover = firstArg<BigDecimal>() to null
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
        coEvery { settingsRepository.setPeriodEndAlreadyHandled(any()) } answers {
            userSettings = userSettings.copy(periodEndAlreadyHandled = firstArg())
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
                    ).copy(id = 1L),
                    Transaction.create(
                        amount = BigDecimal("100.00"),
                        date = today.minusDays(5).atStartOfDay()
                    ).copy(id = 2L),
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

            assertThat(pendingRollover).isEqualTo(BigDecimal("700.00") to null)
            assertThat(midnightPeriodChecker.shouldShowTransitionDialog.value).isTrue()
            assertThat(midnightPeriodChecker.midnightTransitionData.value?.remainingAmount)
                .isEqualTo(BigDecimal("700.00"))
        }

    @Test
    fun `when starting a new period with add-to-today then the carry is folded into the total and offered on the first live day`() =
        runTest {
            val today = LocalDate.now()
            val oldSettings = budget(
                strategy = RemainingBudgetStrategy.ADD_TO_FIRST_DAY,
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

            val savedSlot = mutableListOf<BudgetSettings>()
            coEvery { budgetRepository.saveBudgetSettings(any()) } answers { savedSlot.add(firstArg()) }
            periodManager.persistBudgetSettings(
                budget(
                    strategy = RemainingBudgetStrategy.ADD_TO_FIRST_DAY,
                    startDate = today.minusDays(1),
                    endDate = today.plusDays(28),
                    totalBudget = BigDecimal("1200.00"),
                ),
                forceNewPeriodBoundary = true,
            )

            val saved = savedSlot.single()
            assertThat(saved.totalBudget).isEqualTo(BigDecimal("1800.00"))
            assertThat(saved.rollOverCarryForward).isTrue()
            assertThat(saved.rollOverLimit).isEqualTo(BigDecimal("600.00"))
            assertThat(saved.rollOverAppliedDate).isEqualTo(today)
            coVerify { settingsRepository.setCurrentPeriodRollover(BigDecimal("600.00"), true) }
        }

    @Test
    fun `when finishing early with ask-always and the user picks a strategy then the surplus is queued for the next period, not added back to the finished one`() =
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
            midnightPeriodChecker.resolveUnresolvedSurplus(RemainingBudgetStrategy.SPLIT_EQUALLY)

            coVerify(exactly = 0) { budgetRepository.saveBudgetSettings(any()) }
            assertThat(pendingRollover).isEqualTo(BigDecimal("700.00") to RemainingBudgetStrategy.SPLIT_EQUALLY)
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
    fun `when the user edits the dates of a live period then it stays the same period and keeps its rollover`() =
        runTest {
            val today = LocalDate.now()
            val activeSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.minusDays(1),
                endDate = today.plusDays(29),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns activeSettings
            coEvery { budgetRepository.getTransactions() } returns flowOf(emptyList())
            userSettings = userSettings.copy(currentPeriodId = 999L, currentPeriodStartedAt = 1L)

            val savedSlot = mutableListOf<BudgetSettings>()
            coEvery { budgetRepository.saveBudgetSettings(any()) } answers { savedSlot.add(firstArg()) }

            // Same thing the sheet does: new start date, new budget, no force flag.
            val edited = activeSettings.copy(startDate = today, totalBudget = BigDecimal("1500.00"))
            val result = periodManager.persistBudgetSettings(edited, forceNewPeriodBoundary = false)

            coVerify(exactly = 0) { budgetRepository.archiveCurrentPeriod(any(), any(), any()) }
            coVerify(exactly = 0) { settingsRepository.setCurrentPeriodRollover(any(), any()) }
            assertThat(result.periodId).isEqualTo(999L)
            assertThat(savedSlot.single().startDate).isEqualTo(today)
        }

    @Test
    fun `when Analytics clears the early-finish flag before the new period is saved then the surplus still carries over`() =
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
            periodManager.clearEarlyFinishState() // AnalyticsViewModel.onCreateNewPeriod does this

            val savedSlot = mutableListOf<BudgetSettings>()
            coEvery { budgetRepository.saveBudgetSettings(any()) } answers { savedSlot.add(firstArg()) }
            val newSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today,
                endDate = today.plusDays(27),
                totalBudget = BigDecimal("1200.00"),
            )
            periodManager.persistBudgetSettings(newSettings, forceNewPeriodBoundary = false)

            coVerify(exactly = 1) { budgetRepository.archiveCurrentPeriod(500L, any(), any()) }
            assertThat(savedSlot.single().totalBudget).isEqualTo(BigDecimal("1800.00"))
            assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)
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
            periodManager.clearEarlyFinishState() // AnalyticsViewModel.onCreateNewPeriod does this

            val archivedSettingsSlot = mutableListOf<BudgetSettings>()
            coEvery {
                budgetRepository.archiveCurrentPeriod(any(), any(), any())
            } answers { archivedSettingsSlot.add(secondArg()) }

            val newSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.plusDays(1),
                endDate = today.plusDays(28),
            )
            periodManager.persistBudgetSettings(newSettings, forceNewPeriodBoundary = true)

            assertThat(archivedSettingsSlot.single().endDate).isEqualTo(today)
            assertThat(userSettings.earlyFinishActualDate).isEqualTo(0L) // fully cleared once archived
        }

    @Test
    fun `when the user edits the active budget mid-period while earlyFinishActive is true then it is not silently cleared`() =
        runTest {
            val today = LocalDate.now()
            val activeSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.minusDays(5),
                endDate = today.plusDays(25),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns activeSettings
            coEvery { budgetRepository.getTransactions() } returns flowOf(emptyList())
            userSettings = userSettings.copy(
                currentPeriodId = 999L,
                currentPeriodStartedAt = 1L,
                earlyFinishActive = true,
                earlyFinishActualDate = 123L,
                periodEndAlreadyHandled = true,
            )

            val editedSettings = activeSettings.copy(creditCardCutoffDay = 5)
            periodManager.persistBudgetSettings(editedSettings, forceNewPeriodBoundary = false)

            assertThat(userSettings.earlyFinishActive).isTrue()
            assertThat(userSettings.periodEndAlreadyHandled).isTrue()
        }

    @Test
    fun `when a genuinely new period starts then earlyFinishActive and periodEndAlreadyHandled are cleared`() =
        runTest {
            val today = LocalDate.now()
            val oldSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.minusDays(20),
                endDate = today.plusDays(10),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns oldSettings
            coEvery { budgetRepository.getTransactions() } returns flowOf(emptyList())
            userSettings = userSettings.copy(
                currentPeriodId = 42L,
                currentPeriodStartedAt = 1L,
                earlyFinishActive = true,
                earlyFinishActualDate = 123L,
                periodEndAlreadyHandled = true,
            )

            val newSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today,
                endDate = today.plusDays(27),
            )
            periodManager.persistBudgetSettings(newSettings, forceNewPeriodBoundary = false)

            assertThat(userSettings.earlyFinishActive).isFalse()
            assertThat(userSettings.periodEndAlreadyHandled).isFalse()
        }

    @Test
    fun `an undecided pending surplus survives a period boundary untouched, instead of being silently cleared`() =
        runTest {
            val today = LocalDate.now()
            val oldSettings = budget(
                strategy = RemainingBudgetStrategy.ASK_ALWAYS,
                startDate = today.minusDays(20),
                endDate = today.plusDays(10),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns oldSettings
            coEvery { budgetRepository.getTransactions() } returns flowOf(emptyList())
            userSettings = userSettings.copy(currentPeriodId = 42L, currentPeriodStartedAt = 1L)
            pendingRollover = BigDecimal("150.00") to null // known surplus, still undecided

            val newSettings = budget(
                strategy = RemainingBudgetStrategy.ASK_ALWAYS,
                startDate = today,
                endDate = today.plusDays(27),
            )
            periodManager.persistBudgetSettings(newSettings, forceNewPeriodBoundary = true)

            assertThat(pendingRollover.first).isEqualTo(BigDecimal("150.00"))
            assertThat(pendingRollover.second).isNull()
        }

    @Test
    fun `the rollover queued on early finish is the same number the archived period reports as saved`() =
        runTest {
            val today = LocalDate.now()
            val settings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.minusDays(20),
                endDate = today.plusDays(10),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns settings
            userSettings = userSettings.copy(currentPeriodId = 500L, currentPeriodStartedAt = 1L)

            val coffee = Transaction.create(
                amount = BigDecimal("100.00"),
                date = today.minusDays(10).atStartOfDay(),
                periodId = 500L,
            ).copy(id = 1L)
            val gymTemplate = Transaction.create(
                amount = BigDecimal("50.00"),
                comment = "Gym",
                date = today.minusDays(5).atTime(9, 0),
                periodId = 500L,
                isRecurrent = true,
                recurrentFrequency = RecurrentFrequency.MONTHLY,
            ).copy(id = 2L)
            val gymPaid = Transaction.create(
                amount = BigDecimal("50.00"),
                comment = "Gym",
                date = today.minusDays(5).atTime(9, 5),
                periodId = 500L,
            ).copy(id = 3L)
            val netflixTemplate = Transaction.create(
                amount = BigDecimal("30.00"),
                comment = "Netflix",
                date = settings.startDate.minusMonths(2).plusDays(3).atTime(10, 0),
                periodId = 1L,
                isRecurrent = true,
                recurrentFrequency = RecurrentFrequency.MONTHLY,
            ).copy(id = 4L)
            coEvery { budgetRepository.getTransactions() } returns
                flowOf(listOf(coffee, gymTemplate, gymPaid, netflixTemplate))
            coEvery { budgetRepository.getPaidRecurrentOccurrences() } returns
                flowOf(setOf(PaidRecurrentOccurrence(gymTemplate.id, today.minusDays(5))))

            periodManager.finishBudgetEarly()

            assertThat(pendingRollover.first).isEqualTo(BigDecimal("820.00"))

            val spentSlot = mutableListOf<BigDecimal>()
            coEvery {
                budgetRepository.archiveCurrentPeriod(any(), any(), any())
            } answers { spentSlot.add(thirdArg()) }
            periodManager.persistBudgetSettings(
                budget(
                    strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                    startDate = today.plusDays(1),
                    endDate = today.plusDays(30),
                ),
                forceNewPeriodBoundary = true,
            )

            assertThat(settings.totalBudget.subtract(spentSlot.single())).isEqualTo(BigDecimal("820.00"))
        }

    @Test
    fun `when the next period starts on the day the previous one ended then that day belongs to the new period only`() =
        runTest {
            val today = LocalDate.now()
            val settings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.minusDays(20),
                endDate = today.plusDays(10),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns settings
            userSettings = userSettings.copy(currentPeriodId = 500L, currentPeriodStartedAt = 1L)

            val gym = Transaction.create(
                amount = BigDecimal("50.00"),
                comment = "Gym",
                date = today.minusMonths(1).atTime(9, 0),
                periodId = 1L,
                isRecurrent = true,
                recurrentFrequency = RecurrentFrequency.MONTHLY,
            ).copy(id = 2L)
            coEvery { budgetRepository.getTransactions() } returns flowOf(listOf(gym))
            coEvery { budgetRepository.getPaidRecurrentOccurrences() } returns
                flowOf(emptySet<PaidRecurrentOccurrence>())

            periodManager.finishBudgetEarly()
            assertThat(pendingRollover.first).isEqualTo(BigDecimal("950.00"))

            val archivedSettingsSlot = mutableListOf<BudgetSettings>()
            val spentSlot = mutableListOf<BigDecimal>()
            coEvery {
                budgetRepository.archiveCurrentPeriod(any(), any(), any())
            } answers {
                archivedSettingsSlot.add(secondArg())
                spentSlot.add(thirdArg())
            }
            val savedSlot = mutableListOf<BudgetSettings>()
            coEvery { budgetRepository.saveBudgetSettings(any()) } answers { savedSlot.add(firstArg()) }

            periodManager.persistBudgetSettings(
                budget(
                    strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                    startDate = today,
                    endDate = today.plusDays(30),
                    totalBudget = BigDecimal("1200.00"),
                ),
                forceNewPeriodBoundary = true,
            )

            assertThat(archivedSettingsSlot.single().endDate).isEqualTo(today.minusDays(1))
            assertThat(spentSlot.single()).isEqualTo(BigDecimal.ZERO)
            assertThat(savedSlot.single().totalBudget).isEqualTo(BigDecimal("2200.00"))
            assertThat(pendingRollover.first).isEqualTo(BigDecimal.ZERO)
        }

    @Test
    fun `archiving a period includes a recurring charge projected from before it started, matching what reopening it would show`() =
        runTest {
            val today = LocalDate.now()
            val oldSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.minusDays(20),
                endDate = today.plusDays(10),
            )
            coEvery { budgetRepository.getBudgetSettingsSync() } returns oldSettings

            val netflixAnchor = oldSettings.startDate.minusMonths(1).plusDays(3)
            val netflix = Transaction.create(
                amount = BigDecimal("100.00"),
                comment = "Netflix",
                date = netflixAnchor.atTime(10, 0),
                isRecurrent = true,
                recurrentFrequency = RecurrentFrequency.MONTHLY,
            )
            val coffee = Transaction.create(
                amount = BigDecimal("20.00"),
                comment = "Coffee",
                date = today.minusDays(10).atStartOfDay(),
                periodId = 500L,
            )
            coEvery { budgetRepository.getTransactions() } returns flowOf(listOf(netflix, coffee))
            coEvery { budgetRepository.getPaidRecurrentOccurrences() } returns
                flowOf(emptySet<PaidRecurrentOccurrence>())
            userSettings = userSettings.copy(currentPeriodId = 500L, currentPeriodStartedAt = 1L)

            val spentSlot = mutableListOf<BigDecimal>()
            coEvery {
                budgetRepository.archiveCurrentPeriod(any(), any(), any())
            } answers { spentSlot.add(thirdArg()) }

            val newSettings = budget(
                strategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
                startDate = today.plusDays(11),
                endDate = today.plusDays(40),
            )
            periodManager.persistBudgetSettings(newSettings, forceNewPeriodBoundary = true)

            assertThat(spentSlot.single()).isEqualTo(BigDecimal("120.00"))
        }
}
