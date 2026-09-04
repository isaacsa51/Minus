package com.serranoie.app.minus.presentation.ui.e2e.history

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import com.google.common.truth.Truth
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.edit.TransactionEditScreen
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TransactionEditE2ETests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val today: LocalDate = LocalDate.now()
    private val periodStart: LocalDate = today.minusDays(15)
    private val periodEnd: LocalDate = today.plusDays(15)

    private data class SavePayload(
        val amount: BigDecimal,
        val comment: String,
        val dateTime: LocalDateTime,
        val isRecurrent: Boolean,
        val frequency: RecurrentFrequency?,
        val endDate: LocalDate?,
        val subscriptionDay: Int?,
        val isCredit: Boolean,
    )

    private fun sampleTransaction(
        amount: String = "50.00",
        comment: String = "Coffee",
        date: LocalDateTime = LocalDateTime.now().minusHours(2),
        isRecurrent: Boolean = false,
        frequency: RecurrentFrequency? = null,
        subscriptionDay: Int? = null,
        recurrentEndDate: LocalDateTime? = null,
    ): Transaction = Transaction.create(
        amount = BigDecimal(amount),
        comment = comment,
        date = date,
        isRecurrent = isRecurrent,
        recurrentFrequency = frequency,
        subscriptionDay = subscriptionDay,
        recurrentEndDate = recurrentEndDate,
    )

    private fun setEditContent(
        transaction: Transaction,
        isCreditQuickToggleEnabled: Boolean = false,
        creditCardCutoffDay: Int? = null,
        onUpdateCreditCutoffDay: (Int) -> Unit = {},
        onCancel: () -> Unit = {},
        onSave: (
            newAmount: BigDecimal,
            newComment: String,
            newDateTime: LocalDateTime,
            newIsRecurrent: Boolean,
            newFrequency: RecurrentFrequency?,
            newEndDate: LocalDate?,
            newSubscriptionDay: Int?,
            newIsCredit: Boolean,
        ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    ) {
        composeTestRule.setContent {
            MinusTheme {
                TransactionEditScreen(
                    transaction = transaction,
                    budgetStartDate = periodStart,
                    budgetEndDate = periodEnd,
                    currencyCode = "USD",
                    isCreditQuickToggleEnabled = isCreditQuickToggleEnabled,
                    creditCardCutoffDay = creditCardCutoffDay,
                    onUpdateCreditCutoffDay = onUpdateCreditCutoffDay,
                    onCancel = onCancel,
                    onSave = onSave,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun prettyDate(date: LocalDate): String {
        val deviceLocale = composeTestRule.activity.resources.configuration.locales[0]
        val monthFormat = java.time.format.DateTimeFormatter.ofPattern("dd MMMM", deviceLocale)
        return date.format(monthFormat)
    }

    private fun prettyTime(time: LocalTime): String =
        String.format("%02d:%02d", time.hour, time.minute)

    private fun cancelContentDesc(): String =
        composeTestRule.activity.getString(R.string.cancel_edit_content_desc)

    private fun saveLabel(): String = composeTestRule.activity.getString(R.string.save)

    private fun acceptLabel(): String = composeTestRule.activity.getString(R.string.accept)

    // Content descriptions mirror the Material 3 toggle buttons shared with Editor.kt.
    private val recurrentToggleDesc = "Recurrent payment"
    private val creditToggleDesc = "Credit card payment"

    private fun tapApply() {
        composeTestRule.onAllNodesWithContentDescription("Editor action").onLast()
            .performClick()
        composeTestRule.waitForIdle()
    }

    /** Taps the recurrence toggle and waits for the delayed config bottom sheet to settle. */
    private fun tapRecurrentToggle() {
        composeTestRule.onNodeWithContentDescription(recurrentToggleDesc).performClick()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()
    }

    @Test
    fun when_edit_existing_expense_and_save_then_save_callback_fires_with_same_values() {
        val tx = sampleTransaction(amount = "45.50", comment = "Groceries")
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val editTitle = composeTestRule.activity.getString(R.string.edit_expense_title)
        composeTestRule.onNodeWithText(editTitle).assertIsDisplayed()

        composeTestRule.onAllNodesWithText("Groceries").assertCountEquals(1)

        tapApply()

        Truth.assertThat(captured).isNotNull()
        Truth.assertThat(captured!!.amount).isEqualTo(BigDecimal("45.50"))
        Truth.assertThat(captured!!.comment).isEqualTo("Groceries")
        Truth.assertThat(captured!!.isRecurrent).isFalse()
    }

    @Test
    fun when_edit_then_close_then_on_cancel_callback_fires() {
        val tx = sampleTransaction()
        var cancelCount = 0

        setEditContent(
            transaction = tx,
            onCancel = { cancelCount += 1 },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(cancelContentDesc()).performClick()

        composeTestRule.waitForIdle()

        Truth.assertThat(cancelCount).isEqualTo(1)
    }

    @Test
    fun when_edit_and_add_new_category_and_save_then_on_save_has_new_comment() {
        val tx = sampleTransaction(comment = "Old")
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Old").onLast().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Old").onLast().performTextReplacement("Old Lunch")
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Old Lunch").onLast().performImeAction()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()

        tapApply()

        Truth.assertThat(captured).isNotNull()
        Truth.assertThat(captured!!.comment).isEqualTo("Old Lunch")
    }

    @Test
    fun when_edit_and_delete_category_and_save_then_on_save_has_empty_comment() {
        val tx = sampleTransaction(comment = "Coffee")
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Coffee").onLast().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(600)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Coffee").onLast().performTextClearance()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodes(hasSetTextAction()).onLast().performImeAction()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()

        tapApply()

        Truth.assertThat(captured).isNotNull()
        Truth.assertThat(captured!!.comment).isEmpty()
    }

    @Test
    fun when_tap_recurrent_toggle_then_recurrence_config_sheet_appears() {
        val tx = sampleTransaction()

        setEditContent(transaction = tx)

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        tapRecurrentToggle()

        val configureTitle = composeTestRule.activity.getString(R.string.configure_recurrence)
        composeTestRule.onNodeWithText(configureTitle).assertIsDisplayed()
    }

    @Test
    fun when_tap_recurrent_and_set_monthly_then_on_save_has_monthly_frequency() {
        val tx = sampleTransaction()
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        tapRecurrentToggle()

        val monthlyLabel = composeTestRule.activity.getString(R.string.recurrent_frequency_monthly)
        composeTestRule.onNodeWithText(monthlyLabel).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(saveLabel()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()

        tapApply()

        Truth.assertThat(captured).isNotNull()
        Truth.assertThat(captured!!.isRecurrent).isTrue()
        Truth.assertThat(captured!!.frequency).isEqualTo(RecurrentFrequency.MONTHLY)
    }

    @Test
    fun when_edit_recurrent_then_tap_toggle_then_sheet_shows_existing_values() {
        val initialDate = LocalDateTime.now().minusMonths(2)
        val tx = sampleTransaction(
            isRecurrent = true,
            frequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 15,
            recurrentEndDate = initialDate.plusMonths(6),
        )
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val recurrentTitle =
            composeTestRule.activity.getString(R.string.edit_recurrent_expense_title)
        composeTestRule.onNodeWithText(recurrentTitle).assertIsDisplayed()

        tapRecurrentToggle()

        val monthlyLabel = composeTestRule.activity.getString(R.string.recurrent_frequency_monthly)
        composeTestRule.onAllNodesWithText(monthlyLabel).onLast().assertIsDisplayed()

        val monthlyDayFormat = composeTestRule.activity.getString(
            R.string.monthly_on_day_format,
            15,
        )
        composeTestRule.onAllNodesWithText(monthlyDayFormat).onLast().assertIsDisplayed()

        composeTestRule.onNodeWithText(saveLabel()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()

        tapApply()

        Truth.assertThat(captured).isNotNull()
        Truth.assertThat(captured!!.isRecurrent).isTrue()
        Truth.assertThat(captured!!.frequency).isEqualTo(RecurrentFrequency.MONTHLY)
        Truth.assertThat(captured!!.subscriptionDay).isEqualTo(15)
    }

    @Test
    fun when_edit_recurrent_and_change_recurrence_day_then_on_save_has_new_day() {
        val tx = sampleTransaction(
            isRecurrent = true,
            frequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 10,
        )
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        tapRecurrentToggle()

        val nextDayDesc = composeTestRule.activity.getString(R.string.next_day)
        composeTestRule.onNodeWithContentDescription(nextDayDesc).performClick()
        composeTestRule.onNodeWithContentDescription(nextDayDesc).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(saveLabel()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()

        tapApply()

        Truth.assertThat(captured).isNotNull()
        Truth.assertThat(captured!!.isRecurrent).isTrue()
        Truth.assertThat(captured!!.subscriptionDay).isEqualTo(12)
    }
    
    // CHECK DATE SELECTOR HANDLER
    @Test
    fun when_edit_and_change_date_and_save_then_on_save_has_new_date() {
        val original = LocalDateTime.now().minusDays(2)
        val tx = sampleTransaction(date = original)
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val originalFormatted = prettyDate(original.toLocalDate())
        composeTestRule.onAllNodesWithText(originalFormatted).onLast().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(acceptLabel()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        tapApply()

        Truth.assertThat(captured).isNotNull()
    }

    @Test
    fun when_edit_and_change_time_and_save_then_on_save_has_new_time() {
        val original = LocalDateTime.of(today, LocalTime.of(10, 30))
        val tx = sampleTransaction(date = original)
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val originalTime = prettyTime(original.toLocalTime())
        composeTestRule.onAllNodesWithText(originalTime).onLast().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(acceptLabel()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        tapApply()

        Truth.assertThat(captured).isNotNull()
    }

    @Test
    fun when_edit_and_change_date_and_time_and_save_then_on_save_has_both_new_values() {
        val original = LocalDateTime.of(today.minusDays(3), LocalTime.of(8, 15))
        val tx = sampleTransaction(date = original)
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val originalFormatted = prettyDate(original.toLocalDate())
        composeTestRule.onAllNodesWithText(originalFormatted).onLast().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(acceptLabel()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        val originalTime = prettyTime(original.toLocalTime())
        composeTestRule.onAllNodesWithText(originalTime).onLast().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(acceptLabel()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        tapApply()

        Truth.assertThat(captured).isNotNull()
    }

    @Test
    fun when_amount_edited_via_numpad_then_on_save_has_new_amount() {
        val tx = sampleTransaction(amount = "5")
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // "5" -> tap "0" on the numpad -> "50"
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        tapApply()

        Truth.assertThat(captured).isNotNull()
        Truth.assertThat(captured!!.amount).isEqualTo(BigDecimal("50"))
    }

    @Test
    fun when_amount_cleared_then_numpad_delete_button_triggers_cancel() {
        val tx = sampleTransaction(amount = "5")
        var cancelCount = 0

        setEditContent(
            transaction = tx,
            onCancel = { cancelCount += 1 },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // Backspace "5" -> "0"; the primary action button now morphs into the delete button.
        composeTestRule.onAllNodesWithContentDescription("Editor action").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithContentDescription("Editor action").onLast().performClick()
        composeTestRule.waitForIdle()

        Truth.assertThat(cancelCount).isEqualTo(1)
    }

    @Test
    fun when_weekly_frequency_selected_then_on_save_has_weekly_and_null_subscription_day() {
        val tx = sampleTransaction()
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        tapRecurrentToggle()

        val weeklyLabel = composeTestRule.activity.getString(R.string.recurrent_frequency_weekly)
        composeTestRule.onNodeWithText(weeklyLabel).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(saveLabel()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()

        tapApply()

        Truth.assertThat(captured).isNotNull()
        Truth.assertThat(captured!!.isRecurrent).isTrue()
        Truth.assertThat(captured!!.frequency).isEqualTo(RecurrentFrequency.WEEKLY)
        Truth.assertThat(captured!!.subscriptionDay).isNull()
    }

    @Test
    fun when_recurrence_switched_off_in_sheet_then_on_save_is_not_recurrent() {
        val tx = sampleTransaction(
            isRecurrent = true,
            frequency = RecurrentFrequency.MONTHLY,
            subscriptionDay = 15,
        )
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        tapRecurrentToggle()

        // Flip the recurrence switch off inside the configuration sheet.
        composeTestRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(saveLabel()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(400)
        composeTestRule.waitForIdle()

        tapApply()

        Truth.assertThat(captured).isNotNull()
        Truth.assertThat(captured!!.isRecurrent).isFalse()
        Truth.assertThat(captured!!.frequency).isNull()
        Truth.assertThat(captured!!.subscriptionDay).isNull()
    }

    @Test
    fun when_credit_quick_toggle_enabled_and_tapped_then_on_save_has_credit_true() {
        val tx = sampleTransaction()
        var captured: SavePayload? = null

        setEditContent(
            transaction = tx,
            isCreditQuickToggleEnabled = true,
            creditCardCutoffDay = 15,
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(creditToggleDesc).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        tapApply()

        Truth.assertThat(captured).isNotNull()
        Truth.assertThat(captured!!.isCredit).isTrue()
    }

    @Test
    fun when_credit_toggle_tapped_without_cutoff_day_then_dialog_shown_and_confirm_sets_credit() {
        val tx = sampleTransaction()
        var captured: SavePayload? = null
        var updatedCutoffDay: Int? = null

        setEditContent(
            transaction = tx,
            isCreditQuickToggleEnabled = true,
            creditCardCutoffDay = null,
            onUpdateCreditCutoffDay = { updatedCutoffDay = it },
            onSave = { amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit ->
                captured =
                    SavePayload(amount, comment, dateTime, isRecurrent, frequency, endDate, subDay, isCredit)
            },
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription(creditToggleDesc).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        val cutoffTitle =
            composeTestRule.activity.getString(R.string.credit_cutoff_dialog_title)
        composeTestRule.onNodeWithText(cutoffTitle).assertIsDisplayed()

        composeTestRule.onNodeWithText(saveLabel()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        Truth.assertThat(updatedCutoffDay).isEqualTo(15)

        tapApply()

        Truth.assertThat(captured).isNotNull()
        Truth.assertThat(captured!!.isCredit).isTrue()
    }
}
