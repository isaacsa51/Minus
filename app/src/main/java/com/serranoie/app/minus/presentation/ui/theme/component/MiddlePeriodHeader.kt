package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

@Composable
fun MiddlePeriodHeader(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onShowPastPeriods: () -> Unit = {},
    historyIconModifier: Modifier = Modifier,
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
            Text(
                text = stringResource(R.string.analytics_title),
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
            )
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


@Preview(name = "MiddlePeriodHeader")
@Composable
private fun PreviewMiddlePeriodHeader() {
    MinusTheme {
        Surface {
            MiddlePeriodHeader()
        }
    }
}
