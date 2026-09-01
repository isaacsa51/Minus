package com.serranoie.app.minus.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.domain.model.SavingsPreferences
import com.serranoie.app.minus.domain.model.SavingsSplitPreset
import com.serranoie.app.minus.domain.model.ThemeMode
import com.serranoie.app.minus.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SettingsRepositoryImplTest {

    private class FakePreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences,
        ): Preferences = transform(state.value).also { state.value = it }
    }

    private lateinit var repo: SettingsRepositoryImpl

    @Before
    fun setUp() {
        repo = SettingsRepositoryImpl(FakePreferencesDataStore())
    }

    @Test
    fun `an empty store yields the documented default settings`() = runTest {
        val settings = repo.getSettings()

        assertThat(settings.onboardingCompleted).isFalse()
        assertThat(settings.themeMode).isEqualTo(ThemeMode.SYSTEM)
        assertThat(settings.language).isEqualTo("en")
        assertThat(settings.notificationHour).isEqualTo(UserSettings.DEFAULT_NOTIFICATION_HOUR)
        assertThat(settings.recurrentNotificationHour)
            .isEqualTo(UserSettings.DEFAULT_RECURRENT_NOTIFICATION_HOUR)
        assertThat(settings.isRoundedFontEnabled).isTrue()
        assertThat(settings.showPastTransactions).isTrue()
        assertThat(settings.currentPeriodId).isEqualTo(0L)
    }

    @Test
    fun `getPendingRollover on an empty store is zero with no strategy`() = runTest {
        assertThat(repo.getPendingRollover()).isEqualTo(BigDecimal.ZERO to null)
    }

    @Test
    fun `getRemainingFromLastPeriod on an empty store is zero`() = runTest {
        assertThat(repo.getRemainingFromLastPeriod()).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `theme mode round-trips`() = runTest {
        repo.setThemeMode(ThemeMode.NIGHT)

        assertThat(repo.getSettings().themeMode).isEqualTo(ThemeMode.NIGHT)
    }

    @Test
    fun `a corrupt stored theme mode falls back to SYSTEM`() = runTest {
        repo.setString("theme_mode", "NOT_A_REAL_MODE")

        assertThat(repo.getSettings().themeMode).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `a corrupt stored budget split view period reads back as null`() = runTest {
        repo.setString("budget_split_view_period", "NONSENSE")

        assertThat(repo.getSettings().budgetSplitViewPeriod).isNull()
    }

    @Test
    fun `budget split view period round-trips`() = runTest {
        repo.setBudgetSplitViewPeriod(BudgetPeriod.WEEKLY)

        assertThat(repo.getSettings().budgetSplitViewPeriod).isEqualTo(BudgetPeriod.WEEKLY)
    }

    @Test
    fun `pending rollover round-trips amount and strategy`() = runTest {
        repo.setPendingRollover(BigDecimal("123.45"), RemainingBudgetStrategy.ADD_TO_FIRST_DAY)

        assertThat(repo.getPendingRollover())
            .isEqualTo(BigDecimal("123.45") to RemainingBudgetStrategy.ADD_TO_FIRST_DAY)
    }

    @Test
    fun `a corrupt stored rollover strategy reads back as a null strategy`() = runTest {
        repo.setPendingRollover(BigDecimal("10.00"), RemainingBudgetStrategy.SPLIT_EQUALLY)
        repo.setString("pending_rollover_strategy", "GARBAGE")

        val (amount, strategy) = repo.getPendingRollover()
        assertThat(amount).isEqualTo(BigDecimal("10.00"))
        assertThat(strategy).isNull()
    }

    @Test
    fun `clearPendingRollover removes both keys`() = runTest {
        repo.setPendingRollover(BigDecimal("50.00"), RemainingBudgetStrategy.SPLIT_EQUALLY)

        repo.clearPendingRollover()

        assertThat(repo.getPendingRollover()).isEqualTo(BigDecimal.ZERO to null)
    }

    @Test
    fun `setEarlyFinishActive writes all three fields and clearEarlyFinish resets them`() = runTest {
        repo.setEarlyFinishActive(active = true, actualDate = 111L, originalEndDate = 222L)

        repo.getSettings().let {
            assertThat(it.earlyFinishActive).isTrue()
            assertThat(it.earlyFinishActualDate).isEqualTo(111L)
            assertThat(it.earlyFinishOriginalEndDate).isEqualTo(222L)
        }

        repo.clearEarlyFinish()

        repo.getSettings().let {
            assertThat(it.earlyFinishActive).isFalse()
            assertThat(it.earlyFinishActualDate).isEqualTo(0L)
            assertThat(it.earlyFinishOriginalEndDate).isEqualTo(0L)
        }
    }

    @Test
    fun `last period snapshot round-trips and clears`() = runTest {
        repo.persistLastPeriodSnapshot(periodEndDateMillis = 999L, remainingAmount = BigDecimal("42.50"))

        assertThat(repo.getLastPeriodEnd()).isEqualTo(999L)
        assertThat(repo.getRemainingFromLastPeriod()).isEqualTo(BigDecimal("42.50"))

        repo.clearLastPeriodSnapshot()

        assertThat(repo.getLastPeriodEnd()).isNull()
        assertThat(repo.getRemainingFromLastPeriod()).isEqualTo(BigDecimal.ZERO)
    }

    @Test
    fun `setCurrentPeriod is observable via the boundary flow and the id getter`() = runTest {
        repo.setCurrentPeriod(periodId = 77L, startedAt = 1_000L)

        assertThat(repo.getCurrentPeriodId()).isEqualTo(77L)
        assertThat(repo.observeCurrentPeriodBoundary().first()).isEqualTo(1_000L to 77L)
    }

    @Test
    fun `current period rollover round-trips amount and carry-forward`() = runTest {
        repo.setCurrentPeriodRollover(BigDecimal("15.00"), carryForward = true)

        assertThat(repo.observeCurrentPeriodRollover().first())
            .isEqualTo(BigDecimal("15.00") to true)
    }

    @Test
    fun `setBudgetEndDate with a value then null toggles the key`() = runTest {
        repo.setBudgetEndDate(123_456L)
        assertThat(repo.observeBudgetEndDate().first()).isEqualTo(123_456L)

        repo.setBudgetEndDate(null)
        assertThat(repo.observeBudgetEndDate().first()).isNull()
    }

    @Test
    fun `savings preferences round-trip, and null goal fields are removed`() = runTest {
        repo.setSavingsPreferences(
            SavingsPreferences(
                preset = SavingsSplitPreset.CUSTOM,
                needsPct = 60,
                wantsPct = 25,
                savingsPct = 15,
                savingsGoalAmount = BigDecimal("5000.00"),
                savingsGoalMonths = 10,
            )
        )

        repo.getSettings().savingsPreferences.let {
            assertThat(it.needsPct).isEqualTo(60)
            assertThat(it.wantsPct).isEqualTo(25)
            assertThat(it.savingsPct).isEqualTo(15)
            assertThat(it.savingsGoalAmount).isEqualTo(BigDecimal("5000.00"))
            assertThat(it.savingsGoalMonths).isEqualTo(10)
        }

        repo.setSavingsPreferences(
            SavingsPreferences(
                preset = SavingsSplitPreset.CUSTOM,
                needsPct = 60,
                wantsPct = 25,
                savingsPct = 15,
                savingsGoalAmount = null,
                savingsGoalMonths = null,
            )
        )

        repo.getSettings().savingsPreferences.let {
            assertThat(it.savingsGoalAmount).isNull()
            assertThat(it.savingsGoalMonths).isNull()
        }
    }

    @Test
    fun `arbitrary string keys round-trip`() = runTest {
        assertThat(repo.getString("some_key")).isNull()

        repo.setString("some_key", "some_value")

        assertThat(repo.getString("some_key")).isEqualTo("some_value")
    }

    @Test
    fun `last seen version code round-trips and resets`() = runTest {
        assertThat(repo.getLastSeenVersionCode()).isNull()

        repo.setLastSeenVersionCode(10_500L)
        assertThat(repo.getLastSeenVersionCode()).isEqualTo(10_500L)

        repo.resetLastSeenVersionCode()
        assertThat(repo.getLastSeenVersionCode()).isNull()
    }

    @Test
    fun `resetTutorials clears the tutorial completion flags`() = runTest {
        repo.setTutorialBoxCompleted(true)
        repo.setAnalyticsTutorialCompleted(true)

        repo.resetTutorials()

        repo.getSettings().let {
            assertThat(it.tutorialBoxCompleted).isFalse()
            assertThat(it.analyticsTutorialCompleted).isFalse()
        }
    }

    @Test
    fun `observeSettings reflects a write made after the first read`() = runTest {
        assertThat(repo.observeSettings().first().onboardingCompleted).isFalse()

        repo.setOnboardingCompleted(true)

        assertThat(repo.observeSettings().first().onboardingCompleted).isTrue()
    }
}
