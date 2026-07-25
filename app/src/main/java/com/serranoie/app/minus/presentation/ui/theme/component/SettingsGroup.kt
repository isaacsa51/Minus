package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

/**
 * A flexible settings group container that can hold any composable content.
 *
 * @param modifier Modifier to be applied to the container
 * @param title Optional title displayed above the group
 * @param content The composable content to be displayed inside the group
 */
@Composable
fun FlexibleListGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(vertical = 2.dp)
    ) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

/**
 * A standard settings item with title, subtitle, and customizable content.
 *
 * @param title The main title text
 * @param subtitle Optional subtitle text
 * @param onClick Click handler for the item
 * @param leadingIcon Optional leading icon composable
 * @param trailingContent Optional trailing content composable (defaults to arrow icon)
 * @param showDivider Whether to show a divider below this item
 */
@Composable
fun ListItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.invoke()

            if (leadingIcon != null) {
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface
                )

                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            trailingContent?.invoke() ?: Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showDivider) {
            HorizontalDivider(modifier = Modifier.padding(8.dp))
        }
    }
}

/**
 * A completely customizable settings item that provides only the clickable container.
 *
 * @param onClick Click handler for the item
 * @param content The custom content layout
 */
@Composable
fun CustomSettingsItem(
    onClick: () -> Unit, content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * An expandable accordion list. The header is the first list item (position `First`),
 * followed by selectable items starting from position `Middle`.
 * The chevron animates 180° on expand/collapse. No outer surface — each item
 * carries its own background color.
 *
 * @param isExpanded Whether the list is currently expanded
 * @param onToggleExpanded Callback to toggle the expanded state
 * @param modifier Modifier applied to the container
 * @param headerLabel Text for the header row
 * @param headerSubtitle Optional small description rendered below the header label in
 *                       `bodySmall` / `onSurfaceVariant`. When `null` (default), no
 *                       subtitle line is shown.
 * @param headerVerticalPadding Optional vertical padding for the header row. When `null`
 *                              (the default), the header keeps its standard 12.dp padding.
 *                              Pass a larger `Dp` value (e.g. `20.dp`) to make the header
 *                              card visually taller.
 * @param leadingIcon Composable for the leading icon on the header row
 * @param expandedContent Composable content for the selectable items below the header
 */
@Composable
fun PaddedExpandableList(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    headerLabel: String,
    containerPosition: PaddedListItemPosition = PaddedListItemPosition.First,
    headerSubtitle: String? = null,
    headerSubtitleContent: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    headerVerticalPadding: Dp? = null,
    expandedContent: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        SelectablePaddedItem(
            label = headerLabel,
            isActive = false,
            onClick = onToggleExpanded,
            position = containerPosition,
            verticalPadding = headerVerticalPadding ?: 12.dp,
            subtitle = headerSubtitle,
            subtitleContent = headerSubtitleContent,
            leadingIcon = leadingIcon,
            trailingContent = {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                content = expandedContent
            )
        }
    }
}

/**
 * A selectable row item for use inside [PaddedExpandableList]'s expanded content.
 * Supports two visual states:
 * - **Inactive:** blends into the surface background
 * - **Active:** pill-shaped with secondaryContainer background and larger corner radius
 *
 * @param label The text label for the item
 * @param isActive Whether the item is in active/selected state
 * @param onClick Click handler
 * @param modifier Modifier applied to the item
 * @param position The position in the list (affects inactive corner rounding)
 * @param verticalPadding Vertical padding inside the row. Defaults to 12.dp; pass a larger
 *                        value (e.g. from [PaddedExpandableList]'s `headerVerticalPadding`)
 *                        to make the row visually taller.
 * @param subtitle Optional small description rendered below the label in
 *                 `bodySmall` / `onSurfaceVariant`. When `null` (default), no subtitle is shown.
 * @param leadingIcon Optional leading icon composable
 * @param trailingContent Optional trailing content composable (e.g. chevron)
 */
@Composable
fun SelectablePaddedItem(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    position: PaddedListItemPosition = PaddedListItemPosition.Middle,
    verticalPadding: Dp = 12.dp,
    subtitle: String? = null,
    subtitleContent: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val shape = when {
        isActive -> RoundedCornerShape(16.dp)
        position == PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)
        position == PaddedListItemPosition.First -> RoundedCornerShape(
            topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp
        )
        position == PaddedListItemPosition.Last -> RoundedCornerShape(
            bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp
        )
        else -> RoundedCornerShape(4.dp)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        color = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (isActive) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.invoke()
            if (leadingIcon != null) {
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                subtitleContent?.invoke()
            }
            trailingContent?.invoke()
        }
    }
}

