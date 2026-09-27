package com.markvoronin.immichswipe.feature.duplicates.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.feature.duplicates.DuplicateClusterUiModel
import com.markvoronin.immichswipe.feature.duplicates.DuplicateDecision

data class ZoomData(
    val asset: Asset,
    val decision: DuplicateDecision,
    val initialBounds: Rect,
    val scale: Float,
    val offset: Offset,
    val isGestureActive: Boolean
)

data class FullScreenPreviewData(
    val cluster: DuplicateClusterUiModel,
    val initialIndex: Int
)
