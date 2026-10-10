package com.markvoronin.immichswipe.feature.tutorial

import androidx.annotation.StringRes

enum class TutorialPhase {
    SWIPE,
    DUPLICATES
}

enum class TooltipPlacement {
    AUTO,
    TOP,
    BOTTOM
}

enum class GestureAnimationType {
    NONE,
    SWIPE_HORIZONTAL,
}

data class TutorialStep(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val targetKey: String? = null,
    val placement: TooltipPlacement = TooltipPlacement.AUTO,
    val gestureAnimation: GestureAnimationType = GestureAnimationType.NONE,
)
