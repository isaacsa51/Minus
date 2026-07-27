@file:OptIn(ExperimentalFoundationApi::class)

package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.domain.model.SupportedCurrency
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.ui.theme.component.StatCard
import com.serranoie.app.minus.presentation.ui.theme.titleSmallCondensed
import com.serranoie.app.minus.presentation.util.countDays
import com.serranoie.app.minus.presentation.util.prettyDate
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import logcat.logcat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

@Composable
fun BudgetDisplay(
    budget: BigDecimal,
    budgetState: BudgetState?,
    budgetSettings: BudgetSettings?,
    currencyCode: String = "USD",
    bigVariant: Boolean = true,
    modifier: Modifier = Modifier,
    startDate: Date,
    finishDate: Date?,
    actualFinishDate: Date? = null,
    extraDaysFromRemaining: Int = 0,
    showRolloverStyle: Boolean = true,
    creditOwed: BigDecimal = BigDecimal.ZERO,
    contentPadding: PaddingValues = PaddingValues(vertical = 16.dp, horizontal = 18.dp),
) {
    val currencyFormat =
        symbolOnlyCurrencyFormat(currencyCode)

    val settingsTotalBudget = budgetSettings?.totalBudget
    val stateTotalBudget = budgetState?.totalBudget
    val rolloverFromSettings = budgetSettings?.rollOverLimit?.takeIf { it > BigDecimal.ZERO }

    val displayBudget = when {
        stateTotalBudget != null -> stateTotalBudget
        settingsTotalBudget != null -> settingsTotalBudget
        else -> budget
    }

    val baseBudget = when {
        settingsTotalBudget != null && rolloverFromSettings != null && settingsTotalBudget > rolloverFromSettings -> {
            settingsTotalBudget.subtract(rolloverFromSettings)
        }

        settingsTotalBudget != null && stateTotalBudget != null && stateTotalBudget > settingsTotalBudget -> {
            settingsTotalBudget
        }

        settingsTotalBudget != null -> settingsTotalBudget
        else -> budget
    }

    val rolloverAmount =
        displayBudget.subtract(baseBudget).takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ZERO
    val shouldShowCrossedBaseBudget = showRolloverStyle && rolloverAmount > BigDecimal.ZERO

    val totalBudgetAdjusted = displayBudget.subtract(creditOwed)
    val hasCCDebt = creditOwed > BigDecimal.ZERO

    val finalDisplayBudget = if (hasCCDebt) totalBudgetAdjusted else displayBudget

    val currencySymbol = SupportedCurrency.findByCode(currencyCode)?.symbol ?: "$"
    val formattedValue = currencyFormat.format(finalDisplayBudget)
    val formattedAmount = formattedValue.removePrefix(currencySymbol)

    val valueFontSize = if (bigVariant) {
        MaterialTheme.typography.headlineLarge.fontSize
    } else {
        MaterialTheme.typography.titleLarge.fontSize
    }
    val useAnnotatedValue = currencySymbol.length > 2
    val annotatedDisplayValue: AnnotatedString? = if (useAnnotatedValue) {
        AnnotatedString.Builder().run {
            pushStyle(
                MaterialTheme.typography.titleSmallCondensed.toSpanStyle().copy(
                    fontSize = valueFontSize * 0.65f,
                    fontWeight = FontWeight.Bold,
                    baselineShift = BaselineShift(0f)
                )
            )
            append(currencySymbol)
            pop()
            pushStyle(
                SpanStyle(
                    fontSize = valueFontSize,
                    fontWeight = FontWeight.Light
                )
            )
            append(formattedAmount)
            pop()
            toAnnotatedString()
        }
    } else null
    logcat("BudgetDisplay") {
        "state budget=$budget settingsTotal=$settingsTotalBudget stateTotal=$stateTotalBudget rollOverLimit=${budgetSettings?.rollOverLimit} rollOverCarry=${budgetSettings?.rollOverCarryForward} showRolloverStyle=$showRolloverStyle baseBudget=$baseBudget displayBudget=$displayBudget rolloverAmount=$rolloverAmount showCross=$shouldShowCrossedBaseBudget"
    }

    StatCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        label = if (hasCCDebt) {
            "${stringResource(R.string.total_budget)} - ${currencyFormat.format(creditOwed)} (${stringResource(R.string.credit_owed_label)})"
        } else {
            stringResource(R.string.total_budget)
        },
        value = if (useAnnotatedValue) formattedAmount else formattedValue,
        annotatedValue = annotatedDisplayValue,
        crossedValue = when {
            hasCCDebt -> currencyFormat.format(displayBudget)
            shouldShowCrossedBaseBudget -> currencyFormat.format(baseBudget)
            else -> null
        },
        isWavyCross = false,
        showCrossLine = !hasCCDebt,
        crossedValueColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        valueFontStyle = MaterialTheme.typography.displaySmallEmphasized,
        valueFontSize = valueFontSize,
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.wrapContentWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = prettyDate(
                            startDate,
                            pattern = "dd MMM",
                            simplifyIfToday = false,
                        ),
                        softWrap = true,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMediumCondensed,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Arrow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    )
                    if (actualFinishDate !== null && bigVariant) {
                        CountDaysChip(
                            Modifier
                                .offset(6.dp, (-12).dp)
                                .rotate(6f)
                                .zIndex(1f),
                            fromDate = startDate,
                            toDate = actualFinishDate,
                            extraDays = extraDaysFromRemaining
                        )
                        Cross(
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            CountDaysChip(
                                Modifier,
                                fromDate = startDate,
                                toDate = finishDate!!
                            )
                        }
                    } else if (finishDate !== null && bigVariant) {
                        CountDaysChip(
                            Modifier,
                            fromDate = startDate,
                            toDate = finishDate
                        )
                    }
                }

                Box(
                    modifier = Modifier.wrapContentWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (actualFinishDate !== null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                modifier = Modifier
                                    .offset((-2).dp, (-2).dp)
                                    .rotate(6f),
                                text = prettyDate(
                                    actualFinishDate,
                                    pattern = "dd MMM",
                                    simplifyIfToday = false,
                                ),
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMediumCondensed,
                            )

                            Cross {
                                Box(Modifier.wrapContentSize()) {
                                    Text(
                                        text = if (finishDate !== null) {
                                            prettyDate(
                                                finishDate,
                                                pattern = "dd MMM",
                                                simplifyIfToday = false,
                                            )
                                        } else {
                                            "-"
                                        },
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMediumCondensed,
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = if (finishDate !== null) {
                                prettyDate(
                                    finishDate,
                                    pattern = "dd MMM",
                                    simplifyIfToday = false,
                                )
                            } else {
                                "-"
                            },
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMediumCondensed,
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun CountDaysChip(
    modifier: Modifier = Modifier,
    fromDate: Date,
    toDate: Date,
    extraDays: Int = 0,
) {
    Surface(
        modifier = modifier.requiredHeight(24.dp),
        shape = CircleShape,
        color = LocalContentColor.current,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        val baseDays = countDays(toDate, fromDate)
        val totalDays = baseDays + extraDays.coerceAtLeast(0)

        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp),
                text = pluralStringResource(
                    R.plurals.analytics_days_left,
                    totalDays,
                    totalDays
                ),
                style = MaterialTheme.typography.bodyMediumCondensed,
            )
        }
    }
}

@Composable
fun Cross(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.error,
    isWavy: Boolean = false,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()
        if (enabled) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = this.size.width
                val height = this.size.height
                val offset = Offset(4f, 4f)
                val thickness = 6f

                if (isWavy) {
                    val centerY = height / 2
                    val amplitude = height * 0.15f
                    val wavelength = width / 6
                    val path = Path().apply {
                        moveTo(offset.x, centerY)
                        var x = offset.x
                        while (x < width - offset.x) {
                            val y = centerY + kotlin.math.sin((x / wavelength) * 2 * Math.PI.toFloat()) * amplitude
                            lineTo(x, y)
                            x += 2f
                        }
                    }
                    drawPath(
                        path = path,
                        color = tint,
                        style = Stroke(width = thickness, cap = StrokeCap.Round)
                    )
                } else {
                    drawLine(
                        color = tint,
                        start = Offset(offset.x, height - offset.y),
                        end = Offset(width - offset.x, offset.y),
                        strokeWidth = thickness,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun Arrow(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier) {
        val d = density
        val triSide = 9f * d
        val triHeight = triSide
        val halfSide = triSide / 2

        val right = size.width
        val centerY = size.height / 2

        val thickness = size.height * 0.095f
        val halfThick = thickness / 2
        val lineEnd = right - triHeight + halfThick

        val arrowPath = Path().apply {
            moveTo(right, centerY)
            lineTo(lineEnd, centerY - halfSide)
            moveTo(right, centerY)
            lineTo(lineEnd, centerY + halfSide)
        }

        drawPath(arrowPath, SolidColor(tint), style = Stroke(width = thickness, cap = StrokeCap.Round))

        drawLine(
            color = tint,
            start = Offset(0f, centerY),
            end = Offset(lineEnd + triHeight - 6f, centerY), // 6f is to make the line have an offset to be on top of the triangle.
            strokeWidth = thickness,
            cap = StrokeCap.Round,
        )
    }
}

@Preview(device = "id:pixel_tablet", showBackground = true)
@Composable
private fun PreviewArrow() {
    MinusTheme {
        Box {
            Arrow(
                modifier = Modifier
                    .height(24.dp)
                    .width(100.dp),
            )
        }
    }
}

@Preview(device = "spec:width=800px,height=500px,dpi=320")
@Composable
private fun BudgetDisplayPreview_OverBudget() {
    MinusTheme {
        val startDate = Date()
        val finishDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 3) }.time
        val actualFinishDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }.time

        BudgetDisplay(
            budget = BigDecimal("300.00"),
            budgetState = BudgetState(
                remainingToday = BigDecimal("-15.30"),
                totalSpentToday = BigDecimal("73.30"),
                dailyBudget = BigDecimal("58.00"),
                daysRemaining = 3,
                progress = 1.15f,
                isOverBudget = true,
                totalBudget = BigDecimal("300.00"),
                totalSpentInPeriod = BigDecimal("345.30")
            ),
            budgetSettings = BudgetSettings(
                totalBudget = BigDecimal("300.00"),
                period = BudgetPeriod.WEEKLY,
                startDate = LocalDate.now(),
                currencyCode = "USD"
            ),
            currencyCode = "USD",
            bigVariant = true,
            startDate = startDate,
            finishDate = finishDate,
            actualFinishDate = actualFinishDate
        )
    }
}

