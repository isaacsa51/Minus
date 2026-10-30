package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.editor.category.EditableCategoryTag
import com.serranoie.app.minus.presentation.ui.editor.category.FocusController
import com.serranoie.app.minus.presentation.ui.editor.category.categoryPills
import com.serranoie.app.minus.presentation.ui.editor.category.rememberCategoryHighlight
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class SplitCategoryTagScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    private fun snapshot(input: String, comment: String) {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(Modifier.padding(8.dp)) {
                    EditableCategoryTag(
                        currentComment = comment,
                        tags = listOf("Uber", "Food", "Coffee"),
                        onCommentUpdate = {},
                        editorFocusController = FocusController(),
                        extendWidth = 360.dp,
                        splitInput = input,
                        currencyCode = "USD",
                    )
                }
            }
        }
    }

    @Test
    fun pairedSplitShowsEachCategoryWithItsAmount() = snapshot(input = "15+125+66", comment = "Uber+Food+Coffee")

    @Test
    fun mismatchedSplitFlagsTheUnpairedSide() = snapshot(input = "15+125+66", comment = "Uber+Food")

    @Test
    fun typingHighlightsEachDetectedCategory() {
        paparazzi.snapshot {
            MinusTheme {
                Surface(Modifier.padding(8.dp)) {
                    val tags = listOf("Uber", "Food", "Coffee")
                    val text = "Uber+Food+Cof"
                    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
                    Text(
                        text = rememberCategoryHighlight(tags).filter(AnnotatedString(text)).text,
                        style = MaterialTheme.typography.bodyLarge,
                        onTextLayout = { layout = it },
                        modifier = Modifier
                            .padding(16.dp)
                            .categoryPills(text, tags, { layout }, MaterialTheme.colorScheme.secondaryContainer),
                    )
                }
            }
        }
    }
}
