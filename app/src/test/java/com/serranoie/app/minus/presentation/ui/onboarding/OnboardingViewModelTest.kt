package com.serranoie.app.minus.presentation.ui.onboarding

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.presentation.notification.NotificationScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Unit tests for [OnboardingViewModel]. Verifies the MVI flow:
 *   - every [OnboardingUiIntent] routes through the right handler
 *   - the resulting [OnboardingUiState] matches expectations
 *   - the budget save + notification schedule + settings persist side
 *     effects fire on completion
 *   - the [OnboardingUiEffect.OnboardingCompleted] effect is emitted
 *     after the user has successfully completed onboarding
 *
 * All collaborators are mocked with MockK. The VM's `viewModelScope.launch`
 * uses [Dispatchers.Main], so we install a [StandardTestDispatcher] in
 * `@Before` and reset it in `@After` — same recipe as the
 * [com.serranoie.app.minus.presentation.ui.budget.BudgetViewModelTest].
 *
 * Backticked English test names (when_X_then_Y) following the BDD
 * Given / When / Then convention. The Android DEX bytecode format
 * rejects spaces in method names, so underscores are used as the word
 * separator.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val notificationScheduler: NotificationScheduler = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    /**
     * `viewModelScope` uses `Dispatchers.Main.immediate`, which throws on
     * a plain JVM test. Install a [StandardTestDispatcher] so the
     * launched coroutines run on the test scheduler and can be drained
     * by `runTest` / `advanceUntilIdle`.
     */
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = OnboardingViewModel(
        budgetRepository = budgetRepository,
        notificationScheduler = notificationScheduler,
        settingsRepository = settingsRepository,
    )

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `when_viewmodel_is_created_then_state_has_defaults`() {
        val viewModel = newViewModel()

        val state = viewModel.uiState.value
        assertThat(state.currentStep).isEqualTo(0)
        assertThat(state.budgetInput).isEqualTo("")
        assertThat(state.selectedDays).isEqualTo(1)
        assertThat(state.daysInPeriod).isEqualTo(1)
        assertThat(state.selectedPeriod).isEqualTo(BudgetPeriod.DAILY)
        assertThat(state.startDate).isNull()
        assertThat(state.endDate).isNull()
        assertThat(state.isLoading).isFalse()
        assertThat(state.isCompleted).isFalse()
        assertThat(state.error).isNull()
    }

    // -------------------------------------------------------------------------
    // OnBudgetAmountChanged
    // -------------------------------------------------------------------------

    @Test
    fun `when_budget_amount_changed_with_valid_input_then_state_is_updated`() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("100.00"))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.budgetInput).isEqualTo("100.00")
    }

    @Test
    fun `when_budget_amount_changed_with_multiple_dots_then_input_is_ignored`() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("1.2.3"))

        advanceUntilIdle()
        // The handler rejects inputs with more than one dot.
        assertThat(viewModel.uiState.value.budgetInput).isEqualTo("")
    }

    @Test
    fun `when_budget_amount_changed_with_input_longer_than_10_chars_then_input_is_ignored`() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("12345678901"))

        advanceUntilIdle()
        // The handler rejects inputs longer than 10 characters.
        assertThat(viewModel.uiState.value.budgetInput).isEqualTo("")
    }

    @Test
    fun `when_budget_amount_changed_with_exactly_10_chars_then_input_is_accepted`() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("1234567890"))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.budgetInput).isEqualTo("1234567890")
    }

    // -------------------------------------------------------------------------
    // OnDaysSelected — period mapping
    // -------------------------------------------------------------------------

    @Test
    fun `when_days_selected_1_then_selected_period_is_daily`() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(1))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedDays).isEqualTo(1)
        assertThat(viewModel.uiState.value.selectedPeriod).isEqualTo(BudgetPeriod.DAILY)
    }

    @Test
    fun `when_days_selected_7_then_selected_period_is_weekly`() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(7))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedDays).isEqualTo(7)
        assertThat(viewModel.uiState.value.selectedPeriod).isEqualTo(BudgetPeriod.WEEKLY)
    }

    @Test
    fun `when_days_selected_15_then_selected_period_is_biweekly`() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(15))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedDays).isEqualTo(15)
        assertThat(viewModel.uiState.value.selectedPeriod).isEqualTo(BudgetPeriod.BIWEEKLY)
    }

    @Test
    fun `when_days_selected_30_then_selected_period_is_monthly`() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(30))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedDays).isEqualTo(30)
        assertThat(viewModel.uiState.value.selectedPeriod).isEqualTo(BudgetPeriod.MONTHLY)
    }

    @Test
    fun `when_days_selected_with_unrecognized_value_then_selected_period_falls_back_to_monthly`() = runTest {
        val viewModel = newViewModel()

        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(42))

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedDays).isEqualTo(42)
        assertThat(viewModel.uiState.value.selectedPeriod).isEqualTo(BudgetPeriod.MONTHLY)
    }

    // -------------------------------------------------------------------------
    // OnNextStep / OnPreviousStep
    // -------------------------------------------------------------------------

    @Test
    fun `when_next_step_is_processed_then_current_step_is_incremented`() = runTest {
        val viewModel = newViewModel()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(0)

        viewModel.processIntent(OnboardingUiIntent.OnNextStep)

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(1)
    }

    @Test
    fun `when_next_step_is_processed_at_max_then_current_step_stays_at_max`() = runTest {
        val viewModel = newViewModel()
        // OnboardingStep has 3 entries, so the max index is 2.
        repeat(OnboardingStep.entries.size) { viewModel.processIntent(OnboardingUiIntent.OnNextStep) }
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.entries.size - 1)

        viewModel.processIntent(OnboardingUiIntent.OnNextStep)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(OnboardingStep.entries.size - 1)
    }

    @Test
    fun `when_previous_step_is_processed_then_current_step_is_decremented`() = runTest {
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnNextStep)
        viewModel.processIntent(OnboardingUiIntent.OnNextStep)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(2)

        viewModel.processIntent(OnboardingUiIntent.OnPreviousStep)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(1)
    }

    @Test
    fun `when_previous_step_is_processed_at_zero_then_current_step_stays_at_zero`() = runTest {
        val viewModel = newViewModel()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(0)

        viewModel.processIntent(OnboardingUiIntent.OnPreviousStep)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.currentStep).isEqualTo(0)
    }

    // -------------------------------------------------------------------------
    // OnDateRangeSelected
    // -------------------------------------------------------------------------

    @Test
    fun `when_date_range_selected_with_daily_display_then_selected_period_is_daily`() = runTest {
        coEvery { budgetRepository.saveBudgetSettings(any()) } returns Unit
        coEvery { notificationScheduler.schedulePeriodEndNotification(any()) } returns Unit
        coEvery { notificationScheduler.initializeNotifications() } returns Unit
        coEvery { settingsRepository.setOnboardingCompleted(any()) } returns Unit
        val viewModel = newViewModel()
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 1, 31)
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("100.00"))

        viewModel.processIntent(OnboardingUiIntent.OnDateRangeSelected(start, end, budgetDisplayDays = 1))

        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertThat(state.startDate).isEqualTo(start)
        assertThat(state.endDate).isEqualTo(end)
        // 31 days inclusive
        assertThat(state.selectedDays).isEqualTo(31)
        assertThat(state.daysInPeriod).isEqualTo(1)
        assertThat(state.selectedPeriod).isEqualTo(BudgetPeriod.DAILY)
    }

    @Test
    fun `when_date_range_selected_with_weekly_display_then_selected_period_is_weekly`() = runTest {
        coEvery { budgetRepository.saveBudgetSettings(any()) } returns Unit
        coEvery { notificationScheduler.schedulePeriodEndNotification(any()) } returns Unit
        coEvery { notificationScheduler.initializeNotifications() } returns Unit
        coEvery { settingsRepository.setOnboardingCompleted(any()) } returns Unit
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("100.00"))

        viewModel.processIntent(
            OnboardingUiIntent.OnDateRangeSelected(
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 1, 7),
                budgetDisplayDays = 7,
            )
        )

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedPeriod).isEqualTo(BudgetPeriod.WEEKLY)
    }

    @Test
    fun `when_date_range_selected_with_biweekly_display_then_selected_period_is_biweekly`() = runTest {
        coEvery { budgetRepository.saveBudgetSettings(any()) } returns Unit
        coEvery { notificationScheduler.schedulePeriodEndNotification(any()) } returns Unit
        coEvery { notificationScheduler.initializeNotifications() } returns Unit
        coEvery { settingsRepository.setOnboardingCompleted(any()) } returns Unit
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("100.00"))

        viewModel.processIntent(
            OnboardingUiIntent.OnDateRangeSelected(
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 1, 14),
                budgetDisplayDays = 14,
            )
        )

        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selectedPeriod).isEqualTo(BudgetPeriod.BIWEEKLY)
    }

    @Test
    fun `when_date_range_selected_then_auto_completes_onboarding`() = runTest {
        coEvery { budgetRepository.saveBudgetSettings(any()) } returns Unit
        coEvery { notificationScheduler.schedulePeriodEndNotification(any()) } returns Unit
        coEvery { notificationScheduler.initializeNotifications() } returns Unit
        coEvery { settingsRepository.setOnboardingCompleted(any()) } returns Unit
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("100.00"))

        viewModel.processIntent(
            OnboardingUiIntent.OnDateRangeSelected(
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 1, 30),
                budgetDisplayDays = 30,
            )
        )

        advanceUntilIdle()
        // OnDateRangeSelected calls handleCompleteOnboarding() at the end,
        // so the VM should be marked as completed.
        assertThat(viewModel.uiState.value.isCompleted).isTrue()
    }

    // -------------------------------------------------------------------------
    // OnCompleteOnboarding — success and failure paths
    // -------------------------------------------------------------------------

    @Test
    fun `when_complete_onboarding_is_called_with_valid_budget_then_repository_saves_settings_and_notifications_scheduled_and_settings_persisted`() = runTest {
        coEvery { budgetRepository.saveBudgetSettings(any()) } returns Unit
        coEvery { notificationScheduler.schedulePeriodEndNotification(any()) } returns Unit
        coEvery { notificationScheduler.initializeNotifications() } returns Unit
        coEvery { settingsRepository.setOnboardingCompleted(any()) } returns Unit
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("1500.50"))
        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(30))

        viewModel.processIntent(OnboardingUiIntent.OnCompleteOnboarding)

        advanceUntilIdle()
        coVerify {
            budgetRepository.saveBudgetSettings(match { settings ->
                settings.totalBudget == BigDecimal("1500.50") &&
                    settings.period == BudgetPeriod.MONTHLY &&
                    settings.currencyCode == "USD" &&
                    settings.rollOverEnabled &&
                    !settings.rollOverCarryForward
            })
        }
        coVerify { notificationScheduler.schedulePeriodEndNotification(any()) }
        coVerify { notificationScheduler.initializeNotifications() }
        coVerify { settingsRepository.setOnboardingCompleted(true) }
        assertThat(viewModel.uiState.value.isCompleted).isTrue()
    }

    @Test
    fun `when_complete_onboarding_is_called_with_invalid_budget_string_then_state_does_not_advance`() = runTest {
        val viewModel = newViewModel()
        // Set an unparseable string
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("not a number"))
        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(7))

        viewModel.processIntent(OnboardingUiIntent.OnCompleteOnboarding)

        advanceUntilIdle()
        // BigDecimal("not a number") throws NumberFormatException, the
        // handler returns early — isCompleted stays false.
        assertThat(viewModel.uiState.value.isCompleted).isFalse()
        coVerify(exactly = 0) { budgetRepository.saveBudgetSettings(any()) }
        coVerify(exactly = 0) { settingsRepository.setOnboardingCompleted(any()) }
    }

    @Test
    fun `when_complete_onboarding_is_called_with_zero_budget_then_state_does_not_advance`() = runTest {
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("0"))
        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(7))

        viewModel.processIntent(OnboardingUiIntent.OnCompleteOnboarding)

        advanceUntilIdle()
        // BigDecimal("0") <= BigDecimal.ZERO — handler returns early.
        assertThat(viewModel.uiState.value.isCompleted).isFalse()
        coVerify(exactly = 0) { budgetRepository.saveBudgetSettings(any()) }
    }

    @Test
    fun `when_complete_onboarding_is_called_with_negative_budget_then_state_does_not_advance`() = runTest {
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("-10.00"))
        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(7))

        viewModel.processIntent(OnboardingUiIntent.OnCompleteOnboarding)

        advanceUntilIdle()
        // BigDecimal("-10.00") <= BigDecimal.ZERO — handler returns early.
        assertThat(viewModel.uiState.value.isCompleted).isFalse()
        coVerify(exactly = 0) { budgetRepository.saveBudgetSettings(any()) }
    }

    @Test
    fun `when_complete_onboarding_is_called_with_no_budget_set_then_state_does_not_advance`() = runTest {
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(7))
        // No OnBudgetAmountChanged event — budgetInput stays ""

        viewModel.processIntent(OnboardingUiIntent.OnCompleteOnboarding)

        advanceUntilIdle()
        // BigDecimal("") throws NumberFormatException — handler returns early.
        assertThat(viewModel.uiState.value.isCompleted).isFalse()
        coVerify(exactly = 0) { budgetRepository.saveBudgetSettings(any()) }
    }

    @Test
    fun `when_complete_onboarding_falls_back_to_today_when_start_date_is_null`() = runTest {
        coEvery { budgetRepository.saveBudgetSettings(any()) } returns Unit
        coEvery { notificationScheduler.schedulePeriodEndNotification(any()) } returns Unit
        coEvery { notificationScheduler.initializeNotifications() } returns Unit
        coEvery { settingsRepository.setOnboardingCompleted(any()) } returns Unit
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("100.00"))
        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(7))
        // startDate is null at this point

        viewModel.processIntent(OnboardingUiIntent.OnCompleteOnboarding)

        advanceUntilIdle()
        coVerify {
            budgetRepository.saveBudgetSettings(match { settings ->
                settings.startDate == LocalDate.now() &&
                    settings.endDate == LocalDate.now().plusDays(6)
            })
        }
    }

    @Test
    fun `when_complete_onboarding_calls_schedule_period_end_notification_with_end_date`() = runTest {
        coEvery { budgetRepository.saveBudgetSettings(any()) } returns Unit
        coEvery { notificationScheduler.schedulePeriodEndNotification(any()) } returns Unit
        coEvery { notificationScheduler.initializeNotifications() } returns Unit
        coEvery { settingsRepository.setOnboardingCompleted(any()) } returns Unit
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("100.00"))
        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(7))

        viewModel.processIntent(OnboardingUiIntent.OnCompleteOnboarding)

        advanceUntilIdle()
        coVerify {
            notificationScheduler.schedulePeriodEndNotification(
                match { date -> date == LocalDate.now().plusDays(6) }
            )
        }
    }

    // -------------------------------------------------------------------------
    // Effect channel — OnboardingCompleted
    // -------------------------------------------------------------------------

    @Test
    fun `when_complete_onboarding_succeeds_then_onboarding_completed_effect_is_emitted()`() = runTest {
        coEvery { budgetRepository.saveBudgetSettings(any()) } returns Unit
        coEvery { notificationScheduler.schedulePeriodEndNotification(any()) } returns Unit
        coEvery { notificationScheduler.initializeNotifications() } returns Unit
        coEvery { settingsRepository.setOnboardingCompleted(any()) } returns Unit
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("100.00"))
        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(7))

        viewModel.effects.test {
            viewModel.processIntent(OnboardingUiIntent.OnCompleteOnboarding)
            advanceUntilIdle()
            // Use runCurrent to drain the shared flow emit
            runCurrent()

            val effect = awaitItem()
            assertThat(effect).isEqualTo(OnboardingUiEffect.OnboardingCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when_complete_onboarding_fails_with_invalid_input_then_no_effect_is_emitted`() = runTest {
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("invalid"))

        viewModel.effects.test {
            viewModel.processIntent(OnboardingUiIntent.OnCompleteOnboarding)
            advanceUntilIdle()
            runCurrent()

            // No effect should be emitted because the handler returned
            // early (NumberFormatException was caught).
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when_complete_onboarding_fails_with_repo_exception_then_onboarding_failed_effect_is_emitted`() = runTest {
        coEvery { budgetRepository.saveBudgetSettings(any()) } throws RuntimeException("disk full")
        val viewModel = newViewModel()
        viewModel.processIntent(OnboardingUiIntent.OnBudgetAmountChanged("100.00"))
        viewModel.processIntent(OnboardingUiIntent.OnDaysSelected(7))

        viewModel.effects.test {
            viewModel.processIntent(OnboardingUiIntent.OnCompleteOnboarding)
            advanceUntilIdle()
            runCurrent()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(OnboardingUiEffect.OnboardingFailed::class.java)
            effect as OnboardingUiEffect.OnboardingFailed
            assertThat(effect.message).isEqualTo("disk full")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
