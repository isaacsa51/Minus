package com.serranoie.app.minus.presentation.ui.theme.component.expense

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.serranoie.app.minus.presentation.ui.theme.component.date.DayTotalItem
import com.serranoie.app.minus.presentation.ui.theme.labelMediumCondensed
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import java.math.BigDecimal

@Composable
fun RecurrentPaymentsDivider(
    title: String,
    isExpanded: Boolean,
    onToggleClick: () -> Unit,
    itemCount: Int,
    isSecondary: Boolean = false,
    totalAmount: BigDecimal? = null,
    currencyCode: String = "",
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val color = if (isSecondary) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onToggleClick, interactionSource = interactionSource, indication = null
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
                tint = color,
                modifier = Modifier
            )

            Text(
                text = title, style = MaterialTheme.typography.labelMediumCondensed, color = color
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedContent(
                targetState = totalAmount != null && !isExpanded,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(
                        animationSpec = tween(150)
                    )
                },
                label = "RecurrentDividerAmountCountSwap"
            ) { showAmount ->
                if (showAmount) {
                    val currencyFormat = symbolOnlyCurrencyFormat(currencyCode)
                    DayTotalItem(
                        total = totalAmount!!,
                        currencyFormat = currencyFormat,
                        modifier = Modifier,
                        showLabel = false,
                    )
                } else {
                    Text(
                        text = "$itemCount",
                        style = MaterialTheme.typography.labelMediumCondensed,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun RecurrentPaymentsDividerPreview() {
    MinusTheme {
        RecurrentPaymentsDivider(
            title = stringResource(R.string.recurrent_payments_divider_title_upcoming),
            isExpanded = false,
            onToggleClick = {},
            itemCount = 5,
            isSecondary = false,
            totalAmount = BigDecimal("123.45"),
            currencyCode = "USD",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