/**
 * A selectable card item that hosts arbitrary content, similar to how [PaddedListGroup] exposes
 * a [content] slot. Use this when a single selectable card needs to compose multiple
 * sub-elements (Text, Switch, Row, etc.) while preserving the segmented padded list shape and
 * the active/inactive styling of [SelectablePaddedItem].
 *
 * Supports two visual states:
 * - **Inactive:** blends into the surface background
 * - **Active:** pill-shaped with secondaryContainer background and larger corner radius
 *
 * @param isActive Whether the item is in active/selected state
 * @param onClick Click handler for the whole card
 * @param modifier Modifier applied to the item
 * @param position The position in the list (affects inactive corner rounding)
 * @param content Composable content rendered inside the card (ColumnScope)
 */
@Composable
fun SelectableInfoPaddedItem(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    position: PaddedListItemPosition = PaddedListItemPosition.Middle,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = when {
        isActive -> RoundedCornerShape(16.dp)
        position == PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)
        position == PaddedListItemPosition.First -> RoundedCornerShape(
            topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp
        )
        position == PaddedListItemPosition.Last -> RoundedCornerShape(
            bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp
        )
        else -> RoundedCornerShape(4.dp)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        color = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        contentColor = if (isActive) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/**
 * A padded list group container with rounded corners that handles item positioning automatically.
 *
 * @param modifier Modifier to be applied to the container
 * @param title Optional title displayed above the group
 * @param content The composable content to be displayed inside the group
 */
@Composable
fun PaddedListGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.padding(16.dp)) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content()
        }
    }
}

/**
 * A padded list item with automatic corner rounding based on position.
 *
 * @param title The main title text
 * @param subtitle Optional subtitle text
 * @param icon Leading icon
 * @param onClick Click handler for the item
 * @param position The position of this item in the list (affects corner rounding)
 */
@Composable
fun PaddedListItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    position: PaddedListItemPosition = PaddedListItemPosition.Middle,
) {
    val shape = when (position) {
        PaddedListItemPosition.First -> RoundedCornerShape(
            topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp
        )

        PaddedListItemPosition.Last -> RoundedCornerShape(
            bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp
        )

        PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)
        PaddedListItemPosition.Middle -> RoundedCornerShape(4.dp)
    }

    Surface(
        shape = shape,
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        Row(
            modifier = Modifier
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMediumEmphasized)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * A customizable padded list item with automatic corner rounding.
 *
 * @param onClick Click handler for the item
 * @param position The position of this item in the list (affects corner rounding)
 * @param background The background color for the item
 * @param contentColor The content color for the item
 * @param content The custom content layout
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomPaddedListItem(
    onClick: () -> Unit,
    position: PaddedListItemPosition = PaddedListItemPosition.Middle,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onLongClick: (() -> Unit)? = null,
    borderStroke: BorderStroke? = null,
    customShape: Shape? = null,
    content: @Composable RowScope.() -> Unit
) {
    val shape = customShape ?: when (position) {
        PaddedListItemPosition.First -> RoundedCornerShape(
            topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp
        )

        PaddedListItemPosition.Last -> RoundedCornerShape(
            bottomStart = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, topEnd = 4.dp
        )

        PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)
        PaddedListItemPosition.Middle -> RoundedCornerShape(4.dp)
    }

    Surface(
        shape = shape,
        color = background,
        contentColor = contentColor,
        border = borderStroke,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * A customizable expandable padded list item with automatic corner rounding.
 *
 * @param isExpanded Whether the item is currently expanded
 * @param onToggleExpanded Callback when the item is clicked to toggle expansion
 * @param position The position of this item in the list (affects corner rounding)
 * @param defaultContent The content to show when collapsed
 * @param expandedContent The content to show when expanded
 */
@Composable
fun CustomPaddedExpandableItem(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    position: PaddedListItemPosition = PaddedListItemPosition.Middle,
    modifier: Modifier = Modifier,
    defaultContent: @Composable RowScope.() -> Unit,
    expandedContent: @Composable ColumnScope.() -> Unit
) {
    val shape = when (position) {
        PaddedListItemPosition.First -> RoundedCornerShape(
            topStart = 16.dp, topEnd = 16.dp
        )

        PaddedListItemPosition.Last -> RoundedCornerShape(
            bottomStart = 16.dp, bottomEnd = 16.dp
        )

        PaddedListItemPosition.Single -> RoundedCornerShape(16.dp)
        PaddedListItemPosition.Middle -> RoundedCornerShape(8.dp)
    }

    Surface(
        shape = shape,
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .clickable { onToggleExpanded() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = defaultContent)

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp), content = expandedContent
                )
            }
        }
    }
}

