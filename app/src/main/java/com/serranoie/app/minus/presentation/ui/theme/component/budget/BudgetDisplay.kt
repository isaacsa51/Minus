package com.serranoie.app.minus.presentation.ui.theme.component.budget

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.ui.theme.component.StatCard
import com.serranoie.app.minus.presentation.util.countDays
import com.serranoie.app.minus.presentation.util.prettyDate
import com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat
import logcat.logcat
import java.math.BigDecimal
import java.time.LocalDate
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
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp, horizontal = 18.dp),
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
        label = stringResource(R.string.total_budget),
        value = currencyFormat.format(displayBudget),
        crossedValue = if (shouldShowCrossedBaseBudget) currencyFormat.format(baseBudget) else null,
        crossedValueColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        valueFontStyle = MaterialTheme.typography.displaySmallEmphasized,
        valueFontSize = if (bigVariant) {
            MaterialTheme.typography.headlineLarge.fontSize
        } else {
            MaterialTheme.typography.titleLarge.fontSize
        },
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
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = this.size.width
            val height = this.size.height
            val offset = Offset(4f, 4f)
            val thickness = 6f

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

@Composable
fun Arrow(
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier) {
        val width = this.size.width
        val height = this.size.height
        val heightHalf = height / 2

        val thickness = 6
        val thicknessHalf = thickness / 2

        val trianglePath = Path().let {
            it.moveTo(11f, heightHalf - thicknessHalf)
            it.lineTo(width - 22.4f, heightHalf - thicknessHalf)
            it.lineTo(width - 37.4f, heightHalf - 18)
            it.lineTo(width - 33, heightHalf - 22.4f)
            it.lineTo(width - 10.5f, heightHalf)
            it.lineTo(width - 33, heightHalf + 22.4f)
            it.lineTo(width - 37.4f, heightHalf + 18)
            it.lineTo(width - 22.4f, heightHalf + thicknessHalf)
            it.lineTo(width - 22.4f, heightHalf + thicknessHalf)
            it.lineTo(11f, heightHalf + thicknessHalf)

            it.close()

            it
        }

        drawPath(
            path = trianglePath,
            SolidColor(tint),
            style = Fill
        )
    }
}

@Preview
@Composable
private fun PreviewChart() {
    MinusTheme {
        Box {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
            )
            Arrow(
                modifier = Modifier
                    .height(24.dp)
                    .width(100.dp),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewCross() {
    MinusTheme {
        Cross {
            Text(text = "Days count")
        }
    }
}

@Preview(
    device = "spec:width=800px,height=500px", locale = "es",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)
@Preview(device = "spec:width=800px,height=500px", locale = "en")
@Preview(device = "spec:width=800px,height=500px", locale = "fr")
@Composable
private fun BudgetDisplayPreview_HealthyBudget() {
    MinusTheme {
        val startDate = Date()
        val finishDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 15) }.time

        BudgetDisplay(
            budget = BigDecimal("500.00"),
            budgetState = BudgetState(
                remainingToday = BigDecimal("45.50"),
                totalSpentToday = BigDecimal("12.50"),
                dailyBudget = BigDecimal("58.00"),
                daysRemaining = 15,
                progress = 0.21f,
                isOverBudget = false,
                totalBudget = BigDecimal("500.00"),
                totalSpentInPeriod = BigDecimal("100.00")
            ),
            budgetSettings = BudgetSettings(
                totalBudget = BigDecimal("500.00"),
                period = BudgetPeriod.MONTHLY,
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

@Preview(device = "spec:width=800px,height=500px")
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
            currencyCode = "USD",
            startDate = startDate,
            finishDate = finishDate
        )
    }
}
