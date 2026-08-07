package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.serranoie.app.minus.presentation.ui.analytics.AnalyticsState
import com.serranoie.app.minus.presentation.ui.analytics.GraphGranularity
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class BudgetGraphUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun fixedDate(year: Int, month: Int, day: Int): Date {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    @Test
    fun granularityToggle_updatesSelection() {
        var selectedGranularity = GraphGranularity.DAYS
        
        composeTestRule.setContent {
            MinusTheme {
                BudgetGraph(
                    state = AnalyticsState(
                        graphGranularity = selectedGranularity,
                        startPeriodDate = fixedDate(2026, 8, 1),
                        finishPeriodDate = fixedDate(2026, 8, 31),
                        currencyCode = "USD"
                    ),
                    onGranularityChanged = { selectedGranularity = it }
                )
            }
        }

        composeTestRule.onNodeWithTag(budgetGraphGranularityToggleTag(GraphGranularity.DAYS))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(budgetGraphGranularityToggleTag(GraphGranularity.WEEK))
            .performClick()

        composeTestRule.onNodeWithTag(budgetGraphGranularityToggleTag(GraphGranularity.WEEK))
            .assertIsDisplayed()
    }

    @Test
    fun navigationArrows_visible_whenMultipleWindows() {
        composeTestRule.setContent {
            MinusTheme {
                BudgetGraph(
                    state = AnalyticsState(
                        graphGranularity = GraphGranularity.DAYS, // 1 day steps
                        startPeriodDate = fixedDate(2026, 8, 1),
                        finishPeriodDate = fixedDate(2026, 8, 10), // 10 days > 7 day window
                        currencyCode = "USD"
                    ),
                    onGranularityChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(BUDGET_GRAPH_NEXT_PAGE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(BUDGET_GRAPH_WINDOW_LABEL_TAG).assertIsDisplayed()
    }

    @Test
    fun navigationArrows_hidden_whenSingleWindow() {
        composeTestRule.setContent {
            MinusTheme {
                BudgetGraph(
                    state = AnalyticsState(
                        graphGranularity = GraphGranularity.TOTAL,
                        startPeriodDate = fixedDate(2026, 8, 1),
                        finishPeriodDate = fixedDate(2026, 8, 5),
                        currencyCode = "USD"
                    ),
                    onGranularityChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(BUDGET_GRAPH_NEXT_PAGE_TAG).assertDoesNotExist()
    }
}
