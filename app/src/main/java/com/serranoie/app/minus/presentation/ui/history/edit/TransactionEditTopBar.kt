package com.serranoie.app.minus.presentation.ui.history.edit

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import androidx.compose.ui.tooling.preview.Preview
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme


@Composable
internal fun TransactionEditTopBar(
    isRecurrent: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onCancel,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cancel_edit_content_desc),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = if (isRecurrent) {
                stringResource(R.string.edit_recurrent_expense_title)
            } else {
                stringResource(R.string.edit_expense_title)
            },
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 8.dp)
                .basicMarquee()
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun TransactionEditTopBarPreview() {
    MinusTheme {
        TransactionEditTopBar(
            isRecurrent = false,
            onCancel = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionEditTopBarRecurrentPreview() {
    MinusTheme {
        TransactionEditTopBar(
            isRecurrent = true,
            onCancel = {},
        )
    }
}
