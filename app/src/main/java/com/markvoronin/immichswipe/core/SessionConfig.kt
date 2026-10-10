package com.markvoronin.immichswipe.core

data class SessionConfig(
    val baseUrl: String,
    val apiKey: String,
    val userId: String = "",
)


enum class PlaybackBehavior {
    PAUSE_OTHERS,
    IGNORE
}


enum class IconPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}


enum class DoubleTapAction {
    FULLSCREEN,
    FAVORITE
}

enum class CardDisplayMode {
    FILL,
    FIT
}


enum class SortOrder {
    CHRONOLOGICAL_DESC,
    CHRONOLOGICAL_ASC,
    SHUFFLED,
    SIZE_DESC,
    SIZE_ASC,
    TYPE_VIDEO_FIRST,
    TYPE_PHOTO_FIRST,
    TYPE_VIDEO_FIRST_ASC,
    TYPE_PHOTO_FIRST_ASC,
    TYPE_VIDEO_FIRST_SHUFFLED,
    TYPE_PHOTO_FIRST_SHUFFLED
}


enum class SortCategory {
    TIME,
    SIZE,
    TYPE
}
