package com.markvoronin.immichswipe.core

import android.content.Context
import androidx.compose.ui.graphics.Color
import coil.Coil
import com.markvoronin.immichswipe.data.api.ImmichApi
import com.markvoronin.immichswipe.data.api.RetrofitFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Représente les différents niveaux de santé de la connexion.
 */
enum class ConnectionLevel(val color: Color) {
    ONLINE(Color(0xFF4CAF50)),  // Vert
    ISSUES(Color(0xFFFF9800)),  // Orange
    OFFLINE(Color(0xFFF44336))  // Rouge
}

/**
 * Types de messages de diagnostic prédéfinis pour la traduction.
 */
enum class DiagStatus {
    CONNECTED,
    AUTH_ERROR,
    UNAVAILABLE,
    UNEXPECTED,
    DNS_ERROR,
    TIMEOUT,
    NO_INTERNET,
    CONNECTION_ERROR,
    LOGGED_OUT,
    UNKNOWN
}

data class ConnectionStatus(
    val level: ConnectionLevel = ConnectionLevel.ONLINE,
    val type: DiagStatus = DiagStatus.CONNECTED,
    val statusCode: Int? = null,
    val rawMessage: String? = null,
    val lastUpdate: Long = System.currentTimeMillis()
)

@Singleton
class SessionManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    @Volatile
    private var config: SessionConfig? = null

    @Volatile
    private var _api: ImmichApi? = null

    val api: ImmichApi?
        get() = _api

    private val _sessionConfig = MutableStateFlow<SessionConfig?>(null)
    val sessionConfig: StateFlow<SessionConfig?> = _sessionConfig.asStateFlow()

    // Flux global indiquant la santé de la connexion.
    private val _connectionStatus = MutableStateFlow(ConnectionStatus())
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    val globalShuffleSeed: Long = System.currentTimeMillis()

    fun updateStatus(level: ConnectionLevel, type: DiagStatus, statusCode: Int? = null, rawMessage: String? = null) {
        _connectionStatus.value = ConnectionStatus(level, type, statusCode, rawMessage)
    }

    private fun clearImageCache() {
        try {
            Coil.imageLoader(context).memoryCache?.clear()
        } catch (e: Exception) {
            AppLogger.e("SessionManager", "Error clearing Coil memory cache: ${e.message}")
        }
    }

    @Synchronized
    fun initialize(config: SessionConfig) {
        clearImageCache()
        this.config = config
        this._sessionConfig.value = config
        this._api = RetrofitFactory.create(config)
        this._connectionStatus.value = ConnectionStatus(ConnectionLevel.ONLINE, DiagStatus.CONNECTED)
        AppLogger.i("SessionManager", "Session initialized for user ${config.userId} at ${config.baseUrl}")
    }

    @Synchronized
    fun clear() {
        clearImageCache()
        config = null
        _sessionConfig.value = null
        _api = null
        _connectionStatus.value = ConnectionStatus(ConnectionLevel.OFFLINE, DiagStatus.LOGGED_OUT)
        AppLogger.i("SessionManager", "Session cleared / user logged out")
    }

    fun isLoggedIn(): Boolean = _api != null
    fun getBaseUrl(): String? = config?.baseUrl
    fun getApiKey(): String? = config?.apiKey
    fun getUserId(): String? = config?.userId
}
