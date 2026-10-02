package com.markvoronin.immichswipe.feature.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.verticalFadingEdges(
    scrollState: ScrollState,
    length: Dp = 32.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val lengthPx = length.toPx()
        if (lengthPx <= 0f) return@drawWithContent

        val topFadePx = minOf(scrollState.value.toFloat(), lengthPx)

        val remainingScroll = (scrollState.maxValue - scrollState.value).toFloat().coerceAtLeast(0f)
        val bottomFadePx = if (scrollState.maxValue > 0 && scrollState.maxValue != Int.MAX_VALUE) {
            minOf(remainingScroll, lengthPx)
        } else if (scrollState.maxValue == Int.MAX_VALUE) {
            lengthPx
        } else {
            0f
        }

        if (topFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = 0f,
                    endY = topFadePx
                ),
                blendMode = BlendMode.DstOut
            )
        }

        if (bottomFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = size.height - bottomFadePx,
                    endY = size.height
                ),
                blendMode = BlendMode.DstOut
            )
        }
    }

fun Modifier.verticalFadingEdges(
    lazyListState: LazyListState,
    length: Dp = 32.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val lengthPx = length.toPx()
        if (lengthPx <= 0f) return@drawWithContent

        val topFadePx = if (lazyListState.firstVisibleItemIndex == 0) {
            minOf(lazyListState.firstVisibleItemScrollOffset.toFloat(), lengthPx)
        } else {
            lengthPx
        }

        val bottomFadePx = if (lazyListState.canScrollForward) {
            lengthPx
        } else {
            0f
        }

        if (topFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = 0f,
                    endY = topFadePx
                ),
                blendMode = BlendMode.DstOut
            )
        }

        if (bottomFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = size.height - bottomFadePx,
                    endY = size.height
                ),
                blendMode = BlendMode.DstOut
            )
        }
    }

fun Modifier.verticalFadingEdges(
    lazyGridState: LazyGridState,
    length: Dp = 32.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val lengthPx = length.toPx()
        if (lengthPx <= 0f) return@drawWithContent

        val topFadePx = if (lazyGridState.firstVisibleItemIndex == 0) {
            minOf(lazyGridState.firstVisibleItemScrollOffset.toFloat(), lengthPx)
        } else {
            lengthPx
        }

        val bottomFadePx = if (lazyGridState.canScrollForward) {
            lengthPx
        } else {
            0f
        }

        if (topFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = 0f,
                    endY = topFadePx
                ),
                blendMode = BlendMode.DstOut
            )
        }

        if (bottomFadePx > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = size.height - bottomFadePx,
                    endY = size.height
                ),
                blendMode = BlendMode.DstOut
            )
        }
    }

fun Modifier.horizontalFadingEdges(
    lazyListState: LazyListState,
    length: Dp = 24.dp
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val lengthPx = length.toPx()
        if (lengthPx <= 0f) return@drawWithContent

        val leftFadePx = if (lazyListState.firstVisibleItemIndex == 0) {
            minOf(lazyListState.firstVisibleItemScrollOffset.toFloat(), lengthPx)
        } else {
            lengthPx
        }

        val rightFadePx = if (lazyListState.canScrollForward) {
            lengthPx
        } else {
            0f
        }

        if (leftFadePx > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = 0f,
                    endX = leftFadePx
                ),
                blendMode = BlendMode.DstOut
            )
        }

        if (rightFadePx > 0f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = size.width - rightFadePx,
                    endX = size.width
                ),
                blendMode = BlendMode.DstOut
            )
        }
    }

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val effectiveTitleColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val effectiveIconColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = effectiveIconColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = effectiveTitleColor
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun SettingsToggleItemSmall(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(0.8f)
        )
    }
}

@Composable
fun ThemeButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