enum class PaddedListItemPosition {
    First, Middle, Last, Single
}

@Preview(showBackground = true)
@Composable
fun FlexibleSettingsGroupPreview() {
    MinusTheme {
        LazyColumn {
            item {
                FlexibleListGroup(
                    title = "Standard Items"
                ) {
                    ListItem(
                        title = "Setting 1",
                        subtitle = "Description",
                        onClick = { },
                        showDivider = true
                    )
                    ListItem(
                        title = "Setting 2", onClick = { })
                }
            }

            item {
                // Example 2: Custom content with any composables
                FlexibleListGroup(
                    title = "Custom Content"
                ) {
                    CustomSettingsItem(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom Item with Icon",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "This shows custom layout",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "Value",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(8.dp))

                    // Any other composable can go here
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "You can put any composable content here!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                FlexibleListGroup(
                    title = "Padded Item Variations"
                ) {
                    // Example with leading icon and single position
                    PaddedListItem(
                        title = "Notifications",
                        subtitle = "App, system, and emergency",
                        icon = Icons.Default.Settings,
                        onClick = {},
                        position = PaddedListItemPosition.First
                    )

                    CustomPaddedListItem(
                        onClick = { /* Custom action */ }, position = PaddedListItemPosition.Middle
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Advanced Settings",
                                style = MaterialTheme.typography.bodyMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Developer options and diagnostics",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Example of trailing content: badge or text
                        Text(
                            text = "Beta",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Example with no subtitle and last position
                    PaddedListItem(
                        title = "Reset Settings",
                        icon = Icons.Default.Settings,
                        onClick = {},
                        position = PaddedListItemPosition.Last
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PaddedListGroupPreview() {
    MinusTheme {
        LazyColumn {
            item {
                PaddedListGroup(
                    title = "Settings"
                ) {
                    PaddedListItem(
                        title = "Google",
                        subtitle = "Services and preferences",
                        icon = Icons.Default.Settings,
                        onClick = { },
                        position = PaddedListItemPosition.First
                    )
                    PaddedListItem(
                        title = "Network and Internet",
                        subtitle = "Mobile, Wi-Fi, hotspot",
                        icon = Icons.Default.Settings,
                        onClick = { },
                        position = PaddedListItemPosition.Middle
                    )
                    PaddedListItem(
                        title = "Connected devices",
                        subtitle = "Bluetooth, pairing",
                        icon = Icons.Default.Settings,
                        onClick = { },
                        position = PaddedListItemPosition.Last
                    )
                }
            }

            item {
                PaddedListGroup(
                    title = "Custom Content"
                ) {
                    CustomPaddedListItem(
                        onClick = { }, position = PaddedListItemPosition.First
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom Padded Item",
                                style = MaterialTheme.typography.bodyMediumEmphasized
                            )
                            Text(
                                text = "This shows custom layout with padding",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "Value",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    var isExpanded by remember { mutableStateOf(false) }
                    CustomPaddedExpandableItem(
                        isExpanded = isExpanded,
                        onToggleExpanded = { isExpanded = !isExpanded },
                        position = PaddedListItemPosition.Last,
                        defaultContent = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Expandable Item",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Click to expand/collapse",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        expandedContent = {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "This is the expanded content!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "You can put any composable content here when expanded.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        })
                }
            }
        }
    }
}

@Preview
@Composable
fun PaddedExpandableListPreview() {
    MinusTheme {
        var isExpanded by remember { mutableStateOf(true) }

        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                PaddedListGroup(title = null) {
                    PaddedExpandableList(
                        isExpanded = isExpanded,
                        onToggleExpanded = { isExpanded = !isExpanded },
                        modifier = Modifier.padding(bottom = 16.dp),
                        headerLabel = "Categories",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        expandedContent = {
                            val items = listOf("Food", "Transport", "Shopping", "Entertainment", "Bills")

                            items.forEachIndexed { index, item ->
                                val position = when {
                                    items.size == 1 -> PaddedListItemPosition.Single
                                    index == 0 -> PaddedListItemPosition.Middle
                                    index == items.lastIndex -> PaddedListItemPosition.Last
                                    else -> PaddedListItemPosition.Middle
                                }

                                SelectablePaddedItem(
                                    label = item,
                                    isActive = item == "Shopping",
                                    onClick = { },
                                    position = position,
                                    modifier = Modifier.padding(bottom = 2.dp),
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
