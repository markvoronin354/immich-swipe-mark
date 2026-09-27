package com.markvoronin.immichswipe.feature.duplicates.components

import java.util.Locale

fun formatMiddleEllipsisFileName(fileName: String, maxLength: Int = 22): String {
    if (fileName.length <= maxLength) return fileName

    val lastDotIndex = fileName.lastIndexOf('.')
    val hasExt = lastDotIndex > 0 && lastDotIndex < fileName.length - 1

    val ext = if (hasExt) fileName.substring(lastDotIndex) else ""
    val baseName = if (hasExt) fileName.substring(0, lastDotIndex) else fileName

    val ellipsis = "…"
    val availableForBase = maxLength - ext.length - ellipsis.length

    if (availableForBase < 3) {
        val avail = maxLength - ellipsis.length
        if (avail <= 0) return fileName.take(maxLength)
        val prefixLen = 2.coerceAtMost(avail - 1)
        val suffixLen = avail - prefixLen
        return fileName.take(prefixLen) + ellipsis + fileName.takeLast(suffixLen)
    }

    val prefixLen = 4.coerceAtMost(availableForBase / 3)
    val suffixLen = availableForBase - prefixLen

    return baseName.take(prefixLen) + ellipsis + baseName.takeLast(suffixLen) + ext
}

fun formatSizeStr(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.2f MB", mb)
        else -> String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}

fun formatMediaTime(ms: Long): String {
    val totalSeconds = (ms / 1000).toInt()
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
