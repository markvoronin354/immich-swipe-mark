package com.markvoronin.immichswipe.feature.swipe

import com.markvoronin.immichswipe.core.CardDisplayMode
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.domain.model.Asset

data class SwipeCardConfig(
    val playbackBehavior: PlaybackBehavior,
    val fullscreenButtonPosition: IconPosition,
    val immichButtonPosition: IconPosition,
    val cardDisplayButtonPosition: IconPosition,
    val muteButtonPosition: IconPosition,
    val downloadButtonPosition: IconPosition = IconPosition.TOP_LEFT,
    val shareButtonPosition: IconPosition = IconPosition.TOP_RIGHT,
    val showFullscreenButton: Boolean = true,
    val showImmichButton: Boolean = true,
    val showCardDisplayButton: Boolean = true,
    val showMuteButton: Boolean = true,
    val showDownloadButton: Boolean = false,
    val showShareButton: Boolean = false,
    val cardDisplayMode: CardDisplayMode,
    val tapToSwipeEnabled: Boolean = false,
    val showSizeIndicator: Boolean = false
)

data class SwipeCardActions(
    val onSwipe: (SwipeDecision) -> Unit,
    val onToggleDisplayMode: () -> Unit,
    val onDoubleTap: () -> Unit,
    val onOpenFullscreen: () -> Unit,
    val onDownload: (Asset) -> Unit = {},
    val onShare: (Asset) -> Unit = {},
    val onToggleMute: () -> Unit = {}
)
