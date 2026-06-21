package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.TopSheetValue
import com.serranoie.app.minus.presentation.ui.theme.component.TopSheetLayout
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.rememberSwipeableState
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalWearMaterialApi::class)
class TopSheetScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun topSheetHalfExpanded() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                TopSheetLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    swipeableState = rememberSwipeableState(TopSheetValue.HalfExpanded),
                    sheetContentHalfExpand = {
                        Box(
                            Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Half expanded content")
                        }
                    },
                    sheetContentExpand = {
                        Box(
                            Modifier.fillMaxWidth().height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Expanded content")
                        }
                    },
                )
            }
        }
    }
}
