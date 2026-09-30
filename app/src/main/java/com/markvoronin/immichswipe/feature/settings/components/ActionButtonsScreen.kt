package com.markvoronin.immichswipe.feature.settings.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.TouchApp
import com.markvoronin.immichswipe.core.ImmichOpenMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.feature.settings.SettingsUiState
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel

@Composable
fun ActionButtonsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) {
        onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        // Sub-menu Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(4.dp))
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
                        imageVector = Icons.Default.AdsClick,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.settings_action_buttons_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.settings_action_buttons_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Options List
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconPositionPickerCard(
                title = stringResource(R.string.settings_fullscreen_pos_label),
                icon = Icons.Default.Fullscreen,
                selectedPosition = uiState.fullscreenButtonPosition,
                onPositionSelected = { viewModel.setFullscreenButtonPosition(it) },
                showIcon = uiState.showFullscreenButton,
                onShowIconChange = { viewModel.setShowFullscreenButton(it) }
            )

            ImmichCardAction(
                title = stringResource(R.string.settings_card_immich),
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                selectedPosition = uiState.immichButtonPosition,
                onPositionSelected = { viewModel.setImmichButtonPosition(it) },
                showIcon = uiState.showImmichButton,
                onShowIconChange = { viewModel.setShowImmichButton(it) },
                openMode = uiState.immichOpenMode,
                onOpenModeSelected = { viewModel.setImmichOpenMode(it) },
                longPressWeb = uiState.immichLongPressWeb,
                onLongPressWebChange = { viewModel.setImmichLongPressWeb(it) }
            )

            IconPositionPickerCard(
                title = stringResource(R.string.settings_rotation_pos_label),
                icon = Icons.AutoMirrored.Filled.RotateRight,
                selectedPosition = uiState.rotationButtonPosition,
                onPositionSelected = { viewModel.setRotationButtonPosition(it) },
                showIcon = uiState.showRotationButton,
                onShowIconChange = { viewModel.setShowRotationButton(it) },
                isBeta = true
            )

            IconPositionPickerCard(
                title = stringResource(R.string.settings_mute_pos_label),
                icon = Icons.AutoMirrored.Filled.VolumeOff,
                selectedPosition = uiState.muteButtonPosition,
                onPositionSelected = { viewModel.setMuteButtonPosition(it) },
                showIcon = uiState.showMuteButton,
                onShowIconChange = { viewModel.setShowMuteButton(it) }
            )

            IconPositionPickerCard(
                title = stringResource(R.string.settings_download_pos_label),
                icon = Icons.Default.FileDownload,
                selectedPosition = uiState.downloadButtonPosition,
                onPositionSelected = { viewModel.setDownloadButtonPosition(it) },
                showIcon = uiState.showDownloadButton,
                onShowIconChange = { viewModel.setShowDownloadButton(it) }
            )

            IconPositionPickerCard(
                title = stringResource(R.string.settings_share_pos_label),
                icon = Icons.Default.Share,
                selectedPosition = uiState.shareButtonPosition,
                onPositionSelected = { viewModel.setShareButtonPosition(it) },
                showIcon = uiState.showShareButton,
                onShowIconChange = { viewModel.setShowShareButton(it) }
            )
        }

        Spacer(Modifier.height(88.dp))
    }
}

