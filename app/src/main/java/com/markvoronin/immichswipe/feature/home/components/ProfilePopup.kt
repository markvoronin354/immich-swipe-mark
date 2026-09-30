package com.markvoronin.immichswipe.feature.home.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.ConnectionLevel
import com.markvoronin.immichswipe.core.ConnectionStatus
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.data.local.entity.UserAccountEntity
import com.markvoronin.immichswipe.domain.model.User
import com.markvoronin.immichswipe.feature.settings.components.verticalFadingEdges

@Composable
fun ProfilePopup(
    user: User?,
    savedAccounts: List<UserAccountEntity>,
    connectionStatus: ConnectionStatus,
    onClose: () -> Unit,
    onSettingsClick: () -> Unit,
    onSwitchAccount: (String) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val baseUrl = SessionManager.getBaseUrl()?.removeSuffix("/")
    val apiKey = SessionManager.getApiKey() ?: ""

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalFadingEdges(scrollState, length = 32.dp)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                // Header avec logo et bouton fermer
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.height(24.dp),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Photo de profil grande
                val avatarColor = getAvatarColor(user?.avatarColor)
                val profileModifier = Modifier
                    .size(100.dp)
                    .border(3.dp, avatarColor, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)

                if ((user != null) && (baseUrl != null)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("$baseUrl/api/users/${user.id}/profile-image")
                            .addHeader("x-api-key", apiKey)
                            .crossfade(enable = true)
                            .build(),
                        contentDescription = stringResource(R.string.settings_section_account),
                        placeholder = rememberVectorPainter(Icons.Default.AccountCircle),
                        error = rememberVectorPainter(Icons.Default.AccountCircle),
                        modifier = profileModifier,
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = stringResource(R.string.settings_section_account),
                        modifier = profileModifier,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

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

                Spacer(Modifier.height(24.dp))

                // Diagnostic de connexion
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = connectionStatus.level.color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, connectionStatus.level.color.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(connectionStatus.level.color, CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when(connectionStatus.level) {
                                    ConnectionLevel.ONLINE -> stringResource(R.string.diag_online)
                                    ConnectionLevel.ISSUES -> stringResource(R.string.diag_issues)
                                    ConnectionLevel.OFFLINE -> stringResource(R.string.diag_offline)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = connectionStatus.level.color
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Liste des comptes
                Text(
                    text = stringResource(R.string.profile_saved_accounts),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start).padding(horizontal = 4.dp)
                )
                Spacer(Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Column {
                        savedAccounts.forEachIndexed { index, account ->
                            val isCurrent = account.userId == user?.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { if (!isCurrent) onSwitchAccount(account.userId) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val acAvatarColor = getAvatarColor(account.avatarColor)
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data("${account.baseUrl.removeSuffix("/")}/api/users/${account.userId}/profile-image")
                                            .addHeader("x-api-key", account.apiKey)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = null,
                                        placeholder = rememberVectorPainter(Icons.Default.AccountCircle),
                                        modifier = Modifier
                                            .size(32.dp)
                                            .border(1.dp, acAvatarColor, CircleShape)
                                            .padding(1.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (isCurrent) {
                                        Surface(
                                            modifier = Modifier.size(10.dp),
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
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = account.userEmail,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1
                                    )
                                }
                                
                                IconButton(
                                    onClick = { onRemoveAccount(account.userId) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (index < savedAccounts.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                            }
                        }
                        
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddAccount() }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PersonAdd, 
                                null, 
                                modifier = Modifier.size(32.dp).padding(4.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.profile_add_account),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Actions (Logout)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    PopupActionItem(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        text = stringResource(R.string.profile_logout_button),
                        onClick = onLogout,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Lien Code Source
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://github.com/markvoronin354/immich-swipe-android".toUri()
                            )
                            context.startActivity(intent)
                        }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = rememberVectorPainter(Icons.Default.Code),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.profile_source_code),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Version de l'application
                val packageInfo = remember {
                    try {
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    } catch (_: Exception) {
                        null
                    }
                }
                val versionName = packageInfo?.versionName ?: "3.2.0"
                
                Text(
                    text = "v$versionName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PopupActionItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
