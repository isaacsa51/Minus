package com.serranoie.app.minus.presentation.ui.e2e.pockets

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.calculator.PENDING_SET_ASIDE_KEY_NAME
import com.serranoie.app.minus.domain.calculator.encodePendingSetAside
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.domain.usecase.AddTransactionUseCase
import com.serranoie.app.minus.domain.usecase.ClearEarlyFinishStateUseCase
import com.serranoie.app.minus.domain.usecase.DeleteTransactionUseCase
import com.serranoie.app.minus.domain.usecase.FinishBudgetEarlyUseCase
import com.serranoie.app.minus.domain.usecase.GetCurrentPeriodIdUseCase
import com.serranoie.app.minus.domain.usecase.MarkOnboardingCompletedUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodRolloverUseCase
import com.serranoie.app.minus.domain.usecase.PersistBudgetSettingsUseCase
import com.serranoie.app.minus.domain.usecase.UpdatePeriodEndNotificationTimeUseCase
import com.serranoie.app.minus.presentation.notification.NotificationHelper
import com.serranoie.app.minus.presentation.ui.budget.BudgetExpressionEvaluator
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.presentation.ui.budget.BudgetTransactionHandler
import com.serranoie.app.minus.presentation.ui.budget.BudgetUiState
import com.serranoie.app.minus.presentation.ui.budget.BudgetViewModel
import com.serranoie.app.minus.presentation.ui.budget.BudgetWidgetUpdater
import com.serranoie.app.minus.presentation.ui.budget.mvi.BudgetUiEffect
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetEditorIntent
import com.serranoie.app.minus.presentation.ui.budget.mvi.intent.BudgetNumpadIntent
import com.serranoie.app.minus.presentation.util.ErrorLogRecorder
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class PocketSetAsideE2ETest {

    private lateinit var harness: PocketE2EHarness
    private lateinit var viewModel: BudgetViewModel
    private val collectorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var runningPeriodId = 0L

    @Before
    fun setUp() {
        harness = PocketE2EHarness()
        harness.startPeriod(harness.today.minusDays(5), harness.today.plusDays(24), BigDecimal("1000.00"))
        runningPeriodId = harness.currentPeriodId()
        val errorLogRecorder = ErrorLogRecorder(harness.context)
        val evaluator = BudgetExpressionEvaluator()
        val repository = harness.budgetRepository
        val settings = harness.settingsRepository
        viewModel = BudgetViewModel(
            context = harness.context,
            budgetRepository = repository,
            settingsRepository = settings,
            notificationHelper = mockk<NotificationHelper>(relaxed = true),
            notificationScheduler = harness.notificationScheduler,
            transactionHandler = BudgetTransactionHandler(
                budgetRepository = repository,
                addTransactionUseCase = AddTransactionUseCase(repository),
                deleteTransactionUseCase = DeleteTransactionUseCase(repository),
                budgetExpressionEvaluator = evaluator,
                notificationScheduler = harness.notificationScheduler,
                errorLogRecorder = errorLogRecorder,
            ),
            budgetStateCalculator = BudgetStateCalculator(),
            budgetWidgetUpdater = mockk<BudgetWidgetUpdater>(relaxed = true),
            budgetExpressionEvaluator = evaluator,
            observeCurrentPeriodBoundaryUseCase = ObserveCurrentPeriodBoundaryUseCase(settings),
            observeCurrentPeriodRolloverUseCase = ObserveCurrentPeriodRolloverUseCase(settings),
            getCurrentPeriodIdUseCase = GetCurrentPeriodIdUseCase(settings),
            persistBudgetSettingsUseCase = PersistBudgetSettingsUseCase(harness.periodManager),
            updatePeriodEndNotificationTimeUseCase = UpdatePeriodEndNotificationTimeUseCase(harness.periodManager),
            finishBudgetEarlyUseCase = FinishBudgetEarlyUseCase(harness.periodManager),
            clearEarlyFinishStateUseCase = ClearEarlyFinishStateUseCase(harness.periodManager),
            markOnboardingCompletedUseCase = MarkOnboardingCompletedUseCase(settings),
            midnightTransitionManager = harness.transitionManager,
            errorLogRecorder = errorLogRecorder,
        )
        collectorScope.launch { viewModel.uiState.collect {} }
    }

    @After
    fun tearDown() {
        collectorScope.cancel()
        harness.close()
    }

    private fun awaitState(condition: (BudgetUiState) -> Boolean): BudgetUiState = runBlocking {
        withTimeout(5_000) { viewModel.uiState.first(condition) }
    }

    private fun type(amount: String) {
        amount.forEach { viewModel.processIntent(BudgetNumpadIntent.NumberTapped(it.toString())) }
        awaitState { it.numpadInput == amount }
    }

    private fun setAside(amount: String, pocket: SavingsPocket? = null) {
        type(amount)
        viewModel.processIntent(BudgetEditorIntent.SetSetAsideEnabled(true))
        pocket?.let { viewModel.processIntent(BudgetEditorIntent.PocketSelected(it.id)) }
        awaitState { it.isSetAsideEnabled && (pocket == null || it.selectedPocketId == pocket.id) }
        viewModel.processIntent(BudgetNumpadIntent.ApplyTapped)
        awaitState { it.numpadInput.isEmpty() && !it.isSetAsideEnabled }
    }

    private fun awaitPockets(count: Int) = awaitState { it.pockets.size == count && it.budgetState != null }

    @Test
    fun setting_aside_from_the_numpad_moves_the_amount_into_the_pocket_and_lowers_the_budget_without_spending() {
        val trip = harness.createPocket("Trip")
        awaitPockets(1)

        setAside("150")

        val row = harness.transactions().single()
        assertThat(row.pocketId).isEqualTo(trip.id)
        assertThat(row.isAdjustment).isTrue()
        assertThat(row.periodId).isEqualTo(runningPeriodId)
        assertThat(row.amount).isEqualToIgnoringScale(BigDecimal("150"))
        val state = awaitState { it.budgetState?.totalBudget?.compareTo(BigDecimal("850")) == 0 }
        assertThat(state.budgetState!!.totalSpentInPeriod).isEqualToIgnoringScale(BigDecimal.ZERO)
        assertThat(state.calculationPreview)
            .isEqualTo(harness.context.getString(R.string.pockets_saved_to, symbolOnlyCurrencyFormat("USD").format(BigDecimal("150")), "Trip"))
    }

    @Test
    fun with_several_pockets_only_the_one_picked_receives_the_set_aside() {
        val trip = harness.createPocket("Trip")
        val house = harness.createPocket("House")
        awaitPockets(2)

        setAside("80", pocket = house)

        assertThat(harness.balanceOf(house)).isEqualToIgnoringScale(BigDecimal("80"))
        assertThat(harness.balanceOf(trip)).isEqualToIgnoringScale(BigDecimal.ZERO)
    }

    @Test
    fun with_several_pockets_and_none_picked_nothing_is_saved_and_the_amount_stays() {
        harness.createPocket("Trip")
        harness.createPocket("House")
        awaitPockets(2)
        type("80")
        viewModel.processIntent(BudgetEditorIntent.SetSetAsideEnabled(true))
        awaitState { it.isSetAsideEnabled }

        val effect = applyAndAwaitEffect()

        assertThat((effect as BudgetUiEffect.ShowMessage).message)
            .isEqualTo(harness.context.getString(R.string.pockets_set_aside_pick))
        assertThat(harness.transactions()).isEmpty()
        assertThat(viewModel.uiState.value.numpadInput).isEqualTo("80")
    }

    @Test
    fun a_pocket_deleted_after_it_was_picked_keeps_the_amount_on_the_numpad() {
        val trip = harness.createPocket("Trip")
        awaitPockets(1)
        type("150")
        viewModel.processIntent(BudgetEditorIntent.SetSetAsideEnabled(true))
        awaitState { it.isSetAsideEnabled && it.selectedPocketId == trip.id }
        assertThat(runBlocking { harness.budgetRepository.deleteSavingsPocket(trip.id) }).isTrue()
        awaitState { it.pockets.isEmpty() }

        applyAndAwaitEffect()

        assertThat(harness.transactions()).isEmpty()
        assertThat(viewModel.uiState.value.numpadInput).isEqualTo("150")
    }

    @Test
    fun a_set_aside_staged_from_the_savings_reminder_opens_with_its_amount_and_pocket() {
        harness.createPocket("Trip")
        val house = harness.createPocket("House")
        awaitPockets(2)

        runBlocking {
            harness.settingsRepository.setString(PENDING_SET_ASIDE_KEY_NAME, encodePendingSetAside(house.id, BigDecimal("45")))
        }
        awaitState { it.isSetAsideEnabled && it.selectedPocketId == house.id && it.numpadInput == "45" }
        viewModel.processIntent(BudgetNumpadIntent.ApplyTapped)
        awaitState { it.numpadInput.isEmpty() && !it.isSetAsideEnabled }

        assertThat(harness.balanceOf(house)).isEqualToIgnoringScale(BigDecimal("45"))
    }

    @Test
    fun money_set_aside_mid_budget_is_not_offered_again_when_the_budget_is_finished_early() {
        harness.createPocket("Trip")
        harness.spend("300.00", harness.today.minusDays(2).atTime(10, 0))
        awaitPockets(1)
        setAside("100")

        harness.finishEarly()

        assertThat(harness.transitionManager.midnightTransitionData.value!!.remainingAmount)
            .isEqualToIgnoringScale(BigDecimal("600.00"))
    }

    private fun applyAndAwaitEffect(): BudgetUiEffect = runBlocking {
        val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }
        viewModel.processIntent(BudgetNumpadIntent.ApplyTapped)
        withTimeout(5_000) { effect.await() }
    }
}
