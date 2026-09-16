package com.serranoie.app.minus.presentation.ui.e2e.subscriptions

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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

    private val today = LocalDate.now()

    private val dueTodayItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("16.99"),
            comment = "Netflix",
            date = today.minusMonths(2).atStartOfDay(),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today,
        isInCurrentPeriod = true,
    )

    private val dueSoonItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 2L,
            amount = BigDecimal("9.99"),
            comment = "Spotify",
            date = today.minusDays(4).atStartOfDay(),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.WEEKLY,
        ),
        nextChargeDate = today.plusDays(2),
        isInCurrentPeriod = true,
    )

    private val upcomingItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 3L,
            amount = BigDecimal("49.99"),
            comment = "Gym membership",
            date = today.minusMonths(3).atStartOfDay(),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today.plusDays(12),
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

    private fun scrollToText(text: String) {
        composeTestRule.onNodeWithTag("SubscriptionsScreen").performScrollToNode(hasText(text))
    }

    @Test
    fun when_no_active_subscriptions_then_empty_state_is_shown() {
        setSubscriptionsContent(state = SubscriptionsUiState(isLoading = false))

        val emptyTitle = composeTestRule.activity.getString(R.string.subscriptions_empty_title)
        composeTestRule.onNodeWithText(emptyTitle).assertExists()
    }

    @Test
    fun when_a_subscription_is_due_today_then_its_confirm_and_skip_actions_are_visible() {
        setSubscriptionsContent(
            state = SubscriptionsUiState(
                isLoading = false,
                dueSoon = listOf(dueTodayItem),
                activeCount = 1,
                currencyCode = "USD",
                daysUntilNextCharge = 0L,
            ),
        )

        val todayLabel = composeTestRule.activity.getString(R.string.upcoming_recurrent_today)
        val markPaidLabel = composeTestRule.activity.getString(R.string.subscriptions_item_mark_paid_short)
        val skipLabel = composeTestRule.activity.getString(R.string.subscriptions_item_skip_short)

        scrollToText("Netflix")
        composeTestRule.onNodeWithText("Netflix").assertExists()
        composeTestRule.onNodeWithText(todayLabel).assertExists()
        composeTestRule.onNodeWithText(markPaidLabel).assertExists()
        composeTestRule.onNodeWithText(skipLabel).assertExists()
    }

    @Test
    fun when_a_subscription_is_not_due_today_then_it_has_no_confirm_or_skip_actions() {
        setSubscriptionsContent(
            state = SubscriptionsUiState(
                isLoading = false,
                dueSoon = listOf(dueSoonItem),
                upcoming = listOf(upcomingItem),
                activeCount = 2,
                currencyCode = "USD",
                daysUntilNextCharge = 2L,
            ),
        )

        val markAsPaidLabel = composeTestRule.activity.getString(R.string.subscriptions_mark_as_paid)
        val skipLabel = composeTestRule.activity.getString(R.string.subscriptions_skip_this_cycle)

        scrollToText("Spotify")
        composeTestRule.onNodeWithText("Spotify").assertExists()
        scrollToText("Gym membership")
        composeTestRule.onNodeWithText("Gym membership").assertExists()
        composeTestRule.onNodeWithText(markAsPaidLabel).assertDoesNotExist()
        composeTestRule.onNodeWithText(skipLabel).assertDoesNotExist()
    }

    @Test
    fun when_mark_as_paid_is_tapped_then_onConfirmPaid_receives_the_transaction_and_charge_date() {
        var confirmedTransactionId: Long? = null
        var confirmedDate: LocalDate? = null

        setSubscriptionsContent(
            state = SubscriptionsUiState(
                isLoading = false,
                dueSoon = listOf(dueTodayItem),
                activeCount = 1,
                currencyCode = "USD",
                daysUntilNextCharge = 0L,
            ),
            actions = SubscriptionsActions(
                onConfirmPaid = { transaction, date ->
                    confirmedTransactionId = transaction.id
                    confirmedDate = date
                },
            ),
        )

        val markPaidLabel = composeTestRule.activity.getString(R.string.subscriptions_item_mark_paid_short)
        scrollToText(markPaidLabel)
        composeTestRule.onNodeWithText(markPaidLabel).performClick()

        assert(confirmedTransactionId == dueTodayItem.transaction.id)
        assert(confirmedDate == dueTodayItem.nextChargeDate)
    }

    @Test
    fun when_skip_is_tapped_then_onSkip_receives_the_transaction_and_charge_date() {
        var skippedTransactionId: Long? = null
        var skippedDate: LocalDate? = null

        setSubscriptionsContent(
            state = SubscriptionsUiState(
                isLoading = false,
                dueSoon = listOf(dueTodayItem),
                activeCount = 1,
                currencyCode = "USD",
                daysUntilNextCharge = 0L,
            ),
            actions = SubscriptionsActions(
                onSkip = { transaction, date ->
                    skippedTransactionId = transaction.id
                    skippedDate = date
                },
            ),
        )

        val skipLabel = composeTestRule.activity.getString(R.string.subscriptions_item_skip_short)
        scrollToText(skipLabel)
        composeTestRule.onNodeWithText(skipLabel).performClick()

        assert(skippedTransactionId == dueTodayItem.transaction.id)
        assert(skippedDate == dueTodayItem.nextChargeDate)
    }
}
