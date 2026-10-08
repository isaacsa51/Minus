package com.serranoie.app.minus.presentation.ui.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serranoie.app.minus.data.currency.CurrencyConversionRepository
import com.serranoie.app.minus.domain.currency.ConversionPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class CurrencyConversionViewModel @Inject constructor(private val repository: CurrencyConversionRepository) : ViewModel() {
    val preferences = repository.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConversionPreferences())
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed = _failed.asStateFlow()
    private fun action(block: suspend () -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _failed.value = false
            try { block() } catch (e: CancellationException) { throw e } catch (_: Exception) { _failed.value = true }
            finally { _busy.value = false }
        }
    }
    fun display(code: String?) = action { repository.setDisplayCurrency(code) }
    fun manual(base: String, quote: String, rate: BigDecimal) = action { repository.setManualRate(base, quote, rate) }
    fun remove(base: String, quote: String) = action { repository.removeManualRate(base, quote) }
    fun fetch(base: String, quote: String) = action { repository.fetchReferenceRate(base, quote) }
}
