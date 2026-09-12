package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.rounded.NextPlan
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.colorEditor
import com.serranoie.app.minus.presentation.ui.theme.colorOnEditor
import com.serranoie.app.minus.presentation.ui.theme.colorPrimary
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import java.math.BigDecimal

@Composable
fun DailySurplusDialog(
    surplusAmount: BigDecimal,
    currencyCode: String,
    onAddToToday: () -> Unit,
    onSpread: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (LocalInspectionMode.current) {
        DailySurplusDialogContent(surplusAmount, currencyCode, onAddToToday, onSpread, onDismiss)
    } else {
        Dialog(onDismissRequest = onDismiss) {
            DailySurplusDialogContent(surplusAmount, currencyCode, onAddToToday, onSpread, onDismiss)
        }
    }
}

@Composable
private fun DailySurplusDialogContent(
    surplusAmount: BigDecimal,
    currencyCode: String,
    onAddToToday: () -> Unit,
    onSpread: () -> Unit,
    onDismiss: () -> Unit,
) {
    val formattedSurplus = symbolOnlyCurrencyFormat(currencyCode).format(surplusAmount)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorEditor),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.daily_surplus_dialog_title),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = colorOnEditor,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(colorPrimary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Savings,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(0.52f),
                        tint = colorPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.daily_surplus_dialog_question, formattedSurplus),
                style = MaterialTheme.typography.bodyMedium,
                color = colorOnEditor.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            RolloverActionRow(
                icon = Icons.AutoMirrored.Rounded.NextPlan,
                title = stringResource(R.string.daily_surplus_add_to_today_title),
                description = stringResource(R.string.daily_surplus_add_to_today_desc, formattedSurplus),
                highlighted = true,
                onClick = onAddToToday
            )

            Spacer(modifier = Modifier.height(10.dp))

            RolloverActionRow(
                icon = Icons.AutoMirrored.Rounded.CallSplit,
                title = stringResource(R.string.daily_surplus_spread_title),
                description = stringResource(R.string.daily_surplus_spread_desc, formattedSurplus),
                highlighted = false,
                onClick = onSpread
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = colorOnEditor.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmallEmphasized,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DailySurplusDialogPreview() {
    MinusTheme {
        DailySurplusDialog(
            surplusAmount = BigDecimal("42.50"),
            currencyCode = "USD",
            onAddToToday = {},
            onSpread = {},
            onDismiss = {},
        )
    }
}
