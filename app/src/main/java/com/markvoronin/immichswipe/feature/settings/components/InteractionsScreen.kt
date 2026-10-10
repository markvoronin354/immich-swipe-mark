package com.markvoronin.immichswipe.feature.settings.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.DoubleTapAction
import com.markvoronin.immichswipe.feature.settings.SettingsUiState
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel

@Composable
fun InteractionsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = true) {
        onBack()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalFadingEdges(scrollState, length = 32.dp)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.height(16.dp))

        // Section 1: Button visibility
        SettingsSection(
            title = stringResource(R.string.settings_section_button_visibility),
            icon = Icons.Default.Visibility
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 0. Add to album
                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_show_add_to_album_label),
                    checked = uiState.showAddToAlbumButton,
                    onCheckedChange = { viewModel.setShowAddToAlbumButton(it) },
                    icon = Icons.Default.LibraryAdd
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                // 1. Keep/Delete
                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_show_swipe_buttons_label),
                    checked = uiState.showSwipeButtons,
                    onCheckedChange = { viewModel.setShowSwipeButtons(it) },
                    icon = Icons.Default.AdsClick
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                // 2. Archive
                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_show_archive_label),
                    checked = uiState.showArchiveButton,
                    onCheckedChange = { viewModel.setShowArchiveButton(it) },
                    icon = Icons.Default.Archive
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                // 3. Lock
                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_show_lock_label),
                    checked = uiState.showLockButton,
                    onCheckedChange = { viewModel.setShowLockButton(it) },
                    icon = Icons.Default.Lock
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                // 4. Favorite
                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_tri_favorite),
                    checked = uiState.showFavoriteButton,
                    onCheckedChange = { viewModel.setShowFavorite(it) },
                    icon = Icons.Default.Favorite
                )

                AnimatedVisibility(
                    visible = uiState.showFavoriteButton,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                        SettingsToggleItemSmall(
                            title = stringResource(R.string.settings_auto_next_label),
                            checked = uiState.autoNextOnFav,
                            onCheckedChange = { viewModel.setAutoNextOnFav(it) },
                            icon = Icons.AutoMirrored.Filled.Forward
                        )
                        Text(
                            text = stringResource(R.string.settings_auto_next_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 40.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Section 2: Actions
        SettingsSection(
            title = stringResource(R.string.settings_section_actions),
            icon = Icons.Default.TouchApp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 5. Tap to swipe
                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_tap_to_swipe_label),
                    checked = uiState.tapToSwipeEnabled,
                    onCheckedChange = { viewModel.setTapToSwipeEnabled(it) },
                    icon = Icons.Default.TouchApp
                )
                Text(
                    text = stringResource(R.string.settings_tap_to_swipe_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 40.dp, end = 16.dp, bottom = 8.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                // 6. Double tap
                SettingsToggleItemSmall(
                    title = stringResource(R.string.settings_double_tap_label),
                    checked = uiState.doubleTapEnabled,
                    onCheckedChange = { viewModel.setDoubleTapEnabled(it) },
                    icon = Icons.Default.TouchApp
                )

                AnimatedVisibility(
                    visible = uiState.doubleTapEnabled,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 40.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeButton(
                            text = stringResource(R.string.settings_double_tap_fullscreen),
                            icon = Icons.Default.Fullscreen,
                            selected = uiState.doubleTapAction == DoubleTapAction.FULLSCREEN,
                            onClick = { viewModel.setDoubleTapAction(DoubleTapAction.FULLSCREEN) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeButton(
                            text = stringResource(R.string.settings_double_tap_favorite),
                            icon = Icons.Default.Favorite,
                            selected = uiState.doubleTapAction == DoubleTapAction.FAVORITE,
                            onClick = { viewModel.setDoubleTapAction(DoubleTapAction.FAVORITE) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.navigationBarsPadding().height(24.dp))
    }
}
