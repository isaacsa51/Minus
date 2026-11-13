package com.serranoie.app.minus.presentation.ui.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import com.serranoie.app.minus.presentation.ui.editor.dialogs.RecurrentExpenseDialog
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class RecurrentExpenseDialogScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 5.0,
    )

    private val today = LocalDate.of(2099, 7, 3)

    private val settings = BudgetSettings(
        totalBudget = BigDecimal("1000"),
        period = BudgetPeriod.DAILY,
        startDate = LocalDate.of(2099, 7, 1),
        endDate = LocalDate.of(2099, 7, 10),
        currencyCode = "USD",
        splitMode = BudgetSplitMode.DYNAMIC,
    )

    private val state = BudgetState.EMPTY.copy(
        dailyBudget = BigDecimal("125.00"),
        daysRemaining = 8,
        totalBudget = BigDecimal("1000"),
    )

    private fun snapshot(frequency: RecurrentFrequency, amount: String) {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                RecurrentExpenseDialog(
                    budgetSettings = settings,
                    onDismiss = {},
                    onConfirm = { _, _, _, _ -> },
                    fallbackComments = emptyMap(),
                    budgetState = state,
                    amount = BigDecimal(amount),
                    today = today,
                    initialFrequency = frequency,
                )
            }
        }
    }

    @Test
    fun recurrentExpenseDialogMonthly() = snapshot(RecurrentFrequency.MONTHLY, "300")

    @Test
    fun recurrentExpenseDialogOnce() = snapshot(RecurrentFrequency.ONCE, "300")

    @Test
    fun recurrentExpenseDialogOnceOverBudget() = snapshot(RecurrentFrequency.ONCE, "1200")
}
