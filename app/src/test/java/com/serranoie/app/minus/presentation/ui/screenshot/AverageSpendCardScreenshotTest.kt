package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.budget.AverageSpendCard
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale

class AverageSpendCardScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
    )

    private fun sampleTransactions(): List<Transaction> = listOf(
        Transaction(id=1L, amount=BigDecimal("18.75"), comment="Lunch", date=LocalDateTime.of(2026,1,15,12,30), periodId=7L),
        Transaction(id=2L, amount=BigDecimal("52.00"), comment="Groceries", date=LocalDateTime.of(2026,1,14,10,15), periodId=7L),
        Transaction(id=3L, amount=BigDecimal("8.50"), comment="Coffee", date=LocalDateTime.of(2026,1,13,9,0), periodId=7L),
        Transaction(id=4L, amount=BigDecimal("125.00"), comment="Utilities", date=LocalDateTime.of(2026,1,12,14,0), periodId=7L),
        Transaction(id=5L, amount=BigDecimal("35.25"), comment="Gas", date=LocalDateTime.of(2026,1,13,18,45), periodId=7L)
    )

    @Test
    fun averageSpendCard() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Box(modifier = Modifier.padding(16.dp)) {
                    AverageSpendCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        spends = sampleTransactions(),
                        startDate = null,
                        finishDate = null,
                        currency = "MXN"
                    )
                }
            }
        }
    }
}