package com.serranoie.app.minus.presentation.ui.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.data.csv.CsvImportPreview
import com.serranoie.app.minus.presentation.ui.settings.CsvImportDialog
import com.serranoie.app.minus.presentation.ui.settings.csv.CsvImportPreviewDialog
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class CsvImportPreviewDialogScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    private val fullBackup = CsvImportPreview(
        currentPeriodTransactions = 27,
        budgetTotal = BigDecimal("3773.00"),
        currencyCode = "HKD",
        periodStart = LocalDate.of(2026, 10, 27),
        periodEnd = LocalDate.of(2026, 11, 26),
        completedPeriods = 3,
        recurringPayments = 2,
        discarded = 0,
        replacesAllData = true,
    )

    private fun snap(dialog: CsvImportDialog) {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                CsvImportPreviewDialog(dialog = dialog, onConfirm = {}, onDismiss = {})
            }
        }
    }

    @Test
    fun fullBackup() = snap(CsvImportDialog.Preview("content://x", fullBackup))

    @Test
    fun blockedBackup() = snap(CsvImportDialog.Preview("content://x", fullBackup.copy(discarded = 2)))

    @Test
    fun rowsOnly() = snap(
        CsvImportDialog.Preview(
            "content://x",
            fullBackup.copy(budgetTotal = null, periodStart = null, periodEnd = null, replacesAllData = false),
        ),
    )

    @Test
    fun unreadable() = snap(CsvImportDialog.Unreadable)
}
