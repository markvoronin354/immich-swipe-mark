package com.markvoronin.immichswipe.navigation

import androidx.compose.runtime.Immutable

/**
 * Type-safe navigation keys for Jetpack Navigation 3.
 */
@Immutable
sealed interface NavKey {
    val route: String

    data object Auth : NavKey {
        override val route: String = "auth"
    }

    data object Home : NavKey {
        override val route: String = "home"
    }

    data class Swipe(val albumId: String) : NavKey {
        override val route: String = "swipe/$albumId"
    }

    data object Duplicates : NavKey {
        override val route: String = "duplicates"
    }

    data object Settings : NavKey {
        override val route: String = "settings"
    }
}
