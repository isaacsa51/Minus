package com.serranoie.app.minus.presentation.ui.theme.component.date

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.prettyDate
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat

@Composable
fun HistoryDateDivider(
    date: LocalDate?,
    isExpanded: Boolean = true,
    onToggleClick: () -> Unit = {},
    totalAmount: BigDecimal? = null,
    currencyCode: String = "",
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onToggleClick,
                interactionSource = interactionSource,
                indication = null
            )
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(
                    R.string.expand
                ),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
            )

            Text(
                text = prettyDate(
                    date?.atStartOfDay(),
                    forceShowDate = true,
                    showTime = false,
                    human = true
                ),
                style = MaterialTheme.typography.labelMediumCondensed,
                color = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(
            visible = totalAmount != null && !isExpanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 150)),
            exit = fadeOut(animationSpec = tween(durationMillis = 150))
        ) {
            val currencyFormat = symbolOnlyCurrencyFormat(currencyCode)
            DayTotalItem(
                total = totalAmount!!,
                currencyFormat = currencyFormat,
                modifier = Modifier,
                showLabel = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DayPreviewDividerExpanded() {
    MinusTheme {
        HistoryDateDivider(
            date = LocalDate.now(),
            isExpanded = true,
            totalAmount = BigDecimal("150.00"),
            currencyCode = "USD"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DayPreviewDividerCollapsed() {
    MinusTheme {
        HistoryDateDivider(
            date = LocalDate.now(),
            isExpanded = false,
            totalAmount = BigDecimal("3420.25"),
            currencyCode = "USD"
        )
    }
}
