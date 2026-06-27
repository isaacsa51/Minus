package com.serranoie.app.minus.presentation.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.LocalWindowInsets
import com.serranoie.app.minus.presentation.ui.analytics.Size
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.component.DescriptionButton
import com.serranoie.app.minus.presentation.ui.theme.component.LocalBottomSheetScrollState
import com.serranoie.app.minus.presentation.util.combineColors
import logcat.logcat

private const val TAG = "ISAAC:OnboardingScreen"

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onOnboardingCompleted: () -> Unit = {},
) {
    logcat(TAG) { "OnboardingScreen composed" }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            logcat(TAG) { "OnboardingScreen received effect: $effect" }
            when (effect) {
                OnboardingUiEffect.OnboardingCompleted -> {
                    logcat(TAG) { "OnboardingScreen -> invoking onOnboardingCompleted()" }
                    onOnboardingCompleted()
                }
                is OnboardingUiEffect.OnboardingFailed -> {
                    // Failures are also reflected in [OnboardingUiState.error];
                    // the parent screen (or activity) may surface them.
                }
            }
        }
    }

    OnboardingScreenContent(
        onContinue = {
            logcat(TAG) { "Set Budget tapped -> dispatching OnWelcomeDismissed" }
            viewModel.processIntent(OnboardingUiIntent.OnWelcomeDismissed)
        },
    )
}

@Composable
internal fun OnboardingScreenContent(
    onContinue: () -> Unit = {},
) {
    WelcomeStep(
        onContinue = onContinue,
    )
}

private data class StepItem(
    val number: Int,
    val titleRes: Int,
    val subtitleRes: Int,
)

private val onboardingSteps = listOf(
    StepItem(1, R.string.onboarding_step_1_title, R.string.onboarding_step_1_subtitle),
    StepItem(2, R.string.onboarding_step_2_title, R.string.onboarding_step_2_subtitle),
    StepItem(3, R.string.onboarding_step_3_title, R.string.onboarding_step_3_subtitle),
    StepItem(4, R.string.onboarding_step_4_title, R.string.onboarding_step_4_subtitle),
    StepItem(5, R.string.onboarding_step_5_title, R.string.onboarding_step_5_subtitle),
    StepItem(6, R.string.onboarding_step_6_title, R.string.onboarding_step_6_subtitle),
)

@Composable
private fun WelcomeStep(
    onContinue: () -> Unit = {},
) {
    val localBottomSheetScrollState = LocalBottomSheetScrollState.current
    val statusBarHeight = LocalWindowInsets.current.calculateTopPadding()
    val navigationBarHeight =
        LocalWindowInsets.current.calculateBottomPadding().coerceAtLeast(16.dp)

    val scrollState = rememberScrollState()
    val localDensity = LocalDensity.current
    var pageSize by remember { mutableStateOf(Size(0.dp, 0.dp)) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = if (localBottomSheetScrollState.topPadding > 0.dp) {
                    localBottomSheetScrollState.topPadding
                } else {
                    statusBarHeight
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    pageSize = Size(
                        width = with(localDensity) { it.size.width.toDp() },
                        height = with(localDensity) { it.size.height.toDp() }
                    )
                }
        ) {
            WelcomeBackground(scrollState = scrollState, pageSize = pageSize)

            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.onboarding_welcome_title),
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.onboarding_welcome_intro),
                        style = MaterialTheme.typography.bodyMediumCondensed,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(20.dp))
                    StepGrid(steps = onboardingSteps)

                    Spacer(Modifier.height(24.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = navigationBarHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DescriptionButton(
                        title = { Text(stringResource(R.string.onboarding_set_budget_button)) },
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                        onClick = { onContinue() },
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun WelcomeBackground(
    scrollState: ScrollState,
    pageSize: Size,
) {
    val halfWidth = pageSize.width / 2
    val halfHeight = pageSize.height / 2
    val scroll = with(LocalDensity.current) { scrollState.value.toDp() }

    val starColor = combineColors(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.surface,
        0.5f,
    )

    val infiniteTransition = rememberInfiniteTransition(label = "backgroundTransitions")

    val angle1 by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(10000), RepeatMode.Reverse),
        label = "angle1"
    )

    val angle2 by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(tween(18000), RepeatMode.Reverse),
        label = "angle2"
    )

    val angle3 by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(tween(14000), RepeatMode.Reverse),
        label = "angle3"
    )

    val angle4 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 170f,
        animationSpec = infiniteRepeatable(tween(25000), RepeatMode.Reverse),
        label = "angle4"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Shape 1: Top Right
        Icon(
            modifier = Modifier
                .requiredSize(256.dp)
                .absoluteOffset(x = halfWidth * 0.8f, y = -halfHeight * 0.7f + scroll * 0.35f)
                .rotate(angle1)
                .zIndex(-1f),
            painter = painterResource(R.drawable.shape_soft_star_1),
            tint = starColor,
            contentDescription = null,
        )

        // Shape 2: Bottom Left
        Icon(
            modifier = Modifier
                .requiredSize(256.dp)
                .absoluteOffset(x = -halfWidth * 0.8f, y = halfHeight * 0.6f + scroll * 0.6f)
                .rotate(angle2)
                .zIndex(-1f),
            painter = painterResource(R.drawable.shape_soft_star_2),
            tint = starColor,
            contentDescription = null,
        )

        // Shape 3: Center Left
        Icon(
            modifier = Modifier
                .requiredSize(180.dp)
                .absoluteOffset(x = -halfWidth * 0.9f, y = -halfHeight * 0.2f + scroll * 0.45f)
                .rotate(angle3)
                .zIndex(-1f),
            painter = painterResource(R.drawable.shape_soft_star_1),
            tint = starColor,
            contentDescription = null,
        )

        // Shape 4: Bottom Right
        Icon(
            modifier = Modifier
                .requiredSize(220.dp)
                .absoluteOffset(x = halfWidth * 0.6f, y = halfHeight * 0.2f + scroll * 0.25f)
                .rotate(angle4)
                .zIndex(-1f),
            painter = painterResource(R.drawable.shape_soft_star_2),
            tint = starColor,
            contentDescription = null,
        )
    }
}

@Composable
private fun StepGrid(steps: List<StepItem>) {
    val rows = steps.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { rowSteps ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowSteps.forEach { step ->
                    StepCard(
                        modifier = Modifier.weight(1f),
                        number = step.number,
                        title = stringResource(step.titleRes),
                        description = stringResource(step.subtitleRes),
                    )
                }
                if (rowSteps.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    modifier: Modifier = Modifier,
    number: Int,
    title: String,
    description: String,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingScreenPreview() {
    MinusTheme {
        OnboardingScreenContent()
    }
}
