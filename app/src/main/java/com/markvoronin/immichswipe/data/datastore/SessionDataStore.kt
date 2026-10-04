package com.markvoronin.immichswipe.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// extension DataStore attachée au Context
val Context.dataStore by preferencesDataStore(name = "session")

class SessionDataStore(private val context: Context) {

    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_API_KEY = stringPreferencesKey("api_key")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_SAVED_SERVER_URLS = stringSetPreferencesKey("saved_server_urls")
        private val KEY_AUDIO_FOCUS = stringPreferencesKey("audio_focus")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_FULLSCREEN_ICON_POS = stringPreferencesKey("fullscreen_icon_pos")
        private val KEY_IMMICH_ICON_POS = stringPreferencesKey("immich_icon_pos")
        private val KEY_IMMICH_OPEN_MODE = stringPreferencesKey("immich_open_mode")
        private val KEY_IMMICH_LONG_PRESS_WEB = booleanPreferencesKey("immich_long_press_web")
        private val KEY_CARD_DISPLAY_ICON_POS = stringPreferencesKey("card_display_icon_pos")
        private val KEY_MUTE_ICON_POS = stringPreferencesKey("mute_icon_pos")
        private val KEY_DOWNLOAD_ICON_POS = stringPreferencesKey("download_icon_pos")
        private val KEY_SHARE_ICON_POS = stringPreferencesKey("share_icon_pos")
        private val KEY_SHOW_FULLSCREEN_ICON = androidx.datastore.preferences.core.booleanPreferencesKey("show_fullscreen_icon")
        private val KEY_SHOW_IMMICH_ICON = androidx.datastore.preferences.core.booleanPreferencesKey("show_immich_icon")
        private val KEY_SHOW_CARD_DISPLAY_ICON = androidx.datastore.preferences.core.booleanPreferencesKey("show_card_display_icon")
        private val KEY_SHOW_MUTE_ICON = androidx.datastore.preferences.core.booleanPreferencesKey("show_mute_icon")
        private val KEY_SHOW_DOWNLOAD_ICON = androidx.datastore.preferences.core.booleanPreferencesKey("show_download_icon")
        private val KEY_SHOW_SHARE_ICON = androidx.datastore.preferences.core.booleanPreferencesKey("show_share_icon")
        private val KEY_DEFAULT_LAYOUT_GRID = androidx.datastore.preferences.core.booleanPreferencesKey("default_layout_grid")
        private val KEY_SHOW_FAVORITE = androidx.datastore.preferences.core.booleanPreferencesKey("show_favorite")
        private val KEY_AUTO_NEXT_ON_FAV = androidx.datastore.preferences.core.booleanPreferencesKey("auto_next_on_fav")
        private val KEY_INCLUDE_ARCHIVED = androidx.datastore.preferences.core.booleanPreferencesKey("include_archived")
        private val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
        private val KEY_DEFAULT_CARD_DISPLAY_MODE = stringPreferencesKey("default_card_display_mode")
        private val KEY_SHOW_SWIPE_BUTTONS = androidx.datastore.preferences.core.booleanPreferencesKey("show_swipe_buttons")
        private val KEY_SHOW_ARCHIVE = booleanPreferencesKey("show_archive")
        private val KEY_SHOW_LOCK = booleanPreferencesKey("show_lock")
        private val KEY_SHOW_ADD_TO_ALBUM = booleanPreferencesKey("show_add_to_album")
        private val KEY_BACKUP_WARNING_SHOWN = androidx.datastore.preferences.core.booleanPreferencesKey("backup_warning_shown")
        private val KEY_SYNC_LOCAL_DELETION = androidx.datastore.preferences.core.booleanPreferencesKey("sync_local_deletion")
        private val KEY_TRASH_LOCAL_DELETION = androidx.datastore.preferences.core.booleanPreferencesKey("trash_local_deletion")
        private val KEY_TAP_TO_SWIPE = androidx.datastore.preferences.core.booleanPreferencesKey("tap_to_swipe")
        private val KEY_HAS_COMPLETED_SWIPE_TUTORIAL = booleanPreferencesKey("has_completed_swipe_tutorial")
        private val KEY_HAS_COMPLETED_DUPLICATES_TUTORIAL = booleanPreferencesKey("has_completed_duplicates_tutorial")
    }

    suspend fun saveSession(baseUrl: String, apiKey: String, userId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = baseUrl
            prefs[KEY_API_KEY] = apiKey
            prefs[KEY_USER_ID] = userId
        }
    }

    fun getBaseUrl(): Flow<String?> {
        return context.dataStore.data.map { it[KEY_BASE_URL] }
    }

    fun getApiKey(): Flow<String?> {
        return context.dataStore.data.map { it[KEY_API_KEY] }
    }

    fun getUserId(): Flow<String?> {
        return context.dataStore.data.map { it[KEY_USER_ID] }
    }

    fun getSavedServerUrls(): Flow<Set<String>> {
        return context.dataStore.data.map { it[KEY_SAVED_SERVER_URLS] ?: emptySet() }
    }

    suspend fun saveRecentServerUrl(url: String) {
        if (url.isBlank()) return
        val cleanUrl = url.trim().removeSuffix("/")
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SAVED_SERVER_URLS] ?: emptySet()
            prefs[KEY_SAVED_SERVER_URLS] = current + cleanUrl
        }
    }

    suspend fun removeSavedServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_SAVED_SERVER_URLS] ?: emptySet()
            prefs[KEY_SAVED_SERVER_URLS] = current - url
        }
    }

    fun getAudioFocusMode(): Flow<String?> {
        return context.dataStore.data.map { it[KEY_AUDIO_FOCUS] }
    }

    suspend fun saveAudioFocusMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUDIO_FOCUS] = mode
        }
    }

    fun getThemeMode(): Flow<String?> = context.dataStore.data.map { it[KEY_THEME_MODE] }
    
    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    fun isDynamicColor(): Flow<Boolean> = context.dataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: true }

    suspend fun saveDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    fun getFullscreenIconPosition(): Flow<String?> = context.dataStore.data.map { it[KEY_FULLSCREEN_ICON_POS] }
    
    suspend fun saveFullscreenIconPosition(pos: String) {
        context.dataStore.edit { it[KEY_FULLSCREEN_ICON_POS] = pos }
    }

    fun getImmichIconPosition(): Flow<String?> = context.dataStore.data.map { it[KEY_IMMICH_ICON_POS] }

    suspend fun saveImmichIconPosition(pos: String) {
        context.dataStore.edit { it[KEY_IMMICH_ICON_POS] = pos }
    }

    fun getImmichOpenMode(): Flow<String?> = context.dataStore.data.map { it[KEY_IMMICH_OPEN_MODE] }

    suspend fun saveImmichOpenMode(mode: String) {
        context.dataStore.edit { it[KEY_IMMICH_OPEN_MODE] = mode }
    }

    fun getImmichLongPressWeb(): Flow<Boolean> = context.dataStore.data.map { it[KEY_IMMICH_LONG_PRESS_WEB] ?: true }

    suspend fun saveImmichLongPressWeb(enabled: Boolean) {
        context.dataStore.edit { it[KEY_IMMICH_LONG_PRESS_WEB] = enabled }
    }

    fun getCardDisplayIconPosition(): Flow<String?> = context.dataStore.data.map { it[KEY_CARD_DISPLAY_ICON_POS] }

    suspend fun saveCardDisplayIconPosition(pos: String) {
        context.dataStore.edit { it[KEY_CARD_DISPLAY_ICON_POS] = pos }
    }

    fun getMuteIconPosition(): Flow<String?> = context.dataStore.data.map { it[KEY_MUTE_ICON_POS] }

    suspend fun saveMuteIconPosition(pos: String) {
        context.dataStore.edit { it[KEY_MUTE_ICON_POS] = pos }
    }

    fun getDownloadIconPosition(): Flow<String?> = context.dataStore.data.map { it[KEY_DOWNLOAD_ICON_POS] }

    suspend fun saveDownloadIconPosition(pos: String) {
        context.dataStore.edit { it[KEY_DOWNLOAD_ICON_POS] = pos }
    }

    fun getShareIconPosition(): Flow<String?> = context.dataStore.data.map { it[KEY_SHARE_ICON_POS] }

    suspend fun saveShareIconPosition(pos: String) {
        context.dataStore.edit { it[KEY_SHARE_ICON_POS] = pos }
    }

    fun isShowFullscreenIcon(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_FULLSCREEN_ICON] ?: true }
    suspend fun saveShowFullscreenIcon(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_FULLSCREEN_ICON] = show } }

    fun isShowImmichIcon(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_IMMICH_ICON] ?: true }
    suspend fun saveShowImmichIcon(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_IMMICH_ICON] = show } }

    fun isShowCardDisplayIcon(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_CARD_DISPLAY_ICON] ?: false }
    suspend fun saveShowCardDisplayIcon(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_CARD_DISPLAY_ICON] = show } }

    fun isShowMuteIcon(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_MUTE_ICON] ?: false }
    suspend fun saveShowMuteIcon(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_MUTE_ICON] = show } }

    fun isShowDownloadIcon(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_DOWNLOAD_ICON] ?: true }
    suspend fun saveShowDownloadIcon(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_DOWNLOAD_ICON] = show } }

    fun isShowShareIcon(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_SHARE_ICON] ?: true }
    suspend fun saveShowShareIcon(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_SHARE_ICON] = show } }

    fun isDefaultLayoutGrid(): Flow<Boolean> = context.dataStore.data.map { it[KEY_DEFAULT_LAYOUT_GRID] ?: false }

    suspend fun saveDefaultLayoutGrid(isGrid: Boolean) {
        context.dataStore.edit { it[KEY_DEFAULT_LAYOUT_GRID] = isGrid }
    }

    fun isShowFavorite(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_FAVORITE] ?: true }
    suspend fun saveShowFavorite(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_FAVORITE] = show } }

    fun isAutoNextOnFav(): Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_NEXT_ON_FAV] ?: false }
    suspend fun saveAutoNextOnFav(autoNext: Boolean) { context.dataStore.edit { it[KEY_AUTO_NEXT_ON_FAV] = autoNext } }

    fun isIncludeArchived(): Flow<Boolean> = context.dataStore.data.map { it[KEY_INCLUDE_ARCHIVED] ?: true }
    suspend fun saveIncludeArchived(include: Boolean) { context.dataStore.edit { it[KEY_INCLUDE_ARCHIVED] = include } }

    fun getSortOrder(): Flow<String?> = context.dataStore.data.map { it[KEY_SORT_ORDER] }
    suspend fun saveSortOrder(order: String) { context.dataStore.edit { it[KEY_SORT_ORDER] = order } }

    fun getDefaultCardDisplayMode(): Flow<String?> = context.dataStore.data.map { it[KEY_DEFAULT_CARD_DISPLAY_MODE] }
    suspend fun saveDefaultCardDisplayMode(mode: String) { context.dataStore.edit { it[KEY_DEFAULT_CARD_DISPLAY_MODE] = mode } }

    fun isShowSwipeButtons(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_SWIPE_BUTTONS] ?: false }
    suspend fun saveShowSwipeButtons(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_SWIPE_BUTTONS] = show } }

    fun isShowArchiveButton(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_ARCHIVE] ?: true }
    suspend fun saveShowArchiveButton(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_ARCHIVE] = show } }

    fun isShowLockButton(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_LOCK] ?: false }
    suspend fun saveShowLockButton(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_LOCK] = show } }

    fun isShowAddToAlbumButton(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SHOW_ADD_TO_ALBUM] ?: true }
    suspend fun saveShowAddToAlbumButton(show: Boolean) { context.dataStore.edit { it[KEY_SHOW_ADD_TO_ALBUM] = show } }

    fun isBackupWarningShown(): Flow<Boolean> = context.dataStore.data.map { it[KEY_BACKUP_WARNING_SHOWN] ?: false }
    suspend fun saveBackupWarningShown(shown: Boolean) { context.dataStore.edit { it[KEY_BACKUP_WARNING_SHOWN] = shown } }

    fun isSyncLocalDeletion(): Flow<Boolean> = context.dataStore.data.map { it[KEY_SYNC_LOCAL_DELETION] ?: false }
    suspend fun saveSyncLocalDeletion(sync: Boolean) { context.dataStore.edit { it[KEY_SYNC_LOCAL_DELETION] = sync } }

    fun isTrashLocalDeletion(): Flow<Boolean> = context.dataStore.data.map { it[KEY_TRASH_LOCAL_DELETION] ?: true }

    fun isTapToSwipeEnabled(): Flow<Boolean> = context.dataStore.data.map { it[KEY_TAP_TO_SWIPE] ?: false }
    suspend fun saveTapToSwipeEnabled(enabled: Boolean) { context.dataStore.edit { it[KEY_TAP_TO_SWIPE] = enabled } }

    fun hasCompletedSwipeTutorial(): Flow<Boolean> = context.dataStore.data.map { it[KEY_HAS_COMPLETED_SWIPE_TUTORIAL] ?: false }
    suspend fun setHasCompletedSwipeTutorial(completed: Boolean) { context.dataStore.edit { it[KEY_HAS_COMPLETED_SWIPE_TUTORIAL] = completed } }

    fun hasCompletedDuplicatesTutorial(): Flow<Boolean> = context.dataStore.data.map { it[KEY_HAS_COMPLETED_DUPLICATES_TUTORIAL] ?: false }
    suspend fun setHasCompletedDuplicatesTutorial(completed: Boolean) { context.dataStore.edit { it[KEY_HAS_COMPLETED_DUPLICATES_TUTORIAL] = completed } }

    suspend fun resetTutorials() {
        context.dataStore.edit {
            it[KEY_HAS_COMPLETED_SWIPE_TUTORIAL] = false
            it[KEY_HAS_COMPLETED_DUPLICATES_TUTORIAL] = false
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
