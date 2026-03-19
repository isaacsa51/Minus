package com.serranoie.app.minus.presentation.ui.theme.component.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.CategoryAmount
import com.serranoie.app.minus.presentation.ui.theme.isNightMode
import com.serranoie.app.minus.presentation.util.HarmonizedColorPalette
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.harmonize
import com.serranoie.app.minus.presentation.util.harmonizeWithColor
import com.serranoie.app.minus.presentation.util.toPalette
import java.math.BigDecimal
import java.time.LocalDateTime

data class CategoryUsage(
	val name: String,
	val amount: BigDecimal,
	var color: HarmonizedColorPalette? = null,
	var isSpecial: Boolean = false,
)

var baseColors = listOf(
	Color(0xFFF86BAE),
	Color(0xFFF36FFF),
	Color(0xFFAB96FF),
	Color(0xFF5FC7E7),
	Color(0xFF75E584),
	Color(0xFFFFD386),
	Color(0xFFEF7564),
)

@Composable
fun CategoriesChartCard(
	modifier: Modifier = Modifier,
	spends: List<Transaction>,
	currency: String = "MXN",
	onCategoryClick: ((categoryName: String, categorySpends: List<Transaction>) -> Unit)? = null,
) {
	val isNightMode = isNightMode()
	val labelWithoutTag = "Sin categoria"
	val labelRest = "Restante"
	val maxDisplay = 7

	val colors = baseColors.map {
		toPalette(
			color = harmonizeWithColor(
				designColor = it, sourceColor = MaterialTheme.colorScheme.primary
			),
		)
	}
	val restColor = toPalette(
		color = harmonize(
			designColor = Color(0xFF222222), sourceColor = MaterialTheme.colorScheme.primary
		),
	).copy(
		main = if (isNightMode) Color(0xFFF0F0F0) else Color(0xFF222222),
		onSurface = if (isNightMode) Color(0xFF1A1A1A) else Color(0xFFF4F4F4)
	)
	val stubColor = toPalette(
		color = harmonize(
			designColor = Color(0xFFCCCCCC), sourceColor = MaterialTheme.colorScheme.primary
		),
	).copy(
		main = if (isNightMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFCCCCCC),
	)

	var offsetColor = 0

	val tags = remember(spends) {
		// Convert to CategoryUsage, group by category and sum amounts
		var result = spends.map { it.copy(comment = it.comment.ifEmpty { labelWithoutTag }) }
			.groupBy { it.comment.trim() }.map { tag ->
				CategoryUsage(
					tag.key,
					tag.value.map { it.amount }.reduce { acc, next -> acc + next },
					isSpecial = tag.key == labelWithoutTag,
				)
			}.sortedBy { it.amount }.reversed().toList()

		// Move without tag to the end if list will be overflow
		if (result.size > maxDisplay) {
			result.find { it.name == labelWithoutTag }?.let {
				result = result.filter { tagUsage -> tagUsage.name != labelWithoutTag }
				result = result + it
			}
		}

		// Set colors
		result.subList(0, result.size.coerceAtMost(maxDisplay)).forEachIndexed { index, tagUsage ->
			tagUsage.color = if (tagUsage.name == labelWithoutTag) {
				offsetColor++
				restColor
			} else colors.getOrNull(index - offsetColor) ?: colors.last()
		}

		// Combine rest tags to one
		if (result.size > maxDisplay) {
			result = result.slice(0..<maxDisplay) + CategoryUsage(
				name = labelRest,
				amount = result.slice(maxDisplay until result.size).map { it.amount }
					.reduce { acc, next -> acc + next },
				color = restColor,
				isSpecial = true,
			)
		}

		result
	}

	Card(
		modifier = modifier.fillMaxHeight(),
		shape = RoundedCornerShape(22.dp),
		colors = CardDefaults.cardColors(
			containerColor = combineColors(
				MaterialTheme.colorScheme.surface,
				MaterialTheme.colorScheme.surfaceVariant,
				angle = 0.3f,
			),
		)
	) {
		if (tags.size == 1 && tags.first().name == labelWithoutTag) {
			Box {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp),
					verticalArrangement = Arrangement.Center,
					horizontalAlignment = Alignment.CenterHorizontally,
				) {
					Row(Modifier.fillMaxWidth()) {
						DonutChart(
							modifier = Modifier
								.padding(end = 16.dp, bottom = 8.dp)
								.size(64.dp),
							items = listOf(CategoryUsage("", BigDecimal(360), stubColor)),
						)
						Column {
							Text(
								text = "We can't split your spends by categories",
								style = MaterialTheme.typography.bodyLarge.copy(
									color = MaterialTheme.colorScheme.onSurfaceVariant,
								),
							)
							Spacer(modifier = Modifier.height(4.dp))
							Row(
								modifier = Modifier.fillMaxWidth(),
							) {
								Text(
									text = "Use tags to see chart by categories ",
									style = MaterialTheme.typography.bodyMedium.copy(
										color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f),
									),
								)
							}
						}
					}


				}
			}
		} else {
			DonutChart(
				modifier = Modifier
					.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
					.size(64.dp),
				items = tags,
			)
			FlowRow(
				verticalArrangement = Arrangement.spacedBy(0.dp),
				horizontalArrangement = Arrangement.spacedBy(0.dp),
				modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth()
			) {
				tags.forEach { tag ->
					val categoryTransactions = remember(spends, tag.name) {
						spends.filter {
							val category = if (it.comment.trim()
									.isEmpty()
							) labelWithoutTag else it.comment.trim()
							category == tag.name
						}
					}
					CategoryAmount(
						modifier = Modifier.padding(horizontal = 4.dp),
						value = tag.name,
						amount = tag.amount,
						palette = tag.color,
						isSpecial = tag.isSpecial,
						currency = currency,
						onClick = if (onCategoryClick != null) {
							{ onCategoryClick(tag.name, categoryTransactions) }
						} else null,
					)
				}
			}
		}
	}

}

@Preview(name = "CategoriesChart", device = "spec:width=800px,height=800px")
@Composable
private fun PreviewCategoriesChart() {
	MinusTheme {
		CategoriesChartCard(
			spends = listOf(
				Transaction(
					amount = BigDecimal(100),
					comment = "Food",
					date = LocalDateTime.now(),
					isDeleted = false
				),
				Transaction(
					amount = BigDecimal(50),
					comment = "Streaming",
					date = LocalDateTime.now(),
					isDeleted = false
				),
				Transaction(
					amount = BigDecimal(20),
					comment = "Gas",
					date = LocalDateTime.now(),
					isDeleted = false
				),

				Transaction(
					amount = BigDecimal(200),
					comment = "Gym",
					date = LocalDateTime.now(),
					isDeleted = false
				),

				Transaction(
					amount = BigDecimal(15),
					comment = "Internet",
					date = LocalDateTime.now(),
					isDeleted = false
				),
			)
		)
	}
}