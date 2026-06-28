package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.analytics.dialogs.CategoryAnalytics
import com.serranoie.app.minus.presentation.ui.analytics.dialogs.CategoryAnalyticsState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.LocalWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class CategoryAnalyticsScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    private val testDate: LocalDateTime = LocalDateTime.of(2026, 1, 15, 12, 0)
    private val startPeriodDate: Date = Date.from(
        LocalDate.of(2026, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
    )
    private val finishPeriodDate: Date = Date.from(
        LocalDate.of(2026, 1, 30).atStartOfDay().toInstant(ZoneOffset.UTC)
    )

    @Test
    fun categoryAnalytics_emptyState() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalWindowInsets provides PaddingValues(0.dp)) {
                        CategoryAnalytics(
                            state = CategoryAnalyticsState(
                                categoryName = "Comida",
                                categorySpends = emptyList(),
                                startPeriodDate = startPeriodDate,
                                finishPeriodDate = finishPeriodDate,
                            ),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun categoryAnalytics_singleTransaction() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalWindowInsets provides PaddingValues(0.dp)) {
                        CategoryAnalytics(
                            state = CategoryAnalyticsState(
                                categoryName = "Transporte",
                                categorySpends = listOf(
                                    Transaction(
                                        id = 1L,
                                        amount = BigDecimal("45.00"),
                                        comment = "Bus fare",
                                        date = testDate,
                                        periodId = 7L,
                                    ),
                                ),
                                startPeriodDate = startPeriodDate,
                                finishPeriodDate = finishPeriodDate,
                            ),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun categoryAnalytics_twoTransactions() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalWindowInsets provides PaddingValues(0.dp)) {
                        CategoryAnalytics(
                            state = CategoryAnalyticsState(
                                categoryName = "Entretenimiento",
                                categorySpends = listOf(
                                    Transaction(
                                        id = 1L,
                                        amount = BigDecimal("60.00"),
                                        comment = "Cinema",
                                        date = testDate.minusDays(3),
                                        periodId = 7L,
                                    ),
                                    Transaction(
                                        id = 2L,
                                        amount = BigDecimal("30.00"),
                                        comment = "Streaming",
                                        date = testDate.minusDays(7),
                                        periodId = 7L,
                                    ),
                                ),
                                startPeriodDate = startPeriodDate,
                                finishPeriodDate = finishPeriodDate,
                            ),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun categoryAnalytics_threeTransactionsAcrossDays() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalWindowInsets provides PaddingValues(0.dp)) {
                        CategoryAnalytics(
                            state = CategoryAnalyticsState(
                                categoryName = "Comida",
                                categorySpends = listOf(
                                    Transaction(
                                        id = 1L,
                                        amount = BigDecimal("120.00"),
                                        comment = "Lunch",
                                        date = testDate,
                                        periodId = 7L,
                                    ),
                                    Transaction(
                                        id = 2L,
                                        amount = BigDecimal("85.50"),
                                        comment = "Dinner",
                                        date = testDate.minusDays(2),
                                        periodId = 7L,
                                    ),
                                    Transaction(
                                        id = 3L,
                                        amount = BigDecimal("150.00"),
                                        comment = "Groceries",
                                        date = testDate.minusDays(5),
                                        periodId = 7L,
                                    ),
                                ),
                                startPeriodDate = startPeriodDate,
                                finishPeriodDate = finishPeriodDate,
                            ),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun categoryAnalytics_darkTheme() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalWindowInsets provides PaddingValues(0.dp)) {
                        CategoryAnalytics(
                            state = CategoryAnalyticsState(
                                categoryName = "Comida",
                                categorySpends = listOf(
                                    Transaction(
                                        id = 1L,
                                        amount = BigDecimal("120.00"),
                                        comment = "Lunch",
                                        date = testDate,
                                        periodId = 7L,
                                    ),
                                    Transaction(
                                        id = 2L,
                                        amount = BigDecimal("85.50"),
                                        comment = "Dinner",
                                        date = testDate.minusDays(2),
                                        periodId = 7L,
                                    ),
                                ),
                                startPeriodDate = startPeriodDate,
                                finishPeriodDate = finishPeriodDate,
                            ),
                        )
                    }
                }
            }
        }
    }
}
