@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.theme.component.numpad

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.LeftoverChoice
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.colorButton
import com.serranoie.app.minus.presentation.ui.theme.colorOnButton
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.MorphCornerShape
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.toShape
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleLargeCondensed
import com.serranoie.app.minus.presentation.util.Utils.confirmFeedback
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal
import java.math.RoundingMode

fun leftoverChoiceTag(choice: LeftoverChoice): String = "leftover_choice_${choice.name.lowercase()}"

@Composable
fun LeftoverChoiceList(
    amount: BigDecimal,
    remainingToday: BigDecimal,
    dailyBudget: BigDecimal,
    daysRemaining: Int,
    currencyCode: String,
    selected: LeftoverChoice,
    onSelect: (LeftoverChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val currencyFormat = remember(currencyCode) { symbolOnlyCurrencyFormat(currencyCode) }
    val currencySymbol =
        remember(currencyCode) { SupportedCurrency.findByCode(currencyCode)?.symbol.orEmpty() }
    val isLargeCurrency = currencySymbol.length > 2 || currencyCode.length > 3
    val amountStyle = MaterialTheme.typography.titleLargeEmphasized
    val symbolCondensedSpanStyle = MaterialTheme.typography.titleLargeCondensed.toSpanStyle()
    val share = amount.divide(BigDecimal(daysRemaining.coerceAtLeast(1)), 2, RoundingMode.HALF_UP)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(BUTTON_GAP)
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        LeftoverChoice.entries.forEachIndexed { index, option ->
            val isSelected = option == selected
            val position =
                if (index == 0) PaddedListItemPosition.First else PaddedListItemPosition.Last
            val selection by animateFloatAsState(
                if (isSelected) 1f else 0f,
                label = "leftoverChoice"
            )
            val colors = MaterialTheme.colorScheme
            val today = remainingToday + if (option == LeftoverChoice.SPREAD) share else amount
            val perDay = if (option == LeftoverChoice.SPREAD) dailyBudget + share else dailyBudget
            CustomPaddedListItem(
                onClick = {
                    view.confirmFeedback()
                    onSelect(option)
                },
                position = position,
                customShape = MorphCornerShape(
                    position.toShape(),
                    MaterialTheme.shapes.extraLarge,
                    selection
                ),
                background = lerp(colorButton, colors.secondaryContainer, selection),
                contentColor = lerp(colorOnButton, colors.onSecondaryContainer, selection),
                modifier = Modifier
                    .weight(1f)
                    .testTag(leftoverChoiceTag(option))
                    .semantics(mergeDescendants = true) {
                        this.selected = isSelected
                        role = Role.RadioButton
                    },
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    lerp(
                                        colors.secondaryContainer,
                                        colors.onSecondaryContainer,
                                        selection
                                    ),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = when (option) {
                                    LeftoverChoice.SPREAD -> Icons.Rounded.CalendarViewWeek
                                    LeftoverChoice.CARRY -> Icons.AutoMirrored.Rounded.Redo
                                },
                                contentDescription = null,
                                tint = lerp(
                                    colors.onSecondaryContainer,
                                    colors.secondaryContainer,
                                    selection
                                ),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(
                                when (option) {
                                    LeftoverChoice.SPREAD -> R.string.split_mode_dynamic
                                    LeftoverChoice.CARRY -> R.string.split_mode_carry_over
                                }
                            ),
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Text(
                        text = stringResource(
                            when (option) {
                                LeftoverChoice.SPREAD -> R.string.leftover_choice_spread_desc
                                LeftoverChoice.CARRY -> R.string.leftover_choice_carry_desc
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.censor(),
                    )

                    Column {
                        val amountText = currencyFormat.format(today)
                        val amountAnnotated =
                            if (isLargeCurrency && amountText.startsWith(currencySymbol)) {
                                buildAnnotatedString {
                                    withStyle(
                                        symbolCondensedSpanStyle.copy(
                                            fontSize = amountStyle.fontSize * 0.75f,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    ) {
                                        append(currencySymbol)
                                    }
                                    withStyle(SpanStyle(fontWeight = FontWeight.Light)) {
                                        append(amountText.removePrefix(currencySymbol))
                                    }
                                }
                            } else {
                                AnnotatedString(amountText)
                            }

                        Text(
                            text = amountAnnotated,
                            style = amountStyle,
                            maxLines = 1,
                            modifier = Modifier
                                .basicMarquee()
                                .censor(),
                        )

                        val outcomeText = buildAnnotatedString {
                            if (daysRemaining > 1) {
                                append(
                                    when (option) {
                                        LeftoverChoice.SPREAD -> stringResource(
                                            R.string.leftover_choice_spread_outcome,
                                            currencyFormat.format(perDay),
                                        )

                                        LeftoverChoice.CARRY -> stringResource(
                                            R.string.leftover_choice_carry_outcome,
                                            currencyFormat.format(perDay),
                                        )
                                    }
                                )
                            }
                        }
                        Text(
                            text = outcomeText,
                            style = MaterialTheme.typography.labelSmallCondensed,
                            color = LocalContentColor.current.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.censor(),
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Spread Choice - USD", showBackground = true, widthDp = 300, heightDp = 360)
@Composable
private fun LeftoverChoiceListSpreadPreview() {
    MinusTheme {
        Surface {
            LeftoverChoiceList(
                amount = BigDecimal("25.00"),
                remainingToday = BigDecimal("100.00"),
                dailyBudget = BigDecimal("100.00"),
                daysRemaining = 8,
                currencyCode = "USD",
                selected = LeftoverChoice.SPREAD,
                onSelect = {},
            )
        }
    }
}

@Preview(name = "Carry Choice - EUR", showBackground = true, widthDp = 300, heightDp = 360)
@Composable
private fun LeftoverChoiceListCarryPreview() {
    MinusTheme {
        Surface {
            LeftoverChoiceList(
                amount = BigDecimal("50.00"),
                remainingToday = BigDecimal("80.00"),
                dailyBudget = BigDecimal("60.00"),
                daysRemaining = 14,
                currencyCode = "EUR",
                selected = LeftoverChoice.CARRY,
                onSelect = {},
            )
        }
    }
}

@Preview(name = "Last Day Scenario - JPY", showBackground = true, widthDp = 300, heightDp = 360)
@Composable
private fun LeftoverChoiceListLastDayPreview() {
    MinusTheme {
        Surface {
            LeftoverChoiceList(
                amount = BigDecimal("1500"),
                remainingToday = BigDecimal("4000"),
                dailyBudget = BigDecimal("5000"),
                daysRemaining = 1,
                currencyCode = "JPY",
                selected = LeftoverChoice.SPREAD,
                onSelect = {},
            )
        }
    }
}

@Preview(name = "Long Currency - MAD", showBackground = true, widthDp = 300, heightDp = 360)
@Composable
private fun LeftoverChoiceListLongCurrencyPreview() {
    MinusTheme {
        Surface {
            LeftoverChoiceList(
                amount = BigDecimal("120.00"),
                remainingToday = BigDecimal("350.00"),
                dailyBudget = BigDecimal("200.00"),
                daysRemaining = 5,
                currencyCode = "MAD",
                selected = LeftoverChoice.CARRY,
                onSelect = {},
            )
        }
    }
}

