package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SortCategory
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.SwipeViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SortOrderPopupMenu(
    uiState: SwipeUiState,
    viewModel: SwipeViewModel,
    onDismiss: () -> Unit,
    offsetY: Dp = 41.dp
) {
    val density = LocalDensity.current
    val yOffsetPx = with(density) { -offsetY.roundToPx() }

    var selectedSortOrder by remember { mutableStateOf(uiState.sortOrder) }

    var visible by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    val dismissWithAnimation = {
        if (!isDismissing) {
            isDismissing = true
            visible = false
        }
    }

    LaunchedEffect(isDismissing) {
        if (isDismissing) {
            delay(180.milliseconds)
            if (selectedSortOrder != uiState.sortOrder) {
                viewModel.setSortOrder(selectedSortOrder)
            }
            onDismiss()
        }
    }

    val selectedCategory = when (selectedSortOrder) {
        SortOrder.CHRONOLOGICAL_DESC, SortOrder.CHRONOLOGICAL_ASC, SortOrder.SHUFFLED -> SortCategory.TIME
        SortOrder.SIZE_DESC, SortOrder.SIZE_ASC -> SortCategory.SIZE
        else -> SortCategory.TYPE
    }

    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(0, yOffsetPx),
        onDismissRequest = dismissWithAnimation,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .width(300.dp)
                .height(420.dp)
                .pointerInput(Unit) {
                    detectTapGestures { dismissWithAnimation() }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 200)) +
                        scaleIn(
                            initialScale = 0.85f,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        ) +
                        expandVertically(
                            expandFrom = Alignment.Bottom,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        ),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                       scaleOut(
                           targetScale = 0.85f,
                           animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                       ) +
                       shrinkVertically(
                           shrinkTowards = Alignment.Bottom,
                           animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                       )
            ) {
                Surface(
                    modifier = Modifier
                        .width(300.dp)
                        .pointerInput(Unit) { detectTapGestures {} },
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    shadowElevation = 8.dp,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        AnimatedContent(
                            targetState = selectedCategory,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
                                    fadeOut(animationSpec = tween(90)) using
                                    SizeTransform(
                                        clip = false,
                                        sizeAnimationSpec = { _, _ -> tween(300) }
                                    )
                            },
                            contentAlignment = Alignment.BottomCenter,
                            label = "categoryContent"
                        ) { category ->
                            Column {
                                when (category) {
                                    SortCategory.TIME -> {
                                        SortPopupItem(
                                            R.string.settings_sort_newest,
                                            Icons.Default.ArrowDownward,
                                            selectedSortOrder == SortOrder.CHRONOLOGICAL_DESC
                                        ) {
                                            selectedSortOrder = SortOrder.CHRONOLOGICAL_DESC
                                        }
                                        SortPopupItem(
                                            R.string.settings_sort_oldest,
                                            Icons.Default.ArrowUpward,
                                            selectedSortOrder == SortOrder.CHRONOLOGICAL_ASC
                                        ) {
                                            selectedSortOrder = SortOrder.CHRONOLOGICAL_ASC
                                        }
                                        SortPopupItem(
                                            R.string.settings_sort_shuffled,
                                            Icons.Default.Shuffle,
                                            selectedSortOrder == SortOrder.SHUFFLED
                                        ) {
                                            selectedSortOrder = SortOrder.SHUFFLED
                                        }
                                    }
                                    SortCategory.SIZE -> {
                                        SortPopupItem(
                                            R.string.settings_sort_biggest,
                                            Icons.Default.ExpandMore,
                                            selectedSortOrder == SortOrder.SIZE_DESC
                                        ) {
                                            selectedSortOrder = SortOrder.SIZE_DESC
                                        }
                                        SortPopupItem(
                                            R.string.settings_sort_smallest,
                                            Icons.Default.ExpandLess,
                                            selectedSortOrder == SortOrder.SIZE_ASC
                                        ) {
                                            selectedSortOrder = SortOrder.SIZE_ASC
                                        }
                                    }
                                    SortCategory.TYPE -> {
                                        val currentIsPhoto = selectedSortOrder == SortOrder.TYPE_PHOTO_FIRST ||
                                                selectedSortOrder == SortOrder.TYPE_PHOTO_FIRST_ASC ||
                                                selectedSortOrder == SortOrder.TYPE_PHOTO_FIRST_SHUFFLED

                                        SortPopupItem(
                                            R.string.settings_sort_videos,
                                            Icons.Default.Videocam,
                                            !currentIsPhoto
                                        ) {
                                            selectedSortOrder = when (selectedSortOrder) {
                                                SortOrder.TYPE_PHOTO_FIRST_ASC -> SortOrder.TYPE_VIDEO_FIRST_ASC
                                                SortOrder.TYPE_PHOTO_FIRST_SHUFFLED -> SortOrder.TYPE_VIDEO_FIRST_SHUFFLED
                                                else -> SortOrder.TYPE_VIDEO_FIRST
                                            }
                                        }
                                        SortPopupItem(
                                            R.string.settings_sort_photos,
                                            Icons.Default.Image,
                                            currentIsPhoto
                                        ) {
                                            selectedSortOrder = when (selectedSortOrder) {
                                                SortOrder.TYPE_VIDEO_FIRST_ASC -> SortOrder.TYPE_PHOTO_FIRST_ASC
                                                SortOrder.TYPE_VIDEO_FIRST_SHUFFLED -> SortOrder.TYPE_PHOTO_FIRST_SHUFFLED
                                                else -> SortOrder.TYPE_PHOTO_FIRST
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp)
                                        Spacer(Modifier.height(8.dp))

                                        SortPopupItem(
                                            R.string.settings_sort_newest,
                                            Icons.Default.ArrowDownward,
                                            selectedSortOrder == SortOrder.TYPE_VIDEO_FIRST || selectedSortOrder == SortOrder.TYPE_PHOTO_FIRST
                                        ) {
                                            selectedSortOrder = if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST else SortOrder.TYPE_VIDEO_FIRST
                                        }
                                        SortPopupItem(
                                            R.string.settings_sort_oldest,
                                            Icons.Default.ArrowUpward,
                                            selectedSortOrder == SortOrder.TYPE_VIDEO_FIRST_ASC || selectedSortOrder == SortOrder.TYPE_PHOTO_FIRST_ASC
                                        ) {
                                            selectedSortOrder = if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST_ASC else SortOrder.TYPE_VIDEO_FIRST_ASC
                                        }
                                        SortPopupItem(
                                            R.string.settings_sort_shuffled,
                                            Icons.Default.Shuffle,
                                            selectedSortOrder == SortOrder.TYPE_VIDEO_FIRST_SHUFFLED || selectedSortOrder == SortOrder.TYPE_PHOTO_FIRST_SHUFFLED
                                        ) {
                                            selectedSortOrder = if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST_SHUFFLED else SortOrder.TYPE_VIDEO_FIRST_SHUFFLED
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp)
                        Spacer(Modifier.height(16.dp))

                        CategorySegmentedRow(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { category ->
                                selectedSortOrder = when (category) {
                                    SortCategory.TIME -> SortOrder.CHRONOLOGICAL_DESC
                                    SortCategory.SIZE -> SortOrder.SIZE_DESC
                                    SortCategory.TYPE -> SortOrder.TYPE_VIDEO_FIRST
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
