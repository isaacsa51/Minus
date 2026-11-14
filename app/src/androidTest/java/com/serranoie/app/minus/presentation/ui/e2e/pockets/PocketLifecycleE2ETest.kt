package com.serranoie.app.minus.presentation.ui.e2e.pockets

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.domain.usecase.ClearEarlyFinishStateUseCase
import com.serranoie.app.minus.domain.usecase.ObserveCurrentPeriodBoundaryUseCase
import com.serranoie.app.minus.domain.usecase.PersistBudgetSettingsUseCase
import com.serranoie.app.minus.presentation.ui.analytics.AnalyticsViewModel
import com.serranoie.app.minus.presentation.ui.analytics.SavingsPocketsCard
import com.serranoie.app.minus.presentation.ui.budget.BudgetStateCalculator
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.ErrorLogRecorder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class PocketLifecycleE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var harness: PocketE2EHarness
    private lateinit var viewModel: AnalyticsViewModel
    private val calculator = BudgetStateCalculator()

    @Before
    fun setUp() {
        harness = PocketE2EHarness()
        harness.startPeriod(harness.today.minusDays(5), harness.today.plusDays(24), BigDecimal("1000.00"))
        viewModel = AnalyticsViewModel(
            budgetRepository = harness.budgetRepository,
            settingsRepository = harness.settingsRepository,
            budgetStateCalculator = calculator,
            observeCurrentPeriodBoundaryUseCase = ObserveCurrentPeriodBoundaryUseCase(harness.settingsRepository),
            clearEarlyFinishStateUseCase = ClearEarlyFinishStateUseCase(harness.periodManager),
            persistBudgetSettingsUseCase = PersistBudgetSettingsUseCase(harness.periodManager),
            errorLogRecorder = ErrorLogRecorder(harness.context),
        )
    }

    @After
    fun tearDown() {
        harness.close()
    }

    private fun label(resId: Int, vararg args: Any): String = composeTestRule.activity.getString(resId, *args)

    private fun showPockets() {
        composeTestRule.setContent {
            MinusTheme {
                val state by viewModel.uiState.collectAsState()
                Column {
                    SavingsPocketsCard(
                        pockets = state.pockets,
                        currencyCode = "USD",
                        canMoveMoney = true,
                        onCreatePocket = viewModel::onCreatePocket,
                        onDeposit = { id, amount -> viewModel.onPocketDeposit(id, amount) },
                        onWithdraw = { id, amount -> viewModel.onPocketWithdraw(id, amount) },
                        onDelete = viewModel::onDeletePocket,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun awaitUntil(condition: () -> Boolean) {
        composeTestRule.waitUntil(timeoutMillis = 5_000, condition = condition)
        composeTestRule.waitForIdle()
    }

    private fun trip(): SavingsPocket? = harness.pockets().singleOrNull { it.name == "Trip" }

    private fun createTripFromTheCard() {
        composeTestRule.onNodeWithText(label(R.string.pockets_new)).performClick()
        composeTestRule.onNodeWithText(label(R.string.pockets_name_label)).performTextInput("  Trip ")
        composeTestRule.onNodeWithText(label(R.string.pockets_target_label)).performTextInput("500")
        composeTestRule.onNodeWithText(label(R.string.save)).performClick()
        awaitUntil { trip() != null }
    }

    private fun moveMoneyFromTheCard(buttonRes: Int, amount: String) {
        composeTestRule.onNodeWithText(label(buttonRes)).performClick()
        composeTestRule.onNodeWithText(label(R.string.pockets_amount_label)).performTextInput(amount)
        composeTestRule.onNodeWithText(label(R.string.save)).performClick()
    }

    @Test
    fun a_pocket_goes_from_creation_through_deposit_and_withdrawal_to_deletion() {
        showPockets()

        createTripFromTheCard()
        val created = trip()!!
        assertThat(created.target).isEqualToIgnoringScale(BigDecimal("500"))
        composeTestRule.onNodeWithText("Trip").assertIsDisplayed()

        moveMoneyFromTheCard(R.string.pockets_add_money, "120")
        awaitUntil { harness.balanceOf(created).compareTo(BigDecimal("120")) == 0 }
        composeTestRule.onAllNodesWithText(label(R.string.pockets_delete)).assertCountEquals(0)

        composeTestRule.onNodeWithText(label(R.string.pockets_take_out)).performClick()
        composeTestRule.onNodeWithText(label(R.string.pockets_amount_label)).performTextInput("150")
        composeTestRule.onNodeWithText(label(R.string.save)).assertIsNotEnabled()
        composeTestRule.onNodeWithText(label(R.string.pockets_amount_label)).performTextClearance()
        composeTestRule.onNodeWithText(label(R.string.pockets_amount_label)).performTextInput("120")
        composeTestRule.onNodeWithText(label(R.string.save)).assertIsEnabled().performClick()
        awaitUntil { harness.balanceOf(created).signum() == 0 }

        composeTestRule.onNodeWithText(label(R.string.pockets_delete)).performClick()
        composeTestRule.onNodeWithText(label(R.string.pockets_delete_title, "Trip")).assertIsDisplayed()
        composeTestRule.onNodeWithText(label(R.string.delete)).performClick()
        awaitUntil { trip() == null }

        composeTestRule.onNodeWithText(label(R.string.pockets_empty)).assertIsDisplayed()
        assertThat(harness.transactions().filter { it.pocketId == created.id }.map { it.amount.toPlainString() })
            .containsExactly("120", "-120")
    }

    @Test
    fun money_in_a_pocket_leaves_the_budget_without_counting_as_spending_and_comes_back_when_taken_out() {
        val trip = harness.createPocket("Trip")
        showPockets()

        moveMoneyFromTheCard(R.string.pockets_add_money, "200")
        awaitUntil { harness.balanceOf(trip).compareTo(BigDecimal("200")) == 0 }
        val afterDeposit = currentState()
        assertThat(afterDeposit.totalBudget).isEqualToIgnoringScale(BigDecimal("800"))
        assertThat(afterDeposit.totalSpentInPeriod).isEqualToIgnoringScale(BigDecimal.ZERO)

        moveMoneyFromTheCard(R.string.pockets_take_out, "50")
        awaitUntil { harness.balanceOf(trip).compareTo(BigDecimal("150")) == 0 }
        val afterWithdrawal = currentState()
        assertThat(afterWithdrawal.totalBudget).isEqualToIgnoringScale(BigDecimal("850"))
        assertThat(afterWithdrawal.totalSpentInPeriod).isEqualToIgnoringScale(BigDecimal.ZERO)
    }

    @Test
    fun a_pocket_holding_money_offers_no_delete_and_the_repository_refuses_to_drop_it() {
        val trip = harness.createPocket("Trip")
        assertThat(harness.moveMoney(trip, "75", harness.today.atStartOfDay().plusMinutes(1))).isTrue()
        showPockets()
        awaitUntil { viewModel.uiState.value.pockets.any { it.balance.signum() > 0 } }

        composeTestRule.onAllNodesWithText(label(R.string.pockets_delete)).assertCountEquals(0)
        viewModel.onDeletePocket(trip.id)
        composeTestRule.waitForIdle()

        assertThat(trip()).isNotNull()
        assertThat(harness.balanceOf(trip)).isEqualToIgnoringScale(BigDecimal("75"))
    }

    @Test
    fun the_savings_goal_pocket_shows_its_localized_name_and_can_never_be_deleted() {
        runBlocking { harness.budgetRepository.saveSavingsPocket(SavingsPocket(name = "", target = BigDecimal("500"))) }
        showPockets()
        awaitUntil { viewModel.uiState.value.pockets.isNotEmpty() }

        composeTestRule.onNodeWithText(label(R.string.pockets_goal_name)).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(label(R.string.pockets_delete)).assertCountEquals(0)
    }

    private fun currentState() = runBlocking {
        val settings = harness.budgetRepository.getBudgetSettingsSync()!!
        val user = harness.settingsRepository.getSettings()
        val rows = calculator.filterPeriodTransactions(
            transactions = harness.transactions(),
            settings = settings,
            currentPeriodId = user.currentPeriodId,
            currentPeriodStartedAtMillis = user.currentPeriodStartedAt,
        )
        calculator.calculateBudgetState(settings, rows, harness.today, allTransactions = harness.transactions())
    }
}
