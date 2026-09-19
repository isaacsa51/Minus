package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.censor

@Composable
fun MiddlePeriodHeader(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onShowPastPeriods: () -> Unit = {},
    historyIconModifier: Modifier = Modifier,
    periodLabel: String? = null,
    periodBudget: String? = null,
) {
    val localBottomSheetScrollState = LocalBottomSheetScrollState.current
    val statusBarHeight = LocalWindowInsets.current.calculateTopPadding()
    val topPadding =
        if (localBottomSheetScrollState.topPadding > 0.dp) localBottomSheetScrollState.topPadding else statusBarHeight

    Box(Modifier.padding(top = topPadding)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { onClose() },
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.weight(1F))
            AnimatedContent(
                targetState = periodLabel to periodBudget,
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    fadeIn(tween(500)) togetherWith fadeOut(tween(500)) using SizeTransform(clip = false)
                },
                label = "AnalyticsTitleTransition",
            ) { (label, budget) ->
                if (label != null) {
                    PeriodPill(label = label, budget = budget)
                } else {
                    Text(
                        text = stringResource(R.string.analytics_title),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    )
                }
            }
            Spacer(Modifier.weight(1F))
            IconButton(
                onClick = onShowPastPeriods,
                modifier = historyIconModifier,
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = stringResource(R.string.past_periods_title),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PeriodPill(label: String, budget: String?) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmallEmphasized,
            maxLines = 1,
        )
        if (budget != null) {
            Text(
                modifier = Modifier.censor(),
                text = budget,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Preview(name = "MiddlePeriodHeader")
@Composable
private fun PreviewMiddlePeriodHeader() {
    MinusTheme {
        Surface {
            MiddlePeriodHeader()
        }
    }
}

@Preview(name = "MiddlePeriodHeader - past period")
@Composable
private fun PreviewMiddlePeriodHeaderPastPeriod() {
    MinusTheme {
        Surface {
            MiddlePeriodHeader(
                periodLabel = "15 Aug – 15 Sep 2026",
                periodBudget = "$1,000.00 budget",
            )
        }
    }
}
