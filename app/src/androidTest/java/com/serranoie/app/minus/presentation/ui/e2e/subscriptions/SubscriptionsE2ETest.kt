package com.serranoie.app.minus.presentation.ui.e2e.subscriptions

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.subscriptions.Subscriptions
import com.serranoie.app.minus.presentation.ui.subscriptions.SubscriptionsActions
import com.serranoie.app.minus.presentation.ui.subscriptions.SubscriptionsUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class SubscriptionsE2ETest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2026, 1, 15)

    private val dueSoonItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("16.99"),
            comment = "Netflix",
            date = today.minusMonths(2).atStartOfDay(),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today.plusDays(2),
        isInCurrentPeriod = true,
    )

    private val upcomingItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 2L,
            amount = BigDecimal("49.99"),
            comment = "Gym membership",
            date = today.minusMonths(3).atStartOfDay(),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today.plusDays(18),
        isInCurrentPeriod = false,
    )

    private fun setSubscriptionsContent(
        state: SubscriptionsUiState,
        actions: SubscriptionsActions = SubscriptionsActions(),
    ) {
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.setContent {
            MinusTheme {
                Subscriptions(state = state, actions = actions)
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun when_no_active_subscriptions_then_empty_state_is_shown() {
        setSubscriptionsContent(state = SubscriptionsUiState(isLoading = false))

        val emptyTitle = composeTestRule.activity.getString(R.string.subscriptions_empty_title)
        composeTestRule.onNodeWithText(emptyTitle).assertExists()
    }

    @Test
    fun when_a_due_soon_row_is_expanded_then_confirm_and_skip_actions_appear() {
        setSubscriptionsContent(
            state = SubscriptionsUiState(
                isLoading = false,
                dueSoon = listOf(dueSoonItem),
                activeCount = 1,
                currencyCode = "USD",
            ),
        )

        composeTestRule.onNodeWithText("Netflix").performClick()
        composeTestRule.waitForIdle()

        val markAsPaidLabel = composeTestRule.activity.getString(R.string.mark_as_paid)
        val skipLabel = composeTestRule.activity.getString(R.string.subscriptions_skip_this_cycle)
        composeTestRule.onNodeWithText(markAsPaidLabel).assertExists()
        composeTestRule.onNodeWithText(skipLabel).assertExists()
    }

    @Test
    fun when_an_upcoming_row_is_expanded_then_only_the_confirm_action_appears() {
        setSubscriptionsContent(
            state = SubscriptionsUiState(
                isLoading = false,
                upcoming = listOf(upcomingItem),
                activeCount = 1,
                currencyCode = "USD",
            ),
        )

        composeTestRule.onNodeWithText("Gym membership").performClick()
        composeTestRule.waitForIdle()

        val markAsPaidLabel = composeTestRule.activity.getString(R.string.mark_as_paid)
        composeTestRule.onNodeWithText(markAsPaidLabel).assertExists()
        composeTestRule.onNodeWithText("Gym membership").assertExists()
    }

    @Test
    fun when_mark_as_paid_is_tapped_then_onConfirmPaid_receives_the_transaction_and_charge_date() {
        var confirmedTransactionId: Long? = null
        var confirmedDate: LocalDate? = null

        setSubscriptionsContent(
            state = SubscriptionsUiState(
                isLoading = false,
                dueSoon = listOf(dueSoonItem),
                activeCount = 1,
                currencyCode = "USD",
            ),
            actions = SubscriptionsActions(
                onConfirmPaid = { transaction, date ->
                    confirmedTransactionId = transaction.id
                    confirmedDate = date
                },
            ),
        )

        composeTestRule.onNodeWithText("Netflix").performClick()
        composeTestRule.waitForIdle()
        val markAsPaidLabel = composeTestRule.activity.getString(R.string.mark_as_paid)
        composeTestRule.onNodeWithText(markAsPaidLabel).performClick()

        assert(confirmedTransactionId == dueSoonItem.transaction.id)
        assert(confirmedDate == dueSoonItem.nextChargeDate)
    }

    @Test
    fun when_skip_is_tapped_then_onSkip_receives_the_transaction_and_charge_date() {
        var skippedTransactionId: Long? = null
        var skippedDate: LocalDate? = null

        setSubscriptionsContent(
            state = SubscriptionsUiState(
                isLoading = false,
                dueSoon = listOf(dueSoonItem),
                activeCount = 1,
                currencyCode = "USD",
            ),
            actions = SubscriptionsActions(
                onSkip = { transaction, date ->
                    skippedTransactionId = transaction.id
                    skippedDate = date
                },
            ),
        )

        composeTestRule.onNodeWithText("Netflix").performClick()
        composeTestRule.waitForIdle()
        val skipLabel = composeTestRule.activity.getString(R.string.subscriptions_skip_this_cycle)
        composeTestRule.onNodeWithText(skipLabel).performClick()

        assert(skippedTransactionId == dueSoonItem.transaction.id)
        assert(skippedDate == dueSoonItem.nextChargeDate)
    }
}
