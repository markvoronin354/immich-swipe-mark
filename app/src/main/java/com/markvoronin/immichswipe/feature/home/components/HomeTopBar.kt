package com.markvoronin.immichswipe.feature.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.ConnectionStatus
import com.markvoronin.immichswipe.domain.model.User
import com.markvoronin.immichswipe.feature.settings.SettingsSubMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    isHome: Boolean,
    isSwipeTab: Boolean,
    user: User?,
    connectionStatus: ConnectionStatus,
    onOpenStats: () -> Unit,
    onGlobalReset: () -> Unit,
    onSwipeReset: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    isSettingsTab: Boolean = false,
    activeSubMenu: SettingsSubMenu = SettingsSubMenu.NONE,
    baseUrl: String = "",
    apiKey: String = "",
    onBack: () -> Unit = {},
) {
    val showBackButton = isSettingsTab || isSwipeTab

    Column(modifier = modifier) {
        TopAppBar(
            navigationIcon = {
                AnimatedVisibility(
                    visible = showBackButton,
                    enter = fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                            slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { -it } +
                            expandHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing), expandFrom = Alignment.Start) +
                            scaleIn(initialScale = 0.7f, animationSpec = tween(320, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                           slideOutHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { -it } +
                           shrinkHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Start) +
                           scaleOut(targetScale = 0.7f, animationSpec = tween(260, easing = FastOutSlowInEasing))
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            title = {
                AnimatedContent(
                    targetState = isSettingsTab to activeSubMenu,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                            scaleIn(initialScale = 0.94f, animationSpec = tween(280, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                            scaleOut(targetScale = 0.94f, animationSpec = tween(220, easing = FastOutSlowInEasing)) using
                            SizeTransform(clip = false)
                    },
                    label = "TopBarTitleTransition"
                ) { (settingsTab, subMenu) ->
                    if (settingsTab) {
                        val (icon, titleRes) = when (subMenu) {
                            SettingsSubMenu.INTERACTIONS -> Icons.Default.TouchApp to R.string.settings_interactions_dialog_title
                            SettingsSubMenu.ACTION_BUTTONS -> Icons.Default.AdsClick to R.string.settings_action_buttons_dialog_title
                            else -> Icons.Default.Settings to R.string.settings_title
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = stringResource(titleRes),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                        val logoRes = if (isDark) R.drawable.immichswipe_logo_colors_dark else R.drawable.immichswipe_logo_colors_light

                        Image(
                            painter = painterResource(id = logoRes),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier
                                .height(32.dp)
                                .padding(vertical = 2.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            },
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 1. Stats Icon (Home screen only)
                    AnimatedVisibility(
                        visible = isHome,
                        enter = fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                                expandHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing), expandFrom = Alignment.Start) +
                                scaleIn(initialScale = 0.8f, animationSpec = tween(280, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                               shrinkHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Start) +
                               scaleOut(targetScale = 0.8f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                    ) {
                        IconButton(onClick = onOpenStats) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Statistiques"
                            )
                        }
                    }

                    // 2. Reset Icon (Home or Swipe tab) - Stays 100% static when navigating between Home and Swipe!
                    val showReset = isHome || isSwipeTab
                    AnimatedVisibility(
                        visible = showReset,
                        enter = fadeIn(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                                expandHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing), expandFrom = Alignment.Start) +
                                scaleIn(initialScale = 0.8f, animationSpec = tween(280, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                               shrinkHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing), shrinkTowards = Alignment.Start) +
                               scaleOut(targetScale = 0.8f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                    ) {
                        IconButton(
                            onClick = {
                                if (isHome) onGlobalReset() else onSwipeReset()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = stringResource(
                                    if (isHome) R.string.home_global_reset_button else R.string.swipe_reset_button
                                ),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = if (isHome) 0.7f else 0.8f)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = !isSettingsTab,
                        enter = fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { it } +
                                expandHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing), expandFrom = Alignment.End) +
                                scaleIn(initialScale = 0.8f, animationSpec = tween(320, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                               slideOutHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing)) { it } +
                               shrinkHorizontally(animationSpec = tween(260, easing = FastOutSlowInEasing), shrinkTowards = Alignment.End) +
                               scaleOut(targetScale = 0.8f, animationSpec = tween(260, easing = FastOutSlowInEasing))
                    ) {
                        Box(
                            modifier = Modifier.padding(end = 16.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            UserAvatar(
                                userId = user?.id,
                                baseUrl = baseUrl,
                                apiKey = apiKey,
                                name = user?.name ?: user?.email,
                                avatarColorName = user?.avatarColor,
                                modifier = Modifier.size(32.dp),
                                borderWidth = 1.dp,
                                contentDescription = stringResource(R.string.settings_section_account),
                                onClick = onOpenProfile
                            )

                            Surface(
                                modifier = Modifier
                                    .size(8.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                color = connectionStatus.level.color,
                                shape = CircleShape,
                            ) {}
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        )
    }
}

@Composable
fun SearchBarField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchShape = RoundedCornerShape(16.dp)
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(searchShape)
            .height(52.dp),
        placeholder = { Text(stringResource(R.string.home_search_placeholder), fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.common_cancel), modifier = Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        shape = searchShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
    )
}
