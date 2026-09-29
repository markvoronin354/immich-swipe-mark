package com.markvoronin.immichswipe.feature.settings.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.IconPosition
import com.markvoronin.immichswipe.feature.settings.SettingsUiState
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel

@Composable
fun ActionButtonsDialog(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_action_buttons_dialog_title))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                }
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                IconPositionPicker(
                    title = stringResource(R.string.settings_fullscreen_pos_label),
                    selectedPosition = uiState.fullscreenButtonPosition,
                    onPositionSelected = { viewModel.setFullscreenButtonPosition(it) },
                    showIcon = uiState.showFullscreenButton,
                    onShowIconChange = { viewModel.setShowFullscreenButton(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                IconPositionPicker(
                    title = stringResource(R.string.settings_immich_pos_label),
                    selectedPosition = uiState.immichButtonPosition,
                    onPositionSelected = { viewModel.setImmichButtonPosition(it) },
                    showIcon = uiState.showImmichButton,
                    onShowIconChange = { viewModel.setShowImmichButton(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                IconPositionPicker(
                    title = stringResource(R.string.settings_rotation_pos_label),
                    selectedPosition = uiState.rotationButtonPosition,
                    onPositionSelected = { viewModel.setRotationButtonPosition(it) },
                    showIcon = uiState.showRotationButton,
                    onShowIconChange = { viewModel.setShowRotationButton(it) },
                    isBeta = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                IconPositionPicker(
                    title = stringResource(R.string.settings_mute_pos_label),
                    selectedPosition = uiState.muteButtonPosition,
                    onPositionSelected = { viewModel.setMuteButtonPosition(it) },
                    showIcon = uiState.showMuteButton,
                    onShowIconChange = { viewModel.setShowMuteButton(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                IconPositionPicker(
                    title = stringResource(R.string.settings_download_pos_label),
                    selectedPosition = uiState.downloadButtonPosition,
                    onPositionSelected = { viewModel.setDownloadButtonPosition(it) },
                    showIcon = uiState.showDownloadButton,
                    onShowIconChange = { viewModel.setShowDownloadButton(it) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                IconPositionPicker(
                    title = stringResource(R.string.settings_share_pos_label),
                    selectedPosition = uiState.shareButtonPosition,
                    onPositionSelected = { viewModel.setShareButtonPosition(it) },
                    showIcon = uiState.showShareButton,
                    onShowIconChange = { viewModel.setShowShareButton(it) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        }
    )
}

@Composable
fun IconPositionPicker(
    title: String,
    selectedPosition: IconPosition,
    onPositionSelected: (IconPosition) -> Unit,
    showIcon: Boolean,
    onShowIconChange: (Boolean) -> Unit,
    isBeta: Boolean = false
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (isBeta) {
                    BetaBadge()
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Switch(
                checked = showIcon,
                onCheckedChange = onShowIconChange,
                modifier = Modifier.scale(0.7f)
            )
        }
        
        AnimatedVisibility(
            visible = showIcon,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CornerButton(
                            text = stringResource(R.string.settings_pos_top_left),
                            selected = selectedPosition == IconPosition.TOP_LEFT,
                            onClick = { onPositionSelected(IconPosition.TOP_LEFT) },
                            modifier = Modifier.weight(1f)
                        )
                        CornerButton(
                            text = stringResource(R.string.settings_pos_top_right),
                            selected = selectedPosition == IconPosition.TOP_RIGHT,
                            onClick = { onPositionSelected(IconPosition.TOP_RIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CornerButton(
                            text = stringResource(R.string.settings_pos_bottom_left),
                            selected = selectedPosition == IconPosition.BOTTOM_LEFT,
                            onClick = { onPositionSelected(IconPosition.BOTTOM_LEFT) },
                            modifier = Modifier.weight(1f)
                        )
                        CornerButton(
                            text = stringResource(R.string.settings_pos_bottom_right),
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

@Composable
fun CornerButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
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

