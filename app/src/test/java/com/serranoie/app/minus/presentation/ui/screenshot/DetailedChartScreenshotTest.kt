package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.charts.DetailedChart
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale

class DetailedChartScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 10.0,
    )

    @Test
    fun detailedChart_singlePoint() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    DetailedChart(
                        spends = listOf(
                            sampleTransaction(id = 1L, amount = "100.00", daysAgo = 2),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                    )
                }
            }
        }
    }

    @Test
    fun detailedChart_twoPointsLine() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    DetailedChart(
                        spends = listOf(
                            sampleTransaction(id = 1L, amount = "85.50", daysAgo = 4),
                            sampleTransaction(id = 2L, amount = "120.00", daysAgo = 0),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                    )
                }
            }
        }
    }

    @Test
    fun detailedChart_threePointsAscending() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    DetailedChart(
                        spends = listOf(
                            sampleTransaction(id = 1L, amount = "42.00", daysAgo = 6),
                            sampleTransaction(id = 2L, amount = "85.50", daysAgo = 3),
                            sampleTransaction(id = 3L, amount = "150.00", daysAgo = 0),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                    )
                }
            }
        }
    }

    @Test
    fun detailedChart_fourPointsVarying() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    DetailedChart(
                        spends = listOf(
                            sampleTransaction(id = 1L, amount = "100.00", daysAgo = 7),
                            sampleTransaction(id = 2L, amount = "60.00", daysAgo = 5),
                            sampleTransaction(id = 3L, amount = "180.00", daysAgo = 3),
                            sampleTransaction(id = 4L, amount = "75.00", daysAgo = 0),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                    )
                }
            }
        }
    }

    @Test
    fun detailedChart_darkTheme() {
        Locale.setDefault(Locale.US)
        paparazzi.snapshot {
            MinusTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    DetailedChart(
                        spends = listOf(
                            sampleTransaction(id = 1L, amount = "85.50", daysAgo = 4),
                            sampleTransaction(id = 2L, amount = "120.00", daysAgo = 2),
                            sampleTransaction(id = 3L, amount = "150.00", daysAgo = 0),
                        ),
                        graphColor = MaterialTheme.colorScheme.primary,
                        gridColor = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                    )
                }
            }
        }
    }

    private fun sampleTransaction(
        id: Long,
        amount: String,
        daysAgo: Long,
    ): Transaction = Transaction(
        id = id,
        amount = BigDecimal(amount),
        comment = "Sample",
        date = LocalDateTime.of(2026, 1, 15, 12, 0).minusDays(daysAgo),
        periodId = 7L,
    )
}
