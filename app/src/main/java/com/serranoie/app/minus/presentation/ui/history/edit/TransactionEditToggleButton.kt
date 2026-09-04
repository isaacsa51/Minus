package com.serranoie.app.minus.presentation.ui.history.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

/** Fixed size so the [ToggleButtonDefaults.checkedShape] override renders as an exact circle. */
private val ToggleButtonSize = 40.dp

private val ToggleButtonOuterCorner = ToggleButtonSize / 2

private val ToggleButtonInnerCorner = 6.dp

internal enum class TransactionEditTogglePosition {
    STANDALONE,
    LEADING,
    TRAILING,
}

private fun restShapeFor(position: TransactionEditTogglePosition): RoundedCornerShape =
    when (position) {
        TransactionEditTogglePosition.STANDALONE -> RoundedCornerShape(ToggleButtonOuterCorner)

        TransactionEditTogglePosition.LEADING -> RoundedCornerShape(
            topStart = ToggleButtonOuterCorner,
            bottomStart = ToggleButtonOuterCorner,
            topEnd = ToggleButtonInnerCorner,
            bottomEnd = ToggleButtonInnerCorner,
        )

        TransactionEditTogglePosition.TRAILING -> RoundedCornerShape(
            topStart = ToggleButtonInnerCorner,
            bottomStart = ToggleButtonInnerCorner,
            topEnd = ToggleButtonOuterCorner,
            bottomEnd = ToggleButtonOuterCorner,
        )
    }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TransactionEditToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    position: TransactionEditTogglePosition = TransactionEditTogglePosition.STANDALONE,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below
        ),
        tooltip = {
            PlainTooltip(
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Assertive
                    paneTitle = contentDescription
                }
            ) { Text(contentDescription) }
        },
        state = rememberTooltipState(),
    ) {
        val restShape = restShapeFor(position)
        ToggleButton(
            checked = checked,
            onCheckedChange = onCheckedChange,
            shapes = ToggleButtonShapes(
                shape = restShape,
                pressedShape = restShape,
                checkedShape = CircleShape,
            ),
            colors = ToggleButtonDefaults.toggleButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                checkedContainerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.tertiary,
                checkedContentColor = MaterialTheme.colorScheme.onTertiary,
            ),
            contentPadding = PaddingValues(0.dp),
            modifier = modifier
                .size(ToggleButtonSize)
                .semantics { role = Role.RadioButton },
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionEditToggleButtonPreview() {
    MinusTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TransactionEditToggleButton(
                checked = false,
                onCheckedChange = {},
                icon = Icons.Rounded.CreditCard,
                contentDescription = "Credit card payment",
                position = TransactionEditTogglePosition.LEADING,
            )
            TransactionEditToggleButton(
                checked = false,
                onCheckedChange = {},
                icon = Icons.Rounded.EventRepeat,
                contentDescription = "Recurrent payment",
                position = TransactionEditTogglePosition.TRAILING,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionEditToggleButtonCheckedPreview() {
    MinusTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TransactionEditToggleButton(
                checked = true,
                onCheckedChange = {},
                icon = Icons.Rounded.CreditCard,
                contentDescription = "Credit card payment",
                position = TransactionEditTogglePosition.LEADING,
            )
            TransactionEditToggleButton(
                checked = true,
                onCheckedChange = {},
                icon = Icons.Rounded.EventRepeat,
                contentDescription = "Recurrent payment",
                position = TransactionEditTogglePosition.TRAILING,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TransactionEditToggleButtonStandalonePreview() {
    MinusTheme {
        TransactionEditToggleButton(
            checked = false,
            onCheckedChange = {},
            icon = Icons.Rounded.EventRepeat,
            contentDescription = "Recurrent payment",
        )
    }
}
