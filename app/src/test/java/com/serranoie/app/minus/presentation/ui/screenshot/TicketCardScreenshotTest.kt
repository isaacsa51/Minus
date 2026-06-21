package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.ticket.TransactionTicketCard
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class TicketCardScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun transactionTicketCard() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                TransactionTicketCard(
                    amountFormatted = "$42.50",
                    comment = "Lunch with team",
                    dateText = "Jan 15, 2026",
                    isRecurrent = true,
                    frequencyLabel = "Monthly",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
