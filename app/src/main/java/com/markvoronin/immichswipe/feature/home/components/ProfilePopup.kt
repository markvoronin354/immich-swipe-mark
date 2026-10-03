package com.markvoronin.immichswipe.feature.home.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.ConnectionLevel
import com.markvoronin.immichswipe.core.ConnectionStatus
import com.markvoronin.immichswipe.data.local.entity.UserAccountEntity
import com.markvoronin.immichswipe.domain.model.User
import com.markvoronin.immichswipe.feature.settings.components.verticalFadingEdges

@Composable
fun ProfilePopup(
    user: User?,
    savedAccounts: List<UserAccountEntity>,
    connectionStatus: ConnectionStatus,
    baseUrl: String = "",
    apiKey: String = "",
    onClose: () -> Unit,
    onSettingsClick: () -> Unit,
    onSwitchAccount: (String) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val baseUrlClean = baseUrl.removeSuffix("/")

    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var accountToRemove by remember { mutableStateOf<UserAccountEntity?>(null) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with logo and close button (static)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                    }
                    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                    val logoRes = if (isDark) R.drawable.immichswipe_logo_colors_dark else R.drawable.immichswipe_logo_colors_light
                    
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = null,
                        modifier = Modifier.height(22.dp),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }

                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalFadingEdges(scrollState, length = 32.dp)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(12.dp))

                // Profile Avatar
                UserAvatar(
                    userId = user?.id,
                    baseUrl = baseUrlClean,
                    apiKey = apiKey,
                    name = user?.name ?: user?.email,
                    avatarColorName = user?.avatarColor,
                    modifier = Modifier.size(96.dp),
                    borderWidth = 3.dp,
                    contentDescription = stringResource(R.string.settings_section_account)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = user?.name ?: stringResource(R.string.home_user_fallback),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user?.email ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(Modifier.height(20.dp))

                // Connection Diagnostic Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = connectionStatus.level.color.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, connectionStatus.level.color.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(connectionStatus.level.color, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = when(connectionStatus.level) {
                                ConnectionLevel.ONLINE -> stringResource(R.string.diag_online)
                                ConnectionLevel.ISSUES -> stringResource(R.string.diag_issues)
                                ConnectionLevel.OFFLINE -> stringResource(R.string.diag_offline)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = connectionStatus.level.color
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Saved Accounts Section Header (SettingsSection style)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.profile_saved_accounts),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Saved Accounts Card Container
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        savedAccounts.forEachIndexed { index, account ->
                            val isCurrent = account.userId == user?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!isCurrent) onSwitchAccount(account.userId) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    UserAvatar(
                                        userId = account.userId,
                                        baseUrl = account.baseUrl,
                                        apiKey = account.apiKey,
                                        name = account.userName ?: account.userEmail,
                                        avatarColorName = account.avatarColor,
                                        modifier = Modifier.size(32.dp),
                                        borderWidth = 1.dp,
                                        contentDescription = account.userName
                                    )
                                    if (isCurrent) {
                                        Surface(
                                            modifier = Modifier.size(12.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surface)
                                        ) {
                                            Icon(Icons.Default.Check, null, modifier = Modifier.padding(1.dp), tint = Color.White)
                                        }
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = account.userName ?: "User",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = account.userEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1
                                    )
                                }
                                
                                if (!isCurrent) {
                                    IconButton(
                                        onClick = { accountToRemove = account },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            if (index < savedAccounts.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                        
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddAccount() }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PersonAdd, 
                                null, 
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.profile_add_account),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (savedAccounts.size <= 1) {
                    Spacer(Modifier.height(20.dp))

                    // Logout Action Button (Settings style)
                    Button(
                        onClick = { showLogoutConfirmation = true },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.profile_logout_button),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Source Code Link
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/markvoronin354/immich-swipe-android".toUri()
                            )
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Default.Code),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.profile_source_code),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Application version
                val packageInfo = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    } catch (_: Exception) {
                        null
                    }
                }
                val versionName = packageInfo?.versionName ?: "3.4.0"
                
                Text(
                    text = "v$versionName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(Modifier.height(24.dp))
            }
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
                        onLogout()
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

    if (accountToRemove != null) {
        val targetAccount = accountToRemove!!
        val targetName = targetAccount.userName?.ifBlank { null } ?: targetAccount.userEmail
        AlertDialog(
            onDismissRequest = { accountToRemove = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.profile_remove_account_confirm_title))
                }
            },
            text = {
                Text(stringResource(R.string.profile_remove_account_confirm_msg, targetName))
            },
            confirmButton = {
                Button(
                    onClick = {
                        val userId = targetAccount.userId
                        accountToRemove = null
                        onRemoveAccount(userId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.common_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToRemove = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
