package com.serranoie.app.minus.presentation.ui.e2e.history

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.google.common.truth.Truth
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.History
import com.serranoie.app.minus.presentation.ui.history.HistoryUiIntent
import com.serranoie.app.minus.presentation.ui.history.HistoryUiState
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime

class HistoryScreenE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val today: LocalDate = LocalDate.now()
    private val periodStart: LocalDate = today.minusDays(15)
    private val periodEnd: LocalDate = today.plusDays(15)

    private fun sampleBudgetSettings(
        totalBudget: BigDecimal = BigDecimal("1500.00"),
        period: BudgetPeriod = BudgetPeriod.MONTHLY,
        currencyCode: String = "USD",
        startDate: LocalDate = periodStart,
        endDate: LocalDate = periodEnd,
    ) = BudgetSettings(
        totalBudget = totalBudget,
        period = period,
        startDate = startDate,
        endDate = endDate,
        currencyCode = currencyCode,
        daysInPeriod = 31,
    )

    private fun sampleBudgetState(
        totalBudget: BigDecimal = BigDecimal("1500.00"),
        totalSpentInPeriod: BigDecimal = BigDecimal("250.00"),
        remainingToday: BigDecimal = BigDecimal("1250.00"),
        dailyBudget: BigDecimal = BigDecimal("50.00"),
    ) = BudgetState(
        remainingToday = remainingToday,
        totalSpentToday = BigDecimal("25.00"),
        dailyBudget = dailyBudget,
        daysRemaining = 15,
        progress = 0.17f,
        isOverBudget = false,
        totalBudget = totalBudget,
        totalSpentInPeriod = totalSpentInPeriod,
    )

    private fun sampleTransactions(): List<Transaction> = listOf(
        Transaction.create(
            amount = BigDecimal("45.50"),
            comment = "Groceries",
            date = LocalDateTime.now().minusHours(2),
        ),
        Transaction.create(
            amount = BigDecimal("12.00"),
            comment = "Coffee",
            date = LocalDateTime.now().minusHours(5),
        ),
        Transaction.create(
            amount = BigDecimal("85.00"),
            comment = "Gas",
            date = LocalDateTime.now().minusDays(1),
        ),
        Transaction.create(
            amount = BigDecimal("30.00"),
            comment = "Lunch",
            date = LocalDateTime.now().minusDays(2),
        ),
    )

    private fun setHistoryContent(
        uiState: HistoryUiState,
        onProcessIntent: (HistoryUiIntent) -> Unit = {},
    ) {
        composeTestRule.setContent {
            MinusTheme {
                History(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize(),
                    onProcessIntent = onProcessIntent,
                )
            }
        }
    }

    private fun sampleUpcomingRecurrentItems(): List<UpcomingRecurrentItem> = listOf(
        UpcomingRecurrentItem(
            transaction = Transaction.create(
                amount = BigDecimal("15.00"),
                comment = "Netflix",
                date = LocalDateTime.now().minusDays(10),
                isRecurrent = true,
                recurrentFrequency = RecurrentFrequency.MONTHLY,
            ),
            nextChargeDate = today.plusDays(5),
            isInCurrentPeriod = true,
        )
    )

    @Test
    fun when_recurrent_view_mode_is_horizontal_then_recurrent_items_are_shown_as_cards() {
        val recurrentItems = sampleUpcomingRecurrentItems()
        setHistoryContent(
            uiState = HistoryUiState(
                budgetSettings = sampleBudgetSettings(),
                budgetState = sampleBudgetState(),
                upcomingRecurrentInPeriod = recurrentItems,
                showUpcomingRecurrentInPeriod = true,
                recurrentPaymentsViewMode = RecurrentPaymentsViewMode.HORIZONTAL_LIST,
            ),
        )

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Netflix").assertIsDisplayed()
        val monthlyLabel =
            composeTestRule.activity.getString(R.string.recurrent_ticket_frequency_monthly)
        composeTestRule.onNodeWithText(monthlyLabel, substring = true).assertIsDisplayed()
    }

    @Test
    fun when_recurrent_view_mode_is_vertical_then_recurrent_items_are_shown_as_list_items() {
        val recurrentItems = sampleUpcomingRecurrentItems()
        setHistoryContent(
            uiState = HistoryUiState(
                budgetSettings = sampleBudgetSettings(),
                budgetState = sampleBudgetState(),
                upcomingRecurrentInPeriod = recurrentItems,
                showUpcomingRecurrentInPeriod = true,
                recurrentPaymentsViewMode = RecurrentPaymentsViewMode.VERTICAL_LIST,
            ),
        )

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Netflix").assertIsDisplayed()

        val amount = formatCurrency(BigDecimal("15"))
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    @Test
    fun when_tapping_transaction_then_detail_dialog_is_shown() {
        val transactions = sampleTransactions()
        var capturedIntent: HistoryUiIntent? = null

        setHistoryContent(
            uiState = HistoryUiState(
                budgetSettings = sampleBudgetSettings(),
                budgetState = sampleBudgetState(),
                transactions = transactions,
                displayTransactions = transactions,
                groupedCurrentTransactions = mapOf(today to transactions),
                expandedDates = setOf(today),
            ),
            onProcessIntent = { capturedIntent = it }
        )

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Coffee").performClick()

        Truth.assertThat(capturedIntent)
            .isInstanceOf(HistoryUiIntent.ToggleExpandedTransaction::class.java)
        val transactionId = (capturedIntent as HistoryUiIntent.ToggleExpandedTransaction).transactionId
        Truth.assertThat(transactionId).isEqualTo(transactions.find { it.comment == "Coffee" }?.id)
    }

    @Test
    fun when_swiping_right_on_transaction_then_it_is_removed_from_list() {
        val transactions = sampleTransactions()
        var uiState by mutableStateOf(
            HistoryUiState(
                budgetSettings = sampleBudgetSettings(),
                budgetState = sampleBudgetState(),
                transactions = transactions,
                displayTransactions = transactions,
                groupedCurrentTransactions = mapOf(today to transactions),
                expandedDates = setOf(today),
            )
        )

        composeTestRule.setContent {
            MinusTheme {
                History(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize(),
                    onProcessIntent = { intent ->
                        if (intent is HistoryUiIntent.DeleteTransaction) {
                            val newTransactions =
                                uiState.transactions.filterNot { it.id == intent.transaction.id }
                            uiState = uiState.copy(
                                transactions = newTransactions,
                                displayTransactions = newTransactions,
                                groupedCurrentTransactions = mapOf(today to newTransactions)
                            )
                        }
                    },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Coffee").assertIsDisplayed()

        composeTestRule.onNodeWithText("Coffee").performTouchInput {
            swipeRight()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Coffee").assertCountEquals(0)
    }

    @Test
    fun when_swiping_left_on_transaction_then_transaction_edit_screen_is_displayed() {
        val transactions = sampleTransactions()
        var uiState by mutableStateOf(
            HistoryUiState(
                budgetSettings = sampleBudgetSettings(),
                budgetState = sampleBudgetState(),
                transactions = transactions,
                displayTransactions = transactions,
                groupedCurrentTransactions = mapOf(today to transactions),
                expandedDates = setOf(today),
            )
        )

        composeTestRule.setContent {
            MinusTheme {
                History(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize(),
                    onProcessIntent = { intent ->
                        if (intent is HistoryUiIntent.SetEditingTransaction) {
                            uiState = uiState.copy(editingTransaction = intent.transaction)
                        }
                    },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Coffee").performTouchInput {
            swipeLeft()
        }

        composeTestRule.waitForIdle()

        val editTitle = composeTestRule.activity.getString(R.string.edit_expense_title)
        composeTestRule.onNodeWithText(editTitle).assertIsDisplayed()

        composeTestRule.onAllNodesWithText("Coffee").onLast().assertIsDisplayed()
    }

    @Test
    fun when_tapping_transaction_then_deleting_from_dialog_removes_it_from_list() {
        val transactions = sampleTransactions()
        var uiState by mutableStateOf(
            HistoryUiState(
                budgetSettings = sampleBudgetSettings(),
                budgetState = sampleBudgetState(),
                transactions = transactions,
                displayTransactions = transactions,
                groupedCurrentTransactions = mapOf(today to transactions),
                expandedDates = setOf(today),
            )
        )

        composeTestRule.setContent {
            MinusTheme {
                History(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize(),
                    onProcessIntent = { intent ->
                        when (intent) {
                            is HistoryUiIntent.ToggleExpandedTransaction -> {
                                uiState = uiState.copy(expandedTransactionId = intent.transactionId)
                            }

                            is HistoryUiIntent.DeleteTransaction -> {
                                val newTransactions =
                                    uiState.transactions.filterNot { it.id == intent.transaction.id }
                                uiState = uiState.copy(
                                    transactions = newTransactions,
                                    displayTransactions = newTransactions,
                                    groupedCurrentTransactions = mapOf(today to newTransactions),
                                    expandedTransactionId = null
                                )
                            }

                            else -> {}
                        }
                    },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Coffee").performClick()
        composeTestRule.waitForIdle()

        val deleteLabel = composeTestRule.activity.getString(R.string.delete)
        composeTestRule.onNodeWithText(deleteLabel).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Coffee").assertCountEquals(0)
    }

    @Test
    fun when_tapping_transaction_then_editing_from_dialog_shows_edit_screen() {
        val transactions = sampleTransactions()
        var uiState by mutableStateOf(
            HistoryUiState(
                budgetSettings = sampleBudgetSettings(),
                budgetState = sampleBudgetState(),
                transactions = transactions,
                displayTransactions = transactions,
                groupedCurrentTransactions = mapOf(today to transactions),
                expandedDates = setOf(today),
            )
        )

        composeTestRule.setContent {
            MinusTheme {
                History(
                    uiState = uiState,
                    modifier = Modifier.fillMaxSize(),
                    onProcessIntent = { intent ->
                        when (intent) {
                            is HistoryUiIntent.ToggleExpandedTransaction -> {
                                uiState = uiState.copy(expandedTransactionId = intent.transactionId)
                            }

                            is HistoryUiIntent.SetEditingTransaction -> {
                                uiState = uiState.copy(
                                    editingTransaction = intent.transaction,
                                    expandedTransactionId = null
                                )
                            }

                            else -> {}
                        }
                    },
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Coffee").performClick()
        composeTestRule.waitForIdle()

        val editLabel = composeTestRule.activity.getString(R.string.edit)
        composeTestRule.onNodeWithText(editLabel).performClick()
        composeTestRule.waitForIdle()

        val editTitle = composeTestRule.activity.getString(R.string.edit_expense_title)
        composeTestRule.onNodeWithText(editTitle).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Coffee").onLast().assertIsDisplayed()
    }

    private fun formatCurrency(value: BigDecimal, currencyCode: String = "USD"): String {
        val deviceLocale = composeTestRule.activity.resources.configuration.locales[0]
        val symbol = SupportedCurrency.findByCode(currencyCode)?.symbol ?: "$"
        val formatter = NumberFormat.getNumberInstance(deviceLocale).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        return "$symbol${formatter.format(value)}"
    }

    private fun prettyDate(date: LocalDate): String {
        val deviceLocale = composeTestRule.activity.resources.configuration.locales[0]
        val monthFormat = java.time.format.DateTimeFormatter.ofPattern("dd MMM", deviceLocale)
        return date.format(monthFormat)
    }

    @Test
    fun when_user_opens_history_then_budget_display_shows_amount_total_days_and_dates() {
        val budgetSettings = sampleBudgetSettings()
        val budgetState = sampleBudgetState()
        val transactions = sampleTransactions()

        setHistoryContent(
            uiState = HistoryUiState(
                budgetSettings = budgetSettings,
                budgetState = budgetState,
                transactions = transactions,
                displayTransactions = transactions,
                groupedCurrentTransactions = mapOf(today to transactions),
                expandedDates = setOf(today),
            ),
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val totalBudgetLabel = composeTestRule.activity.getString(R.string.total_budget)
        composeTestRule.onAllNodesWithText(totalBudgetLabel).onLast().assertIsDisplayed()

        val expectedBudget = formatCurrency(BigDecimal("1500"))
        composeTestRule.onAllNodesWithText(expectedBudget).onLast().assertIsDisplayed()

        val expectedDays = composeTestRule.activity.resources.getQuantityString(
            R.plurals.analytics_days_left,
            31,
            31,
        )
        composeTestRule.onAllNodesWithText(expectedDays).onLast().assertIsDisplayed()

        val expectedStart = prettyDate(periodStart)
        val expectedEnd = prettyDate(periodEnd)
        composeTestRule.onAllNodesWithText(expectedStart, substring = true).onLast()
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText(expectedEnd, substring = true).onLast()
            .assertIsDisplayed()

        Truth.assertThat(transactions).hasSize(4)
    }

    @Test
    fun when_history_has_transactions_then_expenses_are_visible_in_list() {
        val transactions = sampleTransactions()

        setHistoryContent(
            uiState = HistoryUiState(
                budgetSettings = sampleBudgetSettings(),
                budgetState = sampleBudgetState(),
                transactions = transactions,
                displayTransactions = transactions,
                groupedCurrentTransactions = mapOf(today to transactions),
                expandedDates = setOf(today),
            ),
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("Groceries").onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Coffee").onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Gas").onLast().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Lunch").onLast().assertIsDisplayed()
    }

    @Test
    fun when_history_is_displayed_then_total_budget_label_is_visible() {
        setHistoryContent(
            uiState = HistoryUiState(
                budgetSettings = sampleBudgetSettings(),
                budgetState = sampleBudgetState(),
            ),
        )

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val totalBudgetLabel = composeTestRule.activity.getString(R.string.total_budget)
        composeTestRule.onAllNodesWithText(totalBudgetLabel).onLast().assertIsDisplayed()

        val expectedBudget = formatCurrency(BigDecimal("1500"))
        composeTestRule.onAllNodesWithText(expectedBudget).onLast().assertIsDisplayed()
    }
}
