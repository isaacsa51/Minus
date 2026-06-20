package com.serranoie.app.minus.presentation.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.domain.model.Transaction

@Composable
fun HistoryScreen(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    readOnly: Boolean = false,
    onQueueDeleteWithUndo: (transaction: Transaction, message: String, onUndo: () -> Unit) -> Unit = { _, _, _ -> },
    onCancelPendingDelete: () -> Unit = {},
    onShowInfoSnackbar: (message: String) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HistoryUiEffect.ShowSnackbar -> onShowInfoSnackbar(effect.message)
            }
        }
    }

    History(
        uiState = uiState,
        modifier = modifier,
        readOnly = readOnly,
        onQueueDeleteWithUndo = onQueueDeleteWithUndo,
        onCancelPendingDelete = onCancelPendingDelete,
        onShowInfoSnackbar = onShowInfoSnackbar,
        onProcessIntent = viewModel::processIntent,
    )
}
