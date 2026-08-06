package com.serranoie.app.minus.presentation.ui.e2e.onboarding

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
import com.google.common.truth.Truth
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.onboarding.OnboardingScreen
import com.serranoie.app.minus.presentation.ui.onboarding.OnboardingUiIntent
import com.serranoie.app.minus.presentation.ui.onboarding.OnboardingViewModel
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.BottomSheetScrollState
import com.serranoie.app.minus.presentation.ui.theme.component.LocalBottomSheetScrollState
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class OnboardingScreenE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setOnboardingContent(
        onOnboardingCompleted: () -> Unit = {},
        viewModel: OnboardingViewModel = OnboardingViewModel(
            settingsRepository = mockk(relaxed = true),
        ),
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalBottomSheetScrollState provides BottomSheetScrollState(0.dp),
                LocalWindowInsets provides PaddingValues(0.dp),
            ) {
                MinusTheme {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onOnboardingCompleted = onOnboardingCompleted,
                    )
                }
            }
        }
    }

    private fun str(resId: Int): String = composeTestRule.activity.getString(resId)

    private fun continueButton() = composeTestRule
        .onAllNodes(hasText(str(R.string.onboarding_set_budget_button)) and hasClickAction())
        .onFirst()

    @Test
    fun when_onboarding_screen_is_rendered_then_welcome_title_is_visible() {
        setOnboardingContent()

        composeTestRule.onNodeWithText(str(R.string.onboarding_welcome_title)).assertIsDisplayed()
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_welcome_title_text_matches_resource() {
        setOnboardingContent()

        val expected = str(R.string.onboarding_welcome_title)
        composeTestRule.onNodeWithText(expected).assertTextEquals(expected)
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_intro_paragraph_is_visible() {
        setOnboardingContent()

        composeTestRule.onNodeWithText(str(R.string.onboarding_welcome_intro))
            .assertIsDisplayed()
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_intro_paragraph_text_matches_resource() {
        setOnboardingContent()

        val expected = str(R.string.onboarding_welcome_intro)
        composeTestRule.onNodeWithText(expected).assertTextEquals(expected)
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_all_six_step_titles_are_visible() {
        setOnboardingContent()

        composeTestRule.onNodeWithText(str(R.string.onboarding_step_1_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.onboarding_step_2_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.onboarding_step_3_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.onboarding_step_4_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.onboarding_step_5_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.onboarding_step_6_title)).assertIsDisplayed()
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_recurring_expenses_step_is_visible() {
        setOnboardingContent()

        composeTestRule.onNodeWithText(str(R.string.onboarding_step_3_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.onboarding_step_3_subtitle))
            .assertIsDisplayed()
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_calculate_on_the_fly_step_is_visible() {
        setOnboardingContent()

        composeTestRule.onNodeWithText(str(R.string.onboarding_step_4_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.onboarding_step_4_subtitle))
            .assertIsDisplayed()
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_analytics_step_is_visible() {
        setOnboardingContent()

        composeTestRule.onNodeWithText(str(R.string.onboarding_step_5_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.onboarding_step_5_subtitle))
            .assertIsDisplayed()
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_continue_button_is_visible() {
        setOnboardingContent()

        continueButton().assertIsDisplayed()
    }

    @Test
    fun when_user_taps_continue_button_then_on_onboarding_completed_fires() {
        var invoked = 0
        setOnboardingContent(onOnboardingCompleted = { invoked++ })

        continueButton().performClick()
        composeTestRule.waitForIdle()

        Truth.assertThat(invoked).isEqualTo(1)
    }

    @Test
    fun when_user_taps_continue_button_twice_then_on_onboarding_completed_fires_twice() {
        var invoked = 0
        setOnboardingContent(onOnboardingCompleted = { invoked++ })

        val button = continueButton()
        button.performClick()
        composeTestRule.waitForIdle()
        button.performClick()
        composeTestRule.waitForIdle()

        Truth.assertThat(invoked).isEqualTo(2)
    }

    @Test
    fun when_on_welcome_dismissed_intent_is_dispatched_then_on_onboarding_completed_fires() {
        val vm = OnboardingViewModel(
            settingsRepository = mockk(relaxed = true),
        )
        var invoked = 0
        setOnboardingContent(onOnboardingCompleted = { invoked++ }, viewModel = vm)

        vm.processIntent(OnboardingUiIntent.OnWelcomeDismissed)
        composeTestRule.waitForIdle()

        Truth.assertThat(invoked).isEqualTo(1)
    }
}
