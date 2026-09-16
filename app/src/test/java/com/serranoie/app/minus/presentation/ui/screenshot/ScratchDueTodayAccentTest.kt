package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.subscriptions.DueTodayCard
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

class ScratchDueTodayAccentTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun dueTodayCardAccentLight() {
        Locale.setDefault(Locale.US)
        val today = LocalDate.now()
        paparazzi.snapshot {
            MinusTheme(darkTheme = false) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    DueTodayCard(
                        item = UpcomingRecurrentItem(
                            transaction = Transaction(
                                id = 1L,
                                amount = BigDecimal("16.99"),
                                comment = "Netflix",
                                date = today.minusMonths(1).atStartOfDay(),
                                isRecurrent = true,
                                recurrentFrequency = RecurrentFrequency.MONTHLY,
                            ),
                            nextChargeDate = today,
                            isInCurrentPeriod = true,
                        ),
                        currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
                        onConfirmPaid = {},
                        onSkip = {},
                        onEdit = {},
                        onDelete = {},
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }

    @Test
    fun dueTodayCardAccentDark() {
        Locale.setDefault(Locale.US)
        val today = LocalDate.now()
        paparazzi.snapshot {
            MinusTheme(darkTheme = true) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    DueTodayCard(
                        item = UpcomingRecurrentItem(
                            transaction = Transaction(
                                id = 1L,
                                amount = BigDecimal("16.99"),
                                comment = "Netflix",
                                date = today.minusMonths(1).atStartOfDay(),
                                isRecurrent = true,
                                recurrentFrequency = RecurrentFrequency.MONTHLY,
                            ),
                            nextChargeDate = today,
                            isInCurrentPeriod = true,
                        ),
                        currencyFormat = NumberFormat.getCurrencyInstance(Locale.US),
                        onConfirmPaid = {},
                        onSkip = {},
                        onEdit = {},
                        onDelete = {},
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
