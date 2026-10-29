package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.android.resources.NightMode
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.ui.quickadd.QuickExpenseSheetContent
import com.serranoie.app.minus.presentation.ui.quickadd.QuickExpenseUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class QuickExpenseSheetScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 5.0,
    )

    private val content = QuickExpenseUiState(
        isLoading = false,
        categories = listOf("Coffee", "Groceries", "Fuel", "Rent", "Gym"),
        selectedCategory = "Groceries",
        amount = "12.50",
        currency = SupportedCurrency.findByCode("EUR"),
    )

    @Composable
    private fun Sheet(state: QuickExpenseUiState) {
        MinusTheme {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                QuickExpenseSheetContent(
                    state = state,
                    onAmountChange = {},
                    onCategoryToggle = {},
                    onSave = {},
                    onCancel = {},
                    onOpenApp = {},
                )
            }
        }
    }

    @Test
    fun quickExpenseContent() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot { Sheet(content) }
    }

    @Test
    fun quickExpenseContentNight() {
        Locale.setDefault(Locale.US)
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(nightMode = NightMode.NIGHT))
        paparazzi.snapshot { Sheet(content) }
    }

    @Test
    fun quickExpenseNoCategories() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            Sheet(content.copy(categories = emptyList(), selectedCategory = null, amount = ""))
        }
    }

    @Test
    fun quickExpenseSaveFailed() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot { Sheet(content.copy(error = QuickExpenseUiState.Error.SaveFailed)) }
    }

    @Test
    fun quickExpenseNeedsBudget() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot { Sheet(QuickExpenseUiState(isLoading = false, needsBudget = true)) }
    }
}
