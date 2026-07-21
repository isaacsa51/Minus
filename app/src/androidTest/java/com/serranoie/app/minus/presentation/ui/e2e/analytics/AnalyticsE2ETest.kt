package com.serranoie.app.minus.presentation.ui.e2e.analytics

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.analytics.Analytics
import com.serranoie.app.minus.presentation.ui.analytics.AnalyticsState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

class AnalyticsE2ETest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val testDate: LocalDateTime = LocalDateTime.of(2026, 1, 15, 12, 0)
    private val startPeriodDate: Date =
        Date.from(
            LocalDate.of(2026, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
        )
    private val finishPeriodDate: Date =
        Date.from(
            LocalDate.of(2026, 1, 30).atStartOfDay().toInstant(ZoneOffset.UTC),
        )

    private fun foodTransactions(): List<Transaction> =
        listOf(
            Transaction(
                id = 1L,
                amount = BigDecimal("120.00"),
                comment = "Comida",
                date = testDate,
                periodId = 7L,
            ),
            Transaction(
                id = 2L,
                amount = BigDecimal("85.50"),
                comment = "Comida",
                date = testDate.minusDays(2),
                periodId = 7L,
            ),
            Transaction(
                id = 3L,
                amount = BigDecimal("150.00"),
                comment = "Comida",
                date = testDate.minusDays(5),
                periodId = 7L,
            ),
        )

    private fun moveTransactions(): List<Transaction> =
        listOf(
            Transaction(
                id = 4L,
                amount = BigDecimal("45.00"),
                comment = "Transporte",
                date = testDate.minusDays(1),
                periodId = 7L,
            ),
        )

    private fun funTransactions(): List<Transaction> =
        listOf(
            Transaction(
                id = 5L,
                amount = BigDecimal("60.00"),
                comment = "Entretenimiento",
                date = testDate.minusDays(3),
                periodId = 7L,
            ),
            Transaction(
                id = 6L,
                amount = BigDecimal("30.00"),
                comment = "Entretenimiento",
                date = testDate.minusDays(7),
                periodId = 7L,
            ),
        )

    private fun allTransactions(): List<Transaction> = foodTransactions() + moveTransactions() + funTransactions()

    private fun setAnalyticsContent() {
        // Android 17 is stricter about activity launch timing. Ensure the test
        // activity is fully resumed before setContent is called, and let the
        // framework idle after so the Compose hierarchy is discoverable.
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.setContent {
            MinusTheme {
                CompositionLocalProvider(LocalWindowInsets provides PaddingValues(0.dp)) {
                    Analytics(
                        state =
                            AnalyticsState(
                                spends = allTransactions(),
                                wholeBudget = BigDecimal("1000.00"),
                                currencyCode = "USD",
                                startPeriodDate = startPeriodDate,
                                finishPeriodDate = finishPeriodDate,
                            ),
                        showTutorialOverride = false,
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun when_category_with_three_transactions_chip_clicked_then_bottom_sheet_shows_title() {
        setAnalyticsContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("CategoryAmount_Comida").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("CategoryAnalyticsTitle").assertExists()
    }

    @Test
    fun when_category_with_two_transactions_chip_clicked_then_bottom_sheet_shows_title() {
        setAnalyticsContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("CategoryAmount_Entretenimiento").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("CategoryAnalyticsTitle").assertExists()
    }

    @Test
    fun when_category_with_single_transaction_chip_clicked_then_bottom_sheet_shows_title() {
        setAnalyticsContent()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("CategoryAmount_Transporte").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("CategoryAnalyticsTitle").assertExists()
    }
}
