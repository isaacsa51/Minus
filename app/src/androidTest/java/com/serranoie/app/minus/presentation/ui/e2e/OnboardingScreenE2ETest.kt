package com.serranoie.app.minus.presentation.ui.e2e

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.onboarding.OnboardingScreen
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.BottomSheetScrollState
import com.serranoie.app.minus.presentation.ui.theme.component.LocalBottomSheetScrollState
import org.junit.Rule
import org.junit.Test

class OnboardingScreenE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setOnboardingContent(
        onSetBudget: () -> Unit = {},
        onClose: () -> Unit = {},
        onOnboardingComplete: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalBottomSheetScrollState provides BottomSheetScrollState(0.dp),
                LocalWindowInsets provides PaddingValues(0.dp),
            ) {
                MinusTheme {
                    OnboardingScreen(
                        onSetBudget = onSetBudget,
                        onClose = onClose,
                        onOnboardingComplete = onOnboardingComplete,
                    )
                }
            }
        }
    }

    private fun str(resId: Int): String = composeTestRule.activity.getString(resId)

    private fun setBudgetButton() = composeTestRule
        .onAllNodes(hasText(str(R.string.onboarding_set_budget_button)) and hasClickAction())
        .onFirst()

    @Test
    fun when_onboarding_screen_is_rendered_then_welcome_title_is_visible() {
        // Given / When
        setOnboardingContent()

        // Then
        composeTestRule.onNodeWithText(str(R.string.onboarding_welcome_title)).assertIsDisplayed()
    }


    @Test
    fun when_onboarding_screen_is_rendered_then_welcome_subtitle_is_visible() {
        // Given / When
        setOnboardingContent()

        // Then
        composeTestRule.onNodeWithText(str(R.string.onboarding_welcome_subtitle))
            .assertIsDisplayed()
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_all_three_step_rows_are_visible() {
        // Given / When
        setOnboardingContent()

        // Then
        composeTestRule.onNodeWithText(str(R.string.onboarding_step_1_subtitle)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.onboarding_step_2_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.onboarding_step_3_title)).assertIsDisplayed()
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_set_budget_button_is_visible() {
        // Given / When
        setOnboardingContent()

        // Then
        setBudgetButton().assertIsDisplayed()
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_welcome_subtitle_text_matches_resource() {
        // Given / When
        setOnboardingContent()

        // Then
        val expected = str(R.string.onboarding_welcome_subtitle)
        composeTestRule.onNodeWithText(expected).assertTextEquals(expected)
    }

    @Test
    fun when_user_taps_set_budget_button_then_on_set_budget_fires() {
        // Given
        var invoked = 0
        setOnboardingContent(onSetBudget = { invoked++ })

        // When
        setBudgetButton().performClick()
        composeTestRule.waitForIdle()

        // Then
        assertThat(invoked).isEqualTo(1)
    }

    @Test
    fun when_user_taps_set_budget_button_then_on_close_and_on_onboarding_complete_do_not_fire() {
        // Given
        var closeCount = 0
        var completeCount = 0
        setOnboardingContent(
            onSetBudget = {},
            onClose = { closeCount++ },
            onOnboardingComplete = { completeCount++ },
        )

        // When
        setBudgetButton().performClick()
        composeTestRule.waitForIdle()

        // Then
        assertThat(closeCount).isEqualTo(0)
        assertThat(completeCount).isEqualTo(0)
    }

    @Test
    fun when_user_taps_set_budget_button_twice_then_on_set_budget_fires_twice() {
        // Given
        var invoked = 0
        setOnboardingContent(onSetBudget = { invoked++ })

        // When
        val button = setBudgetButton()
        button.performClick()
        composeTestRule.waitForIdle()
        button.performClick()
        composeTestRule.waitForIdle()

        // Then
        assertThat(invoked).isEqualTo(2)
    }
}
