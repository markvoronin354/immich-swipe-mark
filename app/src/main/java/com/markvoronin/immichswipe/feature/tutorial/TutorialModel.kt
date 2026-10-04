package com.markvoronin.immichswipe.feature.tutorial

import androidx.annotation.StringRes

enum class TutorialPhase {
    HOME,
    SWIPE,
    DUPLICATES
}

enum class TooltipPlacement {
    AUTO,
    TOP,
    BOTTOM,
    CENTER
}

enum class GestureAnimationType {
    NONE,
    SWIPE_HORIZONTAL,
    TAP
}

data class TutorialStep(
    val id: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descriptionRes: Int,
    val targetKey: String? = null,
    val placement: TooltipPlacement = TooltipPlacement.AUTO,
    val gestureAnimation: GestureAnimationType = GestureAnimationType.NONE,
)
