package com.serranoie.app.minus.presentation.ui.currency

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.currency.ConversionPreferences
import com.serranoie.app.minus.domain.model.SupportedCurrency
import java.math.BigDecimal
import java.util.Locale

private fun normalize(code: String) = code.trim().uppercase(Locale.ROOT)
private fun known(code: String) = SupportedCurrency.findByCode(code) != null || code in setOf("IRR", "IRT")

@Composable
fun CurrencyConversionSettings(viewModel: CurrencyConversionViewModel = hiltViewModel()) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var display by remember(preferences.displayCurrency) { mutableStateOf(preferences.displayCurrency ?: "") }
    var pickDisplay by remember { mutableStateOf(false) }
    var base by remember { mutableStateOf("USD") }
    var quote by remember { mutableStateOf("BRL") }
    var value by remember { mutableStateOf("") }
    val from = normalize(base)
    val to = normalize(quote)
    val validPair = known(from) && known(to) && from != to
    val parsed = value.trim().toBigDecimalOrNull()?.takeIf { it.signum() > 0 && it.precision() <= 30 && it.scale() in -18..24 }
    if (pickDisplay) {
        AlertDialog(onDismissRequest = { pickDisplay = false },
            title = { Text(stringResource(R.string.conversion_display)) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    val codes = (SupportedCurrency.ALL.map { it.code } + listOf("IRR", "IRT")).distinct().sorted()
                    items(codes.size) { index ->
                        val code = codes[index]
                        TextButton(onClick = { display = code; pickDisplay = false }) {
                            Text("$code ${SupportedCurrency.findByCode(code)?.displayName() ?: code}")
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { pickDisplay = false }) { Text(stringResource(android.R.string.cancel)) } })
    }
    OutlinedCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { expanded = !expanded }) { Text(stringResource(R.string.conversion_title)) }
            Text(stringResource(R.string.conversion_display_current, preferences.displayCurrency ?: stringResource(R.string.conversion_off)))
            if (expanded) {
                Text(stringResource(R.string.conversion_description), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(display, { display = it.take(3) }, label = { Text(stringResource(R.string.conversion_display)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { pickDisplay = true }, enabled = !busy) { Text(stringResource(R.string.conversion_choose)) }
                Row {
                    TextButton(onClick = { viewModel.display(normalize(display)) }, enabled = !busy && known(normalize(display))) { Text(stringResource(R.string.conversion_save_display)) }
                    TextButton(onClick = { viewModel.display(null) }, enabled = !busy) { Text(stringResource(R.string.conversion_disable)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(base, { base = it.take(3) }, label = { Text(stringResource(R.string.conversion_from)) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(quote, { quote = it.take(3) }, label = { Text(stringResource(R.string.conversion_to)) }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value, { value = it.take(60) }, label = { Text(stringResource(R.string.conversion_rate)) }, supportingText = { Text(stringResource(R.string.conversion_rate_help)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { parsed?.let { viewModel.manual(from, to, it) } }, enabled = !busy && validPair && parsed != null) { Text(stringResource(R.string.conversion_save_manual)) }
                TextButton(onClick = { viewModel.fetch(from, to) }, enabled = !busy && validPair && from != "IRT" && to != "IRT") { Text(stringResource(R.string.conversion_fetch)) }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (failed) Text(stringResource(R.string.conversion_error), color = MaterialTheme.colorScheme.error)
                Text(stringResource(R.string.conversion_reference_note), style = MaterialTheme.typography.bodySmall)
                preferences.manualRates.forEach { rate ->
                    Text(stringResource(R.string.conversion_manual_record, rate.base, rate.quote, rate.rate.toPlainString()))
                    TextButton(onClick = { viewModel.remove(rate.base, rate.quote) }, enabled = !busy) { Text(stringResource(R.string.conversion_remove, rate.base, rate.quote)) }
                }
                preferences.referenceRates.forEach { rate -> Text(stringResource(R.string.conversion_reference_record, rate.base, rate.quote, rate.rate.toPlainString(), rate.date), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

val LocalConversionPreferences = staticCompositionLocalOf { ConversionPreferences() }

@Composable
fun CurrencyConversionHost(viewModel: CurrencyConversionViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalConversionPreferences provides preferences, content = content)
}

@Composable
fun CurrencyPeriodSummary(base: String, budget: BigDecimal, spent: BigDecimal, modifier: Modifier = Modifier) {
    CurrencyPeriodSummaryContent(base, budget, spent, LocalConversionPreferences.current,
        com.serranoie.app.minus.presentation.util.LocalCensorMode.current, modifier)
}

@Composable
internal fun CurrencyPeriodSummaryContent(base: String, budget: BigDecimal, spent: BigDecimal, preferences: ConversionPreferences, censored: Boolean, modifier: Modifier = Modifier) {
    val target = preferences.displayCurrency ?: return
    if (target == base || censored) return
    val locale = LocalConfiguration.current.locales[0]
    val rate = preferences.rateFor(base, target)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.conversion_title), style = MaterialTheme.typography.labelLarge)
        if (rate == null) {
            Text(stringResource(R.string.conversion_missing, base, target), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(stringResource(R.string.conversion_budget_line, formatConversionAmount(budget, base, locale), formatConversionAmount(preferences.convert(budget, base)!!, target, locale)), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.conversion_spent_line, formatConversionAmount(spent, base, locale), formatConversionAmount(preferences.convert(spent, base)!!, target, locale)), style = MaterialTheme.typography.bodyMedium)
            Text(if (rate.manual) stringResource(R.string.conversion_manual_note, rate.date) else stringResource(R.string.conversion_cached_note, rate.date), style = MaterialTheme.typography.bodySmall)
        }
    }
}
