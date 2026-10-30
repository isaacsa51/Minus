package com.serranoie.app.minus.presentation.ui.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.numpad.SavedCategoriesGrid
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class SavedCategoriesGridScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun splitSelectionHighlightsEveryPickedCategory() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                SavedCategoriesGrid(
                    tags = listOf("Groceries", "Transport", "Food", "Coffee", "Gym"),
                    selectedCategory = "Transport+Food+Coffee",
                    onCategorySelected = {},
                    applyWindowInsets = false,
                )
            }
        }
    }
}
