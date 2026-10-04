package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SortCategory
import kotlin.math.abs

@Composable
fun CategorySegmentedRow(
    selectedCategory: SortCategory,
    onCategorySelected: (SortCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val categories = remember {
        listOf(
            SortCategory.TIME to R.string.sort_category_time,
            SortCategory.SIZE to R.string.sort_category_size,
            SortCategory.TYPE to R.string.sort_category_type
        )
    }

    val selectedIndex = when (selectedCategory) {
        SortCategory.TIME -> 0
        SortCategory.SIZE -> 1
        SortCategory.TYPE -> 2
    }

    // Shevery navigation bar physics: Track child item bounds dynamically in parent
    val buttonBounds = remember { mutableStateMapOf<Int, Rect>() }
    val targetRect = buttonBounds[selectedIndex]
    val firstRect = buttonBounds[0]

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .padding(4.dp)
    ) {
        val totalWidth = maxWidth
        val fallbackSlotWidthPx = with(density) { (totalWidth / categories.size).toPx() }

        val targetWidthPx = targetRect?.width ?: fallbackSlotWidthPx
        val targetRelativeXPx = if (targetRect != null && firstRect != null) {
            targetRect.left - firstRect.left
        } else {
            fallbackSlotWidthPx * selectedIndex
        }

        // Low stiffness, low bouncy spring physics matching Shevery Expressive Navigation Bar
        val springSpec = remember {
            spring<Float>(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        }

        val pillAnimatedX by animateFloatAsState(
            targetValue = targetRelativeXPx,
            animationSpec = springSpec,
            label = "pillX"
        )

        val pillAnimatedWidth by animateFloatAsState(
            targetValue = targetWidthPx,
            animationSpec = springSpec,
            label = "pillWidth"
        )

        // Dynamic jelly squash & stretch derived from transition distance
        val transitDelta = abs(targetRelativeXPx - pillAnimatedX)
        val jellyStretch = (transitDelta / 140f).coerceIn(0f, 0.35f)
        val scaleX = 1.0f + (jellyStretch * 0.18f)
        val scaleY = 1.0f - (jellyStretch * 0.22f)

        // Layer 1: Unselected category labels with bounds tracking
        Row(modifier = Modifier.fillMaxSize()) {
            categories.forEachIndexed { index, (category, stringRes) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { coords ->
                            buttonBounds[index] = coords.boundsInParent()
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onCategorySelected(category)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(stringRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Layer 2: Animated Shevery Jelly Pill Indicator with clipped selected labels
        if (pillAnimatedWidth > 0f) {
            val pillWidthDp = with(density) { pillAnimatedWidth.toDp() }

            Box(
                modifier = Modifier
                    .offset { IntOffset(x = pillAnimatedX.toInt(), y = 0) }
                    .width(pillWidthDp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        this.scaleX = scaleX
                        this.scaleY = scaleY
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .shadow(2.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .offset { IntOffset(x = -pillAnimatedX.toInt(), y = 0) }
                        .wrapContentWidth(unbounded = true, align = Alignment.Start)
                        .requiredWidth(totalWidth)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        categories.forEach { (_, stringRes) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(stringRes),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategorySegmentedRowPreview() {
    var selected by remember { mutableStateOf(SortCategory.TIME) }
    Box(modifier = Modifier.padding(16.dp)) {
        CategorySegmentedRow(
            selectedCategory = selected,
            onCategorySelected = { selected = it }
        )
    }
}
