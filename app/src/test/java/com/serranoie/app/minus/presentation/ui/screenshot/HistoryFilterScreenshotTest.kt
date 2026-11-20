package com.serranoie.app.minus.presentation.ui.screenshot

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.serranoie.app.minus.domain.model.Category
import com.serranoie.app.minus.presentation.ui.history.HistorySearchFilter
import com.serranoie.app.minus.presentation.ui.history.HistoryTransactionKind
import com.serranoie.app.minus.presentation.ui.history.toQueryText
import com.serranoie.app.minus.presentation.ui.history.sections.HistoryFilterSheetContent
import com.serranoie.app.minus.presentation.ui.history.sections.HistorySearchField
import com.serranoie.app.minus.presentation.ui.history.sections.HistorySearchSkeleton
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale

class HistoryFilterScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.SHRINK,
        maxPercentDifference = 5.0,
    )

    private val categories = listOf(
        Category(id = 1L, name = "Food"),
        Category(id = 2L, name = "Transport"),
        Category(id = 3L, name = "Groceries"),
        Category(id = 4L, name = "Coffee"),
    )

    @Test
    fun historySearchFieldFilterButton() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        HistorySearchField(query = "", onQueryChange = {}, filterActive = false, onFilterClick = {})
                        HistorySearchField(
                            query = "coffee",
                            onQueryChange = {},
                            filterActive = true,
                            onFilterClick = {},
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun historySearchFieldWithFilterText() {
        Locale.setDefault(Locale.US)
        val (filterText, rest) = HistorySearchFilter(
            categoryIds = setOf(1L),
            kind = HistoryTransactionKind.EXPENSE,
            minAmount = BigDecimal("50"),
            startDate = LocalDate.of(2030, 10, 1),
            endDate = LocalDate.of(2030, 10, 15),
        ).toQueryText(categories.associate { it.id to it.name }, Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                Surface {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        HistorySearchField(
                            query = "coffee $filterText",
                            onQueryChange = {},
                            filterActive = rest.isActive,
                            onFilterClick = {},
                        )
                        HistorySearchField(
                            query = HistorySearchFilter(minAmount = BigDecimal("50"))
                                .toQueryText(emptyMap(), Locale.US).first,
                            onQueryChange = {},
                            filterActive = false,
                            onFilterClick = {},
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun historyFilterSheetDefault() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    HistoryFilterSheetContent(
                        filter = HistorySearchFilter(),
                        categories = categories,
                        onFilterChange = {},
                        onPickDates = {},
                    )
                }
            }
        }
    }

    @Test
    fun historyFilterSheetActive() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    HistoryFilterSheetContent(
                        filter = HistorySearchFilter(
                            searchCategoryNames = false,
                            categoryIds = setOf(1L, 4L),
                            kind = HistoryTransactionKind.EXPENSE,
                            minAmount = BigDecimal("50"),
                            maxAmount = BigDecimal("200.5"),
                            startDate = LocalDate.of(2030, 3, 1),
                            endDate = LocalDate.of(2030, 3, 15),
                            recurringOnly = true,
                        ),
                        categories = categories,
                        onFilterChange = {},
                        onPickDates = {},
                    )
                }
            }
        }
    }

    @Test
    fun historySearchSkeleton() {
        Locale.setDefault(Locale.US)

        paparazzi.snapshot {
            MinusTheme {
                Surface {
                    HistorySearchSkeleton(modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
