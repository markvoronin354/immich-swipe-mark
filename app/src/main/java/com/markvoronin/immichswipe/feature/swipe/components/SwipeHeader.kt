package com.markvoronin.immichswipe.feature.swipe

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R


@Composable
fun SwipeHeader(
    uiState: SwipeUiState,
    onSummaryClick: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "ProgressBarAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onSummaryClick() },
            contentAlignment = Alignment.CenterStart
        ) {
            val totalWidth = constraints.maxWidth.toFloat()
            val progressWidth = totalWidth * animatedProgress
            val density = LocalDensity.current
            val paddingPx = with(density) { 16.dp.toPx() }
            val spacingPx = paddingPx

            val infoWidthPx = with(density) { 72.dp.toPx() }
            val infoIsInside = progressWidth > infoWidthPx + paddingPx
            val infoTranslationX by animateFloatAsState(
                targetValue = if (infoIsInside)
                    (progressWidth - infoWidthPx - paddingPx).coerceAtLeast(paddingPx)
                else
                    totalWidth - infoWidthPx - paddingPx,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "InfoTranslation"
            )

            val titleThreshold = with(density) { 250.dp.toPx() }
            val titleIsPushed = progressWidth > paddingPx && progressWidth < titleThreshold
            val titleTranslationX by animateFloatAsState(
                targetValue = if (titleIsPushed) progressWidth + spacingPx else paddingPx,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "TitleTranslation"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                HeaderTitle(
                    text = uiState.albumName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { translationX = titleTranslationX }.align(Alignment.CenterStart)
                )
                HeaderInfo(
                    progressText = "${(uiState.progress * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.graphicsLayer { translationX = infoTranslationX }.align(Alignment.CenterStart)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clipToBounds()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .background(Color.Black.copy(alpha = 0.1f))
            ) {
                Box(modifier = Modifier.width(with(density) { totalWidth.toDp() }).fillMaxHeight()) {
                    HeaderTitle(
                        text = uiState.albumName,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer { translationX = titleTranslationX }.align(Alignment.CenterStart)
                    )
                    HeaderInfo(
                        progressText = "${(uiState.progress * 100).toInt()}%",
                        color = Color.White,
                        modifier = Modifier.graphicsLayer { translationX = infoTranslationX }.align(Alignment.CenterStart)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatBadge(label = stringResource(R.string.swipe_keep), count = uiState.allKeptCount, color = MaterialGreen)
            StatBadge(label = stringResource(R.string.swipe_delete), count = uiState.deletedCount, color = MaterialRed)
            StatBadge(label = stringResource(R.string.swipe_remaining), count = uiState.remainingCount, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun HeaderTitle(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun HeaderInfo(progressText: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = progressText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = color
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color
        )
    }
}

@Composable
fun StatBadge(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$count $label",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
