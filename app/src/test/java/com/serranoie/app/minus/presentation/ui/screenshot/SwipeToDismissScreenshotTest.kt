package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.DismissDirection
import com.serranoie.app.minus.presentation.ui.theme.component.DismissValue
import com.serranoie.app.minus.presentation.ui.theme.component.SwipeToDismiss
import com.serranoie.app.minus.presentation.ui.theme.component.rememberDismissState
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
class SwipeToDismissScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun swipeToDismissDefault() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                SwipeToDismiss(
                    state = rememberDismissState(initialValue = DismissValue.Default),
                    modifier = Modifier.size(width = 300.dp, height = 60.dp),
                    directions = setOf(DismissDirection.EndToStart),
                    background = {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Text("Background")
                        }
                    },
                    dismissContent = {
                        Text("Swipe to dismiss")
                    }
                )
            }
        }
    }
}
