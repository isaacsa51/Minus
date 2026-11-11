package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.history.sections.HistorySearchField
import com.serranoie.app.minus.presentation.ui.history.sections.HistorySearchSummary
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class HistorySearchScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 5.0,
    )

    @Test
    fun historySearchEmptyAndFilled() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        HistorySearchField(query = "", onQueryChange = {})
                        HistorySearchField(query = "coffee", onQueryChange = {}, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }

    @Test
    fun historySearchSummary() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        HistorySearchField(query = "coffee", onQueryChange = {})
                        HistorySearchSummary(count = 3, total = "$25.00")
                        HistorySearchSummary(count = 1, total = "$4.00")
                    }
                }
            }
        }
    }
}
