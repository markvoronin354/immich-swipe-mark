package com.markvoronin.immichswipe.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.repository.AccountRepository
import com.markvoronin.immichswipe.data.repository.AuthRepository
import com.markvoronin.immichswipe.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel gérant la logique de l'écran de connexion.
 * Il délègue les appels réseau au AuthRepository.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // On observe la session : si elle devient null (déconnexion), 
        // on réinitialise l'état du formulaire de login.
        observeSessionReset()
        observeSavedServerUrls()
    }

    private fun observeSessionReset() {
        viewModelScope.launch {
            sessionRepository.sessionConfig.collect { config ->
                if (config == null) {
                    resetState()
                }
            }
        }
    }

    private fun observeSavedServerUrls() {
        viewModelScope.launch {
            sessionRepository.savedServerUrls.collect { urls ->
                _uiState.value = _uiState.value.copy(savedServerUrls = urls)
            }
        }
    }

    fun onBaseUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(baseUrl = value)
    }

    fun onApiKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value)
    }

    fun removeSavedServerUrl(url: String) {
        viewModelScope.launch {
            sessionRepository.removeSavedServerUrl(url)
        }
    }

    /**
     * Remet l'état à zéro (utile après une déconnexion).
     */
    fun resetState() {
        val currentSavedUrls = _uiState.value.savedServerUrls
        _uiState.value = AuthUiState(savedServerUrls = currentSavedUrls)
    }

    /**
     * Vide tous les champs de texte du formulaire.
     */
    fun clearAllFields() {
        val currentSavedUrls = _uiState.value.savedServerUrls
        _uiState.value = AuthUiState(savedServerUrls = currentSavedUrls)
    }

    /**
     * Prépare le formulaire pour l'ajout d'un nouveau compte en s'assurant
     * que la clé API est vide. L'URL du serveur peut être optionnellement pré-remplie.
     */
    fun prepareForAddAccount(defaultBaseUrl: String? = null) {
        val initialUrl = defaultBaseUrl?.takeIf { it.isNotEmpty() } ?: _uiState.value.baseUrl
        _uiState.value = _uiState.value.copy(
            baseUrl = initialUrl,
            apiKey = "",
            isLoading = false,
            error = null,
            success = false
        )
    }

    /**
     * Tente de connecter l'utilisateur.
     */
    fun login() {
        viewModelScope.launch {
            val baseUrl = _uiState.value.baseUrl.trim()
            val apiKey = _uiState.value.apiKey.trim()

            AppLogger.i("Auth", "Attempting connection to $baseUrl")

            // Validation basique
            if (baseUrl.isEmpty() || apiKey.isEmpty()) {
                _uiState.value = _uiState.value.copy(error = AuthError.EmptyFields)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // 1. On demande au Repository de vérifier les identifiants
                val user = authRepository.checkCredentials(baseUrl, apiKey)
                AppLogger.i("Auth", "Valid credentials. User: ${user.name} (${user.id})")

                // 2. Si on arrive ici, c'est que la connexion a réussi !
                val config = SessionConfig(baseUrl = baseUrl, apiKey = apiKey, userId = user.id)
                sessionManager.initialize(config)

                // 3. On sauvegarde le compte dans la base locale, la session active et l'historique des URLs
                accountRepository.saveAccount(baseUrl, apiKey, user)
                sessionRepository.saveSession(baseUrl = baseUrl, token = apiKey, userId = user.id)
                sessionRepository.saveRecentServerUrl(baseUrl)

                _uiState.value = _uiState.value.copy(isLoading = false, success = true)

            } catch (e: Exception) {
                val error = when (e) {
                    is java.net.UnknownHostException -> AuthError.Dns
                    is java.net.SocketTimeoutException -> AuthError.Timeout
                    is java.net.ConnectException -> AuthError.Refused
                    is retrofit2.HttpException -> {
                        when (e.code()) {
                            401 -> AuthError.Auth
                            403 -> AuthError.Forbidden
                            404 -> AuthError.NotFound
                            else -> AuthError.Server(e.code())
                        }
                    }
                    is javax.net.ssl.SSLHandshakeException -> AuthError.Ssl
                    else -> AuthError.Unknown(e.localizedMessage)
                }

                AppLogger.e("Auth", "Connection failed: $error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error
                )
            }
        }
    }
}
