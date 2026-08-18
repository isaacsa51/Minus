package com.serranoie.app.minus.presentation.ui.settings.bugreport.component

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.serranoie.app.minus.R
import com.serranoie.app.minus.presentation.ui.theme.MinusTheme

@Composable
fun AttachmentThumbnailCard(
    uri: Uri,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVideo = remember(uri) {
        context.contentResolver.getType(uri)?.startsWith("video/") == true
    }

    Box(modifier = modifier.padding(top = 8.dp, end = 8.dp)) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(uri)
                    .size(96, 96)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(8.dp).padding(2.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                },
                error = {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.bug_report_remove_attachment),
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AttachmentThumbnailCardPreview() {
    MinusTheme {
        AttachmentThumbnailCard(
            uri = Uri.parse("content://com.serranoie.app.minus.fileprovider/preview/screenshot.png"),
            onRemove = {}
        )
    }
}

@Preview(name = "Row of attachments", showBackground = true)
@Composable
private fun AttachmentThumbnailCardRowPreview() {
    MinusTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(3) { index ->
                AttachmentThumbnailCard(
                    uri = Uri.parse("content://com.serranoie.app.minus.fileprovider/preview/screenshot_$index.png"),
                    onRemove = {}
                )
            }
        }
    }
}
