package com.serranoie.app.minus.presentation.ui.theme.component.charts

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.model.Transaction
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.component.CategoryAmount
import com.serranoie.app.minus.presentation.ui.theme.isNightMode
import com.serranoie.app.minus.presentation.util.HarmonizedColorPalette
import com.serranoie.app.minus.presentation.util.combineColors
import com.serranoie.app.minus.presentation.util.harmonizeWithColor
import com.serranoie.app.minus.presentation.util.toPaletteWithTheme
import java.math.BigDecimal
import java.time.LocalDateTime

data class CategoryUsage(
    val name: String,
    val amount: BigDecimal,
    val color: HarmonizedColorPalette? = null,
    val isSpecial: Boolean = false,
)

var baseColors =
    listOf(
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
    spends: List<Transaction>,
    modifier: Modifier = Modifier,
    currency: String = "MXN",
    selectedCategoryName: String? = null,
    onCategoryClick: ((categoryName: String, categorySpends: List<Transaction>) -> Unit)? = null,
    onSelectedCategoryChange: ((String?) -> Unit)? = null,
) {
    val isNightMode = isNightMode()
    val labelWithoutTag = stringResource(R.string.categories_chart_uncategorized)
    val labelRest = stringResource(R.string.categories_chart_rest)
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val maxDisplay = 20

    var internalSelectedCategoryName by remember(selectedCategoryName) { mutableStateOf(selectedCategoryName) }

    LaunchedEffect(selectedCategoryName) {
        internalSelectedCategoryName = selectedCategoryName
    }

    val colors =
        remember(isNightMode, primaryColor) {
            (0 until maxDisplay).map { i ->
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

                toPaletteWithTheme(
                    color =
                        harmonizeWithColor(
                            designColor = baseColor,
                            sourceColor = primaryColor,
                            chromaMultiplier = if (isNightMode) 2f else 1f,
                        ),
                    darkTheme = isNightMode,
                )
            }
        }
    val restColor =
        remember(isNightMode, primaryColor) {
            toPaletteWithTheme(
                color =
                    harmonizeWithColor(
                        designColor = Color(0xFF222222),
                        sourceColor = primaryColor,
                    ),
                darkTheme = isNightMode,
            ).copy(
                main = if (isNightMode) Color(0xFFF0F0F0) else Color(0xFF222222),
                onSurface = if (isNightMode) Color(0xFF1A1A1A) else Color(0xFFF4F4F4),
            )
        }
    val stubColor =
        remember(isNightMode, primaryColor, surfaceVariantColor) {
            toPaletteWithTheme(
                color =
                    harmonizeWithColor(
                        designColor = Color(0xFFCCCCCC),
                        sourceColor = primaryColor,
                    ),
                darkTheme = isNightMode,
            ).copy(
                main = if (isNightMode) surfaceVariantColor else Color(0xFFCCCCCC),
            )
        }

    val filteredSpends = remember(spends) {
        spends.filter { it.amount > BigDecimal.ZERO }
    }

    val tags =
        remember(filteredSpends, labelWithoutTag, labelRest, colors, restColor) {
            var offsetColor = 0
            var rawTags =
                filteredSpends
                    .map { it.copy(comment = it.comment.ifEmpty { labelWithoutTag }) }
                    .groupBy { it.comment.trim() }
                    .map { tag ->
                        CategoryUsage(
                            name = tag.key,
                            amount = tag.value.map { it.amount }.reduce { acc, next -> acc + next },
                            isSpecial = tag.key == labelWithoutTag,
                        )
                    }.sortedByDescending { it.amount }

            if (rawTags.size > maxDisplay) {
                rawTags.find { it.name == labelWithoutTag }?.let { uncategorized ->
                    rawTags = rawTags.filter { it.name != labelWithoutTag } + uncategorized
                }
            }

            val displayTags = rawTags.take(maxDisplay).mapIndexed { index, tagUsage ->
                val palette = if (tagUsage.name == labelWithoutTag) {
                    offsetColor++
                    restColor
                } else {
                    val colorIndex = (index - offsetColor).coerceIn(0, colors.lastIndex)
                    colors[colorIndex]
                }
                tagUsage.copy(color = palette)
            }.toMutableList()

            if (rawTags.size > maxDisplay) {
                val restAmount = rawTags
                    .drop(maxDisplay)
                    .map { it.amount }
                    .reduce { acc, next -> acc + next }

                displayTags.add(
                    CategoryUsage(
                        name = labelRest,
                        amount = restAmount,
                        color = restColor,
                        isSpecial = true,
                    )
                )
            }

            displayTags
        }

    val isEmpty = tags.isEmpty() || (tags.size == 1 && tags.first().name == labelWithoutTag)

    val cardBgColor =
        combineColors(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant,
            t = 0.3f,
        )

    Card(
        modifier = if (isEmpty) modifier else modifier.fillMaxHeight(),
        shape = RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = cardBgColor,
            ),
    ) {
        if (isEmpty) {
            EmptyChartContent(stubColor)
        } else {
            ChartContent(
                tags = tags,
                selectedCategoryName = internalSelectedCategoryName,
                spends = filteredSpends,
                labelWithoutTag = labelWithoutTag,
                currency = currency,
                onCategoryClick = onCategoryClick,
                onSelectionChange = { newCategory ->
                    internalSelectedCategoryName = newCategory
                    onSelectedCategoryChange?.invoke(newCategory)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyChartContent(stubColor: HarmonizedColorPalette) {
    val infiniteTransition = rememberInfiniteTransition(label = "emptyChartRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "emptyChartRotationAngle",
    )

    Box {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(72.dp)
                        .rotate(rotation)
                        .background(
                            color = stubColor.main,
                            shape = MaterialShapes.Flower.toShape()
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QuestionMark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(-rotation)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.categories_chart_empty_title),
                        style =
                            MaterialTheme.typography.bodyLargeEmphasized.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        lineHeight = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.categories_chart_empty_subtitle),
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartContent(
    tags: List<CategoryUsage>,
    selectedCategoryName: String?,
    spends: List<Transaction>,
    labelWithoutTag: String,
    currency: String,
    onCategoryClick: ((String, List<Transaction>) -> Unit)?,
    onSelectionChange: (String?) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DonutChart(
            modifier =
                Modifier
                    .padding(top = 24.dp, bottom = 16.dp)
                    .size(180.dp),
            items = tags,
            selectedIndex = tags.indexOfFirst { it.name == selectedCategoryName },
            onItemClick = { index ->
                if (index in tags.indices) {
                    val tag = tags[index]
                    val newSelection = if (selectedCategoryName == tag.name) null else tag.name
                    onSelectionChange(newSelection)
                    if (newSelection != null) {
                        val categoryTransactions = spends.filter {
                            val category = it.comment.trim().ifEmpty { labelWithoutTag }
                            if (tag.isSpecial) {
                                val standaloneNames = tags.filter { !it.isSpecial }.map { it.name }
                                category !in standaloneNames
                            } else {
                                category == tag.name
                            }
                        }
                        onCategoryClick?.invoke(tag.name, categoryTransactions)
                    }
                } else {
                    onSelectionChange(null)
                }
            },
        )
        FlowRow(
            modifier =
                Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            tags.forEach { tag ->
                val categoryTransactions =
                    remember(spends, tag.name) {
                        spends.filter {
                            val category = it.comment.trim().ifEmpty { labelWithoutTag }
                            if (tag.isSpecial) {
                                val standaloneNames = tags.filter { !it.isSpecial }.map { it.name }
                                category !in standaloneNames
                            } else {
                                category == tag.name
                            }
                        }
                    }
                CategoryAmount(
                    value = tag.name,
                    amount = tag.amount,
                    palette = tag.color,
                    isSpecial = tag.isSpecial,
                    currency = currency,
                    selected = selectedCategoryName == tag.name,
                    onClick = {
                        val newSelection = if (selectedCategoryName == tag.name) null else tag.name
                        onSelectionChange(newSelection)
                        if (newSelection != null) {
                            onCategoryClick?.invoke(tag.name, categoryTransactions)
                        }
                    },
                )
            }
        }
    }
}

@Preview(name = "Empty chart", device = "spec:width=800px,height=800px")
@Composable
private fun PreviewEmptyState() {
    MinusTheme {
        CategoriesChartCard(
            spends = emptyList(),
        )
    }
}

@Preview(name = "CategoriesChart", device = "spec:width=800px,height=800px")
@Composable
private fun PreviewCategoriesChart() {
    MinusTheme {
        CategoriesChartCard(
            spends =
                listOf(
                    Transaction(
                        amount = BigDecimal(100),
                        comment = "Food",
                        date = LocalDateTime.now(),
                        isDeleted = false,
                    ),
                ),
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewCategoriesChartExtremeManyCategories() {
    val categories =
        listOf(
            "Food",
            "Rent",
            "Transport",
            "Entertainment",
            "Health",
            "Education",
            "Shopping",
            "Gift",
            "Tech",
            "Other",
            "Travel",
            "Gym",
            "Subscript.",
            "Pets",
            "Hobbies",
            "Savings",
            "Insurance",
            "Taxes",
            "Repair",
            "Donate",
        )
    MinusTheme {
        CategoriesChartCard(
            spends =
                categories.mapIndexed { index, name ->
                    Transaction(
                        amount = BigDecimal(100 - index * 2),
                        comment = name,
                        date = LocalDateTime.now(),
                    )
                },
            onCategoryClick = { _, _ -> },
        )
    }
}
