package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.expense.ExpenseItem
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale

class ExpenseItemScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun expenseItemFirst() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                ExpenseItem(
                    transaction = sampleTransaction(),
                    currencyFormat = symbolOnlyCurrencyFormat("USD"),
                    position = PaddedListItemPosition.First,
                    onClick = {},
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun expenseItemLast() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                ExpenseItem(
                    transaction = sampleTransaction(),
                    currencyFormat = symbolOnlyCurrencyFormat("USD"),
                    position = PaddedListItemPosition.Last,
                    onClick = {},
                )
            }
        }
    }

    private fun sampleTransaction(): Transaction = Transaction(
        id = 1L,
        amount = BigDecimal("42.50"),
        comment = "Lunch with team",
        date = LocalDateTime.of(2026, 1, 15, 12, 30),
        periodId = 7L,
    )
}
