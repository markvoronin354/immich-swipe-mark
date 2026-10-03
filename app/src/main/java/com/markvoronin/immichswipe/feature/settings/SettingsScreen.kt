package com.markvoronin.immichswipe.feature.settings

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AdsClick
import com.markvoronin.immichswipe.feature.home.components.UserAvatar
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhonelinkErase
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppTheme
import com.markvoronin.immichswipe.core.PlaybackBehavior
import com.markvoronin.immichswipe.feature.settings.components.ActionButtonsScreen
import com.markvoronin.immichswipe.feature.settings.components.ClearCacheDialog
import com.markvoronin.immichswipe.feature.settings.components.DatabaseActionDialog
import com.markvoronin.immichswipe.feature.settings.components.InteractionsScreen
import com.markvoronin.immichswipe.feature.settings.components.LogsDialog
import com.markvoronin.immichswipe.feature.settings.components.SettingsClickableItem
import com.markvoronin.immichswipe.feature.settings.components.SettingsSection
import com.markvoronin.immichswipe.feature.settings.components.SettingsToggleItemSmall
import com.markvoronin.immichswipe.feature.settings.components.ThemeButton
import com.markvoronin.immichswipe.feature.settings.components.verticalFadingEdges

enum class SettingsSubMenu {
    NONE, INTERACTIONS, ACTION_BUTTONS
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            viewModel.setSyncLocalDeletion(sync = false)
            Toast.makeText(context, "Permission denied. Local sync disabled.", Toast.LENGTH_SHORT).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val pendingScope = uiState.pendingDatabaseScope ?: DatabaseScope.USER
            context.contentResolver.openOutputStream(it)?.let { outputStream ->
                viewModel.exportDatabase(pendingScope, outputStream, context)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.let { inputStream ->
                viewModel.importDatabase(inputStream, context)
            }
        }
    }



    LaunchedEffect(uiState.databaseActionStatus) {
        uiState.databaseActionStatus?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearDatabaseActionStatus()
        }
    }

    val settingsScrollState = rememberScrollState()

    val activeSubMenu = when {
        uiState.showInteractionsDialog -> SettingsSubMenu.INTERACTIONS
        uiState.showActionButtonsDialog -> SettingsSubMenu.ACTION_BUTTONS
        else -> SettingsSubMenu.NONE
    }

    AnimatedContent(
        targetState = activeSubMenu,
        transitionSpec = {
            if (targetState != SettingsSubMenu.NONE) {
                (slideInHorizontally(animationSpec = tween(300)) { width -> width } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> -width } + fadeOut(animationSpec = tween(300)))
            } else {
                (slideInHorizontally(animationSpec = tween(300)) { width -> -width } + fadeIn(animationSpec = tween(300)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(300)) { width -> width } + fadeOut(animationSpec = tween(300)))
            }.using(SizeTransform(clip = false))
        },
        label = "settings_menu_transition"
    ) { subMenu ->
        when (subMenu) {
            SettingsSubMenu.INTERACTIONS -> {
                InteractionsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { viewModel.setShowInteractionsDialog(false) },
                    modifier = modifier
                )
            }
            SettingsSubMenu.ACTION_BUTTONS -> {
                ActionButtonsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { viewModel.setShowActionButtonsDialog(false) },
                    modifier = modifier
                )
            }
            SettingsSubMenu.NONE -> {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalFadingEdges(settingsScrollState, length = 32.dp)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(settingsScrollState)
                ) {
                    Spacer(Modifier.height(16.dp))
                    SettingsSection(title = stringResource(R.string.settings_section_appearance), icon = Icons.Default.Palette) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_theme_label),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeButton(
                        text = stringResource(R.string.settings_theme_system),
                        icon = Icons.Default.SettingsSuggest,
                        selected = uiState.themeMode == AppTheme.SYSTEM,
                        onClick = { viewModel.setThemeMode(AppTheme.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeButton(
                        text = stringResource(R.string.settings_theme_light),
                        icon = Icons.Default.LightMode,
                        selected = uiState.themeMode == AppTheme.LIGHT,
                        onClick = { viewModel.setThemeMode(AppTheme.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeButton(
                        text = stringResource(R.string.settings_theme_dark),
                        icon = Icons.Default.DarkMode,
                        selected = uiState.themeMode == AppTheme.DARK,
                        onClick = { viewModel.setThemeMode(AppTheme.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(16.dp))
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_dynamic_color_label),
                        checked = uiState.dynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) },
                        icon = Icons.Default.ColorLens
                    )
                    Text(
                        text = stringResource(R.string.settings_dynamic_color_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 40.dp, end = 16.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.settings_layout_label),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeButton(
                        text = stringResource(R.string.settings_layout_list),
                        icon = Icons.AutoMirrored.Filled.ViewList,
                        selected = !uiState.isDefaultLayoutGrid,
                        onClick = { viewModel.setDefaultLayoutGrid(false) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeButton(
                        text = stringResource(R.string.settings_layout_grid),
                        icon = Icons.Default.GridView,
                        selected = uiState.isDefaultLayoutGrid,
                        onClick = { viewModel.setDefaultLayoutGrid(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }



        Spacer(Modifier.height(16.dp))

        SettingsSection(title = stringResource(R.string.settings_section_interaction), icon = Icons.Default.TouchApp) {
            Column {
                SettingsClickableItem(
                    title = stringResource(R.string.settings_interactions_label),
                    subtitle = stringResource(R.string.settings_interactions_desc),
                    icon = Icons.Default.TouchApp,
                    onClick = { viewModel.setShowInteractionsDialog(true) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                SettingsClickableItem(
                    title = stringResource(R.string.settings_action_buttons_label),
                    subtitle = stringResource(R.string.settings_action_buttons_desc),
                    icon = Icons.Default.AdsClick,
                    onClick = { viewModel.setShowActionButtonsDialog(true) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsToggleItemSmall(
                        title = stringResource(R.string.settings_sync_local_deletion_label),
                        checked = uiState.syncLocalDeletion,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val perms = if (Build.VERSION.SDK_INT >= 33) {
                                    arrayOf(
                                        Manifest.permission.READ_MEDIA_IMAGES,
                                        Manifest.permission.READ_MEDIA_VIDEO
                                    )
                                } else {
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                permissionLauncher.launch(perms)
                            }
                            viewModel.setSyncLocalDeletion(checked)
                        },
                        icon = Icons.Default.PhonelinkErase
                    )
                    Text(
                        text = stringResource(R.string.settings_sync_local_deletion_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 40.dp, end = 16.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_video_behavior_label),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.settings_video_behavior_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeButton(
                            text = stringResource(R.string.settings_video_pause),
                            icon = Icons.AutoMirrored.Filled.VolumeOff,
                            selected = uiState.playbackBehavior == PlaybackBehavior.PAUSE_OTHERS,
                            onClick = { viewModel.setPlaybackBehavior(PlaybackBehavior.PAUSE_OTHERS) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeButton(
                            text = stringResource(R.string.settings_video_ignore),
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            selected = uiState.playbackBehavior == PlaybackBehavior.IGNORE,
                            onClick = { viewModel.setPlaybackBehavior(PlaybackBehavior.IGNORE) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = stringResource(R.string.settings_section_database), icon = Icons.Default.Storage) {
            Column {
                SettingsClickableItem(
                    title = stringResource(R.string.settings_db_delete_label),
                    subtitle = stringResource(R.string.settings_db_delete_desc),
                    icon = Icons.Default.DeleteForever,
                    isDestructive = true,
                    onClick = { viewModel.requestDatabaseAction(DatabaseAction.DELETE, DatabaseScope.USER) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsClickableItem(
                    title = stringResource(R.string.settings_db_export_label),
                    subtitle = stringResource(R.string.settings_db_export_desc),
                    icon = Icons.Default.FileUpload,
                    onClick = { viewModel.requestDatabaseAction(DatabaseAction.EXPORT, DatabaseScope.USER) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsClickableItem(
                    title = stringResource(R.string.settings_db_import_label),
                    subtitle = stringResource(R.string.settings_db_import_desc),
                    icon = Icons.Default.FileDownload,
                    onClick = { viewModel.requestDatabaseAction(DatabaseAction.IMPORT, DatabaseScope.ALL) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = stringResource(R.string.settings_section_debug), icon = Icons.Default.BugReport) {
            Column {
                SettingsClickableItem(
                    title = stringResource(R.string.settings_view_logs_label),
                    subtitle = stringResource(R.string.settings_view_logs_desc),
                    icon = Icons.Default.History,
                    onClick = { viewModel.setShowLogs(true) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsClickableItem(
                    title = stringResource(R.string.settings_clear_cache_label),
                    subtitle = stringResource(R.string.settings_clear_cache_desc),
                    icon = Icons.Default.DeleteSweep,
                    isDestructive = true,
                    onClick = { viewModel.setShowClearCacheConfirmation(true) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = stringResource(R.string.settings_section_about), icon = Icons.Default.Info) {
            Column {
                SettingsClickableItem(
                    title = stringResource(R.string.settings_privacy_policy_label),
                    subtitle = stringResource(R.string.settings_privacy_policy_desc),
                    icon = Icons.Default.PrivacyTip,
                    onClick = { uriHandler.openUri("https://github.com/markvoronin354/immich-swipe-android/blob/main/PRIVACY_POLICY.md") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                SettingsClickableItem(
                    title = stringResource(R.string.settings_source_code_label),
                    subtitle = stringResource(R.string.settings_source_code_desc),
                    icon = Icons.Default.Code,
                    onClick = { uriHandler.openUri("https://github.com/markvoronin354/immich-swipe-android") }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            onClick = { uriHandler.openUri("https://ko-fi.com/markvoronin354") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolunteerActivism,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.settings_buy_me_a_coffee),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(title = stringResource(R.string.settings_section_account), icon = Icons.Default.Person) {
            var showLogoutConfirmation by remember { mutableStateOf(false) }
            var showRemoveAllConfirmation by remember { mutableStateOf(false) }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        userId = null,
                        baseUrl = "",
                        apiKey = "",
                        name = uiState.userName,
                        avatarColorName = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(text = uiState.userName, style = MaterialTheme.typography.titleMedium)
                        Text(text = stringResource(R.string.profile_connected_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                if (uiState.savedAccountsCount <= 1) {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showLogoutConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_logout_button), fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { showRemoveAllConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_remove_all_accounts_button), fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (showLogoutConfirmation) {
                AlertDialog(
                    onDismissRequest = { showLogoutConfirmation = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.profile_logout_confirm_title))
                        }
                    },
                    text = {
                        Text(stringResource(R.string.profile_logout_confirm_msg))
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLogoutConfirmation = false
                                viewModel.logout()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.profile_logout_button))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutConfirmation = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }

            if (showRemoveAllConfirmation) {
                AlertDialog(
                    onDismissRequest = { showRemoveAllConfirmation = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.profile_remove_all_accounts_confirm_title))
                        }
                    },
                    text = {
                        Text(stringResource(R.string.profile_remove_all_accounts_confirm_msg))
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showRemoveAllConfirmation = false
                                viewModel.removeAllAccounts()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.profile_remove_all_accounts_button))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRemoveAllConfirmation = false }) {
                            Text(stringResource(R.string.common_cancel))
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(88.dp))
            }
        }
        }
    }

    if (uiState.showLogsDialog) {
        LogsDialog(
            rawLogs = viewModel.getLogs(),
            context = context,
            clipboard = clipboard,
            scope = scope,
            onClearLogs = { viewModel.clearLogs() },
            onDismiss = { viewModel.setShowLogs(false) }
        )
    }

    uiState.pendingDatabaseAction?.let { action ->
        val pendingScope = uiState.pendingDatabaseScope ?: DatabaseScope.USER
        DatabaseActionDialog(
            action = action,
            scope = pendingScope,
            userName = uiState.userName,
            onScopeChange = { act, sc -> viewModel.requestDatabaseAction(act, sc) },
            onConfirm = { act, sc ->
                when(act) {
                    DatabaseAction.DELETE -> viewModel.executeDelete(sc, context)
                    DatabaseAction.EXPORT -> {
                        val fileName = "immich_swipe_backup_${if(sc == DatabaseScope.ALL) "total" else "user"}_${System.currentTimeMillis()}.json"
                        exportLauncher.launch(fileName)
                    }
                    DatabaseAction.IMPORT -> importLauncher.launch("application/json")
                }
            },
            onDismiss = { viewModel.dismissDatabaseConfirmation() }
        )
    }

    if (uiState.showClearCacheConfirmation) {
        ClearCacheDialog(
            context = context,
            onConfirm = { ctx -> viewModel.clearAppCache(ctx) },
            onDismiss = { viewModel.setShowClearCacheConfirmation(false) }
        )
    }
}
