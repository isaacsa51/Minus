package com.serranoie.app.minus.presentation.ui.onboarding

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = OnboardingViewModel(
        settingsRepository = settingsRepository,
    )

    private suspend fun <T> ReceiveTurbine<T>.awaitCondition(predicate: (T) -> Boolean): T {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    @Test
    fun `when_viewmodel_is_created_then_state_has_defaults`() = runTest {
        val viewModel = newViewModel()
        viewModel.uiState.test {
            val state = awaitCondition { !it.isCompleted }
            assertThat(state.isCompleted).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when_welcome_dismissed_is_called_then_settings_marked_completed_and_effect_emitted`() = runTest {
        coEvery { settingsRepository.setOnboardingCompleted(any()) } returns Unit
        val viewModel = newViewModel()

        viewModel.uiState.test {
            val uiTurbine = this
            uiTurbine.awaitCondition { !it.isCompleted }
            
            viewModel.effects.test {
                val effectTurbine = this
                viewModel.processIntent(OnboardingUiIntent.OnWelcomeDismissed)
                
                uiTurbine.awaitCondition { it.isCompleted }
                coVerify { settingsRepository.setOnboardingCompleted(true) }

                val effect = effectTurbine.awaitItem()
                assertThat(effect).isEqualTo(OnboardingUiEffect.OnboardingCompleted)
                effectTurbine.cancelAndIgnoreRemainingEvents()
            }
            uiTurbine.cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when_welcome_dismissed_fails_with_repo_exception_then_onboarding_failed_effect_is_emitted`() = runTest {
        coEvery { settingsRepository.setOnboardingCompleted(any()) } throws RuntimeException("datastore locked")
        val viewModel = newViewModel()

        viewModel.effects.test {
            viewModel.processIntent(OnboardingUiIntent.OnWelcomeDismissed)

            val effect = awaitItem()
            assertThat(effect).isInstanceOf(OnboardingUiEffect.OnboardingFailed::class.java)
            effect as OnboardingUiEffect.OnboardingFailed
            assertThat(effect.message).isEqualTo("datastore locked")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
