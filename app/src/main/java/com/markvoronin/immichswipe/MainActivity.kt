package com.markvoronin.immichswipe

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.feature.auth.AuthScreen
import com.markvoronin.immichswipe.feature.common.LoadingScreen
import com.markvoronin.immichswipe.feature.home.homeScreen
import com.markvoronin.immichswipe.ui.theme.ImmichSwipeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.init(applicationContext)
        AppLogger.i("MainActivity", "Application started")

        enableEdgeToEdge()

        // enableEdgeToEdge est conservé pour permettre le dessin derrière les barres système.
        
        // On verrouille l'application en mode Portrait par défaut.
        @SuppressLint("SourceLockedOrientationActivity")
        if (savedInstanceState == null) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val state by appViewModel.uiState.collectAsStateWithLifecycle()

            // Détermination du thème à appliquer
            val useDarkTheme = when (state.themeMode) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            ImmichSwipeTheme(darkTheme = useDarkTheme, dynamicColor = state.dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        state.isLoading -> {
                            LoadingScreen()
                        }

                        state.isLoggedIn -> {
                            val activeUserId = state.activeUserId
                            val api = sessionManager.api
                            val baseUrl = sessionManager.getBaseUrl()
                            val apiKey = sessionManager.getApiKey()
                            
                            if ((api != null) && (baseUrl != null) && (apiKey != null) && (activeUserId != null)) {
                                val sessionKey = "$activeUserId-$baseUrl-$apiKey"
                                
                                key(sessionKey) {
                                    homeScreen(
                                        viewModel = hiltViewModel(key = sessionKey),
                                        sessionKey = sessionKey,
                                        deepLinkIntent = intent
                                    )
                                }
                            } else {
                                LoadingScreen()
                            }
                        }

                        else -> {
                            AuthScreen(
                                viewModel = hiltViewModel()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
