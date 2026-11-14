package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.calculator.PocketPeriodFlow
import com.serranoie.app.minus.domain.calculator.SavingsPocketStatus
import com.serranoie.app.minus.domain.model.SavingsPocket
import com.serranoie.app.minus.presentation.ui.analytics.SavingsPocketsCard
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class SavingsPocketsCardScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 5.0,
    )

    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
    }

    private fun card(
        pockets: List<SavingsPocketStatus>,
        canMoveMoney: Boolean = true,
        periodFlow: PocketPeriodFlow? = null,
    ) {
        paparazzi.snapshot {
            MinusTheme {
                Surface {
                    Box(modifier = Modifier.padding(16.dp)) {
                        SavingsPocketsCard(
                            pockets = pockets,
                            currencyCode = "USD",
                            canMoveMoney = canMoveMoney,
                            onCreatePocket = { _, _ -> },
                            onDeposit = { _, _ -> },
                            onWithdraw = { _, _ -> },
                            onDelete = {},
                            periodFlow = periodFlow,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun withPockets() = card(
        listOf(
            SavingsPocketStatus(
                SavingsPocket(id = 1, name = "Japan trip", target = BigDecimal("30000"), targetDate = LocalDate.of(2099, 6, 1)),
                BigDecimal("12400"),
            ),
            SavingsPocketStatus(SavingsPocket(id = 2, name = "Emergency"), BigDecimal("850.50")),
            SavingsPocketStatus(SavingsPocket(id = 3, name = "New laptop", target = BigDecimal("1500")), BigDecimal.ZERO),
            SavingsPocketStatus(SavingsPocket(id = 4, name = "", target = BigDecimal("15000"), targetDate = LocalDate.of(2027, 5, 13)), BigDecimal.ZERO),
        ),
    )

    @Test
    fun empty() = card(emptyList())

    @Test
    fun periodFinished() = card(
        listOf(SavingsPocketStatus(SavingsPocket(id = 1, name = "Japan trip", target = BigDecimal("30000")), BigDecimal("12400"))),
        canMoveMoney = false,
        periodFlow = PocketPeriodFlow(added = BigDecimal("1200"), withdrawn = BigDecimal("150")),
    )

    @Test
    fun periodFinishedWithoutMovement() = card(
        listOf(SavingsPocketStatus(SavingsPocket(id = 1, name = "Japan trip", target = BigDecimal("30000")), BigDecimal("12400"))),
        canMoveMoney = false,
    )
}
