package com.markvoronin.immichswipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        initializeTutorialState()
        observeSession()
        observeTheme()
        observeDynamicColor()
    }

    private fun initializeTutorialState() {
        viewModelScope.launch {
            sessionRepository.initializeTutorialState()
        }
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionRepository.sessionConfig.collect { config ->
                if (config != null) {
                    sessionManager.initialize(config)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        activeUserId = config.userId
                    )
                } else {
                    sessionManager.clear()



                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = false,
                        activeUserId = null
                    )
                }
            }
        }
    }

    private fun observeTheme() {
        viewModelScope.launch {
            sessionRepository.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
    }

    private fun observeDynamicColor() {
        viewModelScope.launch {
            sessionRepository.dynamicColor.collect { enabled ->
                _uiState.value = _uiState.value.copy(dynamicColor = enabled)
            }
        }
    }
}
