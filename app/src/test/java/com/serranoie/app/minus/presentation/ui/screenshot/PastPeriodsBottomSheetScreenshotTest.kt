package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.ArchivedBudget
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.analytics.dialogs.PastPeriodsBottomSheet
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class PastPeriodsBottomSheetScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    // Almost fully spent, and more than 4 distinct categories -- every category must render as
    // its own segment/legend entry (the legend scrolls), and the bar must still visually reach
    // the end (minus the tiny "saved" sliver) instead of stopping short.
    @Test
    fun pastPeriodsBottomSheet_moreThanFourCategoriesFillsBar() {
        Locale.setDefault(Locale.US)
        val periodStart = LocalDate.of(2026, 7, 15)
        val periodEnd = LocalDate.of(2026, 7, 29)

        val period = ArchivedBudget(
            periodId = 1L,
            totalBudget = BigDecimal("11400.00"),
            spentAmount = BigDecimal("11399.78"),
            startDate = periodStart,
            endDate = periodEnd,
            currencyCode = "USD",
            periodType = BudgetPeriod.BIWEEKLY,
        )

        val transactions = listOf(
            Transaction(amount = BigDecimal("4450.00"), comment = "cerrajero", date = periodStart.plusDays(1).atStartOfDay(), periodId = 1L),
            Transaction(amount = BigDecimal("3800.00"), comment = "comida", date = periodStart.plusDays(2).atStartOfDay(), periodId = 1L),
            Transaction(amount = BigDecimal("1500.00"), comment = "café", date = periodStart.plusDays(3).atStartOfDay(), periodId = 1L),
            Transaction(amount = BigDecimal("800.00"), comment = "antojos", date = periodStart.plusDays(4).atStartOfDay(), periodId = 1L),
            Transaction(amount = BigDecimal("500.00"), comment = "gasolina", date = periodStart.plusDays(5).atStartOfDay(), periodId = 1L),
            Transaction(amount = BigDecimal("349.78"), comment = "salida", date = periodStart.plusDays(6).atStartOfDay(), periodId = 1L),
            Transaction(amount = BigDecimal("200.00"), comment = "aseo", date = periodStart.plusDays(7).atStartOfDay(), periodId = 1L),
            Transaction(amount = BigDecimal("200.00"), comment = "uber", date = periodStart.plusDays(8).atStartOfDay(), periodId = 1L),
        )

        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalWindowInsets provides PaddingValues(0.dp)) {
                        PastPeriodsBottomSheet(
                            periods = listOf(period),
                            allTransactions = transactions,
                            onPeriodClick = {},
                        )
                    }
                }
            }
        }
    }

    // More categories than the color palette has entries -- every segment must still get a real
    // color (cycling the palette) instead of falling back to a flat gray for the overflow. Also
    // under budget, so the trailing "Saved" segment (diagonal stripe + legend entry) must render.
    @Test
    fun pastPeriodsBottomSheet_manyCategoriesCycleColorsAndShowSavedStripe() {
        Locale.setDefault(Locale.US)
        val periodStart = LocalDate.of(2026, 4, 1)
        val periodEnd = LocalDate.of(2026, 4, 15)

        val period = ArchivedBudget(
            periodId = 1L,
            totalBudget = BigDecimal("11400.00"),
            spentAmount = BigDecimal("7804.65"),
            startDate = periodStart,
            endDate = periodEnd,
            currencyCode = "USD",
            periodType = BudgetPeriod.BIWEEKLY,
        )

        val comments = listOf(
            "ender", "uber", "ani", "salida", "antojo", "celular", "gym",
            "comida", "cafe", "gasolina", "aseo", "regalos", "higiene",
        )
        val transactions = comments.mapIndexed { index, comment ->
            Transaction(
                amount = BigDecimal("600.00") - BigDecimal(index * 20),
                comment = comment,
                date = periodStart.plusDays(index.toLong() % 14).atStartOfDay(),
                periodId = 1L,
            )
        }

        paparazzi.snapshot {
            MinusTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalWindowInsets provides PaddingValues(0.dp)) {
                        PastPeriodsBottomSheet(
                            periods = listOf(period),
                            allTransactions = transactions,
                            onPeriodClick = {},
                        )
                    }
                }
            }
        }
    }

    // Few enough categories that the "Money Saved" legend entry stays in frame (the other tests
    // above have too many categories for it to fit before the row scrolls off-screen).
    @Test
    fun pastPeriodsBottomSheet_savedLegendEntryLabel() {
        Locale.setDefault(Locale.US)
        val periodStart = LocalDate.of(2026, 7, 19)
        val periodEnd = LocalDate.of(2026, 8, 17)

        val period = ArchivedBudget(
            periodId = 1L,
            totalBudget = BigDecimal("1000.00"),
            spentAmount = BigDecimal("850.00"),
            startDate = periodStart,
            endDate = periodEnd,
            currencyCode = "USD",
            periodType = BudgetPeriod.MONTHLY,
        )

        val transactions = listOf(
            Transaction(amount = BigDecimal("650.00"), comment = "Food", date = periodStart.plusDays(2).atStartOfDay(), periodId = 1L),
            Transaction(amount = BigDecimal("200.00"), comment = "Coffee", date = periodStart.plusDays(3).atStartOfDay(), periodId = 1L),
        )

        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalWindowInsets provides PaddingValues(0.dp)) {
                        PastPeriodsBottomSheet(
                            periods = listOf(period),
                            allTransactions = transactions,
                            onPeriodClick = {},
                        )
                    }
                }
            }
        }
    }

    // A virtual/reconstructed period (negative periodId, sliced from orphaned transactions
    // rather than a stored ArchivedBudget row) must show the "Estimated" badge instead of the
    // usual on-track/over-budget/saved status chip, so it reads as a best-effort figure.
    @Test
    fun pastPeriodsBottomSheet_virtualPeriodShowsEstimatedBadge() {
        Locale.setDefault(Locale.US)
        val periodStart = LocalDate.of(2026, 6, 1)
        val periodEnd = LocalDate.of(2026, 6, 30)

        val period = ArchivedBudget(
            periodId = -202606L,
            totalBudget = BigDecimal("1000.00"),
            spentAmount = BigDecimal("150.00"),
            startDate = periodStart,
            endDate = periodEnd,
            currencyCode = "USD",
            periodType = BudgetPeriod.MONTHLY,
        )

        val transactions = listOf(
            Transaction(amount = BigDecimal("100.00"), comment = "Internet", date = periodStart.plusDays(4).atStartOfDay()),
            Transaction(amount = BigDecimal("50.00"), comment = "Coffee", date = periodStart.plusDays(9).atStartOfDay()),
        )

        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalWindowInsets provides PaddingValues(0.dp)) {
                        PastPeriodsBottomSheet(
                            periods = listOf(period),
                            allTransactions = transactions,
                            onPeriodClick = {},
                        )
                    }
                }
            }
        }
    }

    // A mix of real and virtual periods -- the wavy divider must appear exactly once, right
    // before the first estimated period, not between every real period above it.
    @Test
    fun pastPeriodsBottomSheet_wavyDividerMarksEstimatedSection() {
        Locale.setDefault(Locale.US)

        val realPeriod = ArchivedBudget(
            periodId = 1L,
            totalBudget = BigDecimal("1000.00"),
            spentAmount = BigDecimal("400.00"),
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 8, 14),
            currencyCode = "USD",
            periodType = BudgetPeriod.BIWEEKLY,
        )
        val estimatedPeriodNewer = ArchivedBudget(
            periodId = -202607L,
            totalBudget = BigDecimal("1000.00"),
            spentAmount = BigDecimal("300.00"),
            startDate = LocalDate.of(2026, 7, 1),
            endDate = LocalDate.of(2026, 7, 31),
            currencyCode = "USD",
            periodType = BudgetPeriod.MONTHLY,
        )
        val estimatedPeriodOlder = ArchivedBudget(
            periodId = -202606L,
            totalBudget = BigDecimal("1000.00"),
            spentAmount = BigDecimal("150.00"),
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 30),
            currencyCode = "USD",
            periodType = BudgetPeriod.MONTHLY,
        )

        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalWindowInsets provides PaddingValues(0.dp)) {
                        PastPeriodsBottomSheet(
                            periods = listOf(realPeriod, estimatedPeriodNewer, estimatedPeriodOlder),
                            allTransactions = emptyList(),
                            onPeriodClick = {},
                        )
                    }
                }
            }
        }
    }
}
