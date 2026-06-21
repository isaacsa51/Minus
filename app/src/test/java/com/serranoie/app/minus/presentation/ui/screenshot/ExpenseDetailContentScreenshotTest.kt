package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.expense.ExpenseDetailContent
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class ExpenseDetailContentScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun expenseDetailReadOnly() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    ExpenseDetailContent(
                        transaction = Transaction(
                            id = 1L,
                            amount = BigDecimal("42.50"),
                            comment = "Lunch with team",
                            date = LocalDate.of(2026, 1, 15).atTime(12, 30),
                            periodId = 7L,
                        ),
                        isRecurrentExpense = false,
                        operationNumber = "#42",
                        operationTime = "12:30 PM",
                        totalAmountText = "$42.50",
                        details = listOf(
                            "Category" to "Food",
                            "Date" to "Jan 15, 2026",
                            "Period" to "January 2026",
                        ),
                        onMarkAsPaid = null,
                        onEdit = {},
                        onDelete = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
