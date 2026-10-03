package com.markvoronin.immichswipe.feature.home

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.SyncLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.feature.auth.AuthScreen
import com.markvoronin.immichswipe.feature.auth.AuthViewModel
import com.markvoronin.immichswipe.feature.duplicates.DuplicatesScreen
import com.markvoronin.immichswipe.feature.duplicates.DuplicatesViewModel
import com.markvoronin.immichswipe.feature.home.components.AlbumGrid
import com.markvoronin.immichswipe.feature.home.components.AlbumList
import com.markvoronin.immichswipe.feature.home.components.ErrorView
import com.markvoronin.immichswipe.feature.home.components.HomeTopBar
import com.markvoronin.immichswipe.feature.home.components.ProfilePopup
import com.markvoronin.immichswipe.feature.home.components.StatsPopup
import com.markvoronin.immichswipe.feature.settings.SettingsScreen
import com.markvoronin.immichswipe.feature.settings.SettingsSubMenu
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel
import com.markvoronin.immichswipe.feature.swipe.SwipeScreen
import com.markvoronin.immichswipe.navigation.DeepLinkHandler
import com.markvoronin.immichswipe.navigation.NavKey

private val navKeySaver = Saver<SnapshotStateList<NavKey>, List<String>>(
    save = { list -> list.map { it.route } },
    restore = { savedRoutes ->
        val restoredList = mutableStateListOf<NavKey>()
        savedRoutes.forEach { route ->
            val key = when {
                route == "auth" -> NavKey.Auth
                route == "home" -> NavKey.Home
                route.startsWith("swipe/") -> NavKey.Swipe(route.removePrefix("swipe/"))
                route == "duplicates" -> NavKey.Duplicates
                route == "settings" -> NavKey.Settings
                else -> NavKey.Home
            }
            restoredList.add(key)
        }
        if (restoredList.isEmpty()) restoredList.add(NavKey.Home)
        restoredList
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    sessionKey: String,
    modifier: Modifier = Modifier,
    deepLinkIntent: Intent? = null
) {
    val uiState: HomeUiState by viewModel.uiState.collectAsStateWithLifecycle()

    val backStack = rememberSaveable(inputs = arrayOf(sessionKey), saver = navKeySaver) {
        mutableStateListOf<NavKey>(NavKey.Home)
    }

    LaunchedEffect(deepLinkIntent) {
        val parsedKey = DeepLinkHandler.parseIntent(deepLinkIntent)
        if (parsedKey != null && backStack.lastOrNull() != parsedKey) {
            if (parsedKey is NavKey.Swipe) {
                if (backStack.firstOrNull() != NavKey.Home) {
                    backStack.clear()
                    backStack.add(NavKey.Home)
                }
                backStack.add(parsedKey)
            } else {
                backStack.add(parsedKey)
            }
        }
    }

    val currentTopKey = backStack.lastOrNull() ?: NavKey.Home
    val isHome = currentTopKey is NavKey.Home

    LaunchedEffect(Unit) {
        viewModel.loadUser()
    }

    val virtualAllName = stringResource(R.string.home_virtual_all_assets)
    val virtualAllDesc = stringResource(R.string.home_virtual_all_assets_desc)
    val virtualOrphansName = stringResource(R.string.home_virtual_orphans)
    val virtualOrphansDesc = stringResource(R.string.home_virtual_orphans_desc)
    val virtualDuplicatesName = stringResource(R.string.home_virtual_duplicates)
    val virtualDuplicatesDesc = stringResource(R.string.home_virtual_duplicates_desc)

    LaunchedEffect(virtualAllName, virtualAllDesc, virtualOrphansName, virtualOrphansDesc, virtualDuplicatesName, virtualDuplicatesDesc) {
        viewModel.updateVirtualNames(Album.VIRTUAL_ALL_ID, virtualAllName, virtualAllDesc)
        viewModel.updateVirtualNames(Album.VIRTUAL_ORPHANS_ID, virtualOrphansName, virtualOrphansDesc)
        viewModel.updateVirtualNames(Album.VIRTUAL_DUPLICATES_ID, virtualDuplicatesName, virtualDuplicatesDesc)
    }

    BackHandler(enabled = backStack.size > 1) {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    val settingsViewModel: SettingsViewModel = hiltViewModel(
        key = "settings-$sessionKey"
    )
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val activeSubMenu = when {
        currentTopKey !is NavKey.Settings -> SettingsSubMenu.NONE
        settingsUiState.showInteractionsDialog -> SettingsSubMenu.INTERACTIONS
        settingsUiState.showActionButtonsDialog -> SettingsSubMenu.ACTION_BUTTONS
        else -> SettingsSubMenu.NONE
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            HomeTopBar(
                isHome = isHome,
                isSwipeTab = currentTopKey is NavKey.Swipe || currentTopKey is NavKey.Duplicates,
                isSettingsTab = currentTopKey is NavKey.Settings,
                activeSubMenu = activeSubMenu,
                user = uiState.user,
                connectionStatus = uiState.connectionStatus,
                baseUrl = uiState.baseUrl,
                apiKey = uiState.apiKey,
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                onOpenStats = { viewModel.toggleStatsPopup(visible = true) },
                onGlobalReset = { viewModel.toggleGlobalResetConfirmation(true) },
                onSwipeReset = { viewModel.requestReset() },
                onOpenProfile = { viewModel.toggleProfilePopup(visible = true) },
                onBack = {
                    if (activeSubMenu != SettingsSubMenu.NONE) {
                        settingsViewModel.setShowInteractionsDialog(false)
                        settingsViewModel.setShowActionButtonsDialog(false)
                    } else if (backStack.size > 1) {
                        backStack.removeAt(backStack.lastIndex)
                    } else {
                        backStack.clear()
                        backStack.add(NavKey.Home)
                    }
                }
            )
        },
        bottomBar = {}
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = backStack,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    entryProvider = entryProvider {
                        entry<NavKey.Home> {
                            if (uiState.isLoading && uiState.albums.isEmpty()) {
                                Box(Modifier.fillMaxSize()) {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                }
                            } else if (uiState.error != null) {
                                ErrorView(error = uiState.error!!) { viewModel.loadUser() }
                            } else if (uiState.filteredAlbums.isEmpty() && uiState.searchQuery.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                        Spacer(Modifier.height(8.dp))
                                        Text(stringResource(R.string.home_no_results), color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            } else {
                                Crossfade(
                                    targetState = uiState.isGridView,
                                    animationSpec = tween(durationMillis = 500),
                                    label = "LayoutSwitch"
                                ) { isGrid ->
                                    if (isGrid) {
                                        AlbumGrid(
                                            groupedAlbums = uiState.groupedAlbums,
                                            treatedCounts = uiState.albumTreatedCounts,
                                            unsyncedChanges = uiState.albumUnsyncedChanges,
                                            collapsedCategories = uiState.collapsedCategories,
                                            isRefreshing = uiState.isRefreshing,
                                            baseUrl = uiState.baseUrl,
                                            apiKey = uiState.apiKey,
                                            onRefresh = { viewModel.refreshAlbums() },
                                            onAlbumClick = { album ->
                                                viewModel.onAlbumSelected(album)
                                                if (album.id == Album.VIRTUAL_DUPLICATES_ID) {
                                                    backStack.add(NavKey.Duplicates)
                                                } else {
                                                    backStack.add(NavKey.Swipe(album.id))
                                                }
                                            },
                                            onToggleCategory = viewModel::toggleCategory
                                        )
                                    } else {
                                        AlbumList(
                                            groupedAlbums = uiState.groupedAlbums,
                                            treatedCounts = uiState.albumTreatedCounts,
                                            unsyncedChanges = uiState.albumUnsyncedChanges,
                                            collapsedCategories = uiState.collapsedCategories,
                                            isRefreshing = uiState.isRefreshing,
                                            baseUrl = uiState.baseUrl,
                                            apiKey = uiState.apiKey,
                                            onRefresh = { viewModel.refreshAlbums() },
                                            onAlbumClick = { album ->
                                                viewModel.onAlbumSelected(album)
                                                if (album.id == Album.VIRTUAL_DUPLICATES_ID) {
                                                    backStack.add(NavKey.Duplicates)
                                                } else {
                                                    backStack.add(NavKey.Swipe(album.id))
                                                }
                                            },
                                            onToggleCategory = viewModel::toggleCategory
                                        )
                                    }
                                }
                            }
                        }
                        entry<NavKey.Swipe> { key ->
                            val selectedAlbum = uiState.filteredAlbums.firstOrNull { it.id == key.albumId }
                                ?: uiState.selectedAlbum?.takeIf { it.id == key.albumId }
                                ?: Album(
                                    id = key.albumId,
                                    albumName = if (key.albumId == Album.VIRTUAL_ALL_ID) virtualAllName else (uiState.virtualNames[key.albumId] ?: "Album"),
                                    description = uiState.virtualDescriptions[key.albumId],
                                    assetCount = 0,
                                    albumThumbnailAssetId = null
                                )

                            SwipeScreen(
                                album = selectedAlbum,
                                sessionKey = sessionKey,
                                resetSignal = viewModel.resetRequestSignal,
                                userQuotaBytes = uiState.user?.quotaUsageInBytes,
                                onBack = {
                                    if (backStack.size > 1) {
                                        backStack.removeAt(backStack.lastIndex)
                                    }
                                }
                            )
                        }
                        entry<NavKey.Duplicates> {
                            val duplicatesViewModel: DuplicatesViewModel = hiltViewModel(
                                key = "duplicates-$sessionKey"
                            )
                            DuplicatesScreen(
                                viewModel = duplicatesViewModel,
                                resetSignal = viewModel.resetRequestSignal
                            )
                        }
                        entry<NavKey.Settings> {
                            SettingsScreen(
                                viewModel = settingsViewModel
                            )
                        }
                    }
                )

                // Temporarily hidden floating navigation bar
                if (false) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 64.dp, vertical = 24.dp)
                            .navigationBarsPadding()
                    ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                        shape = CircleShape,
                        shadowElevation = 8.dp,
                        tonalElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            modifier = Modifier.height(52.dp),
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                            NavigationBarItem(
                                selected = currentTopKey is NavKey.Home,
                                onClick = {
                                    if (currentTopKey !is NavKey.Home) {
                                        backStack.clear()
                                        backStack.add(NavKey.Home)
                                        viewModel.refreshAlbums()
                                    }
                                },
                                icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home), modifier = Modifier.size(24.dp)) },
                                alwaysShowLabel = false,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                            NavigationBarItem(
                                selected = currentTopKey is NavKey.Swipe || currentTopKey is NavKey.Duplicates,
                                onClick = {
                                    if (currentTopKey !is NavKey.Swipe && currentTopKey !is NavKey.Duplicates) {
                                        val targetAlbum = uiState.selectedAlbum
                                        if (targetAlbum?.id == Album.VIRTUAL_DUPLICATES_ID) {
                                            backStack.add(NavKey.Duplicates)
                                        } else {
                                            val targetId = targetAlbum?.id ?: Album.VIRTUAL_ALL_ID
                                            backStack.add(NavKey.Swipe(targetId))
                                        }
                                    }
                                },
                                icon = { Icon(Icons.Default.Swipe, contentDescription = stringResource(R.string.nav_swipe), modifier = Modifier.size(24.dp)) },
                                alwaysShowLabel = false,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }
            }
            }
        }
    }

    if (uiState.showBackupWarning) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBackupWarning() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SyncLock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.backup_warning_title))
                }
            },
            text = { Text(stringResource(R.string.backup_warning_msg)) },
            confirmButton = {
                Button(onClick = { viewModel.dismissBackupWarning() }) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (currentTopKey !is NavKey.Settings) {
                        backStack.add(NavKey.Settings)
                    }
                    viewModel.dismissBackupWarning()
                }) {
                    Text(stringResource(R.string.backup_warning_settings))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (uiState.showProfilePopup) {
        ProfilePopup(
            user = uiState.user,
            savedAccounts = uiState.savedAccounts,
            connectionStatus = uiState.connectionStatus,
            baseUrl = uiState.baseUrl,
            apiKey = uiState.apiKey,
            onClose = { viewModel.toggleProfilePopup(visible = false) },
            onSettingsClick = { 
                if (currentTopKey !is NavKey.Settings) {
                    backStack.add(NavKey.Settings)
                }
                viewModel.toggleProfilePopup(visible = false)
            },
            onSwitchAccount = { viewModel.switchAccount(it) },
            onRemoveAccount = { viewModel.removeAccount(it) },
            onAddAccount = { viewModel.startAddAccount() },
            onLogout = { viewModel.logout() }
        )
    }

    if (uiState.isLoggingInToAnotherAccount) {
        val currentBaseUrl = uiState.baseUrl

        val authViewModel: AuthViewModel = hiltViewModel(
            key = "add_account_auth_viewmodel"
        )

        LaunchedEffect(Unit) {
            authViewModel.prepareForAddAccount(currentBaseUrl)
        }

        Dialog(
            onDismissRequest = { viewModel.cancelAddAccount() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column {
                    TopAppBar(
                        title = { Text(stringResource(R.string.profile_add_account_title)) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.cancelAddAccount() }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_cancel))
                            }
                        }
                    )
                    AuthScreen(
                        viewModel = authViewModel
                    )
                }
            }
        }
        
        val activeUserId by viewModel.getSessionRepository().sessionConfig.collectAsState(initial = null)
        LaunchedEffect(activeUserId) {
            if ((activeUserId != null) && (activeUserId?.userId != uiState.user?.id)) {
                viewModel.cancelAddAccount()
            }
        }
    }

    if (uiState.showStatsPopup) {
        StatsPopup(
            stats = uiState.stats,
            onClose = { viewModel.toggleStatsPopup(visible = false) }
        )
    }

    if (uiState.showGlobalResetConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleGlobalResetConfirmation(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.home_global_reset_confirm_title))
                }
            },
            text = { Text(stringResource(R.string.home_global_reset_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.resetAllDecisions() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleGlobalResetConfirmation(false) }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}
