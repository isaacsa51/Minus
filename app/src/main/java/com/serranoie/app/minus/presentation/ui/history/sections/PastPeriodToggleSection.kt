package com.serranoie.app.minus.presentation.ui.history.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.WavyDivider
import java.math.BigDecimal
import java.time.LocalDate

internal fun LazyListScope.pastPeriodToggleSection(
    groupedPastTransactions: Map<LocalDate?, List<Transaction>>,
    showPastPeriod: Boolean,
    onToggleShowPastPeriod: () -> Unit,
) {
    if (groupedPastTransactions.isEmpty()) return

    item("wavy-divider") {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    onToggleShowPastPeriod()
                },
        ) {
            WavyDivider(
                text = if (showPastPeriod) "Ocultar gastos del periodo pasado" else "Mostrar gastos del periodo pasado",
                horizontalPadding = 0.dp,
                amplitude = 4f,
                wavelength = 45f,
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun PastPeriodToggleSectionPreview() {
    val today = LocalDate.now()
    val pastDate = today.minusDays(20)
    val tx = Transaction(
        id = 1L,
        amount = BigDecimal("30.00"),
        comment = "Lunch",
        date = pastDate.atStartOfDay(),
        isDeleted = false,
    )
    MinusTheme {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            pastPeriodToggleSection(
                groupedPastTransactions = mapOf(pastDate to listOf(tx)),
                showPastPeriod = false,
                onToggleShowPastPeriod = {},
            )
        }
    }
}
