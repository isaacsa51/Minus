package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.island.IslandBudgetPage
import com.serranoie.app.minus.presentation.island.PreviewIslandHostInfo
import com.serranoie.app.minus.presentation.island.PreviewIslandOverSplitSnapshot
import com.serranoie.app.minus.presentation.island.PreviewIslandSnapshot
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class IslandBudgetPageScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 5.0,
    )

    @Test
    fun islandBudgetPage() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                IslandBudgetPage(snapshot = PreviewIslandSnapshot, info = PreviewIslandHostInfo, onOpen = {})
            }
        }
    }

    @Test
    fun islandBudgetPageOverSplit() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                IslandBudgetPage(snapshot = PreviewIslandOverSplitSnapshot, info = PreviewIslandHostInfo, onOpen = {})
            }
        }
    }
}
