package com.serranoie.app.minus.presentation.ui.theme.component.budget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.serranoie.app.minus.domain.model.BudgetPeriod
import com.serranoie.app.minus.domain.model.BudgetSettings
import com.serranoie.app.minus.domain.model.BudgetState
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.StatCard
import com.serranoie.app.minus.presentation.util.countDays
import com.serranoie.app.minus.presentation.util.prettyDate
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale

const val DAY = 24 * 60 * 60 * 1000

/**
 * Displays budget information: total budget, date range, and stats.
 */
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
	contentPadding: PaddingValues = PaddingValues(vertical = 16.dp, horizontal = 24.dp),
) {
	val currencyFormat = com.serranoie.app.minus.presentation.util.symbolOnlyCurrencyFormat(currencyCode)

	val displayBudget = budgetState?.totalBudget ?: budget

	StatCard(
		modifier = modifier.fillMaxWidth(),
		contentPadding = contentPadding,
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.onSurface,
			contentColor = MaterialTheme.colorScheme.surfaceVariant
		),
		label = "Presupuesto total",
		value = currencyFormat.format(displayBudget),
valueFontStyle = MaterialTheme.typography.displayMedium,
		valueFontSize = if (bigVariant) MaterialTheme.typography.headlineLarge.fontSize else MaterialTheme.typography.titleLarge.fontSize,
		content = {
			Spacer(modifier = Modifier.height(16.dp))

			Row(
				modifier = Modifier
					.fillMaxWidth()
					.height(IntrinsicSize.Min),
				verticalAlignment = Alignment.CenterVertically
			) {
				Box(
					modifier = Modifier.width(60.dp), contentAlignment = Alignment.CenterStart
				) {
					Text(
						text = prettyDate(
							startDate,
							pattern = "dd MMM",
							simplifyIfToday = false,
						),
						softWrap = false,
						overflow = TextOverflow.Ellipsis,
						style = MaterialTheme.typography.bodyMedium,
						fontSize = if (bigVariant) MaterialTheme.typography.bodySmall.fontSize else MaterialTheme.typography.labelSmall.fontSize,
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
								Modifier, fromDate = startDate, toDate = finishDate!!
							)
						}
					} else if (finishDate !== null && bigVariant) {
						CountDaysChip(
							Modifier, fromDate = startDate, toDate = finishDate
						)
					}
				}

				Box(
					modifier = Modifier.width(60.dp), contentAlignment = Alignment.CenterEnd
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
								style = MaterialTheme.typography.bodyMedium,
								fontSize = if (bigVariant) MaterialTheme.typography.bodySmall.fontSize else MaterialTheme.typography.labelSmall.fontSize,
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
										style = MaterialTheme.typography.bodyMedium,
										fontSize = if (bigVariant) MaterialTheme.typography.bodySmall.fontSize else MaterialTheme.typography.labelSmall.fontSize,
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
							style = MaterialTheme.typography.bodyMedium,
							fontSize = if (bigVariant) MaterialTheme.typography.bodySmall.fontSize else MaterialTheme.typography.labelSmall.fontSize,
						)
					}
				}
			}
		})
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
				modifier = Modifier.padding(12.dp, 0.dp),
				text = if (totalDays >= 2 || totalDays == 0) "$totalDays días" else "$totalDays día",
				style = MaterialTheme.typography.bodyMedium,
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
			val offset = Offset(6f, 6f)
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
			path = trianglePath, SolidColor(tint), style = Fill
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

@Preview(device = "spec:width=800px,height=500px")
@Composable
private fun BudgetDisplayPreview_HealthyBudget() {
	MinusTheme {
		val startDate = Date()
		val finishDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 15) }.time

		BudgetDisplay(
			budget = BigDecimal("500.00"), budgetState = BudgetState(
				remainingToday = BigDecimal("45.50"),
				totalSpentToday = BigDecimal("12.50"),
				dailyBudget = BigDecimal("58.00"),
				daysRemaining = 15,
				progress = 0.21f,
				isOverBudget = false,
				totalBudget = BigDecimal("500.00"),
				totalSpentInPeriod = BigDecimal("100.00")
			), budgetSettings = BudgetSettings(
				totalBudget = BigDecimal("500.00"),
				period = BudgetPeriod.MONTHLY,
				startDate = LocalDate.now(),
				currencyCode = "USD"
			), currencyCode = "USD", startDate = startDate, finishDate = finishDate
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
