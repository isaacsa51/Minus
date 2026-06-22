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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
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

    /**
     * Tests construct a [OnboardingViewModel] directly (mocking the
     * repositories) and pass it to the screen. This avoids the
     * `@AndroidEntryPoint` requirement on the host Activity, which
     * would otherwise break `hiltViewModel()` resolution.
     */
    private fun setOnboardingContent(
        onOnboardingCompleted: () -> Unit = {},
        viewModel: OnboardingViewModel = OnboardingViewModel(
            budgetRepository = mockk(relaxed = true),
            notificationScheduler = mockk(relaxed = true),
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

    // -------------------------------------------------------------------------
    // Hero
    // -------------------------------------------------------------------------

    @Test
    fun when_onboarding_screen_is_rendered_then_welcome_title_is_visible() {
        setOnboardingContent()

        composeTestRule.onNodeWithText(str(R.string.onboarding_welcome_title)).assertIsDisplayed()
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_welcome_subtitle_is_visible() {
        setOnboardingContent()

        composeTestRule.onNodeWithText(str(R.string.onboarding_welcome_subtitle))
            .assertIsDisplayed()
    }

    @Test
    fun when_onboarding_screen_is_rendered_then_welcome_subtitle_text_matches_resource() {
        setOnboardingContent()

        val expected = str(R.string.onboarding_welcome_subtitle)
        composeTestRule.onNodeWithText(expected).assertTextEquals(expected)
    }

    // -------------------------------------------------------------------------
    // Intro paragraph — explains what Minus is
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Feature grid — 6 steps covering what the user can do in Minus
    // -------------------------------------------------------------------------

    @Test
    fun when_onboarding_screen_is_rendered_then_all_six_step_titles_are_visible() {
        setOnboardingContent()

        // 6 steps: Set a budget, Track expenses, Recurring expenses,
        // Calculate on the fly, See your analytics, Spend wisely
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

    // -------------------------------------------------------------------------
    // CTA button — "Continue"
    // -------------------------------------------------------------------------

    @Test
    fun when_onboarding_screen_is_rendered_then_continue_button_is_visible() {
        setOnboardingContent()

        continueButton().performScrollTo().assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Effect wiring — OnWelcomeDismissed → OnboardingCompleted
    // -------------------------------------------------------------------------
    //
    // The welcome step's "Continue" button dispatches
    // [OnboardingUiIntent.OnWelcomeDismissed]. The VM marks the
    // onboarding flag as completed in settings and emits
    // [OnboardingUiEffect.OnboardingCompleted]; the screen forwards
    // that to the navigation callback. We exercise the same wiring
    // by dispatching the intent directly (and also by tapping the
    // button) so the test isn't sensitive to UI flakiness.

    @Test
    fun when_user_taps_continue_button_then_on_onboarding_completed_fires() {
        var invoked = 0
        setOnboardingContent(onOnboardingCompleted = { invoked++ })

        continueButton().performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertThat(invoked).isEqualTo(1)
    }

    @Test
    fun when_user_taps_continue_button_twice_then_on_onboarding_completed_fires_twice() {
        var invoked = 0
        setOnboardingContent(onOnboardingCompleted = { invoked++ })

        val button = continueButton()
        button.performScrollTo().performClick()
        composeTestRule.waitForIdle()
        button.performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertThat(invoked).isEqualTo(2)
    }

    @Test
    fun when_on_welcome_dismissed_intent_is_dispatched_then_on_onboarding_completed_fires() {
        val vm = OnboardingViewModel(
            budgetRepository = mockk(relaxed = true),
            notificationScheduler = mockk(relaxed = true),
            settingsRepository = mockk(relaxed = true),
        )
        var invoked = 0
        setOnboardingContent(onOnboardingCompleted = { invoked++ }, viewModel = vm)

        vm.processIntent(OnboardingUiIntent.OnWelcomeDismissed)
        composeTestRule.waitForIdle()

        assertThat(invoked).isEqualTo(1)
    }
}
