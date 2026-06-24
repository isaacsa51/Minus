package com.serranoie.app.minus.presentation.ui.history.edit

import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

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
        modifier = modifier,
        colors = ToggleButtonColors(
            // Unchecked: tertiary container tint at 50% — soft, secondary feel.
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
            contentColor = tertiary,
            // Checked: tertiary tint at 22% — visually selected without screaming.
            checkedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            checkedContentColor = tertiary,
            disabledContentColor = MaterialTheme.colorScheme.outline,
            disabledContainerColor = MaterialTheme.colorScheme.outlineVariant
        ),
    ) {
        Icon(
            imageVector = Icons.Rounded.EventRepeat,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.recurrent_toggle_label), style = MaterialTheme.typography.titleSmallEmphasized)
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