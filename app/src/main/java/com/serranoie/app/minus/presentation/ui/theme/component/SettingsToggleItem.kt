@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

/**
 * The circular leading icon used by every settings row.
 *
 * @param icon The glyph rendered inside the circle
 * @param active Inverts the circle onto the active container colours, matching a
 *               [SettingsToggleItem] that is switched on
 * @param tint Overrides the glyph colour, e.g. `colorScheme.error` for a row that needs attention
 */
@Composable
fun SettingsLeadingIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    tint: Color? = null,
) {
    val container by animateColorAsState(
        if (active) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "settingsLeadingIconContainer",
    )
    val glyph by animateColorAsState(
        tint ?: if (active) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primary
        },
        label = "settingsLeadingIconTint",
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = glyph,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * A settings row that toggles a boolean.
 *
 * The whole row is one toggleable target: the [Switch] is decorative and the card owns the click,
 * so screen readers announce a single `Role.Switch` node. Turning it on morphs the corners to
 * [MaterialTheme.shapes]`.extraLarge` and cross-fades onto the secondary container.
 *
 * @param label Optional short tag rendered under the title, e.g. "Experimental"
 * @param expandDescription When true the [description] is hidden until the row is switched on and
 *                          then expands into view; when false it always sits under the title
 */
@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    position: PaddedListItemPosition = PaddedListItemPosition.Middle,
    label: String? = null,
    expandDescription: Boolean = true,
) {
    val toggleState = stringResource(
        if (checked) R.string.settings_feature_state_on else R.string.settings_feature_state_off
    )
    val selection by animateFloatAsState(
        if (checked) 1f else 0f,
        label = "settingsToggleSelection",
    )
    val container by animateColorAsState(
        if (checked) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "settingsToggleContainer",
    )
    val onContainer by animateColorAsState(
        if (checked) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "settingsToggleOnContainer",
    )
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val shape = MorphCornerShape(
        position.toShape(),
        MaterialTheme.shapes.extraLarge,
        selection,
    )
    val rowToggleModifier = Modifier
        .toggleable(
            value = checked,
            onValueChange = { onToggle() },
            role = Role.Switch,
        )
        .semantics {
            stateDescription = toggleState
        }

    val row: @Composable RowScope.() -> Unit = {
        SettingsLeadingIcon(icon = icon, active = checked)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMediumEmphasized,
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmallEmphasized,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            if (!expandDescription) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))
        VerticalDivider(
            modifier = Modifier.height(32.dp),
            color = dividerColor,
        )
        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }

    if (expandDescription) {
        CustomPaddedExpandableItem(
            isExpanded = checked,
            onToggleExpanded = onToggle,
            position = position,
            background = container,
            contentColor = onContainer,
            customShape = shape,
            modifier = modifier,
            rowModifier = rowToggleModifier,
            defaultContent = row,
            expandedContent = {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    color = dividerColor,
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            },
        )
    } else {
        CustomPaddedListItem(
            onClick = onToggle,
            position = position,
            modifier = modifier,
            rowModifier = rowToggleModifier,
            background = container,
            contentColor = onContainer,
            customShape = shape,
            content = row,
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingsToggleItemPreview() {
    MinusTheme {
        PaddedListGroup(title = "Preview") {
            SettingsToggleItem(
                icon = Icons.Rounded.Palette,
                title = "Material You",
                description = "Use the colours from your wallpaper across the app.",
                checked = false,
                onToggle = {},
                position = PaddedListItemPosition.First,
            )
            SettingsToggleItem(
                icon = Icons.Rounded.Contrast,
                title = "Pure black",
                description = "Use pure black backgrounds and surfaces in dark mode.",
                checked = true,
                onToggle = {},
                position = PaddedListItemPosition.Last,
                label = "Experimental",
            )
        }
    }
}
