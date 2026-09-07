package com.serranoie.app.wear.minus.presentation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.serranoie.app.wear.minus.presentation.theme.MinusTheme

@Composable
internal fun WearBudgetPill(
    amountText: String,
    statusLabel: String,
    progress: Float,
    isOverBudget: Boolean,
    modifier: Modifier = Modifier,
) {
    val target = if (isOverBudget) 1f else progress.coerceIn(0f, 1f)
    val animatedFill by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 500),
        label = "budgetPillFill",
    )

    val statusColor = budgetStatusColor(target)
    val fillColor = lerp(statusColor, Color.Black, 0.32f)
    val trackColor = fillColor.copy(alpha = 0.20f)
    val contentColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(animatedFill)
                .clip(RoundedCornerShape(topEndPercent = 100, bottomEndPercent = 100))
                .background(fillColor),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = amountText,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun budgetStatusColor(t: Float): Color {
    val good = Color(0xFF81C784)
    val notGood = Color(0xFFFFB74D)
    val bad = Color(0xFFE57373)
    val p = t.coerceIn(0f, 1f)
    return if (p <= 0.5f) {
        lerp(good, notGood, p / 0.5f)
    } else {
        lerp(notGood, bad, (p - 0.5f) / 0.5f)
    }
}

@Preview(device = "id:wearos_small_round", showBackground = true, showSystemUi = true)
@Composable
private fun WearBudgetPillPreview() {
    MinusTheme {
        WearBudgetPill(
            amountText = "$42.50",
            statusLabel = "left today",
            progress = 0.35f,
            isOverBudget = false,
        )
    }
}

@Preview(device = "id:wearos_small_round", showBackground = true, showSystemUi = true)
@Composable
private fun WearBudgetPillMidPreview() {
    MinusTheme {
        WearBudgetPill(
            amountText = "$18.00",
            statusLabel = "left today",
            progress = 0.7f,
            isOverBudget = false,
        )
    }
}

@Preview(device = "id:wearos_small_round", showBackground = true, showSystemUi = true)
@Composable
private fun WearBudgetPillOverPreview() {
    MinusTheme {
        WearBudgetPill(
            amountText = "-$12.00",
            statusLabel = "over budget",
            progress = 1f,
            isOverBudget = true,
        )
    }
}
