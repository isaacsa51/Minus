package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.island.IslandBudgetPage
import com.serranoie.app.minus.presentation.island.IslandBudgetSnapshot
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
    fun islandBudgetPage() = snapshotOver(Modifier.background(Color.Black), PreviewIslandSnapshot)

    @Test
    fun islandBudgetPageOverSplit() = snapshotOver(Modifier.background(Color.Black), PreviewIslandOverSplitSnapshot)

    @Test
    fun islandBudgetPageOnTransparentIsland() = snapshotOver(
        Modifier.background(Brush.linearGradient(listOf(Color(0xFF3A6BD9), Color(0xFFD9466B), Color(0xFFE8B23A)))),
        PreviewIslandSnapshot,
    )

    private fun snapshotOver(backdrop: Modifier, snapshot: IslandBudgetSnapshot) {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot { Page(backdrop, snapshot) }
    }

    @Composable
    private fun Page(backdrop: Modifier, snapshot: IslandBudgetSnapshot) {
        CompositionLocalProvider(LocalInspectionMode provides true) {
            Box(backdrop) {
                IslandBudgetPage(snapshot = snapshot, info = PreviewIslandHostInfo, onOpen = {})
            }
        }
    }
}
