package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

data class BarSegment(
    val weight: Float,
    val color: Color
)

@Composable
fun LinearSavingBar(
    segments: List<BarSegment>,
    modifier: Modifier = Modifier,
    gap: Int = 1,
    secondarySegments: List<BarSegment> = emptyList(),
    separatorWeight: Float = 0f
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .height(10.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(gap.dp)
        ) {
            segments.forEach { segment ->
                if (segment.weight > 0) {
                    Box(
                        modifier = Modifier
                            .weight(segment.weight.coerceAtLeast(0.0001f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(segment.color)
                    )
                }
            }

            if (separatorWeight > 0 || secondarySegments.isNotEmpty()) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                )
            }

            secondarySegments.forEach { segment ->
                if (segment.weight > 0) {
                    Box(
                        modifier = Modifier
                            .weight(segment.weight.coerceAtLeast(0.0001f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(segment.color)
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Preview(showBackground = true)
@Composable
private fun LinearSavingBarPreview() {
    MinusTheme {
        LinearSavingBar(
            segments = listOf(
                BarSegment(weight = 0.5f, color = Color.Green),
                BarSegment(weight = 0.3f, color = Color.Yellow),
                BarSegment(weight = 0.2f, color = Color.Red)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(16.dp),
            secondarySegments = listOf(
                BarSegment(weight = 0.1f, color = Color.Blue)
            ),
            separatorWeight = 0.05f
        )
    }
}
