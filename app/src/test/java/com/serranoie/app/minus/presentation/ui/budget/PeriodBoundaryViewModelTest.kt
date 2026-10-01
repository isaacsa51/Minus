package com.serranoie.app.minus.presentation.ui.budget

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.time.MidnightPeriodChecker
import com.serranoie.app.minus.domain.time.MidnightTransitionManager
import com.serranoie.app.minus.domain.time.TimeProvider
import com.serranoie.app.minus.domain.usecase.ClearEarlyFinishStateUseCase
import com.serranoie.app.minus.domain.usecase.FinishBudgetEarlyUseCase
import com.serranoie.app.minus.domain.usecase.GetCurrentPeriodIdUseCase
import com.serranoie.app.minus.domain.usecase.MarkOnboardingCompletedUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodRolloverUseCase
import com.serranoie.app.minus.domain.usecase.PersistBudgetSettingsUseCase
import com.serranoie.app.minus.domain.usecase.UpdatePeriodEndNotificationTimeUseCase
import com.serranoie.app.minus.presentation.notification.NotificationHelper
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetEditorIntent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PeriodBoundaryViewModelTest {

    private val context: Context = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val notificationHelper: NotificationHelper = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)
    private val transactionHandler: BudgetTransactionHandler = mockk(relaxed = true)
    private val budgetWidgetUpdater: BudgetWidgetUpdater = mockk(relaxed = true)
    private val timeProvider: TimeProvider = mockk(relaxed = true)

    private val calculator = BudgetStateCalculator()
    private lateinit var periodManager: BudgetPeriodManager
    private lateinit var checker: MidnightPeriodChecker

    private val settingsFlow = MutableStateFlow<BudgetSettings?>(null)
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private val boundaryFlow = MutableStateFlow(0L to 0L)
    private val rolloverFlow = MutableStateFlow(BigDecimal.ZERO to false)
    private val userSettingsFlow = MutableStateFlow(UserSettings.DEFAULT)
    private val pendingRolloverFlow =
        MutableStateFlow(BigDecimal.ZERO to null as RemainingBudgetStrategy?)

    private var userSettings: UserSettings
        get() = userSettingsFlow.value
        set(value) {
            userSettingsFlow.value = value
        }

    private val today: LocalDate = LocalDate.now()
    private var clock = 1_700_000_000_000L

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        every { timeProvider.nowEpochMillis() } answers { clock }
        every { budgetRepository.getBudgetSettings() } returns settingsFlow
        every { budgetRepository.getTransactions() } returns transactionsFlow
        every { budgetRepository.getQueuedTransactions() } returns MutableStateFlow(emptyList())
        every { budgetRepository.getActiveCategories() } returns MutableStateFlow(emptyList())
        every { budgetRepository.getPaidRecurrentOccurrences() } returns
            MutableStateFlow(emptySet<PaidRecurrentOccurrence>())
        coEvery { budgetRepository.getBudgetSettingsSync() } answers { settingsFlow.value }
        coEvery { budgetRepository.saveBudgetSettings(any()) } answers {
            settingsFlow.value = firstArg()
        }

        coEvery { settingsRepository.getSettings() } answers { userSettings }
        every { settingsRepository.observeSettings() } returns userSettingsFlow
        coEvery { settingsRepository.setCurrentPeriod(any(), any()) } answers {
            val id = firstArg<Long>()
            val startedAt = secondArg<Long>()
            userSettings = userSettings.copy(currentPeriodId = id, currentPeriodStartedAt = startedAt)
            boundaryFlow.value = startedAt to id
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
        coEvery { settingsRepository.setPeriodEndAlreadyHandled(any()) } answers {
            userSettings = userSettings.copy(periodEndAlreadyHandled = firstArg())
        }
        coEvery { settingsRepository.getPendingRollover() } answers { pendingRolloverFlow.value }
        every { settingsRepository.observePendingRollover() } returns pendingRolloverFlow
        coEvery { settingsRepository.setPendingRollover(any(), any()) } answers {
            pendingRolloverFlow.value = firstArg<BigDecimal>() to secondArg()
        }
        coEvery { settingsRepository.clearPendingRollover() } answers {
            pendingRolloverFlow.value = BigDecimal.ZERO to null
        }
        coEvery { settingsRepository.markSurplusUnresolved(any()) } answers {
            pendingRolloverFlow.value = firstArg<BigDecimal>() to null
        }
        coEvery { settingsRepository.getCurrentPeriodId() } answers { userSettings.currentPeriodId }

        checker = MidnightPeriodChecker(budgetRepository, settingsRepository)
        periodManager = BudgetPeriodManager(
            budgetRepository = budgetRepository,
            settingsRepository = settingsRepository,
            timeProvider = timeProvider,
            notificationScheduler = notificationScheduler,
            midnightPeriodChecker = checker,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(): BudgetViewModel {
        val transitionManager = MidnightTransitionManager(checker)
        return BudgetViewModel(
            context = context,
            budgetRepository = budgetRepository,
            settingsRepository = settingsRepository,
            notificationHelper = notificationHelper,
            notificationScheduler = notificationScheduler,
            transactionHandler = transactionHandler,
            budgetStateCalculator = calculator,
            budgetWidgetUpdater = budgetWidgetUpdater,
            budgetExpressionEvaluator = BudgetExpressionEvaluator(),
            observeCurrentPeriodBoundaryUseCase = ObserveCurrentPeriodBoundaryUseCase(settingsRepository)
                .let { _ -> mockk<ObserveCurrentPeriodBoundaryUseCase>(relaxed = true).also { uc -> every { uc() } returns boundaryFlow } },
            observeCurrentPeriodRolloverUseCase = mockk<ObserveCurrentPeriodRolloverUseCase>(relaxed = true)
                .also { uc -> every { uc() } returns rolloverFlow },
            getCurrentPeriodIdUseCase = GetCurrentPeriodIdUseCase(settingsRepository),
            persistBudgetSettingsUseCase = PersistBudgetSettingsUseCase(periodManager),
            updatePeriodEndNotificationTimeUseCase = mockk<UpdatePeriodEndNotificationTimeUseCase>(relaxed = true),
            finishBudgetEarlyUseCase = FinishBudgetEarlyUseCase(periodManager),
            clearEarlyFinishStateUseCase = ClearEarlyFinishStateUseCase(periodManager),
            markOnboardingCompletedUseCase = mockk<MarkOnboardingCompletedUseCase>(relaxed = true),
            midnightTransitionManager = transitionManager,
        )
    }

    private fun periodSettings(start: LocalDate, end: LocalDate, total: String) = BudgetSettings(
        totalBudget = BigDecimal(total),
        period = BudgetPeriod.MONTHLY,
        startDate = start,
        endDate = end,
        currencyCode = "USD",
        daysInPeriod = 30,
        remainingBudgetStrategy = RemainingBudgetStrategy.SPLIT_EQUALLY,
        splitMode = BudgetSplitMode.STATIC,
    )

    @Test
    fun `the pill drops the closed period's spend when the next period keeps the start date and runs longer`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val viewModel = newViewModel()

            viewModel.uiState.test {
                clock += 60_000
                viewModel.processIntent(
                    BudgetEditorIntent.UpdateSettings(
                        periodSettings(today, today.plusDays(4), "550.00"),
                    ),
                )
                advanceUntilIdle()
                val periodA = userSettings.currentPeriodId
                assertThat(periodA).isGreaterThan(0L)

                clock += 60_000
                transactionsFlow.value = listOf(
                    Transaction(
                        id = 1L,
                        amount = BigDecimal("100.00"),
                        comment = "Spend in period A",
                        date = today.atTime(10, 0),
                        createdAt = clock,
                        periodId = periodA,
                    ),
                )
                advanceUntilIdle()
                val duringA = expectMostRecentItem()
                assertThat("spentInA=${duringA.budgetState?.totalSpentInPeriod}")
                    .isEqualTo("spentInA=100.00")

                clock += 60_000
                viewModel.processIntent(BudgetEditorIntent.FinishBudgetEarly)
                advanceUntilIdle()

                clock += 60_000
                viewModel.processIntent(
                    BudgetEditorIntent.UpdateSettings(
                        periodSettings(today, today.plusDays(15), "550.00"),
                    ),
                )
                advanceUntilIdle()

                val after = expectMostRecentItem()
                val diagnostic = "periodA=$periodA now=${after.currentPeriodId} " +
                    "startedAt=${after.currentPeriodStartedAtMillis} " +
                    "window=${after.budgetSettings?.startDate}..${after.budgetSettings?.getPeriodEndDate()}"

                assertThat("$diagnostic spent=${after.budgetState?.totalSpentInPeriod}")
                    .isEqualTo("$diagnostic spent=0")

                cancelAndIgnoreRemainingEvents()
            }
        }
}
