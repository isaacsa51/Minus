package com.serranoie.app.minus.presentation.ui.editor.category

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodyMediumCondensed
import com.serranoie.app.minus.presentation.util.haptic.HapticUtil.performUIHaptic

@Composable
fun PocketTag(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        label = "pocketTagContainer",
    )
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton) {
                performUIHaptic(view)
                onClick()
            },
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMediumCondensed,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(min = 28.dp)
                .wrapContentHeight(align = Alignment.CenterVertically),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "Pocket tags")
@Composable
private fun PocketTagPreview() {
    MinusTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PocketTag(label = "Savings", selected = false, onClick = {})
            PocketTag(label = "Japan trip", selected = true, onClick = {})
        }
    }
}
