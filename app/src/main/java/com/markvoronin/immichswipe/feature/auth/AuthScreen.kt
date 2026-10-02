package com.markvoronin.immichswipe.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markvoronin.immichswipe.R


/**
 * Écran d'authentification.
 */
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(15.dp))
                val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                val logoRes = if (isDark) R.drawable.immichswipe_logo_colors_dark else R.drawable.immichswipe_logo_colors_light
                
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier
                        .height(50.dp)
                        .padding(vertical = 4.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(8.dp))

                var urlDropdownExpanded by remember { mutableStateOf(false) }
                val textFieldShape = RoundedCornerShape(16.dp)

                Box(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = state.baseUrl,
                        onValueChange = viewModel::onBaseUrlChange,
                        label = { Text(stringResource(R.string.login_url_label)) },
                        placeholder = { Text(stringResource(R.string.login_url_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) },
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
                            .clip(textFieldShape)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = textFieldShape
                            )
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && state.savedServerUrls.isNotEmpty()) {
                                    urlDropdownExpanded = true
                                }
                            }
                            .semantics {
                                contentType = ContentType.Username
                            },
                        singleLine = true,
                        shape = textFieldShape,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )

                    DropdownMenu(
                        expanded = urlDropdownExpanded && state.savedServerUrls.isNotEmpty(),
                        onDismissRequest = { urlDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
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

                TextField(
                    value = state.apiKey,
                    onValueChange = viewModel::onApiKeyChange,
                    label = { Text(stringResource(R.string.login_api_key_label)) },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = if (state.apiKey.isNotEmpty()) {
                        {
                            IconButton(onClick = { viewModel.onApiKeyChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.login_clear_fields)
                                )
                            }
                        }
                    } else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(textFieldShape)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = textFieldShape
                        )
                        .semantics {
                            contentType = ContentType.Password
                        },
                    singleLine = true,
                    shape = textFieldShape,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearAllFields() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        enabled = !state.isLoading && (state.baseUrl.isNotEmpty() || state.apiKey.isNotEmpty()),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(R.string.login_clear_fields), fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = { viewModel.login() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.login_button), fontWeight = FontWeight.Bold)
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
        }
    }
}