@Preview(device = "spec:width=800px,height=500px,dpi=320")
@Composable
private fun BudgetDisplayPreview_RolloverSplit() {
    MinusTheme {
        val startDate = Date()
        val finishDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 0) }.time

        BudgetDisplay(
            budget = BigDecimal("1333.50"),
            budgetState = BudgetState(
                remainingToday = BigDecimal("1533.50"),
                totalSpentToday = BigDecimal.ZERO,
                dailyBudget = BigDecimal("1533.50"),
                daysRemaining = 1,
                progress = 0f,
                isOverBudget = false,
                totalBudget = BigDecimal("1533.50"),
                totalSpentInPeriod = BigDecimal.ZERO
            ),
            budgetSettings = BudgetSettings(
                totalBudget = BigDecimal("1333.50"),
                period = BudgetPeriod.DAILY,
                startDate = LocalDate.now(),
                currencyCode = "USD"
            ),
            currencyCode = "USD",
            startDate = startDate,
            finishDate = finishDate
        )
    }
}

@Preview(device = "spec:width=800px,height=500px")
@Composable
private fun BudgetDisplayPreview_NullState() {
    MinusTheme {
        val startDate = Date()
        val finishDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 15) }.time

        BudgetDisplay(
            budget = BigDecimal.ZERO,
            budgetState = null,
            budgetSettings = null,
            currencyCode = "MAD",
            startDate = startDate,
            finishDate = finishDate
        )
    }
}

