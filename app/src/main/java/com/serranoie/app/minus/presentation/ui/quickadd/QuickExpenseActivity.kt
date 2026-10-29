package com.serranoie.app.minus.presentation.ui.quickadd

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.serranoie.app.minus.R
import com.serranoie.app.minus.data.repository.SettingsRepository
import com.serranoie.app.minus.presentation.MainActivity
import com.serranoie.app.minus.presentation.dynamicColorEnabled
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.ThemeManager
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logcat.asLog
import logcat.logcat
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class QuickExpenseActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var themeManager: ThemeManager

    private val viewModel: QuickExpenseViewModel by viewModels()

    private val themeLoaded = mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepository.observeSettings()
            .onEach { settings ->
                themeManager.applyUserSettings(applicationContext, settings)
                themeLoaded.value = true
            }
            .catch {
                logcat { "Quick add could not load settings: ${it.asLog()}" }
                themeLoaded.value = true
            }
            .launchIn(lifecycleScope)

        viewModel.effects
            .onEach { effect ->
                when (effect) {
                    is QuickExpenseUiEffect.Saved -> onSaved(effect.queuedForNextPeriod)
                }
            }
            .launchIn(lifecycleScope)

        setContent {
            if (!themeLoaded.value) return@setContent
            MinusTheme(dynamicColor = applicationContext.dynamicColorEnabled) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(state.isLoading, state.needsBudget) {
                    if (!state.isLoading && !state.needsBudget) runCatching { focusRequester.requestFocus() }
                }
                ModalBottomSheet(
                    onDismissRequest = ::finish,
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
                    QuickExpenseSheetContent(
                        state = state,
                        onAmountChange = { viewModel.processIntent(QuickExpenseUiIntent.AmountChanged(it)) },
                        onCategoryToggle = { viewModel.processIntent(QuickExpenseUiIntent.CategoryToggled(it)) },
                        onSave = { viewModel.processIntent(QuickExpenseUiIntent.Save) },
                        onCancel = ::finish,
                        onOpenApp = ::openApp,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .imePadding(),
                        focusRequester = focusRequester,
                    )
                }
            }
        }
    }

    private fun onSaved(queuedForNextPeriod: Boolean) {
        val state = viewModel.uiState.value
        val amount = state.currency?.let { symbolOnlyCurrencyFormat(it.code).format(BigDecimal(state.amount)) }
            ?: state.amount
        val message = if (queuedForNextPeriod) {
            getString(R.string.notification_scan_queued_message, amount)
        } else {
            getString(R.string.quick_add_saved, amount)
        }
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}
