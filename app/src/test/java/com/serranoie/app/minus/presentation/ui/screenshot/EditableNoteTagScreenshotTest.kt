package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.editor.category.FocusController
import com.serranoie.app.minus.presentation.ui.editor.note.EditableNoteTag
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class EditableNoteTagScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        maxPercentDifference = 10.0,
    )

    @Test
    fun editableNoteTagCollapsed() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val toolbarWidth = maxWidth - 48.dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(44.dp)
                            .horizontalScroll(rememberScrollState(), reverseScrolling = true)
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                    ) {
                        EditableNoteTag(
                            currentNote = "Airport taxi — split with Sam",
                            onNoteUpdate = {},
                            editorFocusController = remember { FocusController() },
                            extendWidth = toolbarWidth,
                        )
                    }
                }
            }
        }
    }
}
