package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.google.common.truth.Truth.assertThat
import com.serranoie.app.minus.presentation.ui.history.sections.rememberLatestDismiss
import org.junit.Rule
import org.junit.Test

class HistoryFilterDismissTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun aDismissKeptFromTheFirstFrameStillSeesFiltersPickedLater() {
        var firstDismiss: (() -> Unit)? = null
        val seen = mutableListOf<String>()

        val view = ComposeView(paparazzi.context).apply {
            setContent {
                var filter by remember { mutableStateOf("no filter") }
                Sheet(filter = filter, onDismiss = { seen += it }) { if (firstDismiss == null) firstDismiss = it }
                LaunchedEffect(Unit) { filter = "Food" }
            }
        }
        paparazzi.gif(view, start = 0, end = 100)

        firstDismiss!!.invoke()
        assertThat(seen).containsExactly("Food")
    }

    @Composable
    private fun Sheet(filter: String, onDismiss: (String) -> Unit, keep: (() -> Unit) -> Unit) {
        keep(rememberLatestDismiss { onDismiss(filter) })
        Box(Modifier.size(8.dp))
    }
}
