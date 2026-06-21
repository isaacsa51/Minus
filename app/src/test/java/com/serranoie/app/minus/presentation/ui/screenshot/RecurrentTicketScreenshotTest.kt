package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.ticket.RecurrentTicketCard
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class RecurrentTicketScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun recurrentTicketCard() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                RecurrentTicketCard(
                    title = "Netflix Subscription",
                    amountFormatted = "$16.99",
                    nextChargeDate = "Jan 20, 2026",
                    frequencyLabel = "Monthly",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
