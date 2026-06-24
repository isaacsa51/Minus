package com.serranoie.app.minus.presentation.ui.history.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.history.RecurrentPaymentsViewMode
import com.serranoie.app.minus.presentation.ui.theme.component.WavyDivider
import com.serranoie.app.minus.presentation.ui.theme.component.expense.SwipeableUpcomingRecurrentItem
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import java.time.LocalDate
import java.text.NumberFormat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.serranoie.app.minus.domain.model.RecurrentFrequency
import java.math.BigDecimal

internal fun LazyListScope.futureRecurrentSection(
    futureRecurrentOutOfPeriod: List<UpcomingRecurrentItem>,
    showOutOfPeriodSubscriptions: Boolean,
    onToggleShowOutOfPeriodSubscriptions: () -> Unit,
    recurrentPaymentsViewMode: RecurrentPaymentsViewMode,
    currencyFormat: NumberFormat,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit,
    onClick: (Transaction) -> Unit,
) {
    if (futureRecurrentOutOfPeriod.isEmpty()) return

    item("future-recurrent-toggle") {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    onToggleShowOutOfPeriodSubscriptions()
                },
        ) {
            WavyDivider(
                text = if (showOutOfPeriodSubscriptions) {
                    "Ocultar subscripciones fuera del periodo"
                } else {
                    "Mostrar subscripciones fuera del periodo"
                },
                horizontalPadding = 0.dp,
                amplitude = 4f,
                wavelength = 45f,
            )
        }
    }

    item("future-recurrent-content") {
        AnimatedVisibility(
            visible = showOutOfPeriodSubscriptions,
            enter = expandVertically(
                animationSpec = tween(300),
                expandFrom = Alignment.Top,
            ) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(
                animationSpec = tween(300),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(animationSpec = tween(300)),
        ) {
            RecurrentItemsContent(
                items = futureRecurrentOutOfPeriod,
                recurrentPaymentsViewMode = recurrentPaymentsViewMode,
                currencyFormat = currencyFormat,
                verticalItem = { _, item, position ->
                    SwipeableUpcomingRecurrentItem(
                        item = item,
                        currencyFormat = currencyFormat,
                        position = position,
                        onDelete = { onDelete(item.transaction) },
                        onEdit = { onEdit(item.transaction) },
                        onClick = { onClick(item.transaction) },
                    )
                },
                horizontalKeyPrefix = "future",
                onClick = onClick,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FutureRecurrentSectionPreview() {
    val today = LocalDate.now()
    val sampleItem = UpcomingRecurrentItem(
        transaction = Transaction(
            id = 1L,
            amount = BigDecimal("99.00"),
            comment = "Spotify",
            date = today.atStartOfDay(),
            isDeleted = false,
            isRecurrent = true,
            recurrentFrequency = RecurrentFrequency.MONTHLY,
        ),
        nextChargeDate = today.plusMonths(2),
        isInCurrentPeriod = false,
    )
    MinusTheme {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            futureRecurrentSection(
                futureRecurrentOutOfPeriod = listOf(sampleItem),
                showOutOfPeriodSubscriptions = true,
                onToggleShowOutOfPeriodSubscriptions = {},
                recurrentPaymentsViewMode = RecurrentPaymentsViewMode.HORIZONTAL_LIST,
                currencyFormat = NumberFormat.getCurrencyInstance(),
                onDelete = {},
                onEdit = {},
                onClick = {},
            )
        }
    }
}
