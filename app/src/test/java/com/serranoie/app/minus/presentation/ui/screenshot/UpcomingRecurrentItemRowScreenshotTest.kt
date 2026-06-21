package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.material3.ExperimentalMaterial3Api
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItemRow
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

class UpcomingRecurrentItemRowScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun upcomingRecurrentItemFirst() {
        Locale.setDefault(Locale.US)
        // Fixed far-future dates so the snapshot is deterministic across runs.
        // The component derives "in X weeks" copy from `LocalDate.now()` —
        // if we used `now().plusYears(...)` the X count would shift each time
        // the golden is regenerated, producing 1-pixel text differences that
        // are not real regressions.
        val nextCharge = LocalDate.of(2099, 6, 18)
        paparazzi.snapshot {
            MinusTheme {
                UpcomingRecurrentItemRow(
                    item = UpcomingRecurrentItem(
                        transaction = Transaction(
                            id = 1L,
                            amount = BigDecimal("16.99"),
                            comment = "Netflix",
                            date = LocalDateTime.of(2026, 1, 1, 9, 0),
                            isRecurrent = true,
                            recurrentFrequency = RecurrentFrequency.MONTHLY,
                            subscriptionDay = nextCharge.dayOfMonth,
                            periodId = 7L,
                        ),
                        nextChargeDate = nextCharge,
                        isInCurrentPeriod = true,
                    ),
                    currencyFormat = symbolOnlyCurrencyFormat("USD"),
                    position = PaddedListItemPosition.First,
                    isOutOfPeriod = false,
                    onClick = {},
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun upcomingRecurrentItemLast() {
        Locale.setDefault(Locale.US)
        // Fixed far-future date — see `upcomingRecurrentItemFirst`.
        val nextCharge = LocalDate.of(2099, 6, 25)
        paparazzi.snapshot {
            MinusTheme {
                UpcomingRecurrentItemRow(
                    item = UpcomingRecurrentItem(
                        transaction = Transaction(
                            id = 2L,
                            amount = BigDecimal("9.99"),
                            comment = "Spotify",
                            date = LocalDateTime.of(2026, 1, 8, 8, 0),
                            isRecurrent = true,
                            recurrentFrequency = RecurrentFrequency.WEEKLY,
                            periodId = 7L,
                        ),
                        nextChargeDate = nextCharge,
                        isInCurrentPeriod = false,
                    ),
                    currencyFormat = symbolOnlyCurrencyFormat("USD"),
                    position = PaddedListItemPosition.Last,
                    isOutOfPeriod = true,
                    onClick = {},
                )
            }
        }
    }
}
