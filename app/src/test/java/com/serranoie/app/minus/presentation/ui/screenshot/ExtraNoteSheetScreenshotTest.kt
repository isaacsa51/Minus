package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.editor.note.ExtraNoteSheetContent
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class ExtraNoteSheetScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun extraNoteSheetEmpty() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot { MinusTheme { Sheet(TextFieldValue("")) } }
    }

    @Test
    fun extraNoteSheetWithText() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme { Sheet(TextFieldValue("Airport taxi — split with Sam, reimburse later")) }
        }
    }

    @Composable
    private fun Sheet(value: TextFieldValue) {
        ExtraNoteSheetContent(
            value = value,
            onValueChange = {},
            onDone = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        )
    }
}
