package com.markvoronin.immichswipe.navigation

import android.content.Intent
import android.net.Uri
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.domain.model.Album

/**
 * Handles deep links for navigation across the app.
 * Supports URIs like:
 * - immichswipe://swipe/{albumId}
 * - immichswipe://album/{albumId}
 * - immichswipe://duplicates
 * - immichswipe://settings
 */
object DeepLinkHandler {

    /**
     * Parses an incoming Intent and extracts a NavKey destination if a valid deep link URI is present.
     */
    fun parseIntent(intent: Intent?): NavKey? {
        val uri: Uri = intent?.data ?: return null
        return parseUri(uri)
    }

    fun parseUri(uri: Uri): NavKey? {
        val scheme = uri.scheme
        if (scheme != "immichswipe" && scheme != "http" && scheme != "https") {
            return null
        }
        AppLogger.d("DeepLinkHandler", "Parsing deep link URI: $uri")
        val host = uri.host ?: ""
        val pathSegments = uri.pathSegments

        return when (host) {
            "swipe", "album" -> {
                val rawId = pathSegments.firstOrNull() ?: uri.getQueryParameter("id") ?: Album.VIRTUAL_ALL_ID
                NavKey.Swipe(normalizeAlbumId(rawId))
            }
            "duplicates" -> NavKey.Duplicates
            "settings" -> NavKey.Settings
            "home" -> parseHomePath(pathSegments)
            else -> null
        }
    }

    private fun parseHomePath(pathSegments: List<String>): NavKey {
        val firstSegment = pathSegments.firstOrNull() ?: return NavKey.Home
        return when (firstSegment) {
            "swipe", "album" -> {
                val rawId = pathSegments.getOrNull(1) ?: Album.VIRTUAL_ALL_ID
                NavKey.Swipe(normalizeAlbumId(rawId))
            }
            "duplicates" -> NavKey.Duplicates
            "settings" -> NavKey.Settings
            else -> NavKey.Home
        }
    }

    private fun normalizeAlbumId(rawId: String): String {
        return when (rawId) {
            "virtual-all", "virtual_all", "all" -> Album.VIRTUAL_ALL_ID
            "virtual-orphans", "orphans" -> Album.VIRTUAL_ORPHANS_ID
            "virtual-duplicates", "duplicates" -> Album.VIRTUAL_DUPLICATES_ID
            else -> rawId
        }
    }
}
