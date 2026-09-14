package com.serranoie.app.minus.presentation.ui.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
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

    private val today = LocalDate.of(2026, 1, 15)

    private fun populatedState() = SubscriptionsUiState(
        isLoading = false,
        dueSoon = listOf(
            UpcomingRecurrentItem(
                transaction = Transaction(
                    id = 1L,
                    amount = BigDecimal("16.99"),
                    comment = "Video streaming",
                    date = today.minusMonths(2).atStartOfDay(),
                    isRecurrent = true,
                    recurrentFrequency = RecurrentFrequency.MONTHLY,
                ),
                nextChargeDate = today.plusDays(2),
                isInCurrentPeriod = true,
            ),
            UpcomingRecurrentItem(
                transaction = Transaction(
                    id = 2L,
                    amount = BigDecimal("9.99"),
                    comment = "Weekly app",
                    date = today.minusDays(4).atStartOfDay(),
                    isRecurrent = true,
                    recurrentFrequency = RecurrentFrequency.WEEKLY,
                ),
                nextChargeDate = today.plusDays(3),
                isInCurrentPeriod = true,
            ),
        ),
        upcoming = listOf(
            UpcomingRecurrentItem(
                transaction = Transaction(
                    id = 3L,
                    amount = BigDecimal("49.99"),
                    comment = "Gym membership",
                    date = today.minusMonths(3).atStartOfDay(),
                    isRecurrent = true,
                    recurrentFrequency = RecurrentFrequency.MONTHLY,
                ),
                nextChargeDate = today.plusDays(18),
                isInCurrentPeriod = false,
            ),
        ),
        monthlyTotal = BigDecimal("76.98"),
        activeCount = 3,
        currencyCode = "USD",
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
