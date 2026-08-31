package com.serranoie.app.minus.presentation.ui.e2e.analytics

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.analytics.Analytics
import com.serranoie.app.minus.presentation.ui.analytics.AnalyticsState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Date

class AnalyticsTutorialE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setAnalyticsContent(hasSpends: Boolean) {
        val spends = if (hasSpends) {
            listOf(
                Transaction(
                    id = 1L,
                    amount = BigDecimal("50.00"),
                    comment = "Test",
                    date = LocalDateTime.now()
                )
            )
        } else emptyList()

        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.setContent {
            MinusTheme {
                CompositionLocalProvider(LocalWindowInsets provides PaddingValues(0.dp)) {
                    Analytics(
                        state = AnalyticsState(
                            transactions = spends,
                            spends = spends,
                            wholeBudget = BigDecimal("1000.00"),
                            isLoading = false,
                            startPeriodDate = Date(),
                            finishPeriodDate = Date()
                        ),
                        showTutorialOverride = true
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun awaitTutorialStep(resId: Int, timeoutMillis: Long = 5_000L) {
        val text = composeTestRule.activity.getString(resId)
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() &&
                runCatching { composeTestRule.onNodeWithText(text).assertIsDisplayed() }.isSuccess
        }
        composeTestRule.onNodeWithText(text).assertIsDisplayed()
    }

    @Test
    fun when_no_spends_tutorial_shows_header_step_first() {
        setAnalyticsContent(hasSpends = false)

        val title = composeTestRule.activity.getString(R.string.analytics_tutorial_header_title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()

        composeTestRule.onNodeWithText(title).performClick()

        awaitTutorialStep(R.string.analytics_tutorial_budget_title)
    }

    @Test
    fun when_has_spends_tutorial_shows_heatmap_step_first() {
        setAnalyticsContent(hasSpends = true)

        val title = composeTestRule.activity.getString(R.string.analytics_tutorial_heatmap_title)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()

        composeTestRule.onNodeWithText(title).performClick()

        awaitTutorialStep(R.string.analytics_tutorial_minmax_title)
    }
}
