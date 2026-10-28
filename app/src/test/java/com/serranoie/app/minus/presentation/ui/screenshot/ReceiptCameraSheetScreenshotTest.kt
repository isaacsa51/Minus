package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.receipt.ReceiptScan
import com.serranoie.app.minus.presentation.ui.editor.receipt.ReceiptCameraSheetContent
import com.serranoie.app.minus.presentation.ui.editor.receipt.ReceiptCameraUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class ReceiptCameraSheetScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        maxPercentDifference = 10.0,
    )

    private val budgetState = BudgetState(
        remainingToday = BigDecimal("14.28"),
        totalSpentToday = BigDecimal("35.72"),
        dailyBudget = BigDecimal("50.00"),
        daysRemaining = 12,
        progress = 0.58f,
        isOverBudget = false,
        totalBudget = BigDecimal("900.00"),
        totalSpentInPeriod = BigDecimal("522.45"),
    )

    @Test
    fun searchingForTotal() = snapshot(ReceiptCameraUiState())

    @Test
    fun detectedTotalOverTodaysBudget() = snapshot(
        ReceiptCameraUiState(detected = ReceiptScan(merchant = "OXXO", total = BigDecimal("123.56"))),
    )

    @Test
    fun detectedTotalWithinTodaysBudget() = snapshot(
        ReceiptCameraUiState(detected = ReceiptScan(merchant = "OXXO", total = BigDecimal("4.50"))),
        budget = budgetState.copy(remainingToday = BigDecimal("50.00"), totalSpentToday = BigDecimal.ZERO),
    )

    @Test
    fun capturing() = snapshot(
        ReceiptCameraUiState(
            detected = ReceiptScan(merchant = "OXXO", total = BigDecimal("123.56")),
            isCapturing = true,
        ),
    )

    @Test
    fun captureFailed() = snapshot(ReceiptCameraUiState(captureFailed = true))

    @Test
    fun halfwayPullingDown() = snapshot(ReceiptCameraUiState(), progress = 0.5f)

    private fun snapshot(
        state: ReceiptCameraUiState,
        progress: Float = 1f,
        budget: BudgetState = budgetState,
    ) {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                ReceiptCameraSheetContent(
                    progress = progress,
                    state = state,
                    startHeightPx = 900f,
                    budgetState = budget,
                    viewPeriod = BudgetPeriod.DAILY,
                    splitMode = BudgetSplitMode.DYNAMIC,
                    currencyCode = "USD",
                    onClose = {},
                    onCapture = {},
                    preview = { modifier -> FakeCameraFeed(modifier) },
                )
            }
        }
    }

    @Composable
    private fun FakeCameraFeed(modifier: Modifier) {
        Box(
            modifier = modifier.background(
                Brush.verticalGradient(listOf(Color(0xFF8FB3E8), Color(0xFF3D6FC4))),
            ),
        )
    }
}
