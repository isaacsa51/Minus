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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.serranoie.app.minus.R
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
    Color(0xFF64B5F6),
    Color(0xFFAED581),
    Color(0xFFFFB74D),
    Color(0xFFBA68C8),
    Color(0xFF4DB6AC),
    Color(0xFF9575CD),
    Color(0xFFF06292),
)

@Composable
fun CategoriesChartCard(
    modifier: Modifier = Modifier,
    spends: List<Transaction>,
    currency: String = "MXN",
    onCategoryClick: ((categoryName: String, categorySpends: List<Transaction>) -> Unit)? = null,
) {
    val isNightMode = isNightMode()
    val labelWithoutTag = stringResource(R.string.categories_chart_uncategorized)
    val labelRest = stringResource(R.string.categories_chart_rest)
    val maxDisplay = 20

    var selectedCategoryName by remember { mutableStateOf<String?>(null) }

    val colors = (0 until maxDisplay).map { i ->
        val baseSize = baseColors.size
        val colorIndex = i % baseSize
        val iteration = i / baseSize

        var baseColor = baseColors[colorIndex]

        if (iteration > 0) {
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(baseColor.toArgb(), hsl)
            hsl[0] = (hsl[0] + (iteration * 137.5f)) % 360f
            baseColor = Color(ColorUtils.HSLToColor(hsl))
        }

        toPalette(
            color = harmonizeWithColor(
                designColor = baseColor,
                sourceColor = MaterialTheme.colorScheme.primary,
                chromaFactor = if (isNightMode) 2f else 1f
            ),
        )
    }
    val restColor = toPalette(
        color = harmonize(
            designColor = Color(0xFF222222),
            sourceColor = MaterialTheme.colorScheme.primary
        ),
    ).copy(
        main = if (isNightMode) Color(0xFFF0F0F0) else Color(0xFF222222),
        onSurface = if (isNightMode) Color(0xFF1A1A1A) else Color(0xFFF4F4F4)
    )
    val stubColor = toPalette(
        color = harmonize(
            designColor = Color(0xFFCCCCCC),
            sourceColor = MaterialTheme.colorScheme.primary
        ),
    ).copy(
        main = if (isNightMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFCCCCCC),
    )

    var offsetColor = 0

    val tags = remember(spends) {
        var result = spends.map { it.copy(comment = it.comment.ifEmpty { labelWithoutTag }) }
            .groupBy { it.comment.trim() }.map { tag ->
                CategoryUsage(
                    tag.key,
                    tag.value.map { it.amount }.reduce { acc, next -> acc + next },
                    isSpecial = tag.key == labelWithoutTag,
                )
            }.sortedBy { it.amount }.reversed().toList()

        if (result.size > maxDisplay) {
            result.find { it.name == labelWithoutTag }?.let {
                result = result.filter { tagUsage -> tagUsage.name != labelWithoutTag }
                result = result + it
            }
        }

        result.subList(0, result.size.coerceAtMost(maxDisplay)).forEachIndexed { index, tagUsage ->
            tagUsage.color = if (tagUsage.name == labelWithoutTag) {
                offsetColor++
                restColor
            } else {
                val colorIndex = (index - offsetColor).coerceIn(0, colors.lastIndex)
                colors[colorIndex]
            }
        }

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

    val isEmpty = tags.isEmpty() || (tags.size == 1 && tags.first().name == labelWithoutTag)

    val cardBgColor = combineColors(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant,
        angle = 0.3f,
    )

    Card(
        modifier = if (isEmpty) modifier else modifier.fillMaxHeight(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor,
        )
    ) {
        if (isEmpty) {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(64.dp),
                            items = listOf(CategoryUsage("", BigDecimal(360), stubColor)),
                            holeColor = cardBgColor
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.categories_chart_empty_title),
                                style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = stringResource(R.string.categories_chart_empty_subtitle),
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DonutChart(
                    modifier = Modifier
                        .padding(top = 24.dp, bottom = 16.dp)
                        .size(180.dp),
                    items = tags,
                    selectedIndex = tags.indexOfFirst { it.name == selectedCategoryName },
                    holeColor = cardBgColor
                )
                FlowRow(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    tags.forEach { tag ->
                        val categoryTransactions = remember(spends, tag.name) {
                            spends.filter {
                                val category = it.comment.trim().ifEmpty { labelWithoutTag }
                                category == tag.name
                            }
                        }
                        CategoryAmount(
                            value = tag.name,
                            amount = tag.amount,
                            palette = tag.color,
                            isSpecial = tag.isSpecial,
                            currency = currency,
                            onClick = {
                                selectedCategoryName = if (selectedCategoryName == tag.name) null else tag.name
                                onCategoryClick?.invoke(tag.name, categoryTransactions)
                            },
                        )
                    }
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

@PreviewLightDark
@Composable
private fun PreviewCategoriesChartExtremeManyCategories() {
    val categories = listOf(
        "Food", "Rent", "Transport", "Entertainment", "Health",
        "Education", "Shopping", "Gift", "Tech", "Other",
        "Travel", "Gym", "Subscript.", "Pets", "Hobbies",
        "Savings", "Insurance", "Taxes", "Repair", "Donate"
    )
    MinusTheme {
        CategoriesChartCard(
            spends = categories.mapIndexed { index, name ->
                Transaction(
                    amount = BigDecimal(100 - index * 2), // Varying amounts
                    comment = name,
                    date = LocalDateTime.now()
                )
            },
            onCategoryClick = { _, _ -> } // Mock listener to enable click logic
        )
    }
}
