package com.markvoronin.immichswipe

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.core.cache.CacheManager
import com.markvoronin.immichswipe.feature.auth.AuthScreen
import com.markvoronin.immichswipe.feature.common.LoadingScreen
import com.markvoronin.immichswipe.feature.home.HomeScreen
import com.markvoronin.immichswipe.ui.theme.ImmichSwipeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.init(applicationContext)
        AppLogger.i("MainActivity", "Application démarrée")
        
        // Maintenance du cache en arrière-plan
        lifecycleScope.launch {
            CacheManager.performMaintenance(applicationContext)
        }

        enableEdgeToEdge()

        // Mode immersif : On cache les barres système (status et navigation) au lancement.
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
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
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                        },
                        label = "ScreenTransition",
                        modifier = Modifier.fillMaxSize(),
                    ) { targetState ->
                        when {
                            targetState.isLoading -> {
                                LoadingScreen()
                            }

                            targetState.isLoggedIn -> {
                                val activeUserId = targetState.activeUserId
                                val api = sessionManager.api
                                val baseUrl = sessionManager.getBaseUrl()
                                val apiKey = sessionManager.getApiKey()
                                
                                if ((api != null) && (baseUrl != null) && (apiKey != null) && (activeUserId != null)) {
                                    val sessionKey = "$activeUserId-$baseUrl-$apiKey"
                                    
                                    key(sessionKey) {
                                        HomeScreen(
                                            viewModel = hiltViewModel(key = sessionKey),
                                            sessionKey = sessionKey
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
    }
}
