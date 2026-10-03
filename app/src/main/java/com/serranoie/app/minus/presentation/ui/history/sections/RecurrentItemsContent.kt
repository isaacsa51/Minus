package com.serranoie.app.minus.presentation.ui.history.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition
import com.serranoie.app.minus.presentation.ui.theme.component.expense.UpcomingRecurrentItem

@Composable
internal fun RecurrentItemsContent(
    items: List<UpcomingRecurrentItem>,
    verticalItem: @Composable (index: Int, item: UpcomingRecurrentItem, position: PaddedListItemPosition) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            verticalItem(
                index,
                item,
                paddedListItemPosition(index, items.lastIndex, items.size)
            )
            if (index < items.lastIndex) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}