@Preview(device = "spec:width=800px,height=500px")
@Composable
private fun BudgetDisplayPreview_DebtAdjusted() {
    MinusTheme {
        val startDate = Date()
        val finishDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }.time

        BudgetDisplay(
            budget = BigDecimal("500.00"),
            budgetState = BudgetState(
                remainingToday = BigDecimal("120.50"),
                totalSpentToday = BigDecimal("30.00"),
                dailyBudget = BigDecimal("70.00"),
                daysRemaining = 7,
                progress = 0.75f,
                isOverBudget = false,
                totalBudget = BigDecimal("500.00"),
                totalSpentInPeriod = BigDecimal("379.50")
            ),
            budgetSettings = BudgetSettings(
                totalBudget = BigDecimal("500.00"),
                period = BudgetPeriod.WEEKLY,
                startDate = LocalDate.now(),
                currencyCode = "USD"
            ),
            currencyCode = "USD",
            startDate = startDate,
            finishDate = finishDate,
            creditOwed = BigDecimal("35.25")
        )
    }
}

@Preview(device = "spec:width=800px,height=500px")
@Composable
private fun BudgetDisplayPreview_LongCurrency() {
    MinusTheme {
        val startDate = Date()
        val finishDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 7) }.time

        BudgetDisplay(
            budget = BigDecimal("10830.65"),
            budgetState = null,
            budgetSettings = null,
            currencyCode = "QAR",
            startDate = startDate,
            finishDate = finishDate
        )
    }
}
