@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.editor.receipt

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSplitMode
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.budget.pill.calculateBudgetMetrics
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.font.format.symbolOnlyCurrencyFormat
import com.serranoie.app.minus.presentation.util.harmonizeWithColor
import com.serranoie.app.minus.presentation.util.toPaletteWithTheme
import java.math.BigDecimal
import kotlin.math.roundToInt

private val ShutterAreaHeight = 152.dp
private val SheetCornerRadius = 36.dp

@Composable
fun ReceiptCameraSheet(
    visible: Boolean,
    state: ReceiptCameraUiState,
    startHeightPx: Float,
    budgetState: BudgetState?,
    viewPeriod: BudgetPeriod,
    splitMode: BudgetSplitMode,
    currencyCode: String,
    onClose: () -> Unit,
    onCapture: () -> Unit,
    onPickImage: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleTorch: () -> Unit,
    preview: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inspection = LocalInspectionMode.current
    val progress = remember { Animatable(if (inspection && visible) 1f else 0f) }
    var shown by remember { mutableStateOf(inspection && visible) }

    LaunchedEffect(visible) {
        if (visible) {
            shown = true
            progress.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
            )
        } else if (shown) {
            progress.animateTo(0f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
            shown = false
        }
    }

    if (!shown) return

    BackHandler(enabled = visible, onBack = onClose)

    ReceiptCameraSheetContent(
        progress = progress.value,
        state = state,
        startHeightPx = startHeightPx,
        budgetState = budgetState,
        viewPeriod = viewPeriod,
        splitMode = splitMode,
        currencyCode = currencyCode,
        onClose = onClose,
        onCapture = onCapture,
        onPickImage = onPickImage,
        onSwitchCamera = onSwitchCamera,
        onToggleTorch = onToggleTorch,
        preview = { previewModifier -> if (visible) preview(previewModifier) },
        modifier = modifier,
    )
}

@Composable
fun ReceiptCameraSheetContent(
    progress: Float,
    state: ReceiptCameraUiState,
    startHeightPx: Float,
    budgetState: BudgetState?,
    viewPeriod: BudgetPeriod,
    splitMode: BudgetSplitMode,
    currencyCode: String,
    onClose: () -> Unit,
    onCapture: () -> Unit,
    preview: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    onPickImage: () -> Unit = {},
    onSwitchCamera: () -> Unit = {},
    onToggleTorch: () -> Unit = {},
) {
    val density = LocalDensity.current
    val revealed = progress.coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent()
                }
            }
            .background(Color.Black.copy(alpha = revealed)),
    ) {
        val fullHeightPx = constraints.maxHeight.toFloat()
        val shutterAreaPx = with(density) { ShutterAreaHeight.toPx() }
        val targetHeightPx = (fullHeightPx - shutterAreaPx).coerceAtLeast(startHeightPx)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .layout { measurable, constraints ->
                    val height = lerp(startHeightPx, targetHeightPx, progress).roundToInt().coerceAtLeast(0)
                    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                    layout(placeable.width, height) { placeable.place(0, 0) }
                }
                .clip(RoundedCornerShape(bottomStart = SheetCornerRadius, bottomEnd = SheetCornerRadius))
                .background(Color.Black),
        ) {
            preview(Modifier.fillMaxSize())
            FramingSquare(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = revealed })

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 8.dp, end = 16.dp, top = 8.dp)
                    .graphicsLayer { alpha = revealed },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.receipt_camera_close),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
                DetectionPill(
                    state = state,
                    budgetState = budgetState,
                    viewPeriod = viewPeriod,
                    splitMode = splitMode,
                    currencyCode = currencyCode,
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.hasFlash) {
                FlashToggle(
                    torchOn = state.torchOn,
                    onToggle = onToggleTorch,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .graphicsLayer { alpha = revealed },
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(ShutterAreaHeight)
                .navigationBarsPadding()
                .graphicsLayer { alpha = revealed },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(36.dp),
            ) {
                SideButton(
                    icon = Icons.Rounded.PhotoLibrary,
                    description = stringResource(R.string.receipt_camera_gallery),
                    enabled = !state.isCapturing,
                    onClick = onPickImage,
                )
                ShutterButton(isCapturing = state.isCapturing, onCapture = onCapture)
                if (state.canSwitchCamera) {
                    SideButton(
                        icon = Icons.Rounded.Cameraswitch,
                        description = stringResource(R.string.receipt_camera_switch),
                        enabled = !state.isCapturing,
                        onClick = onSwitchCamera,
                    )
                } else {
                    Spacer(Modifier.size(SideButtonSize))
                }
            }
        }
    }
}

