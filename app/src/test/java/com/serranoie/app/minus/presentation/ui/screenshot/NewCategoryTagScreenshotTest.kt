package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.editor.category.NewCategoryTag
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class NewCategoryTagScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun newCategoryTagCollapsed() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    NewCategoryTag(onCreateCategory = { true }, extendWidth = 345.dp)
                }
            }
        }
    }

    @Test
    fun newCategoryTagSaved() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    NewCategoryTag(
                        onCreateCategory = { true },
                        extendWidth = 345.dp,
                        startSavedName = "Groceries",
                    )
                }
            }
        }
    }

    @Test
    fun newCategoryTagSavedWithLongName() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    NewCategoryTag(
                        onCreateCategory = { true },
                        extendWidth = 345.dp,
                        startSavedName = "Weekend groceries and household supplies",
                    )
                }
            }
        }
    }
}
