package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListGroup
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class SettingsGroupScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    @Test
    fun paddedListGroupAppearance() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PaddedListGroup(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Appearance",
                    ) {
                        PaddedListItem(
                            title = "Dark Mode",
                            subtitle = "Enable dark theme",
                            icon = Icons.Filled.DarkMode,
                            onClick = {},
                            position = PaddedListItemPosition.First,
                        )
                        PaddedListItem(
                            title = "Light Mode",
                            subtitle = "Enable light theme",
                            icon = Icons.Filled.LightMode,
                            onClick = {},
                            position = PaddedListItemPosition.Last,
                        )
                    }
                }
            }
        }
    }
}
