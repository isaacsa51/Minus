package com.serranoie.app.minus.presentation.ui.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.subscriptions.ChargeStatus
import com.serranoie.app.minus.presentation.ui.subscriptions.DayCharge
import com.serranoie.app.minus.presentation.ui.subscriptions.Subscriptions
import com.serranoie.app.minus.presentation.ui.subscriptions.SubscriptionsUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class SubscriptionsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        maxPercentDifference = 10.0,
    )

    // Subscriptions renders relative-date text off the real clock (matching how
    // UpcomingRecurrentItemRow already behaves elsewhere), so fixture dates are
    // built from LocalDate.now() rather than a fixed calendar date.
    private val today: LocalDate get() = LocalDate.now()

    private fun populatedState() = SubscriptionsUiState(
        isLoading = false,
        dueSoon = listOf(
            UpcomingRecurrentItem(
                transaction = Transaction(
                    id = 1L,
                    amount = BigDecimal("16.99"),
                    comment = "Netflix",
                    date = today.minusMonths(2).atStartOfDay(),
                    isRecurrent = true,
                    recurrentFrequency = RecurrentFrequency.MONTHLY,
                ),
                nextChargeDate = today,
                isInCurrentPeriod = true,
            ),
            UpcomingRecurrentItem(
                transaction = Transaction(
                    id = 2L,
                    amount = BigDecimal("9.99"),
                    comment = "Spotify",
                    date = today.minusDays(4).atStartOfDay(),
                    isRecurrent = true,
                    recurrentFrequency = RecurrentFrequency.WEEKLY,
                ),
                nextChargeDate = today.plusDays(2),
                isInCurrentPeriod = true,
            ),
            UpcomingRecurrentItem(
                transaction = Transaction(
                    id = 3L,
                    amount = BigDecimal("7.99"),
                    comment = "Cloud storage",
                    date = today.minusDays(9).atStartOfDay(),
                    isRecurrent = true,
                    recurrentFrequency = RecurrentFrequency.BIWEEKLY,
                ),
                nextChargeDate = today.plusDays(5),
                isInCurrentPeriod = true,
            ),
        ),
        upcoming = listOf(
            UpcomingRecurrentItem(
                transaction = Transaction(
                    id = 4L,
                    amount = BigDecimal("49.99"),
                    comment = "Gym membership",
                    date = today.minusMonths(3).atStartOfDay(),
                    isRecurrent = true,
                    recurrentFrequency = RecurrentFrequency.MONTHLY,
                ),
                nextChargeDate = today.plusDays(12),
                isInCurrentPeriod = false,
            ),
            UpcomingRecurrentItem(
                transaction = Transaction(
                    id = 5L,
                    amount = BigDecimal("34.00"),
                    comment = "Phone plan",
                    date = today.minusMonths(4).atStartOfDay(),
                    isRecurrent = true,
                    recurrentFrequency = RecurrentFrequency.MONTHLY,
                ),
                nextChargeDate = today.plusDays(18),
                isInCurrentPeriod = false,
            ),
        ),
        monthlyTotal = BigDecimal("84.96"),
        activeCount = 5,
        currencyCode = "USD",
        daysUntilNextCharge = 0L,
        periodStart = today.withDayOfMonth(1),
        periodEnd = today.withDayOfMonth(today.lengthOfMonth()),
        chargesByDay = buildMap {
            put(
                today,
                listOf(DayCharge(Transaction(id = 1L, amount = BigDecimal("16.99"), comment = "Netflix", date = today.atStartOfDay()), ChargeStatus.PENDING)),
            )
            put(
                today.plusDays(2),
                listOf(DayCharge(Transaction(id = 2L, amount = BigDecimal("9.99"), comment = "Spotify", date = today.atStartOfDay()), ChargeStatus.PENDING)),
            )
            if (today.plusDays(5).month == today.month) {
                put(
                    today.plusDays(5),
                    listOf(
                        DayCharge(Transaction(id = 3L, amount = BigDecimal("7.99"), comment = "Cloud storage", date = today.atStartOfDay()), ChargeStatus.PAID),
                        DayCharge(Transaction(id = 4L, amount = BigDecimal("49.99"), comment = "Gym membership", date = today.atStartOfDay()), ChargeStatus.SKIPPED),
                        DayCharge(Transaction(id = 5L, amount = BigDecimal("34.00"), comment = "Phone plan", date = today.atStartOfDay()), ChargeStatus.PENDING),
                    ),
                )
            }
        },
        periodBudgetTotal = BigDecimal("300.00"),
        periodCommittedTotal = BigDecimal("84.96"),
        monthlyTotalByFrequency = mapOf(
            RecurrentFrequency.MONTHLY to BigDecimal("83.99"),
            RecurrentFrequency.WEEKLY to BigDecimal("43.30"),
            RecurrentFrequency.BIWEEKLY to BigDecimal("17.31"),
        ),
    )

    @Test
    fun subscriptionsEmptyState() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                Subscriptions(state = SubscriptionsUiState(isLoading = false))
            }
        }
    }

    @Test
    fun subscriptionsDueSoonAndUpcoming() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                Subscriptions(state = populatedState())
            }
        }
    }

    @Test
    fun subscriptionsDueSoonAndUpcoming_darkTheme() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme(darkTheme = true) {
                Subscriptions(state = populatedState())
            }
        }
    }
}
