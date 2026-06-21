package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableExpenseItem
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale

class SwipeableExpenseItemScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun swipeableExpenseItem() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Column(modifier = Modifier.height(200.dp)) {
                    SwipeableExpenseItem(
                        transaction = Transaction(
                            id = 1L,
                            amount = BigDecimal("42.50"),
                            comment = "Lunch with team",
                            date = LocalDateTime.of(2026, 1, 15, 12, 30),
                            periodId = 7L,
                        ),
                        currencyFormat = symbolOnlyCurrencyFormat("USD"),
                        position = PaddedListItemPosition.Single,
                        onDelete = {},
                        onEdit = {},
                        readOnly = false,
                        isBeingDeleted = false,
                        onClick = {},
                    )
                }
            }
        }
    }
}
