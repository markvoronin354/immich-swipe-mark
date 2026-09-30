package com.markvoronin.immichswipe.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri

object ImmichLauncher {
    private const val IMMICH_PACKAGE_NAME = "app.alextran.immich"

    /**
     * Opens the asset directly in the official Immich app or web browser based on mode.
     */
    fun openAssetInImmich(
        context: Context,
        baseUrl: String?,
        assetId: String,
        mode: ImmichOpenMode = ImmichOpenMode.APP
    ) {
        if (assetId.isBlank()) return

        if (mode == ImmichOpenMode.WEB) {
            if (!baseUrl.isNullOrBlank()) {
                openInWeb(context, baseUrl, assetId)
            } else {
                Toast.makeText(context, "Server URL is not configured", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val cleanBaseUrl = baseUrl?.removeSuffix("/") ?: ""
        val webUri = if (cleanBaseUrl.isNotBlank()) "$cleanBaseUrl/photos/$assetId".toUri() else null
        val customUri = "immich://asset?id=$assetId".toUri()

        var handled = tryLaunchAppUri(context, customUri)
        if (!handled && webUri != null) {
            handled = tryLaunchAppUri(context, webUri)
        }

        if (!handled) {
            if (webUri != null) {
                openInWeb(context, cleanBaseUrl, assetId)
            } else {
                openPlayStore(context)
            }
        }
    }

    private fun tryLaunchAppUri(context: Context, uri: Uri): Boolean {
        val appIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(IMMICH_PACKAGE_NAME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(appIntent)
            true
        } catch (e: ActivityNotFoundException) {
            AppLogger.d("ImmichLauncher", "Failed to launch Immich app with URI ($uri): ${e.message}")
            false
        }
    }

    /**
     * Opens the web URL ($baseUrl/photos/$assetId) in standard browser.
     */
    fun openInWeb(context: Context, baseUrl: String, assetId: String) {
        if (baseUrl.isBlank()) return
        val cleanBaseUrl = baseUrl.removeSuffix("/")
        val url = "$cleanBaseUrl/photos/$assetId"
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            AppLogger.e("ImmichLauncher", "Failed to open web URL: $url", e)
            Toast.makeText(context, "Could not open web browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPlayStore(context: Context) {
        try {
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                "market://details?id=$IMMICH_PACKAGE_NAME".toUri()
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Immich app is not installed", Toast.LENGTH_SHORT).show()
        }
    }
}
