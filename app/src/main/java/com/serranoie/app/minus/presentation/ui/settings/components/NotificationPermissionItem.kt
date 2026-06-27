package com.serranoie.app.minus.presentation.ui.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.component.CustomPaddedListItem
import com.serranoie.app.minus.presentation.ui.theme.component.PaddedListItemPosition

@Composable
fun NotificationPermissionItem(
    granted: Boolean,
    onClick: () -> Unit,
    position: PaddedListItemPosition,
) {
    CustomPaddedListItem(
        onClick = onClick,
        position = position,
        borderStroke = if (!granted) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        } else {
            null
        },
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
            contentDescription = null,
            tint = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_notification_permission_title),
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = if (granted) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            Text(
                text = if (granted) {
                    stringResource(R.string.settings_notification_permission_granted_subtitle)
                } else {
                    stringResource(R.string.settings_notification_permission_denied_subtitle)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
        if (!granted) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
private fun PreviewNotificationPermissionItem() {
    Column {
        NotificationPermissionItem(
            granted = true,
            onClick = {},
            position = PaddedListItemPosition.First,
        )

        NotificationPermissionItem(
            granted = false,
            onClick = {},
            position = PaddedListItemPosition.Last,
        )
    }
}
