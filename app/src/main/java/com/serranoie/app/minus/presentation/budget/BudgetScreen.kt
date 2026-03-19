package com.serranoie.app.minus.presentation.budget

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.component.RolloverDialog
import com.serranoie.app.minus.presentation.ui.theme.component.budget.BudgetDisplay
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorBackground
import com.serranoie.app.minus.presentation.ui.theme.component.budget.SpendBudgetCard
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Currency
import java.util.Date
import java.util.Locale

/**
 * Pure Budget Screen composable with no ViewModel dependencies.
 * Shows budget info, input display, and editing interface.
 * Fully previewable with mock data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    uiState: BudgetUiState,
    onDeleteTransaction: (Transaction) -> Unit,
    onCommentClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            UnifiedEditorDisplay(
                input = uiState.numpadInput,
                currencyCode = uiState.budgetSettings?.currencyCode ?: "USD",
                budgetState = uiState.budgetState,
                budgetSettings = uiState.budgetSettings,
                onCommentClick = onCommentClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = "Estado del presupuesto",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // SpendBudgetCard showing current budget info
                val totalBudget = uiState.budgetState?.totalBudget ?: BigDecimal.ZERO
                val spent = uiState.budgetState?.totalSpentInPeriod ?: BigDecimal.ZERO

                SpendBudgetCard(
                    modifier = Modifier.fillMaxWidth(),
                    budget = totalBudget,
                    spend = spent
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * Unified editor display - shows typed value with cursor, or WholeBudgetCard when idle.
 */
@Composable
private fun UnifiedEditorDisplay(
    input: String,
    currencyCode: String,
    budgetState: BudgetState?,
    budgetSettings: BudgetSettings?,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormat = remember(currencyCode) {
        com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat(currencyCode)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // When idle: show BudgetDisplay with total, date range and stats
            if (input.isEmpty()) {
                val startDate = budgetSettings?.startDate?.let { 
                    Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
                } ?: Date()
                
                val finishDate = budgetSettings?.endDate?.let { 
                    Date.from(it.atStartOfDay(ZoneId.systemDefault()).toInstant())
                } ?: budgetSettings?.let { 
                    val days = it.getDaysForPeriod()
                    val endLocalDate = it.startDate.plusDays(days.toLong())
                    Date.from(endLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                }
                
                val budget = budgetState?.totalBudget ?: BigDecimal.ZERO

                BudgetDisplay(
                    budget = budget,
                    budgetState = budgetState,
                    budgetSettings = budgetSettings,
                    currencyCode = currencyCode,
                    bigVariant = true,
                    modifier = Modifier.fillMaxWidth(),
                    startDate = startDate,
                    finishDate = finishDate
                )
            } else {
                // Value display with cursor when typing
                ValueDisplay(
                    input = input,
                    currencyFormat = currencyFormat
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Comment button - enabled only when there's input
            AssistChip(
                onClick = onCommentClick,
                label = { Text("Comentario") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                enabled = input.isNotEmpty()
            )
        }
    }
}

/**
 * Value display with blinking cursor.
 * Shows formatted value when typing, or just cursor when idle.
 */
@Composable
private fun ValueDisplay(
    input: String,
    currencyFormat: NumberFormat
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursorBlink")
    
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(530, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val displayText = remember(input, currencyFormat) {
        when {
            input.isEmpty() -> ""
            input == "." -> currencyFormat.format(BigDecimal.ZERO) + "."
            else -> try {
                val value = input.toBigDecimalOrNull() ?: BigDecimal.ZERO
                currencyFormat.format(value)
            } catch (e: Exception) {
                input
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        // The value text
        Text(
            text = displayText,
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )

        // Blinking cursor - always visible
        Box(
            modifier = Modifier
                .padding(start = 2.dp)
                .width(3.dp)
                .height(48.dp)
                .alpha(cursorAlpha)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/**
 * Wrapper composable that connects ViewModel to pure BudgetScreen.
 * Use this in production code.
 */
@Composable
fun BudgetScreenWithViewModel(
    viewModel: BudgetViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Show rollover dialog if needed
    if (uiState.showRolloverDialog) {
        RolloverDialog(
            remainingAmount = uiState.remainingFromPreviousPeriod,
            currencyCode = uiState.budgetSettings?.currencyCode ?: "USD",
            onSplitEqually = {
                viewModel.onEvent(BudgetUiEvent.OnRolloverSplitEqually(uiState.remainingFromPreviousPeriod))
            },
            onCarryToNextDay = {
                viewModel.onEvent(BudgetUiEvent.OnRolloverCarryToNextDay(uiState.remainingFromPreviousPeriod))
            },
            onDismiss = {
                viewModel.onEvent(BudgetUiEvent.OnDismissRolloverDialog)
            }
        )
    }

    BudgetScreen(
        uiState = uiState,
        onDeleteTransaction = { transaction ->
            viewModel.onEvent(BudgetUiEvent.OnDeleteTransaction(transaction))
        },
        onCommentClick = {
            // TODO: Open comment input dialog/sheet
        },
        modifier = modifier
    )
}

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun BudgetScreenPreview_Typing() {
    MinusTheme {
        BudgetScreen(
            uiState = BudgetUiState(
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("500.00"),
                    period = BudgetPeriod.DAILY,
                    startDate = LocalDate.now(),
                    currencyCode = "USD"
                ),
                budgetState = BudgetState(
                    remainingToday = BigDecimal("45.50"),
                    totalSpentToday = BigDecimal("12.50"),
                    dailyBudget = BigDecimal("58.00"),
                    daysRemaining = 15,
                    progress = 0.21f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("500.00"),
                    totalSpentInPeriod = BigDecimal("12.50")
                ),
                transactions = emptyList(),
                numpadInput = "20",
                isNumpadValid = true
            ),
            onDeleteTransaction = {},
            onCommentClick = {}
        )
    }
}

@Preview(showBackground = true, device = "id:pixel_5")
@Composable
fun BudgetScreenPreview_Idle() {
    MinusTheme {
        BudgetScreen(
            uiState = BudgetUiState(
                budgetSettings = BudgetSettings(
                    totalBudget = BigDecimal("500.00"),
                    period = BudgetPeriod.WEEKLY,
                    startDate = LocalDate.now(),
                    currencyCode = "USD"
                ),
                budgetState = BudgetState(
                    remainingToday = BigDecimal("45.50"),
                    totalSpentToday = BigDecimal("12.50"),
                    dailyBudget = BigDecimal("58.00"),
                    daysRemaining = 15,
                    progress = 0.21f,
                    isOverBudget = false,
                    totalBudget = BigDecimal("500.00"),
                    totalSpentInPeriod = BigDecimal("12.50")
                ),
                transactions = listOf(
                    Transaction(
                        id = 1,
                        amount = BigDecimal("12.50"),
                        comment = "Lunch",
                        date = LocalDateTime.now().minusHours(2)
                    )
                ),
                numpadInput = "",
                isNumpadValid = false
            ),
            onDeleteTransaction = {},
            onCommentClick = {}
        )
    }
}
