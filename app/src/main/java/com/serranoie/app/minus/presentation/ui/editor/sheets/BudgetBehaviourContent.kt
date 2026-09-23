@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.editor.sheets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.RemainingBudgetStrategy
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListGroup
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.util.Utils.confirmFeedback
import com.serranoie.app.minus.presentation.util.censor
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal

@Composable
internal fun BudgetBehaviourContent(
    strategy: RemainingBudgetStrategy,
    splitMode: BudgetSplitMode,
    exampleLeftover: BigDecimal,
    periodDays: Int,
    currencyCode: String,
    onStrategySelected: (RemainingBudgetStrategy) -> Unit,
    onSplitModeSelected: (BudgetSplitMode) -> Unit,
    applyLabel: String,
    onBack: () -> Unit,
    onApply: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                Text(
                    text = stringResource(R.string.budget_behaviour_title),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.budget_behaviour_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            PaddedListGroup(title = stringResource(R.string.budget_behaviour_surplus_title)) {
                SurplusStrategyCard(
                    strategy = strategy,
                    onStrategySelected = onStrategySelected,
                    exampleLeftover = exampleLeftover,
                    periodDays = periodDays,
                    currencyCode = currencyCode,
                )
            }

            BehaviourOptionGroup(
                title = stringResource(R.string.split_mode_label),
                options = listOf(BudgetSplitMode.DYNAMIC, BudgetSplitMode.CARRY_OVER, BudgetSplitMode.STATIC),
                selected = splitMode,
                onSelect = onSplitModeSelected,
                optionTag = ::budgetSplitModeOptionTag,
                optionTitle = { option ->
                    stringResource(
                        when (option) {
                            BudgetSplitMode.STATIC -> R.string.split_mode_static
                            BudgetSplitMode.DYNAMIC -> R.string.split_mode_dynamic
                            BudgetSplitMode.CARRY_OVER -> R.string.split_mode_carry_over
                        }
                    )
                },
                optionDescription = { option ->
                    stringResource(
                        when (option) {
                            BudgetSplitMode.STATIC -> R.string.split_mode_static_desc
                            BudgetSplitMode.DYNAMIC -> R.string.split_mode_dynamic_desc
                            BudgetSplitMode.CARRY_OVER -> R.string.split_mode_carry_over_desc
                        }
                    )
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            FilledTonalButton(
                onClick = onBack,
                shape = CircleShape.copy(topEnd = CornerSize(8.dp), bottomEnd = CornerSize(8.dp)),
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Button(
                onClick = onApply,
                shape = CircleShape.copy(topStart = CornerSize(8.dp), bottomStart = CornerSize(8.dp)),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .testTag(BUDGET_PERIOD_APPLY_BUTTON_TAG),
            ) {
                Text(applyLabel, style = MaterialTheme.typography.labelMediumEmphasized)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SurplusStrategyCard(
    strategy: RemainingBudgetStrategy,
    onStrategySelected: (RemainingBudgetStrategy) -> Unit,
    exampleLeftover: BigDecimal,
    periodDays: Int,
    currencyCode: String,
) {
    val view = LocalView.current
    val currencyFormat = remember(currencyCode) { symbolOnlyCurrencyFormat(currencyCode) }
    val strategies = listOf(
        RemainingBudgetStrategy.ASK_ALWAYS,
        RemainingBudgetStrategy.SPLIT_EQUALLY,
        RemainingBudgetStrategy.ADD_TO_FIRST_DAY,
    )
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                strategies.forEachIndexed { index, option ->
                    ToggleButton(
                        checked = option == strategy,
                        onCheckedChange = {
                            view.confirmFeedback()
                            onStrategySelected(option)
                        },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            strategies.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton }
                            .testTag(budgetStrategyOptionTag(option)),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = Color.Transparent,
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            text = stringResource(
                                when (option) {
                                    RemainingBudgetStrategy.ASK_ALWAYS -> R.string.budget_behaviour_surplus_ask
                                    RemainingBudgetStrategy.SPLIT_EQUALLY -> R.string.budget_behaviour_surplus_spread
                                    RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> R.string.budget_behaviour_surplus_first_day
                                }
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                AnimatedContent(
                    targetState = strategy,
                    modifier = Modifier.weight(1f),
                    transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                    label = "surplusPreview",
                ) { shown ->
                    val leftover = currencyFormat.format(exampleLeftover)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (shown) {
                                RemainingBudgetStrategy.ASK_ALWAYS ->
                                    stringResource(R.string.budget_behaviour_surplus_preview_ask)

                                RemainingBudgetStrategy.SPLIT_EQUALLY -> stringResource(
                                    R.string.budget_behaviour_surplus_preview_spread,
                                    leftover,
                                    pluralStringResource(R.plurals.days, periodDays, periodDays),
                                )

                                RemainingBudgetStrategy.ADD_TO_FIRST_DAY ->
                                    stringResource(R.string.budget_behaviour_surplus_preview_first_day, leftover)
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .censor(),
                        )
                        val badge = when (shown) {
                            RemainingBudgetStrategy.ASK_ALWAYS -> null
                            RemainingBudgetStrategy.SPLIT_EQUALLY -> stringResource(
                                R.string.budget_behaviour_surplus_per_day,
                                currencyFormat.format(
                                    if (periodDays > 0) {
                                        exampleLeftover.divide(BigDecimal(periodDays), 2, java.math.RoundingMode.HALF_UP)
                                    } else {
                                        BigDecimal.ZERO
                                    }
                                ),
                            )
                            RemainingBudgetStrategy.ADD_TO_FIRST_DAY -> "+$leftover"
                        }
                        if (badge != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            AccentBadge(text = badge, modifier = Modifier.censor())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> BehaviourOptionGroup(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    optionTag: (T) -> String,
    optionTitle: @Composable (T) -> String,
    optionDescription: @Composable (T) -> String,
) {
    val view = LocalView.current
    PaddedListGroup(title = title, modifier = Modifier.selectableGroup()) {
        options.forEachIndexed { index, option ->
            val isSelected = option == selected
            CustomPaddedListItem(
                onClick = {
                    view.confirmFeedback()
                    onSelect(option)
                },
                position = when {
                    options.size == 1 -> PaddedListItemPosition.Single
                    index == 0 -> PaddedListItemPosition.First
                    index == options.lastIndex -> PaddedListItemPosition.Last
                    else -> PaddedListItemPosition.Middle
                },
                modifier = Modifier
                    .testTag(optionTag(option))
                    .semantics(mergeDescendants = true) {
                        this.selected = isSelected
                        role = Role.RadioButton
                    },
                background = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                ) {
                    Text(text = optionTitle(option), style = MaterialTheme.typography.bodyLargeEmphasized)
                    Text(
                        text = optionDescription(option),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalContentColor.current.copy(alpha = 0.8f),
                    )
                }
                AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(24.dp),
                    )
                }
            }
        }
    }
}
