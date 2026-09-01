package com.serranoie.app.minus.presentation.ui.settings

import android.app.AlarmManager
import android.content.Context
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.ContrastMode
import com.serranoie.app.minus.domain.model.PeriodMappingMode
import com.serranoie.app.minus.domain.model.ThemeMode
import com.serranoie.app.minus.domain.model.TypographyMode
import com.serranoie.app.minus.domain.model.UserSettings
import com.serranoie.app.minus.domain.usecase.UpdatePeriodEndNotificationTimeUseCase
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.util.CensorManager
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val context: Context = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val budgetRepository: BudgetRepository = mockk(relaxed = true)
    private val updateNotificationTimeUseCase: UpdatePeriodEndNotificationTimeUseCase = mockk(relaxed = true)
    private val censorManager: CensorManager = mockk()
    private val censored = MutableStateFlow(false)
    private val alarmManager: AlarmManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { censorManager.isCensored } returns censored
        every { censorManager.setCensored(any()) } just Runs
        every { settingsRepository.observeSettings() } returns flowOf(UserSettings.DEFAULT)
        every { budgetRepository.getBudgetSettings() } returns flowOf(null)
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        every { alarmManager.canScheduleExactAlarms() } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = SettingsViewModel(
        context = context,
        settingsRepository = settingsRepository,
        budgetRepository = budgetRepository,
        updateNotificationTimeUseCase = updateNotificationTimeUseCase,
        censorManager = censorManager,
    )

    private fun budgetSettings(cutoff: Int? = null) = BudgetSettings(
        totalBudget = BigDecimal("1000.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = java.time.LocalDate.of(2026, 1, 1),
        creditCardCutoffDay = cutoff,
    )

    private suspend fun <T> ReceiveTurbine<T>.awaitCondition(predicate: (T) -> Boolean): T {
        while (true) {
            val item = awaitItem()
            if (predicate(item)) return item
        }
    }

    @Test
    fun `theme mode is mapped to a display string`() = runTest {
        every { settingsRepository.observeSettings() } returns
            flowOf(UserSettings.DEFAULT.copy(themeMode = ThemeMode.NIGHT))

        newViewModel().uiState.test {
            assertThat(awaitCondition { it.currentTheme == "Dark" }.currentTheme).isEqualTo("Dark")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `contrast and typography modes are mapped to display strings`() = runTest {
        every { settingsRepository.observeSettings() } returns flowOf(
            UserSettings.DEFAULT.copy(
                contrastMode = ContrastMode.HIGH,
                typographyMode = TypographyMode.CONDENSED,
            )
        )

        newViewModel().uiState.test {
            val s = awaitCondition { it.currentContrast == "High" && it.currentTypography == "Condensed" }
            assertThat(s.currentContrast).isEqualTo("High")
            assertThat(s.currentTypography).isEqualTo("Condensed")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the credit card cutoff day comes from the active budget settings`() = runTest {
        every { budgetRepository.getBudgetSettings() } returns flowOf(budgetSettings(cutoff = 7))

        newViewModel().uiState.test {
            assertThat(awaitCondition { it.creditCardCutoffDay == 7 }.creditCardCutoffDay).isEqualTo(7)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the censored flag is reflected from the censor manager`() = runTest {
        censored.value = true

        newViewModel().uiState.test {
            assertThat(awaitCondition { it.isCensored }.isCensored).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onThemeChange maps the label to the enum and persists it`() = runTest {
        newViewModel().onThemeChange("Dark")
        coVerify { settingsRepository.setThemeMode(ThemeMode.NIGHT) }

        newViewModel().onThemeChange("Light")
        coVerify { settingsRepository.setThemeMode(ThemeMode.LIGHT) }

        newViewModel().onThemeChange("something else")
        coVerify { settingsRepository.setThemeMode(ThemeMode.SYSTEM) }
    }

    @Test
    fun `onContrastChange persists the mapped contrast mode`() = runTest {
        newViewModel().onContrastChange("Medium")
        coVerify { settingsRepository.setContrastMode(ContrastMode.MEDIUM) }
    }

    @Test
    fun `onMaterialYouToggle flips the current value and persists it`() = runTest {
        newViewModel().onMaterialYouToggle()
        coVerify { settingsRepository.setDynamicColorEnabled(true) }
    }

    @Test
    fun `onRoundedFontToggle flips the current value and persists it`() = runTest {
        newViewModel().onRoundedFontToggle()
        coVerify { settingsRepository.setRoundedFontEnabled(false) }
    }

    @Test
    fun `onAmoledToggle flips the current value and persists it`() = runTest {
        newViewModel().onAmoledToggle()
        coVerify { settingsRepository.setAmoledEnabled(true) }
    }

    @Test
    fun `onShowPastTransactionsToggle flips the current value and persists it`() = runTest {
        newViewModel().onShowPastTransactionsToggle()
        coVerify { settingsRepository.setShowPastTransactions(false) }
    }

    @Test
    fun `onCensorModeToggle flips the censor manager`() = runTest {
        newViewModel().onCensorModeToggle()
        coVerify { censorManager.setCensored(true) }
    }

    @Test
    fun `onNotificationTimeChange persists the time and reschedules`() = runTest {
        newViewModel().onNotificationTimeChange(7, 30)

        coVerify { settingsRepository.setNotificationTime(7, 30) }
        coVerify { updateNotificationTimeUseCase.invoke(7, 30) }
    }

    @Test
    fun `onRecurrentNotificationTimeChange persists the time and reschedules the recurrent job`() = runTest {
        newViewModel().onRecurrentNotificationTimeChange(6, 15)

        coVerify { settingsRepository.setRecurrentNotificationTime(6, 15) }
        coVerify { updateNotificationTimeUseCase.updateRecurrentNotificationTime(6, 15) }
    }

    @Test
    fun `onCutoffDayChange writes the day onto the existing budget settings`() = runTest {
        io.mockk.coEvery { budgetRepository.getBudgetSettingsSync() } returns budgetSettings()

        newViewModel().onCutoffDayChange(12)

        coVerify { budgetRepository.saveBudgetSettings(match { it.creditCardCutoffDay == 12 }) }
    }

    @Test
    fun `onCutoffDayChange does nothing when there is no budget yet`() = runTest {
        io.mockk.coEvery { budgetRepository.getBudgetSettingsSync() } returns null

        newViewModel().onCutoffDayChange(12)

        coVerify(exactly = 0) { budgetRepository.saveBudgetSettings(any()) }
    }

    @Test
    fun `onPeriodMappingModeChange persists the mode`() = runTest {
        newViewModel().onPeriodMappingModeChange(PeriodMappingMode.CALENDAR_BUCKET)
        coVerify { settingsRepository.setPeriodMappingMode(PeriodMappingMode.CALENDAR_BUCKET) }
    }

    @Test
    fun `onRecurrentPaymentsViewModeChange persists the mode`() = runTest {
        newViewModel().onRecurrentPaymentsViewModeChange(RecurrentPaymentsViewMode.HORIZONTAL_LIST)
        coVerify { settingsRepository.setRecurrentPaymentsViewMode(RecurrentPaymentsViewMode.HORIZONTAL_LIST) }
    }

    @Test
    fun `onResetTutorial delegates to the repository`() = runTest {
        newViewModel().onResetTutorial()
        coVerify { settingsRepository.resetTutorials() }
    }

    @Test
    fun `onBugReportClick emits the bug report navigation effect and consumeEffect clears it`() {
        val vm = newViewModel()

        vm.onBugReportClick()
        assertThat(vm.effects.value).isEqualTo(SettingsUiEffect.NavigateToBugReport)

        vm.consumeEffect()
        assertThat(vm.effects.value).isNull()
    }

    @Test
    fun `onBack emits the back navigation effect`() {
        val vm = newViewModel()

        vm.onBack()

        assertThat(vm.effects.value).isEqualTo(SettingsUiEffect.NavigateBack)
    }
}
