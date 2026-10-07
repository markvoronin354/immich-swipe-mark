package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SortCategory
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.SwipeViewModel

@Composable
fun SortOrderPopupMenu(
    uiState: SwipeUiState,
    viewModel: SwipeViewModel,
    onDismiss: () -> Unit,
) {
    var selectedSortOrder by remember { mutableStateOf(uiState.sortOrder) }

    val selectedCategory = when (selectedSortOrder) {
        SortOrder.CHRONOLOGICAL_DESC, SortOrder.CHRONOLOGICAL_ASC, SortOrder.SHUFFLED -> SortCategory.TIME
        SortOrder.SIZE_DESC, SortOrder.SIZE_ASC -> SortCategory.SIZE
        else -> SortCategory.TYPE
    }

    val handleDismiss = {
        if (selectedSortOrder != uiState.sortOrder) {
            viewModel.setSortOrder(selectedSortOrder)
        }
        onDismiss()
    }

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { handleDismiss() }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(580.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .width(320.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { /* Consume taps inside surface */ }
                        },
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 10.dp,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
            Column(
                modifier = Modifier.padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                                    selectedSortOrder = if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST_SHUFFLED else SortOrder.TYPE_PHOTO_FIRST_SHUFFLED
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
