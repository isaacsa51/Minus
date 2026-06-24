package com.serranoie.app.minus.presentation.ui.history.sections

import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition

/**
 * Computes the [PaddedListItemPosition] for an item in a list of
 * [size] elements, given the item's [index] and the last [lastIndex].
 *
 * Returns [PaddedListItemPosition.Single] when there is exactly one
 * element; otherwise uses the index relative to the bounds of the
 * list to pick [PaddedListItemPosition.First], [PaddedListItemPosition.Middle],
 * or [PaddedListItemPosition.Last].
 *
 * Used by [transactionDateSections] and [pastTransactionDateSections]
 * to give each swipeable row the correct rounded-corner treatment.
 */
internal fun paddedListItemPosition(
    index: Int,
    lastIndex: Int,
    size: Int,
): PaddedListItemPosition = when {
    size == 1 -> PaddedListItemPosition.Single
    index == 0 -> PaddedListItemPosition.First
    index == lastIndex -> PaddedListItemPosition.Last
    else -> PaddedListItemPosition.Middle
}
