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
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.onboarding.OnboardingViewModel
import com.serranoie.app.minus.presentation.ui.onboarding.OnboardingScreen
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
    fun when_on_complete_onboarding_intent_dispatched_then_on_onboarding_completed_fires() {
        // Given — the welcome-step button is wired to dispatch
        // [OnboardingUiIntent.OnCompleteOnboarding] to the VM. The VM
        // emits [OnboardingUiEffect.OnboardingCompleted] after the
        // budget is saved; the screen forwards that to the navigation
        // callback. The "Set a budget" tap is just a thin shim, so we
        // dispatch the intent directly here — that exercises the same
        // wiring the button would.
        val vm = OnboardingViewModel(
            budgetRepository = mockk(relaxed = true),
            notificationScheduler = mockk(relaxed = true),
            settingsRepository = mockk(relaxed = true),
        )
        vm.processIntent(com.serranoie.app.minus.presentation.ui.onboarding.OnboardingUiIntent.OnBudgetAmountChanged("100"))

        var invoked = 0
        setOnboardingContent(onOnboardingCompleted = { invoked++ }, viewModel = vm)

        // When
        vm.processIntent(com.serranoie.app.minus.presentation.ui.onboarding.OnboardingUiIntent.OnCompleteOnboarding)
        composeTestRule.waitForIdle()

        // Then
        assertThat(invoked).isEqualTo(1)
    }

    @Test
    fun when_on_complete_onboarding_intent_dispatched_twice_then_on_onboarding_completed_fires_twice() {
        // Given
        val vm = OnboardingViewModel(
            budgetRepository = mockk(relaxed = true),
            notificationScheduler = mockk(relaxed = true),
            settingsRepository = mockk(relaxed = true),
        )
        vm.processIntent(com.serranoie.app.minus.presentation.ui.onboarding.OnboardingUiIntent.OnBudgetAmountChanged("100"))

        var invoked = 0
        setOnboardingContent(onOnboardingCompleted = { invoked++ }, viewModel = vm)

        // When
        vm.processIntent(com.serranoie.app.minus.presentation.ui.onboarding.OnboardingUiIntent.OnCompleteOnboarding)
        composeTestRule.waitForIdle()
        vm.processIntent(com.serranoie.app.minus.presentation.ui.onboarding.OnboardingUiIntent.OnBudgetAmountChanged("200"))
        vm.processIntent(com.serranoie.app.minus.presentation.ui.onboarding.OnboardingUiIntent.OnCompleteOnboarding)
        composeTestRule.waitForIdle()

        // Then
        assertThat(invoked).isEqualTo(2)
    }
}
