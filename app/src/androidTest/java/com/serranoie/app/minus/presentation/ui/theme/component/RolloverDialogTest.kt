package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class RolloverDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun formattedUsd(amount: BigDecimal): String =
        symbolOnlyCurrencyFormat("USD").format(amount)

    private fun renderDialog(
        remainingAmount: BigDecimal = BigDecimal("300"),
        spentAmount: BigDecimal = BigDecimal("700"),
        periodLabel: String = "1 Aug - 31 Aug",
        onSplitEqually: () -> Unit = {},
        onCarryToNextDay: () -> Unit = {},
        onDismiss: () -> Unit = {},
        onViewAnalytics: (() -> Unit)? = null,
    ) {
        composeTestRule.setContent {
            MinusTheme {
                RolloverDialog(
                    remainingAmount = remainingAmount,
                    currencyCode = "USD",
                    periodLabel = periodLabel,
                    spentAmount = spentAmount,
                    onSplitEqually = onSplitEqually,
                    onCarryToNextDay = onCarryToNextDay,
                    onDismiss = onDismiss,
                    onViewAnalytics = onViewAnalytics,
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun when_shown_then_remaining_amount_is_displayed() {
        renderDialog(remainingAmount = BigDecimal("300"))

        composeTestRule.onNodeWithText(formattedUsd(BigDecimal("300"))).assertIsDisplayed()
    }

    @Test
    fun when_shown_then_period_label_is_displayed() {
        renderDialog(periodLabel = "1 Aug - 31 Aug")

        composeTestRule.onNodeWithText("1 Aug - 31 Aug").assertIsDisplayed()
    }

    @Test
    fun when_view_analytics_not_provided_then_that_option_is_hidden() {
        renderDialog(onViewAnalytics = null)

        val label = composeTestRule.activity.getString(R.string.rollover_dialog_view_analytics_title)
        composeTestRule.onNodeWithText(label).assertDoesNotExist()
    }

    @Test
    fun when_view_analytics_provided_then_that_option_is_visible() {
        renderDialog(onViewAnalytics = {})

        val label = composeTestRule.activity.getString(R.string.rollover_dialog_view_analytics_title)
        composeTestRule.onNodeWithText(label).assertIsDisplayed()
    }

    @Test
    fun when_split_equally_tapped_then_callback_is_invoked() {
        var invoked = false
        renderDialog(onSplitEqually = { invoked = true })

        val label = composeTestRule.activity.getString(R.string.rollover_dialog_split_equally_title)
        composeTestRule.onNodeWithText(label).performClick()

        assertThat(invoked).isTrue()
    }

    @Test
    fun when_carry_to_next_day_tapped_then_callback_is_invoked() {
        var invoked = false
        renderDialog(onCarryToNextDay = { invoked = true })

        val label = composeTestRule.activity.getString(R.string.rollover_dialog_carry_to_tomorrow_title)
        composeTestRule.onNodeWithText(label).performClick()

        assertThat(invoked).isTrue()
    }

    @Test
    fun when_view_analytics_tapped_then_callback_is_invoked() {
        var invoked = false
        renderDialog(onViewAnalytics = { invoked = true })

        val label = composeTestRule.activity.getString(R.string.rollover_dialog_view_analytics_title)
        composeTestRule.onNodeWithText(label).performClick()

        assertThat(invoked).isTrue()
    }

    @Test
    fun when_cancel_tapped_then_dismiss_callback_is_invoked() {
        var invoked = false
        renderDialog(onDismiss = { invoked = true })

        val label = composeTestRule.activity.getString(R.string.cancel)
        composeTestRule.onNodeWithText(label).performClick()

        assertThat(invoked).isTrue()
    }

    @Test
    fun when_large_remaining_amount_then_it_still_renders_without_crashing() {
        renderDialog(remainingAmount = BigDecimal("999999.99"), spentAmount = BigDecimal("50000"))

        composeTestRule.onNodeWithText(formattedUsd(BigDecimal("999999.99"))).assertIsDisplayed()
    }
}
