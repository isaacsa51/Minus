package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.FirstLaunchTutorialStage
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.LocalWindowSize
import com.serranoie.app.minus.presentation.ui.budget.BudgetUiState
import com.serranoie.app.minus.presentation.ui.editor.AnimState
import com.serranoie.app.minus.presentation.ui.home.MainScreenActions
import com.serranoie.app.minus.presentation.ui.home.MainScreenContent
import com.serranoie.app.minus.presentation.ui.home.MainScreenUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class MainScreenScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        maxPercentDifference = 10.0,
    )

    @Test
    fun mainScreenPhoneIdle() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MainScreenPreviewContent(
                budgetUiState = sampleMainBudgetUiState().copy(numpadInput = ""),
                windowSizeClass = WindowWidthSizeClass.Compact,
            )
        }
    }

    @Test
    fun mainScreenPhoneEditingExpense() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MainScreenPreviewContent(
                budgetUiState = sampleMainBudgetUiState().copy(
                    numpadInput = "42.80",
                    isNumpadValid = true,
                    animState = AnimState.EDITING,
                    currentComment = "Dinner",
                    tags = listOf("food", "friends"),
                ),
                windowSizeClass = WindowWidthSizeClass.Compact,
            )
        }
    }
}

@Composable
private fun MainScreenPreviewContent(
    budgetUiState: BudgetUiState,
    windowSizeClass: WindowWidthSizeClass,
) {
    CompositionLocalProvider(
        LocalWindowSize provides windowSizeClass,
        LocalWindowInsets provides PaddingValues(0.dp),
    ) {
        MinusTheme {
            MainScreenContent(
                mainScreenState =
                    MainScreenUiState(
                        onboardingCompleted = true,
                        tutorialStage = FirstLaunchTutorialStage.COMPLETED,
                        showCreditQuickToggleFeature = true,
                        directCategoryPopupEnabled = false,
                        categoryGridModeEnabled = false,
                        showBudgetPeriodSheet = false,
                        forceBudgetPeriodSheetSetup = false,
                        selectedViewPeriod = BudgetPeriod.DAILY,
                    ),
                budgetUiState = budgetUiState,
                actions =
                    MainScreenActions(
                        onProcessIntent = {},
                        onAdvanceTutorial = {},
                    ),
                openWalletOnStart = false,
            )
        }
    }
}

private fun sampleMainBudgetUiState(): BudgetUiState = BudgetUiState(
    budgetSettings = BudgetSettings(
        totalBudget = BigDecimal("900.00"),
        period = BudgetPeriod.MONTHLY,
        startDate = LocalDate.of(2026, 1, 1),
        endDate = LocalDate.of(2026, 1, 30),
        currencyCode = "USD",
        daysInPeriod = 30,
    ),
    budgetState = BudgetState(
        remainingToday = BigDecimal("31.25"),
        totalSpentToday = BigDecimal("18.75"),
        dailyBudget = BigDecimal("50.00"),
        daysRemaining = 12,
        progress = 0.58f,
        isOverBudget = false,
        totalBudget = BigDecimal("900.00"),
        totalSpentInPeriod = BigDecimal("522.45"),
    ),
    transactions = listOf(
        Transaction(
            id = 1L,
            amount = BigDecimal("18.75"),
            comment = "Lunch",
            date = LocalDate.of(2026, 1, 15).atTime(12, 30),
            periodId = 7L,
        ),
    ),
    selectedDate = LocalDate.of(2026, 1, 15),
    currentPeriodStartedAtMillis = LocalDate.of(2026, 1, 1).toEpochDay(),
    currentPeriodId = 7L,
)
