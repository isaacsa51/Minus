package com.serranoie.app.minus.presentation.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.domain.calculator.CategoryLimitPace
import com.serranoie.app.minus.domain.calculator.CategoryLimitStatus
import com.serranoie.app.minus.domain.model.CategoryLimit
import com.serranoie.app.minus.domain.model.CategoryLimitUnit
import com.serranoie.app.minus.presentation.ui.editor.category.CategoryLimitBar
import com.serranoie.app.minus.presentation.ui.editor.category.categoryLimitColor
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed
import com.serranoie.app.minus.presentation.util.font.format.formatCurrencySymbolOnly
import java.math.BigDecimal
import java.time.LocalDate

@Composable
fun CategoryLimitsCard(
    statuses: List<CategoryLimitStatus>,
    currencyCode: String,
    modifier: Modifier = Modifier,
) {
    if (statuses.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.analytics_category_limits_title),
                style = MaterialTheme.typography.bodySmallCondensed,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        statuses.forEach { status ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = status.name,
                        style = MaterialTheme.typography.bodyMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = when (status.pace) {
                            CategoryLimitPace.OVER -> stringResource(
                                R.string.category_limit_over,
                                formatCurrencySymbolOnly(status.spent.subtract(status.limit.amount), currencyCode),
                            )
                            CategoryLimitPace.AHEAD -> stringResource(R.string.category_limit_ahead)
                            CategoryLimitPace.ON_TRACK -> stringResource(R.string.category_limit_on_track)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (status.pace == CategoryLimitPace.ON_TRACK) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            categoryLimitColor(status.pace)
                        },
                    )
                }
                CategoryLimitBar(status = status, currencyCode = currencyCode)
            }
        }
    }
}

@Preview(name = "Analytics category limits")
@Composable
private fun CategoryLimitsCardPreview() {
    fun status(name: String, spent: String, limit: String, unit: CategoryLimitUnit, pace: CategoryLimitPace) =
        CategoryLimitStatus(
            name = name,
            limit = CategoryLimit(BigDecimal(limit), unit),
            spent = BigDecimal(spent),
            windowStart = LocalDate.of(2026, 11, 1),
            windowEnd = LocalDate.of(2026, 11, 30),
            pace = pace,
        )
    MinusTheme {
        Surface {
            CategoryLimitsCard(
                statuses = listOf(
                    status("Transport", "30", "120", CategoryLimitUnit.PERIOD, CategoryLimitPace.ON_TRACK),
                    status("Food", "240", "300", CategoryLimitUnit.PERIOD, CategoryLimitPace.AHEAD),
                    status("Coffee", "62", "50", CategoryLimitUnit.WEEKLY, CategoryLimitPace.OVER),
                ),
                currencyCode = "USD",
            )
        }
    }
}
