package com.serranoie.app.minus.presentation.island

import android.content.Context
import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.PaidRecurrentOccurrence
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.RecurrentOccurrenceStatus
import com.serranoie.app.minus.domain.model.Transaction
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class IslandBudgetSnapshotTest {

    private val today = LocalDate.of(2099, 6, 18)
    private lateinit var savedLocale: Locale

    @Before
    fun setUp() {
        savedLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(savedLocale)
    }

    private fun weekly(id: Long, name: String, amount: String, firstCharge: LocalDate, isDeleted: Boolean = false) =
        Transaction(
            id = id,
            amount = BigDecimal(amount),
            comment = name,
            date = firstCharge.atTime(9, 0),
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.WEEKLY,
            isDeleted = isDeleted,
        )

    private val snapshot = IslandBudgetSnapshot(
        date = today,
        currency = "EUR",
        viewPeriod = BudgetPeriod.DAILY,
        splitRemaining = BigDecimal("24.50"),
        splitSpent = BigDecimal("15.50"),
        splitBudget = BigDecimal("40"),
        spendProgress = 0.39f,
        isOverBudget = false,
        isOverSplit = false,
        periodLeft = BigDecimal("612.40"),
        daysLeft = 12,
        creditOwed = BigDecimal.ZERO,
        nextCharge = IslandCharge("Netflix", BigDecimal("12.99"), today.plusDays(1)),
        startDate = LocalDate.of(2099, 6, 1),
        finishDate = LocalDate.of(2099, 6, 30),
    )

    @Test
    fun `a charge due today is the next charge`() {
        val gym = weekly(1, "Gym", "30", firstCharge = today.minusWeeks(2))

        assertThat(nextIslandCharge(listOf(gym), emptySet(), today))
            .isEqualTo(IslandCharge("Gym", BigDecimal("30"), today))
    }

    @Test
    fun `the earliest charge within tomorrow wins`() {
        val netflix = weekly(1, "Netflix", "12.99", firstCharge = today.plusDays(1).minusWeeks(1))
        val gym = weekly(2, "Gym", "30", firstCharge = today.minusWeeks(1))

        assertThat(nextIslandCharge(listOf(netflix, gym), emptySet(), today)?.name).isEqualTo("Gym")
    }

    @Test
    fun `a charge already paid or skipped today is not shown`() {
        val gym = weekly(1, "Gym", "30", firstCharge = today.minusWeeks(1))
        val netflix = weekly(2, "Netflix", "12.99", firstCharge = today.plusDays(1).minusWeeks(1))
        val resolved = setOf(PaidRecurrentOccurrence(1, today, RecurrentOccurrenceStatus.SKIPPED))

        assertThat(nextIslandCharge(listOf(gym, netflix), resolved, today)?.name).isEqualTo("Netflix")
    }

    @Test
    fun `charges further than tomorrow and deleted series are ignored`() {
        val later = weekly(1, "Rent", "800", firstCharge = today.plusDays(2).minusWeeks(1))
        val deleted = weekly(2, "Old gym", "30", firstCharge = today.minusWeeks(1), isDeleted = true)
        val oneOff = Transaction(id = 3, amount = BigDecimal("5"), date = today.atTime(10, 0))

        assertThat(nextIslandCharge(listOf(later, deleted, oneOff), emptySet(), today)).isNull()
    }

    @Test
    fun `overspend is what went past today's split`() {
        val over = snapshot.copy(splitSpent = BigDecimal("48.20"), splitRemaining = BigDecimal.ZERO, isOverSplit = true)

        assertThat(over.overspend()).isEqualTo(BigDecimal("8.20"))
        assertThat(snapshot.overspend()).isNull()
    }

    @Test
    fun `a snapshot saved before this update still reads, with empty new fields`() {
        val prefs = FakePrefs(
            "date" to today.toString(),
            "currency" to "EUR",
            "viewPeriod" to "DAILY",
            "splitRemaining" to "24.50",
            "periodLeft" to "612.40",
            "daysLeft" to 12,
            "totalBudget" to "1500",
            "settingsTotalBudget" to "1500",
            "creditOwed" to "0",
            "startDate" to "2099-06-01",
            "finishDate" to "2099-06-30",
        )

        val read = IslandBudgetStore.read(prefs.context())

        assertThat(read?.splitRemaining).isEqualTo(BigDecimal("24.50"))
        assertThat(read?.splitSpent).isEqualTo(BigDecimal.ZERO)
        assertThat(read?.splitBudget).isEqualTo(BigDecimal.ZERO)
        assertThat(read?.nextCharge).isNull()
    }

    @Test
    fun `the new fields survive a save and read`() {
        val prefs = FakePrefs()

        IslandBudgetStore.update(prefs.context(), snapshot)

        assertThat(IslandBudgetStore.read(prefs.context())).isEqualTo(snapshot)
    }

    private class FakePrefs(vararg initial: Pair<String, Any?>) : SharedPreferences {
        val values = mutableMapOf(*initial)

        fun context(): Context = mockk { every { getSharedPreferences(any(), any()) } returns this@FakePrefs }

        override fun getAll(): MutableMap<String, *> = values
        override fun getString(key: String, defValue: String?) = values[key] as String? ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?) = defValues
        override fun getInt(key: String, defValue: Int) = values[key] as Int? ?: defValue
        override fun getLong(key: String, defValue: Long) = values[key] as Long? ?: defValue
        override fun getFloat(key: String, defValue: Float) = values[key] as Float? ?: defValue
        override fun getBoolean(key: String, defValue: Boolean) = values[key] as Boolean? ?: defValue
        override fun contains(key: String) = key in values
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener) = Unit

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(key: String, value: String?) = apply { values[key] = value }
            override fun putStringSet(key: String, values: MutableSet<String>?) = this
            override fun putInt(key: String, value: Int) = apply { values[key] = value }
            override fun putLong(key: String, value: Long) = apply { values[key] = value }
            override fun putFloat(key: String, value: Float) = apply { values[key] = value }
            override fun putBoolean(key: String, value: Boolean) = apply { values[key] = value }
            override fun remove(key: String) = apply { values.remove(key) }
            override fun clear() = apply { values.clear() }
            override fun commit() = true
            override fun apply() = Unit
        }
    }
}