@Composable
private fun SideButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SideButtonSize)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun FlashToggle(
    torchOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container by animateColorAsState(
        if (torchOn) Color.White else Color.Black.copy(alpha = 0.45f),
        tween(180),
        label = "flashContainer",
    )
    val content by animateColorAsState(
        if (torchOn) Color(0xFF1B1B1B) else Color.White,
        tween(180),
        label = "flashContent",
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(container)
            .toggleable(value = torchOn, role = Role.Switch, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (torchOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
            contentDescription = stringResource(R.string.receipt_camera_flash),
            tint = content,
            modifier = Modifier.size(24.dp),
        )
    }
}

private val SideButtonSize = 56.dp

@Composable
private fun DetectionPill(
    state: ReceiptCameraUiState,
    budgetState: BudgetState?,
    viewPeriod: BudgetPeriod,
    splitMode: BudgetSplitMode,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    val detectedTotal = state.detected?.total
    val colors = detectionPillColors(
        detectedTotal = detectedTotal,
        failed = state.captureFailed,
        budgetState = budgetState,
        viewPeriod = viewPeriod,
        splitMode = splitMode,
    )
    val container by animateColorAsState(colors.first, tween(220), label = "receiptPillContainer")
    val content by animateColorAsState(colors.second, tween(220), label = "receiptPillContent")
    val currencyFormat = remember(currencyCode) {
        symbolOnlyCurrencyFormat(
            currencyCode,
            minimumFractionDigits = SupportedCurrency.findByCode(currencyCode)?.defaultFractionDigits ?: 2,
        )
    }

    val label = when {
        state.captureFailed -> stringResource(R.string.receipt_camera_retry)
        detectedTotal != null -> stringResource(R.string.receipt_camera_detected, currencyFormat.format(detectedTotal))
        else -> stringResource(R.string.receipt_camera_point_at_total)
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 20.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "receiptPillLabel",
        ) { text ->
            Text(
                text = text,
                color = content,
                style = MaterialTheme.typography.titleMediumEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun detectionPillColors(
    detectedTotal: BigDecimal?,
    failed: Boolean,
    budgetState: BudgetState?,
    viewPeriod: BudgetPeriod,
    splitMode: BudgetSplitMode,
): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    if (failed) return scheme.errorContainer to scheme.onErrorContainer
    if (detectedTotal == null) return scheme.surfaceContainerHighest to scheme.onSurface
    if (budgetState == null) return scheme.primaryContainer to scheme.onPrimaryContainer

    val isDark = isSystemInDarkTheme()
    val status = MinusTheme.budgetStatus
    val primary = scheme.primary
    return remember(budgetState, viewPeriod, splitMode, detectedTotal, status, primary, isDark) {
        val progress = calculateBudgetMetrics(budgetState, viewPeriod, splitMode, detectedTotal).spendProgress
        val statusColor = combineColors(listOf(status.good, status.notGood, status.bad), progress)
        val palette = toPaletteWithTheme(harmonizeWithColor(statusColor, primary), isDark)
        palette.container to palette.onContainer
    }
}

@Composable
private fun ShutterButton(isCapturing: Boolean, onCapture: () -> Unit) {
    val description = stringResource(R.string.receipt_camera_capture)
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(Color(0xFFDADADA))
            .clickable(enabled = !isCapturing, onClick = onCapture)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isCapturing) {
            LoadingIndicator(modifier = Modifier.size(40.dp), color = Color(0xFF1B1B1B))
        }
    }
}

@Composable
private fun FramingSquare(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val side = minOf(size.width, size.height) * RECEIPT_FRAME_FRACTION
        val left = center.x - side / 2f
        val top = center.y - side / 2f
        val arm = FrameArmLength.toPx()
        val radius = FrameCornerRadius.toPx()
        val corner = Path().apply {
            moveTo(left, top + arm)
            lineTo(left, top + radius)
            arcTo(Rect(left, top, left + radius * 2, top + radius * 2), 180f, 90f, false)
            lineTo(left + arm, top)
        }
        val stroke = Stroke(width = FrameStrokeWidth.toPx(), cap = StrokeCap.Round)
        listOf(1f to 1f, -1f to 1f, 1f to -1f, -1f to -1f).forEach { (sx, sy) ->
            scale(scaleX = sx, scaleY = sy, pivot = center) {
                drawPath(corner, Color.White, style = stroke)
            }
        }
    }
}

private val FrameArmLength = 36.dp
private val FrameCornerRadius = 20.dp
private val FrameStrokeWidth = 4.dp
