package com.markvoronin.immichswipe.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.SessionConfig
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.api.RetrofitFactory
import com.markvoronin.immichswipe.data.local.AppDatabase
import com.markvoronin.immichswipe.data.repository.AccountRepository
import com.markvoronin.immichswipe.data.repository.AssetRepository
import com.markvoronin.immichswipe.data.repository.AuthRepository
import com.markvoronin.immichswipe.data.repository.SwipeDecisionRepository
import com.markvoronin.immichswipe.domain.model.Album
import com.markvoronin.immichswipe.feature.auth.AuthScreen
import com.markvoronin.immichswipe.feature.auth.AuthViewModel
import com.markvoronin.immichswipe.feature.auth.AuthViewModelFactory
import com.markvoronin.immichswipe.feature.duplicates.DuplicatesScreen
import com.markvoronin.immichswipe.feature.duplicates.DuplicatesViewModel
import com.markvoronin.immichswipe.feature.duplicates.DuplicatesViewModelFactory
import com.markvoronin.immichswipe.feature.home.components.AlbumGrid
import com.markvoronin.immichswipe.feature.home.components.AlbumList
import com.markvoronin.immichswipe.feature.home.components.ErrorView
import com.markvoronin.immichswipe.feature.home.components.HomeTopBar
import com.markvoronin.immichswipe.feature.home.components.ProfilePopup
import com.markvoronin.immichswipe.feature.home.components.StatsPopup
import com.markvoronin.immichswipe.feature.home.components.SwipePlaceholder
import com.markvoronin.immichswipe.feature.settings.SettingsScreen
import com.markvoronin.immichswipe.feature.settings.SettingsSubMenu
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel
import com.markvoronin.immichswipe.feature.settings.SettingsViewModelFactory
import com.markvoronin.immichswipe.feature.swipe.SwipeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    assetRepository: AssetRepository,
    swipeDecisionRepository: SwipeDecisionRepository,
    sessionKey: String,
    modifier: Modifier = Modifier,
) {
    val uiState: HomeUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isHome = uiState.currentTab == HomeTab.HOME

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

    BackHandler(enabled = uiState.currentTab != HomeTab.HOME) {
        viewModel.goBack()
    }

    val settingsViewModel: SettingsViewModel = viewModel(
        key = "settings-$sessionKey",
        factory = SettingsViewModelFactory(
            viewModel.getSessionRepository(),
            swipeDecisionRepository
        )
    )
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val activeSubMenu = when {
        uiState.currentTab != HomeTab.SETTINGS -> SettingsSubMenu.NONE
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
                isSwipeTab = uiState.currentTab == HomeTab.SWIPE,
                isSettingsTab = uiState.currentTab == HomeTab.SETTINGS,
                activeSubMenu = activeSubMenu,
                user = uiState.user,
                connectionStatus = uiState.connectionStatus,
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
                    } else {
                        viewModel.goBack()
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
                AnimatedContent(
                    targetState = uiState.currentTab,
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally(animationSpec = tween(300)) { width -> direction * width } + fadeIn(tween(300)))
                            .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> -direction * width } + fadeOut(tween(300)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "TabTransition",
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) { targetTab ->
                    when (targetTab) {
                        HomeTab.HOME -> {
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
                                            onRefresh = { viewModel.refreshAlbums() },
                                            onAlbumClick = { viewModel.onAlbumSelected(it) },
                                            onToggleCategory = viewModel::toggleCategory
                                        )
                                    } else {
                                        AlbumList(
                                            groupedAlbums = uiState.groupedAlbums,
                                            treatedCounts = uiState.albumTreatedCounts,
                                            unsyncedChanges = uiState.albumUnsyncedChanges,
                                            collapsedCategories = uiState.collapsedCategories,
                                            isRefreshing = uiState.isRefreshing,
                                            onRefresh = { viewModel.refreshAlbums() },
                                            onAlbumClick = { viewModel.onAlbumSelected(it) },
                                            onToggleCategory = viewModel::toggleCategory
                                        )
                                    }
                                }
                            }
                        }
                        HomeTab.SWIPE -> {
                            if (uiState.selectedAlbum?.id == Album.VIRTUAL_DUPLICATES_ID) {
                                val duplicatesViewModel: DuplicatesViewModel = viewModel(
                                    key = "duplicates-$sessionKey",
                                    factory = DuplicatesViewModelFactory(
                                        api = RetrofitFactory.create(
                                            SessionConfig(
                                                SessionManager.getBaseUrl() ?: "", 
                                                SessionManager.getApiKey() ?: "",
                                                uiState.user?.id ?: ""
                                            )
                                        ),
                                        swipeDecisionRepository = swipeDecisionRepository
                                    )
                                )
                                DuplicatesScreen(
                                    viewModel = duplicatesViewModel,
                                    resetSignal = viewModel.resetRequestSignal
                                )
                            } else if (uiState.selectedAlbum != null) {
                                SwipeScreen(
                                    album = uiState.selectedAlbum!!,
                                    assetRepository = assetRepository,
                                    swipeDecisionRepository = swipeDecisionRepository,
                                    sessionRepository = viewModel.getSessionRepository(),
                                    sessionKey = sessionKey,
                                    resetSignal = viewModel.resetRequestSignal,
                                    userQuotaBytes = uiState.user?.quotaUsageInBytes,
                                    onBack = { viewModel.goBack() }
                                )
                            } else {
                                SwipePlaceholder(selectedAlbum = null)
                            }
                        }
                        HomeTab.SETTINGS -> {
                            SettingsScreen(
                                viewModel = settingsViewModel
                            )
                        }
                    }
                }

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
                                selected = uiState.currentTab == HomeTab.HOME,
                                onClick = { viewModel.onTabSelected(HomeTab.HOME) },
                                icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home), modifier = Modifier.size(24.dp)) },
                                alwaysShowLabel = false,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                            NavigationBarItem(
                                selected = uiState.currentTab == HomeTab.SWIPE,
                                onClick = { viewModel.onTabSelected(HomeTab.SWIPE) },
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
                    viewModel.onTabSelected(HomeTab.SETTINGS)
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
            onClose = { viewModel.toggleProfilePopup(visible = false) },
            onSettingsClick = { 
                viewModel.onTabSelected(HomeTab.SETTINGS)
                viewModel.toggleProfilePopup(visible = false)
            },
            onSwitchAccount = { viewModel.switchAccount(it) },
            onRemoveAccount = { viewModel.removeAccount(it) },
            onAddAccount = { viewModel.startAddAccount() },
            onLogout = { viewModel.logout() }
        )
    }

    if (uiState.isLoggingInToAnotherAccount) {
        val authRepository = remember { AuthRepository() }
        val database = AppDatabase.getDatabase(LocalContext.current)
        val accountRepository = remember { AccountRepository(database.userAccountDao()) }
        val currentBaseUrl = remember { SessionManager.getBaseUrl() ?: "" }

        val authViewModel: AuthViewModel = viewModel(
            key = "add_account_auth_viewmodel",
            factory = AuthViewModelFactory(
                viewModel.getSessionRepository(),
                authRepository,
                accountRepository
            )
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