@Composable
fun IconPositionPickerCard(
    title: String,
    icon: ImageVector,
    selectedPosition: IconPosition,
    onPositionSelected: (IconPosition) -> Unit,
    showIcon: Boolean,
    onShowIconChange: (Boolean) -> Unit,
    isBeta: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = if (showIcon) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (showIcon) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (showIcon) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.outline,
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

                    Column {
                        if (isBeta) {
                            BetaBadge()
                            Spacer(Modifier.height(2.dp))
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showIcon) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = showIcon,
                    onCheckedChange = onShowIconChange,
                    modifier = Modifier.scale(0.8f)
                )
            }

            AnimatedVisibility(
                visible = showIcon,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CornerButton(
                                text = stringResource(R.string.settings_pos_top_left),
                                icon = Icons.Default.NorthWest,
                                selected = selectedPosition == IconPosition.TOP_LEFT,
                                onClick = { onPositionSelected(IconPosition.TOP_LEFT) },
                                modifier = Modifier.weight(1f)
                            )
                            CornerButton(
                                text = stringResource(R.string.settings_pos_top_right),
                                icon = Icons.Default.NorthEast,
                                selected = selectedPosition == IconPosition.TOP_RIGHT,
                                onClick = { onPositionSelected(IconPosition.TOP_RIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CornerButton(
                                text = stringResource(R.string.settings_pos_bottom_left),
                                icon = Icons.Default.SouthWest,
                                selected = selectedPosition == IconPosition.BOTTOM_LEFT,
                                onClick = { onPositionSelected(IconPosition.BOTTOM_LEFT) },
                                modifier = Modifier.weight(1f)
                            )
                            CornerButton(
                                text = stringResource(R.string.settings_pos_bottom_right),
                                icon = Icons.Default.SouthEast,
                                selected = selectedPosition == IconPosition.BOTTOM_RIGHT,
                                onClick = { onPositionSelected(IconPosition.BOTTOM_RIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CornerButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ImmichCardAction(
    title: String,
    icon: ImageVector,
    selectedPosition: IconPosition,
    onPositionSelected: (IconPosition) -> Unit,
    showIcon: Boolean,
    onShowIconChange: (Boolean) -> Unit,
    openMode: ImmichOpenMode,
    onOpenModeSelected: (ImmichOpenMode) -> Unit,
    longPressWeb: Boolean,
    onLongPressWebChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = if (showIcon) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (showIcon) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (showIcon) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (showIcon) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = showIcon,
                    onCheckedChange = onShowIconChange,
                    modifier = Modifier.scale(0.8f)
                )
            }

            AnimatedVisibility(
                visible = showIcon,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.settings_immich_pos_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CornerButton(
                                text = stringResource(R.string.settings_pos_top_left),
                                icon = Icons.Default.NorthWest,
                                selected = selectedPosition == IconPosition.TOP_LEFT,
                                onClick = { onPositionSelected(IconPosition.TOP_LEFT) },
                                modifier = Modifier.weight(1f)
                            )
                            CornerButton(
                                text = stringResource(R.string.settings_pos_top_right),
                                icon = Icons.Default.NorthEast,
                                selected = selectedPosition == IconPosition.TOP_RIGHT,
                                onClick = { onPositionSelected(IconPosition.TOP_RIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CornerButton(
                                text = stringResource(R.string.settings_pos_bottom_left),
                                icon = Icons.Default.SouthWest,
                                selected = selectedPosition == IconPosition.BOTTOM_LEFT,
                                onClick = { onPositionSelected(IconPosition.BOTTOM_LEFT) },
                                modifier = Modifier.weight(1f)
                            )
                            CornerButton(
                                text = stringResource(R.string.settings_pos_bottom_right),
                                icon = Icons.Default.SouthEast,
                                selected = selectedPosition == IconPosition.BOTTOM_RIGHT,
                                onClick = { onPositionSelected(IconPosition.BOTTOM_RIGHT) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.settings_immich_open_mode_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OpenModeButton(
                            text = stringResource(R.string.settings_immich_open_mode_app),
                            icon = Icons.Default.PhoneAndroid,
                            selected = openMode == ImmichOpenMode.APP,
                            onClick = { onOpenModeSelected(ImmichOpenMode.APP) },
                            modifier = Modifier.weight(1f)
                        )
                        OpenModeButton(
                            text = stringResource(R.string.settings_immich_open_mode_web),
                            icon = Icons.Default.Language,
                            selected = openMode == ImmichOpenMode.WEB,
                            onClick = { onOpenModeSelected(ImmichOpenMode.WEB) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    AnimatedVisibility(
                        visible = openMode == ImmichOpenMode.APP,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.settings_immich_long_press_web),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = longPressWeb,
                                onCheckedChange = onLongPressWebChange,
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OpenModeButton(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun BetaBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Text(
            text = stringResource(R.string.common_beta),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
