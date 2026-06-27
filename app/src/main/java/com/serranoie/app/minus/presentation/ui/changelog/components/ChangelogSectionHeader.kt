package com.serranoie.app.minus.presentation.ui.changelog.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.domain.model.changelog.ReleaseType
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

@Composable
internal fun ChangelogSectionHeader(
    title: String,
    type: ReleaseType,
    modifier: Modifier = Modifier,
) {
    val accent = changelogSectionAccent(type)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = changelogSectionIcon(type),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized.copy(
                    letterSpacing = 1.5.sp,
                ),
                color = accent,
            )
        }
    }
}

@Composable
private fun changelogSectionAccent(type: ReleaseType): Color = when (type) {
    ReleaseType.FEATURE -> MaterialTheme.colorScheme.tertiary
    ReleaseType.IMPROVEMENT -> MaterialTheme.colorScheme.primary
    ReleaseType.BUG_FIX -> MaterialTheme.colorScheme.error
}

private fun changelogSectionIcon(type: ReleaseType): ImageVector = when (type) {
    ReleaseType.FEATURE -> Icons.Rounded.Add
    ReleaseType.IMPROVEMENT -> Icons.Rounded.AutoAwesome
    ReleaseType.BUG_FIX -> Icons.Rounded.BugReport
}

@Preview(showBackground = true, name = "Section header — Bug Fixes", heightDp = 56)
@Composable
private fun ChangelogSectionHeaderBugFixPreview() {
    MinusTheme {
        ChangelogSectionHeader(title = "Bug Fixes", type = ReleaseType.BUG_FIX)
    }
}

@Preview(showBackground = true, name = "Section header — New Features", heightDp = 56)
@Composable
private fun ChangelogSectionHeaderFeaturePreview() {
    MinusTheme {
        ChangelogSectionHeader(title = "New Features", type = ReleaseType.FEATURE)
    }
}

@Preview(showBackground = true, name = "Section header — Improvements", heightDp = 56)
@Composable
private fun ChangelogSectionHeaderImprovementPreview() {
    MinusTheme {
        ChangelogSectionHeader(title = "Improvements", type = ReleaseType.IMPROVEMENT)
    }
}