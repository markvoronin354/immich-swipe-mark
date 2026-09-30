package com.markvoronin.immichswipe.feature.settings.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.feature.settings.SettingsUiState
import com.markvoronin.immichswipe.feature.settings.SettingsViewModel

@Composable
fun InteractionsScreen(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
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

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                // 5. Auto-advance
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                // 6. Tap to swipe
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
            }
        }

        Spacer(Modifier.height(88.dp))
    }
}
