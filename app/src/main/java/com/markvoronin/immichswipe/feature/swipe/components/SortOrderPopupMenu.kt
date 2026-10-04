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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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

@Composable
fun SortOrderPopupMenu(
    uiState: SwipeUiState,
    viewModel: SwipeViewModel,
    onDismiss: () -> Unit,
    offsetY: Dp = 41.dp
) {
    val density = LocalDensity.current
    val yOffsetPx = with(density) { -offsetY.roundToPx() }

    Popup(
        alignment = Alignment.BottomCenter,
        offset = IntOffset(0, yOffsetPx),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .width(300.dp)
                .height(420.dp) // Fixed height to prevent Popup window resize stutter while fitting all categories
                .pointerInput(Unit) {
                    detectTapGestures { onDismiss() } // Dismiss if tapping outside surface but inside the fixed box
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .pointerInput(Unit) { detectTapGestures {} }, // Consume taps on the surface
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                AnimatedContent(
                    targetState = uiState.sortCategory,
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
                                SortPopupItem(R.string.settings_sort_newest, Icons.Default.ArrowDownward, uiState.sortOrder == SortOrder.CHRONOLOGICAL_DESC) {
                                    viewModel.setSortOrder(SortOrder.CHRONOLOGICAL_DESC)
                                    onDismiss()
                                }
                                SortPopupItem(R.string.settings_sort_oldest, Icons.Default.ArrowUpward, uiState.sortOrder == SortOrder.CHRONOLOGICAL_ASC) {
                                    viewModel.setSortOrder(SortOrder.CHRONOLOGICAL_ASC)
                                    onDismiss()
                                }
                                SortPopupItem(R.string.settings_sort_shuffled, Icons.Default.Shuffle, uiState.sortOrder == SortOrder.SHUFFLED) {
                                    viewModel.setSortOrder(SortOrder.SHUFFLED)
                                    onDismiss()
                                }
                            }
                            SortCategory.SIZE -> {
                                SortPopupItem(R.string.settings_sort_biggest, Icons.Default.ExpandMore, uiState.sortOrder == SortOrder.SIZE_DESC) {
                                    viewModel.setSortOrder(SortOrder.SIZE_DESC)
                                    onDismiss()
                                }
                                SortPopupItem(R.string.settings_sort_smallest, Icons.Default.ExpandLess, uiState.sortOrder == SortOrder.SIZE_ASC) {
                                    viewModel.setSortOrder(SortOrder.SIZE_ASC)
                                    onDismiss()
                                }
                            }
                            SortCategory.TYPE -> {
                        val currentIsPhoto = uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST ||
                                uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST_ASC ||
                                uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST_SHUFFLED

                        SortPopupItem(R.string.settings_sort_videos, Icons.Default.Videocam, !currentIsPhoto) {
                            val subOrder = when(uiState.sortOrder) {
                                SortOrder.TYPE_PHOTO_FIRST_ASC -> SortOrder.TYPE_VIDEO_FIRST_ASC
                                SortOrder.TYPE_PHOTO_FIRST_SHUFFLED -> SortOrder.TYPE_VIDEO_FIRST_SHUFFLED
                                else -> SortOrder.TYPE_VIDEO_FIRST
                            }
                            viewModel.setSortOrder(subOrder)
                            onDismiss()
                        }
                        SortPopupItem(R.string.settings_sort_photos, Icons.Default.Image, currentIsPhoto) {
                            val subOrder = when(uiState.sortOrder) {
                                SortOrder.TYPE_VIDEO_FIRST_ASC -> SortOrder.TYPE_PHOTO_FIRST_ASC
                                SortOrder.TYPE_VIDEO_FIRST_SHUFFLED -> SortOrder.TYPE_PHOTO_FIRST_SHUFFLED
                                else -> SortOrder.TYPE_PHOTO_FIRST
                            }
                            viewModel.setSortOrder(subOrder)
                            onDismiss()
                        }

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp)
                        Spacer(Modifier.height(8.dp))

                        SortPopupItem(
                            R.string.settings_sort_newest,
                            Icons.Default.ArrowDownward,
                            uiState.sortOrder == SortOrder.TYPE_VIDEO_FIRST || uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST
                        ) {
                            viewModel.setSortOrder(if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST else SortOrder.TYPE_VIDEO_FIRST)
                            onDismiss()
                        }
                        SortPopupItem(
                            R.string.settings_sort_oldest,
                            Icons.Default.ArrowUpward,
                            uiState.sortOrder == SortOrder.TYPE_VIDEO_FIRST_ASC || uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST_ASC
                        ) {
                            viewModel.setSortOrder(if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST_ASC else SortOrder.TYPE_VIDEO_FIRST_ASC)
                            onDismiss()
                        }
                        SortPopupItem(
                            R.string.settings_sort_shuffled,
                            Icons.Default.Shuffle,
                            uiState.sortOrder == SortOrder.TYPE_VIDEO_FIRST_SHUFFLED || uiState.sortOrder == SortOrder.TYPE_PHOTO_FIRST_SHUFFLED
                        ) {
                            viewModel.setSortOrder(if (currentIsPhoto) SortOrder.TYPE_PHOTO_FIRST_SHUFFLED else SortOrder.TYPE_VIDEO_FIRST_SHUFFLED)
                            onDismiss()
                        }
                    }
                }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), thickness = 0.5.dp)
                Spacer(Modifier.height(16.dp))

                CategorySegmentedRow(
                    selectedCategory = uiState.sortCategory,
                    onCategorySelected = { category ->
                        viewModel.setSortCategory(category)
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
