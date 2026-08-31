@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.settings.appearance

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.domain.model.ContrastMode
import com.serranoie.app.minus.domain.model.TypographyMode
import com.serranoie.app.minus.presentation.ui.settings.SettingsUiState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.displayLargeCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleLargeCondensed
import com.serranoie.app.minus.presentation.ui.theme.titleMediumCondensed

@Composable
fun AppearanceMockupPager(state: SettingsUiState) {
    val pagerState = rememberPagerState { 3 }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 16.dp
        ) { page ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                when (page) {
                    0 -> MockDashboardScreen(state)
                    1 -> MockNumpadScreen(state)
                    2 -> MockPillScreen(state)
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outlineVariant
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun MockDashboardScreen(state: SettingsUiState) {
    AppearanceMockupContainer(state) { targetState ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MockBudgetDisplay(targetState.currentTypography)
            MockBody()
        }
    }
}

@Composable
private fun MockNumpadScreen(state: SettingsUiState) {
    AppearanceMockupContainer(state) { targetState ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .height(26.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(8.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(Modifier.width(4.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) { }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) { }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "$5",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                style = when (targetState.currentTypography) {
                    "Condensed" -> MaterialTheme.typography.displayLargeCondensed
                    "Expressive" -> MaterialTheme.typography.displayLargeEmphasized
                    else -> MaterialTheme.typography.displayLarge
                }.copy(fontSize = 32.sp)
            )

            Spacer(Modifier.weight(1f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(3f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val rowCounts = listOf(3, 3, 3)
                        rowCounts.forEach { _ ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                                    alpha = 0.5f
                                                ), CircleShape
                                            )
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(2f)
                                    .height(34.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                            alpha = 0.5f
                                        ), CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                            alpha = 0.3f
                                        ), CircleShape
                                    )
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) { }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(100.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MockPillScreen(state: SettingsUiState) {
    AppearanceMockupContainer(state) { targetState ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(30.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            )
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(12.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .width(60.dp)
                            .height(8.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(12.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                CircleShape
                            )
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "9",
                            style = when (targetState.currentTypography) {
                                "Condensed" -> MaterialTheme.typography.titleLargeCondensed
                                "Expressive" -> MaterialTheme.typography.titleLargeEmphasized
                                else -> MaterialTheme.typography.titleLarge
                            },
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(6.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(10.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        RoundedCornerShape(4.dp)
                    )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(0.8f, 1f, 1.2f).forEachIndexed { index, weight ->
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(30.dp)
                            .background(
                                if (index == 3) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.3f
                                ),
                                RoundedCornerShape(8.dp)
                            )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(Color.Transparent, RoundedCornerShape(16.dp))
                    .border(
                        2.dp,
                        MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(8.dp)
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun AppearanceMockupContainer(
    state: SettingsUiState,
    content: @Composable (SettingsUiState) -> Unit
) {
    val darkTheme = when (state.currentTheme) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }
    val typographyMode = when (state.currentTypography) {
        "Condensed" -> TypographyMode.CONDENSED
        "Expressive" -> TypographyMode.EXPRESSIVE
        else -> TypographyMode.SYSTEM
    }
    val contrastMode = when (state.currentContrast) {
        "Medium" -> ContrastMode.MEDIUM
        "High" -> ContrastMode.HIGH
        else -> ContrastMode.NORMAL
    }

    MinusTheme(
        darkTheme = darkTheme,
        dynamicColor = state.isMaterialYouEnabled,
        typographyMode = typographyMode,
        appColorScheme = state.currentColorScheme,
        contrastMode = contrastMode,
        isAmoledEnabled = state.isAmoledEnabled
    ) {
        Surface(
            modifier = Modifier
                .height(340.dp)
                .width(200.dp),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            color = MaterialTheme.colorScheme.surface
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                label = "MockupAnimation"
            ) { targetState ->
                content(targetState)
            }
        }
    }
}

@Composable
private fun MockBudgetDisplay(typography: String) {
    val textStyle = when (typography) {
        "Condensed" -> MaterialTheme.typography.titleMediumCondensed
        "Expressive" -> MaterialTheme.typography.titleMediumEmphasized
        "System" -> TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        else -> MaterialTheme.typography.titleLarge
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        color = MaterialTheme.colorScheme.onSurface,
        contentColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(8.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(4.dp)
                    )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$ 1,234.56",
                color = MaterialTheme.colorScheme.surfaceVariant,
                style = textStyle,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MockBody() {
    val itemsCount = 2
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(12.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        RoundedCornerShape(12.dp)
                    )
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(itemsCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(8.dp)
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(6.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurface,
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
internal fun MockDashboardPreview() {
    MinusTheme {
        MockDashboardScreen(SettingsUiState())
    }
}

@Preview(showBackground = true)
@Composable
internal fun MockNumpadPreview() {
    MinusTheme {
        MockNumpadScreen(SettingsUiState())
    }
}

@Preview(showBackground = true)
@Composable
internal fun MockPillPreview() {
    MinusTheme {
        MockPillScreen(SettingsUiState())
    }
}
