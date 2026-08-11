package com.serranoie.app.minus.presentation.ui.history.edit

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedToggleButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonColors
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.labelSmallCondensed

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun RecurrenceToggleButton(
    isRecurrent: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tertiary = MaterialTheme.colorScheme.tertiary

    ElevatedToggleButton(
        checked = isRecurrent,
        onCheckedChange = onToggle,
        modifier = modifier.height(40.dp),
        colors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
            checkedContainerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.tertiary,
            checkedContentColor = MaterialTheme.colorScheme.onTertiary
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.EventRepeat,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(
            text = stringResource(R.string.recurrent_toggle_label),
            style = MaterialTheme.typography.labelSmallEmphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecurrenceToggleButtonOnPreview() {
    MinusTheme {
        RecurrenceToggleButton(
            isRecurrent = true,
            onToggle = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecurrenceToggleButtonOffPreview() {
    MinusTheme {
        RecurrenceToggleButton(
            isRecurrent = false,
            onToggle = {},
        )
    }
}
