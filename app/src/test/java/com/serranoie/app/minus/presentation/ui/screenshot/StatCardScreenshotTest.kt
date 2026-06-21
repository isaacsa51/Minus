package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.StatCard
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class StatCardScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun statCardBasic() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    StatCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        value = "\$522.45",
                        label = "Total Spent",
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp)
                    )
                }
            }
        }
    }

    @Test
    fun statCardWithCrossedValue() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    StatCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        value = "\$522.45",
                        label = "Total Spent",
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
                        crossedValue = "\$900.00",
                        crossedValueColor = Color(0xFFE57373)
                    )
                }
            }
        }
    }
}