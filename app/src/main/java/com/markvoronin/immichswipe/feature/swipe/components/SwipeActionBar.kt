package com.markvoronin.immichswipe.feature.swipe.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SortOrder
import com.markvoronin.immichswipe.feature.swipe.MaterialGreen
import com.markvoronin.immichswipe.feature.swipe.MaterialRed
import com.markvoronin.immichswipe.feature.swipe.SwipeDecision
import com.markvoronin.immichswipe.feature.swipe.SwipeUiState
import com.markvoronin.immichswipe.feature.swipe.SwipeViewModel

@Composable
fun SwipeActionBar(
    uiState: SwipeUiState,
    viewModel: SwipeViewModel,
    showSortMenu: Boolean,
    onSortMenuToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (uiState.showSwipeButtons) 16.dp else 24.dp),
        horizontalArrangement = if (uiState.showSwipeButtons) Arrangement.SpaceEvenly else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (uiState.showSwipeButtons) {
            val haptic = LocalHapticFeedback.current
            var isLongPressActive by remember { mutableStateOf(false) }
            var dragStartedX by remember { mutableFloatStateOf(0f) }
            val currentUiStateDelete by rememberUpdatedState(uiState)
            val densityDelete = LocalDensity.current
            val stepPxDelete = with(densityDelete) { 36.dp.toPx() }

            FloatingActionButton(
                onClick = { if (!uiState.isBulkDeleteMode && !uiState.isBulkKeepMode) viewModel.onSwipe(SwipeDecision.DELETE) },
                containerColor = if (uiState.isBulkDeleteMode) MaterialRed else MaterialTheme.colorScheme.errorContainer,
                contentColor = if (uiState.isBulkDeleteMode) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                isLongPressActive = true
                                dragStartedX = offset.x
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.enterBulkMode(isDelete = true)
                            },
                            onDragEnd = {
                                isLongPressActive = false
                                viewModel.executeBulkAction()
                            },
                            onDragCancel = {
                                isLongPressActive = false
                                viewModel.exitBulkMode()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (isLongPressActive) {
                                    val state = currentUiStateDelete
                                    val totalDrag = change.position.x - dragStartedX
                                    if (totalDrag > 0) {
                                        val itemsToSelect = (totalDrag / stepPxDelete).toInt()
                                        val selection = mutableSetOf<String>()
                                        var lastIdx = state.currentIndex
                                        for (i in 0..itemsToSelect) {
                                            val idx = state.currentIndex + i
                                            if (idx < state.assets.size) {
                                                selection.add(state.assets[idx].id)
                                                lastIdx = idx
                                            }
                                        }
                                        viewModel.setBulkSelection(selection, lastIdx)
                                    } else {
                                        viewModel.setBulkSelection(
                                            setOfNotNull(state.assets.getOrNull(state.currentIndex)?.id),
                                            state.currentIndex
                                        )
                                    }
                                }
                            }
                        )
                    }
            ) {
                Icon(
                    imageVector = if (uiState.isBulkDeleteMode) Icons.Default.DeleteSweep else Icons.Default.Delete,
                    contentDescription = stringResource(R.string.swipe_delete)
                )
            }
        }

        IconButton(
            onClick = { viewModel.undo() },
            enabled = uiState.history.isNotEmpty(),
            modifier = Modifier.size(if (uiState.showSwipeButtons) 36.dp else 44.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.swipe_undo),
                modifier = Modifier.size(if (uiState.showSwipeButtons) 22.dp else 26.dp)
            )
        }

        IconButton(
            onClick = {
                if (!uiState.swapSummaryArchive) viewModel.toggleArchive()
                else viewModel.toggleSummary(true)
            },
            modifier = Modifier.size(if (uiState.showSwipeButtons) 36.dp else 44.dp)
        ) {
            Icon(
                imageVector = if (!uiState.swapSummaryArchive) Icons.Default.Archive else Icons.Default.Assessment,
                contentDescription = if (!uiState.swapSummaryArchive) stringResource(R.string.swipe_archive) else stringResource(R.string.swipe_summary_title),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(if (uiState.showSwipeButtons) 22.dp else 26.dp)
            )
        }

        if (uiState.showFavoriteButton) {
            val isFav = uiState.currentAsset?.let { uiState.isFavorite(it.id) } ?: false
            IconButton(
                onClick = { viewModel.toggleFavorite() },
                modifier = Modifier.size(if (uiState.showSwipeButtons) 36.dp else 44.dp)
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.swipe_favorite),
                    tint = if (isFav) Color.Red else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(if (uiState.showSwipeButtons) 22.dp else 26.dp)
                )
            }
        }

        IconButton(
            onClick = { viewModel.toggleLock() },
            modifier = Modifier.size(if (uiState.showSwipeButtons) 36.dp else 44.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = stringResource(R.string.swipe_locked),
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(if (uiState.showSwipeButtons) 22.dp else 26.dp)
            )
        }

        Box {
            IconButton(
                onClick = { onSortMenuToggle(true) },
                modifier = Modifier.size(if (uiState.showSwipeButtons) 36.dp else 44.dp)
            ) {
                val icon = when (uiState.sortOrder) {
                    SortOrder.SHUFFLED -> Icons.Default.Shuffle
                    SortOrder.CHRONOLOGICAL_ASC -> Icons.Default.ArrowUpward
                    SortOrder.CHRONOLOGICAL_DESC -> Icons.Default.ArrowDownward
                    SortOrder.SIZE_DESC -> Icons.Default.ExpandMore
                    SortOrder.SIZE_ASC -> Icons.Default.ExpandLess
                    SortOrder.TYPE_VIDEO_FIRST, SortOrder.TYPE_VIDEO_FIRST_ASC, SortOrder.TYPE_VIDEO_FIRST_SHUFFLED -> Icons.Default.Videocam
                    SortOrder.TYPE_PHOTO_FIRST, SortOrder.TYPE_PHOTO_FIRST_ASC, SortOrder.TYPE_PHOTO_FIRST_SHUFFLED -> Icons.Default.Image
                }

                val tint = if (uiState.sortOrder != SortOrder.CHRONOLOGICAL_DESC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                val baseSize = if (uiState.showSwipeButtons) 24.dp else 28.dp

                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.settings_sort_order_label),
                    tint = tint,
                    modifier = Modifier.size(baseSize)
                )
            }

            if (showSortMenu) {
                SortOrderPopupMenu(
                    uiState = uiState,
                    viewModel = viewModel,
                    onDismiss = { onSortMenuToggle(false) }
                )
            }
        }

        if (uiState.showSwipeButtons) {
            val haptic = LocalHapticFeedback.current
            var isLongPressActive by remember { mutableStateOf(false) }
            var dragStartedX by remember { mutableFloatStateOf(0f) }
            val currentUiStateKeep by rememberUpdatedState(uiState)
            val densityKeep = LocalDensity.current
            val stepPxKeep = with(densityKeep) { 36.dp.toPx() }

            FloatingActionButton(
                onClick = { if (!uiState.isBulkKeepMode && !uiState.isBulkDeleteMode) viewModel.onSwipe(SwipeDecision.KEEP) },
                containerColor = if (uiState.isBulkKeepMode) MaterialGreen else MaterialGreen,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                isLongPressActive = true
                                dragStartedX = offset.x
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.enterBulkMode(isDelete = false)
                            },
                            onDragEnd = {
                                isLongPressActive = false
                                viewModel.executeBulkAction()
                            },
                            onDragCancel = {
                                isLongPressActive = false
                                viewModel.exitBulkMode()
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (isLongPressActive) {
                                    val state = currentUiStateKeep
                                    val totalDrag = dragStartedX - change.position.x
                                    if (totalDrag > 0) {
                                        val itemsToSelect = (totalDrag / stepPxKeep).toInt()
                                        val selection = mutableSetOf<String>()
                                        var lastIdx = state.currentIndex
                                        for (i in 0..itemsToSelect) {
                                            val idx = state.currentIndex + i
                                            if (idx < state.assets.size) {
                                                selection.add(state.assets[idx].id)
                                                lastIdx = idx
                                            }
                                        }
                                        viewModel.setBulkSelection(selection, lastIdx)
                                    } else {
                                        viewModel.setBulkSelection(
                                            setOfNotNull(state.assets.getOrNull(state.currentIndex)?.id),
                                            state.currentIndex
                                        )
                                    }
                                }
                            }
                        )
                    }
            ) {
                Icon(
                    imageVector = if (uiState.isBulkKeepMode) Icons.Default.DoneAll else Icons.Default.Check,
                    contentDescription = stringResource(R.string.swipe_keep)
                )
            }
        }
    }
}
