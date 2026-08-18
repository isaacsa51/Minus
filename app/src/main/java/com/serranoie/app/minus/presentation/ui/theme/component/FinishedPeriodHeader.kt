package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.analytics.Size
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.util.combineColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FinishedPeriodHeader(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    hasSpends: Boolean = false,
    isOverBudget: Boolean = false,
) {
    val localDensity = LocalDensity.current
    val statusBarHeight = LocalWindowInsets.current.calculateTopPadding()

    var headerSize by remember { mutableStateOf(Size(0.dp, 0.dp)) }
    val scroll = with(localDensity) { scrollState.value.toDp() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = statusBarHeight)
            .onGloballyPositioned {
                headerSize = Size(
                    width = with(localDensity) { it.size.width.toDp() },
                    height = with(localDensity) { it.size.height.toDp() })
            },
        contentAlignment = Alignment.Center,
    ) {
        val halfWidth = headerSize.width / 2
        val halfHeight = headerSize.height / 2

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .absoluteOffset(y = scroll * 0.25f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))
            Text(
                text = stringResource(R.string.finished_period_header_title),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            if (!hasSpends) {
                Text(
                    text = stringResource(R.string.finished_period_header_no_spends),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            } else if (isOverBudget) {
                Text(
                    text = stringResource(R.string.finished_period_header_over_budget),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = stringResource(R.string.finished_period_header_summary),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(64.dp))
        }

        val starColor = combineColors(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.surface,
            0.5f,
        )

        val angleStar1 by rememberInfiniteTransition("angleStar1").animateFloat(
            label = "angleStar1",
            initialValue = -20f,
            targetValue = 20f,
            animationSpec = infiniteRepeatable(tween(10000), RepeatMode.Reverse)
        )

        val angleStar2 by rememberInfiniteTransition("angleStar2").animateFloat(
            label = "angleStar2",
            initialValue = -50f,
            targetValue = 50f,
            animationSpec = infiniteRepeatable(tween(18000), RepeatMode.Reverse)
        )

        val angleShape1 by rememberInfiniteTransition("angleShape1").animateFloat(
            label = "angleShape1",
            initialValue = -35f,
            targetValue = 35f,
            animationSpec = infiniteRepeatable(tween(14000), RepeatMode.Reverse)
        )

        val angleShape2 by rememberInfiniteTransition("angleShape2").animateFloat(
            label = "angleShape2",
            initialValue = 40f,
            targetValue = -40f,
            animationSpec = infiniteRepeatable(tween(22000), RepeatMode.Reverse)
        )

        val shapeColor = starColor.copy(alpha = 0.6f)

        Box(
            modifier = Modifier
                .requiredSize(256.dp)
                .absoluteOffset(x = -halfWidth * 1.0f, y = -halfHeight * 0.9f + scroll * 0.15f)
                .rotate(angleShape1)
                .zIndex(-2f)
                .background(color = shapeColor, shape = MaterialShapes.Cookie9Sided.toShape())
        )
        Box(
            modifier = Modifier
                .requiredSize(256.dp)
                .absoluteOffset(x = halfWidth * 1.05f, y = halfHeight * 0.95f + scroll * 0.2f)
                .rotate(angleShape2)
                .zIndex(-2f)
                .background(color = shapeColor, shape = MaterialShapes.Gem.toShape())
        )
    }
}

@Preview(name = "FinishedPeriodHeader")
@Composable
private fun PreviewFinishedPeriodHeader() {
    MinusTheme {
        Surface {
            FinishedPeriodHeader()
        }
    }
}

@Preview(name = "FinishedPeriodHeader - OverBudget")
@Composable
private fun PreviewFinishedPeriodHeaderOverBudget() {
    MinusTheme {
        Surface {
            FinishedPeriodHeader(
                hasSpends = true,
                isOverBudget = true,
            )
        }
    }
}
