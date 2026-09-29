package com.markvoronin.immichswipe.feature.swipe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Asset

@Composable
fun MetadataPanel(
    asset: Asset,
    onClose: () -> Unit,
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    panelProgress: Float = 0f,
    maxHeightPx: Float = 0f
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(panelProgress) {
        if (panelProgress == 0f) {
            scrollState.scrollTo(0)
        }
    }

    // Connection to pass downward drags to the parent when at the top of the scroll
    val nestedScrollConnection = remember(scrollState, panelProgress, maxHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If dragging DOWN (available.y > 0) and at top of scroll, consume to move panel down
                // If dragging UP (available.y < 0) and at top of scroll when panel is not fully expanded, consume to move panel up
                if (available.y > 0 && scrollState.value == 0) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                } else if (available.y < 0 && scrollState.value == 0 && panelProgress < 1f) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Handle cases where scroll reached the top during the gesture
                if (available.y > 0 && scrollState.value == 0) {
                    onDrag(available.y)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                onDragEnd()
                return super.onPostFling(consumed, available)
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(nestedScrollConnection),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp)
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.size(40.dp, 4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.swipe_metadata_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(R.string.common_close), modifier = Modifier.size(20.dp)) }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            MetadataRow(Icons.Default.Description, stringResource(R.string.swipe_metadata_file), asset.originalFileName ?: stringResource(R.string.diag_unknown))
            MetadataRow(Icons.Default.CalendarToday, stringResource(R.string.swipe_metadata_date), asset.fileCreatedAt.substringBefore("T"))
            val formatLabel = if (asset.fileExtension != null) "${asset.type} (.${asset.fileExtension.lowercase()})" else asset.type
            MetadataRow(Icons.Default.Info, stringResource(R.string.swipe_metadata_format), formatLabel)
            asset.exifInfo?.let { exif ->
                val sizeLabel = exif.fileSizeInBytes?.let { formatSize(it) } ?: "N/A"
                MetadataRow(Icons.Default.SdStorage, stringResource(R.string.swipe_metadata_size), sizeLabel)
                MetadataRow(Icons.Default.AspectRatio, stringResource(R.string.swipe_metadata_resolution), "${exif.imageWidth ?: "?"} x ${exif.imageHeight ?: "?"}")
            } ?: run {
                MetadataRow(Icons.Default.SdStorage, stringResource(R.string.swipe_metadata_size), stringResource(R.string.swipe_loading))
                MetadataRow(Icons.Default.AspectRatio, stringResource(R.string.swipe_metadata_resolution), stringResource(R.string.swipe_loading))
            }
        }
    }
}

@Composable
fun MetadataRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.width(80.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
