package com.serranoie.app.minus.presentation.ui.editor.note

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed

/**
 * A compact, tap-to-open chip that shows the transaction's optional free-form note (or a prompt to
 * add one). Tapping it opens [ExtraNoteSheet] — the note is edited there, never inline, so it never
 * competes with the category picker for row space.
 */
@Composable
fun ExtraNoteChip(
    note: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onlyIcon: Boolean = false,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(CircleShape)
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Rounded.EditNote,
                contentDescription = null,
            )
            if (!onlyIcon || note.isNotEmpty()) {
                Spacer(Modifier.width(if (onlyIcon) 8.dp else 4.dp))
                Text(
                    text = note.ifEmpty { stringResource(R.string.add_extra_note) },
                    style = MaterialTheme.typography.bodyMediumCondensed,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ExtraNoteChipPreview() {
    MinusTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            ExtraNoteChip(note = "Gas for the road trip", onClick = {})
        }
    }
}

@Preview
@Composable
private fun ExtraNoteChipEmptyPreview() {
    MinusTheme {
        Box(modifier = Modifier.padding(8.dp)) {
            ExtraNoteChip(note = "", onClick = {})
        }
    }
}
