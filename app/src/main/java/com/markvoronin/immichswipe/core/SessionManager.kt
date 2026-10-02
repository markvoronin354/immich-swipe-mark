package com.markvoronin.immichswipe.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            AppLogger.d("SessionManager", "Network restored")
            if (isLoggedIn() && _connectionStatus.value.level == ConnectionLevel.OFFLINE) {
                updateStatus(ConnectionLevel.ONLINE, DiagStatus.CONNECTED)
            }
        }

        override fun onLost(network: Network) {
            AppLogger.d("SessionManager", "Network connection lost")
            if (isLoggedIn()) {
                updateStatus(ConnectionLevel.OFFLINE, DiagStatus.NO_INTERNET)
            }
        }
    }

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            AppLogger.e("SessionManager", "Failed to register network callback: ${e.message}")
        }
    }

    fun isNetworkAvailable(): Boolean {
        val cm = connectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

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
        this._api = RetrofitFactory.create(config) { level, type, statusCode, rawMessage ->
            updateStatus(level, type, statusCode, rawMessage)
        }
        val initialLevel = if (isNetworkAvailable()) ConnectionLevel.ONLINE else ConnectionLevel.OFFLINE
        val initialType = if (isNetworkAvailable()) DiagStatus.CONNECTED else DiagStatus.NO_INTERNET
        this._connectionStatus.value = ConnectionStatus(initialLevel, initialType)
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
