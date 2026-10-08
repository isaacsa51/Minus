package com.serranoie.app.minus.presentation.ui.currency

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.serranoie.app.minus.domain.currency.ConversionPreferences
import com.serranoie.app.minus.domain.currency.ExchangeRate
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class CurrencyConversionUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val prefs = ConversionPreferences("BRL", referenceRates = listOf(ExchangeRate("USD", "BRL", BigDecimal("5"), "2026-10-08", false)))
    @Test fun showsPairedTotals() {
        rule.setContent { MinusTheme { CurrencyPeriodSummaryContent("USD", BigDecimal("100"), BigDecimal("12.50"), prefs, false) } }
        rule.onNodeWithText("Budget: USD 100.00 ≈ BRL 500.00").assertIsDisplayed()
        rule.onNodeWithText("Spent: USD 12.50 ≈ BRL 62.50").assertIsDisplayed()
    }
    @Test fun missingRateIsExplicit() {
        rule.setContent { MinusTheme { CurrencyPeriodSummaryContent("EUR", BigDecimal("100"), BigDecimal("12.50"), prefs, false) } }
        rule.onNodeWithText("No saved EUR/BRL rate. Set a rate in Settings.").assertIsDisplayed()
    }
    @Test fun privacyHidesConversion() {
        rule.setContent { MinusTheme { CurrencyPeriodSummaryContent("USD", BigDecimal("100"), BigDecimal("12.50"), prefs, true) } }
        rule.onNodeWithText("Budget: USD 100.00 ≈ BRL 500.00").assertDoesNotExist()
    }
}
