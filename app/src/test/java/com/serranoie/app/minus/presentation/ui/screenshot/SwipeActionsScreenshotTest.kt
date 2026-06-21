package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.DefaultSwipeActionsConfig
import com.serranoie.app.minus.presentation.ui.theme.component.SwipeActions
import com.serranoie.app.minus.presentation.ui.theme.component.SwipeActionsConfig
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class SwipeActionsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun swipeActionsDefault() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                SwipeActions(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    enabled = true,
                    startActionsConfig = DefaultSwipeActionsConfig,
                    endActionsConfig = DefaultSwipeActionsConfig,
                    content = {
                        Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                            Text("Swipe me")
                        }
                    }
                )
            }
        }
    }
}