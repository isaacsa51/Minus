package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.date.DaysLeftCard
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class DaysLeftCardScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    private fun fixedDate(year: Int, month: Int, day: Int): Date {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay().toInstant(ZoneOffset.UTC))
    }

    @Test
    fun daysLeftCardMidMonth() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    DaysLeftCard(
                        modifier = Modifier,
                        startDate = fixedDate(2026, 1, 1),
                        finishDate = fixedDate(2026, 1, 30)
                    )
                }
            }
        }
    }
}