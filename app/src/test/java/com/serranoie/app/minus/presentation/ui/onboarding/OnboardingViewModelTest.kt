package com.serranoie.app.minus.presentation.ui.onboarding

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = OnboardingViewModel(
        settingsRepository = settingsRepository,
    )

    @Test
    fun `when_viewmodel_is_created_then_state_has_defaults`() {
        val viewModel = newViewModel()

        val state = viewModel.uiState.value
        assertThat(state.isCompleted).isFalse()
    }

    @Test
    fun `when_welcome_dismissed_is_called_then_settings_marked_completed_and_effect_emitted`() = runTest {
        coEvery { settingsRepository.setOnboardingCompleted(any()) } returns Unit
        val viewModel = newViewModel()

        viewModel.effects.test {
            viewModel.processIntent(OnboardingUiIntent.OnWelcomeDismissed)
            advanceUntilIdle()
            runCurrent()

            coVerify { settingsRepository.setOnboardingCompleted(true) }
            assertThat(viewModel.uiState.value.isCompleted).isTrue()

            val effect = awaitItem()
            assertThat(effect).isEqualTo(OnboardingUiEffect.OnboardingCompleted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when_welcome_dismissed_fails_with_repo_exception_then_onboarding_failed_effect_is_emitted`() = runTest {
        coEvery { settingsRepository.setOnboardingCompleted(any()) } throws RuntimeException("datastore locked")
        val viewModel = newViewModel()

        viewModel.effects.test {
            viewModel.processIntent(OnboardingUiIntent.OnWelcomeDismissed)
            advanceUntilIdle()
            runCurrent()

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(OnboardingUiEffect.OnboardingFailed::class.java)
            effect as OnboardingUiEffect.OnboardingFailed
            assertThat(effect.message).isEqualTo("datastore locked")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
