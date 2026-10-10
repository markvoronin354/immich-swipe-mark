package com.markvoronin.immichswipe.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.core.AppLogger
import com.markvoronin.immichswipe.feature.settings.components.LogsDialog

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    var apiKeyVisible by remember { mutableStateOf(false) }
    var urlDropdownExpanded by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        val minScreenHeight = maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = minScreenHeight)
                .padding(horizontal = 28.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (onCancel != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.profile_add_account_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(Modifier.height(1.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.height(180.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_icon_foreground),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(200.dp),
                        contentScale = ContentScale.Fit
                    )

                    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                    val textLogoRes = if (isDark) R.drawable.immichswipe_text_colors_dark else R.drawable.immichswipe_text_colors_light

                    Image(
                        painter = painterResource(id = textLogoRes),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier
                            .padding(top = 135.dp)
                            .height(48.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                val fieldShape = RoundedCornerShape(14.dp)

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.baseUrl,
                        onValueChange = viewModel::onBaseUrlChange,
                        label = { Text(stringResource(R.string.login_url_label)) },
                        placeholder = { Text(stringResource(R.string.login_url_placeholder)) },
                        trailingIcon = if (state.baseUrl.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.onBaseUrlChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.login_clear_fields)
                                    )
                                }
                            }
                        } else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && state.savedServerUrls.isNotEmpty()) {
                                    urlDropdownExpanded = true
                                }
                            }
                            .semantics {
                                contentType = ContentType.Username
                            },
                        singleLine = true,
                        shape = fieldShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    DropdownMenu(
                        expanded = urlDropdownExpanded && state.savedServerUrls.isNotEmpty(),
                        onDismissRequest = { urlDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        state.savedServerUrls.forEach { url ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = url,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { viewModel.removeSavedServerUrl(url) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.onBaseUrlChange(url)
                                    urlDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.apiKey,
                    onValueChange = viewModel::onApiKeyChange,
                    label = { Text(stringResource(R.string.login_api_key_label)) },
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle key visibility",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentType = ContentType.Password
                        },
                    singleLine = true,
                    shape = fieldShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(Modifier.height(4.dp))

                var showApiKeyTutorial by remember { mutableStateOf(false) }

                Text(
                    text = stringResource(R.string.login_api_key_tutorial_link),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp)
                        .clickable { showApiKeyTutorial = true }
                )

                if (showApiKeyTutorial) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showApiKeyTutorial = false },
                        title = { Text(stringResource(R.string.login_api_key_tutorial_title)) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.login_api_key_tutorial_step1))
                                Text(stringResource(R.string.login_api_key_tutorial_step2))
                                Text(stringResource(R.string.login_api_key_tutorial_step3))
                                Text(stringResource(R.string.login_api_key_tutorial_step4))
                                Text(stringResource(R.string.login_api_key_tutorial_step5))
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showApiKeyTutorial = false }) {
                                Text(stringResource(R.string.tutorial_finish))
                            }
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.login() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !state.isLoading,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.login_button),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (onCancel != null) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.common_back),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = { viewModel.clearAllFields() },
                        enabled = !state.isLoading && (state.baseUrl.isNotEmpty() || state.apiKey.isNotEmpty()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.login_clear_fields),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.error != null,
                    enter = fadeIn() + expandVertically()
                ) {
                    state.error?.let { error ->
                        val errorMessage = when (error) {
                            AuthError.EmptyFields -> stringResource(R.string.login_error_empty)
                            AuthError.Dns -> stringResource(R.string.login_error_dns)
                            AuthError.Timeout -> stringResource(R.string.login_error_timeout)
                            AuthError.Refused -> stringResource(R.string.login_error_refused)
                            AuthError.Auth -> stringResource(R.string.login_error_auth)
                            AuthError.Forbidden -> stringResource(R.string.login_error_forbidden)
                            AuthError.NotFound -> stringResource(R.string.login_error_not_found)
                            AuthError.Ssl -> stringResource(R.string.login_error_ssl)
                            is AuthError.Server -> stringResource(R.string.login_error_server, error.code)
                            is AuthError.Unknown -> error.message ?: stringResource(R.string.login_error_unknown)
                        }
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                if (state.success) {
                    Text(
                        text = stringResource(R.string.login_success),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (versionName.isNotEmpty()) "v$versionName " else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Logs",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { showLogsDialog = true }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        if (showLogsDialog) {
            LogsDialog(
                rawLogs = AppLogger.getLogs(),
                context = context,
                clipboard = clipboard,
                scope = scope,
                onClearLogs = { AppLogger.clearLogs() },
                onDismiss = { showLogsDialog = false }
            )
        }
    }
}
