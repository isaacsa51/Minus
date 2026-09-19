package com.serranoie.app.minus.presentation.notification

import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.data.repository.BudgetRepository
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import io.mockk.mockk
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class NotificationSchedulerTest {

    private val scheduler = NotificationScheduler(
        context = mockk(relaxed = true),
        budgetRepository = mockk<BudgetRepository>(relaxed = true),
        settingsRepository = mockk<SettingsRepository>(relaxed = true),
    )

    private fun recurringTx(
        date: LocalDateTime?,
        frequency: RecurrentFrequency?,
        endDate: LocalDate? = null,
        subscriptionDay: Int? = null,
    ) = Transaction.create(
        amount = BigDecimal("9.99"),
        date = date,
        isRecurrent = true,
        recurrentFrequency = frequency,
        recurrentEndDate = endDate?.atStartOfDay(),
        subscriptionDay = subscriptionDay,
    )

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test
    fun `stepped occurrence is the start date when today is on or before it`() {
        assertThat(scheduler.nextSteppedOccurrence(d(2026, 3, 10), d(2026, 3, 10), 7))
            .isEqualTo(d(2026, 3, 10))
        assertThat(scheduler.nextSteppedOccurrence(d(2026, 3, 10), d(2026, 3, 5), 7))
            .isEqualTo(d(2026, 3, 10))
    }

    @Test
    fun `weekly stepped occurrence lands on the next seven-day multiple`() {
        assertThat(scheduler.nextSteppedOccurrence(d(2026, 3, 10), d(2026, 3, 12), 7))
            .isEqualTo(d(2026, 3, 17))
        assertThat(scheduler.nextSteppedOccurrence(d(2026, 3, 10), d(2026, 3, 17), 7))
            .isEqualTo(d(2026, 3, 17))
        assertThat(scheduler.nextSteppedOccurrence(d(2026, 3, 10), d(2026, 3, 18), 7))
            .isEqualTo(d(2026, 3, 24))
    }

    @Test
    fun `biweekly stepped occurrence lands on the next fourteen-day multiple`() {
        assertThat(scheduler.nextSteppedOccurrence(d(2026, 3, 10), d(2026, 3, 24), 14))
            .isEqualTo(d(2026, 3, 24))
        assertThat(scheduler.nextSteppedOccurrence(d(2026, 3, 10), d(2026, 3, 25), 14))
            .isEqualTo(d(2026, 4, 7))
    }

    @Test
    fun `monthly occurrence is this month when the billing day is still ahead`() {
        assertThat(scheduler.nextMonthlyOccurrence(d(2026, 1, 1), d(2026, 3, 5), 15))
            .isEqualTo(d(2026, 3, 15))
    }

    @Test
    fun `monthly occurrence rolls to next month when the billing day has passed`() {
        assertThat(scheduler.nextMonthlyOccurrence(d(2026, 1, 1), d(2026, 3, 20), 15))
            .isEqualTo(d(2026, 4, 15))
    }

    @Test
    fun `monthly occurrence is today when the billing day is today`() {
        assertThat(scheduler.nextMonthlyOccurrence(d(2026, 1, 1), d(2026, 3, 15), 15))
            .isEqualTo(d(2026, 3, 15))
    }

    @Test
    fun `monthly billing day is clamped to the length of the target month`() {
        assertThat(scheduler.nextMonthlyOccurrence(d(2026, 1, 1), d(2026, 2, 10), 31))
            .isEqualTo(d(2026, 2, 28)) // 2026 is not a leap year
        assertThat(scheduler.nextMonthlyOccurrence(d(2026, 1, 1), d(2026, 2, 1), 30))
            .isEqualTo(d(2026, 2, 28))
    }

    @Test
    fun `monthly occurrence never precedes the recurrence start date`() {
        assertThat(scheduler.nextMonthlyOccurrence(d(2026, 6, 1), d(2026, 3, 5), 15))
            .isEqualTo(d(2026, 6, 1))
    }

    @Test
    fun `monthly billing day on the last day of a 31-day month resolves to that day`() {
        assertThat(scheduler.nextMonthlyOccurrence(d(2026, 1, 1), d(2026, 1, 31), 31))
            .isEqualTo(d(2026, 1, 31))
    }

    private val notifyAt9 = 9 to 0

    @Test
    fun `no occurrence for a transaction without a date`() {
        val result = scheduler.nextOccurrenceDateTime(
            recurringTx(date = null, frequency = RecurrentFrequency.WEEKLY),
            LocalDateTime.of(2026, 3, 9, 12, 0),
            notifyAt9,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `no occurrence for a transaction without a recurrence frequency`() {
        val result = scheduler.nextOccurrenceDateTime(
            recurringTx(date = LocalDateTime.of(2026, 3, 10, 8, 0), frequency = null),
            LocalDateTime.of(2026, 3, 9, 12, 0),
            notifyAt9,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `weekly notification fires on the first future occurrence at the configured time`() {
        val result = scheduler.nextOccurrenceDateTime(
            recurringTx(LocalDateTime.of(2026, 3, 10, 8, 0), RecurrentFrequency.WEEKLY),
            now = LocalDateTime.of(2026, 3, 9, 12, 0),
            notificationTime = notifyAt9,
        )
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 3, 10, 9, 0))
    }

    @Test
    fun `an occurrence whose notification time has already passed is skipped to the next one`() {
        val result = scheduler.nextOccurrenceDateTime(
            recurringTx(LocalDateTime.of(2026, 3, 10, 8, 0), RecurrentFrequency.WEEKLY),
            now = LocalDateTime.of(2026, 3, 10, 10, 0), // already past 09:00 today
            notificationTime = notifyAt9,
        )
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 3, 17, 9, 0))
    }

    @Test
    fun `occurrences already marked paid are skipped`() {
        val result = scheduler.nextOccurrenceDateTime(
            recurringTx(LocalDateTime.of(2026, 3, 10, 8, 0), RecurrentFrequency.WEEKLY),
            now = LocalDateTime.of(2026, 3, 9, 12, 0),
            notificationTime = notifyAt9,
            paidDates = setOf(d(2026, 3, 10)),
        )
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 3, 17, 9, 0))
    }

    @Test
    fun `no occurrence once the next one would fall after the recurrence end date`() {
        val result = scheduler.nextOccurrenceDateTime(
            recurringTx(
                date = LocalDateTime.of(2026, 3, 10, 8, 0),
                frequency = RecurrentFrequency.WEEKLY,
                endDate = d(2026, 3, 12),
            ),
            now = LocalDateTime.of(2026, 3, 11, 12, 0),
            notificationTime = notifyAt9,
        )
        assertThat(result).isNull()
    }

    @Test
    fun `monthly notification uses the subscription day, not the original transaction day`() {
        val result = scheduler.nextOccurrenceDateTime(
            recurringTx(
                date = LocalDateTime.of(2026, 1, 5, 8, 0),
                frequency = RecurrentFrequency.MONTHLY,
                subscriptionDay = 20,
            ),
            now = LocalDateTime.of(2026, 3, 3, 12, 0),
            notificationTime = notifyAt9,
        )
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 3, 20, 9, 0))
    }

    @Test
    fun `lead days move the trigger ahead of the occurrence and roll past a lead already gone`() {
        val weekly = recurringTx(date = LocalDateTime.of(2026, 3, 10, 8, 0), frequency = RecurrentFrequency.WEEKLY)

        val ahead = scheduler.nextOccurrenceDateTime(
            weekly, now = LocalDateTime.of(2026, 3, 5, 12, 0), notificationTime = notifyAt9, leadDays = 3,
        )
        assertThat(ahead).isEqualTo(LocalDateTime.of(2026, 3, 7, 9, 0))

        val rolled = scheduler.nextOccurrenceDateTime(
            weekly, now = LocalDateTime.of(2026, 3, 8, 12, 0), notificationTime = notifyAt9, leadDays = 3,
        )
        assertThat(rolled).isEqualTo(LocalDateTime.of(2026, 3, 14, 9, 0))

        val resolved = scheduler.nextOccurrenceDateTime(
            weekly, now = LocalDateTime.of(2026, 3, 5, 12, 0), notificationTime = notifyAt9,
            paidDates = setOf(d(2026, 3, 10)), leadDays = 3,
        )
        assertThat(resolved).isEqualTo(LocalDateTime.of(2026, 3, 14, 9, 0))
    }

    @Test
    fun `credit cutoff reminder fires three days before the due date of the last cutoff`() {
        // cutoff 15 Sep -> due 5 Oct (Mon) -> reminder 2 Oct
        val result = scheduler.nextCreditCutoffReminderDateTime(
            cutoffDay = 15,
            now = LocalDateTime.of(2026, 9, 18, 12, 0),
            notificationTime = notifyAt9,
        )
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 10, 2, 9, 0))
    }

    @Test
    fun `credit cutoff reminder skips a due date that already passed and rolls to the next cycle`() {
        // cutoff 15 Aug -> due 4 Sep is already past on 10 Sep; next cutoff 15 Sep -> due 5 Oct -> reminder 2 Oct
        val result = scheduler.nextCreditCutoffReminderDateTime(
            cutoffDay = 15,
            now = LocalDateTime.of(2026, 9, 10, 12, 0),
            notificationTime = notifyAt9,
        )
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 10, 2, 9, 0))
    }

    @Test
    fun `credit cutoff reminder moves to the next cycle once today's slot has passed`() {
        val result = scheduler.nextCreditCutoffReminderDateTime(
            cutoffDay = 15,
            now = LocalDateTime.of(2026, 10, 2, 9, 30),
            notificationTime = notifyAt9,
        )
        // cutoff 15 Oct -> due 4 Nov (Wed) -> reminder 1 Nov
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 11, 1, 9, 0))
    }
}
