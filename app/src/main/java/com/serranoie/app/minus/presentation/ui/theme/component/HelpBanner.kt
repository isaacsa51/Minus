package com.serranoie.app.minus.presentation.ui.theme.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme
import com.serranoie.app.minus.presentation.ui.theme.bodySmallCondensed

@Composable
fun HelpBanner(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.settings_what_is_this_for),
    icon: ImageVector = Icons.AutoMirrored.Outlined.Help,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmallCondensed,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 14.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun HelpBanner(
    text: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.settings_what_is_this_for),
    icon: ImageVector = Icons.AutoMirrored.Outlined.Help,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
) {
    HelpBanner(
        text = AnnotatedString(text),
        modifier = modifier,
        title = title,
        icon = icon,
        backgroundColor = backgroundColor,
    )
}

@PreviewLightDark
@Composable
private fun HelpBannerPreview() {
    MinusTheme {
        HelpBanner(
            text = "Minus reads the notifications of the apps you pick, on your device only.",
            modifier = Modifier.padding(14.dp),
        )
    }
}
