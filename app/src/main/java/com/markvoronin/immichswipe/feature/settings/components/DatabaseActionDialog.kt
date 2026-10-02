package com.markvoronin.immichswipe.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markvoronin.immichswipe.R
import com.markvoronin.immichswipe.feature.settings.DatabaseAction
import com.markvoronin.immichswipe.feature.settings.DatabaseScope

@Composable
fun AutoResizedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = FontWeight.Bold
) {
    var resizedTextStyle by remember(text, style) { mutableStateOf(style) }
    var shouldDraw by remember(text, style) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        modifier = modifier.drawWithContent {
            if (shouldDraw) {
                drawContent()
            }
        },
        softWrap = false,
        maxLines = 1,
        style = resizedTextStyle,
        onTextLayout = { result ->
            if (result.didOverflowWidth) {
                if (resizedTextStyle.fontSize.isSp && resizedTextStyle.fontSize.value > 12f) {
                    resizedTextStyle = resizedTextStyle.copy(
                        fontSize = (resizedTextStyle.fontSize.value * 0.9f).sp
                    )
                } else {
                    shouldDraw = true
                }
            } else {
                shouldDraw = true
            }
        }
    )
}

@Composable
fun DatabaseActionDialog(
    action: DatabaseAction,
    scope: DatabaseScope,
    userName: String,
    onScopeChange: (DatabaseAction, DatabaseScope) -> Unit,
    onConfirm: (DatabaseAction, DatabaseScope) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val titleRes = when(action) {
                DatabaseAction.DELETE -> R.string.settings_db_confirm_delete_title
                DatabaseAction.EXPORT -> R.string.settings_db_confirm_export_title
                DatabaseAction.IMPORT -> R.string.settings_db_confirm_import_title
            }
            AutoResizedText(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val msgRes = when(action) {
                    DatabaseAction.DELETE -> R.string.settings_db_confirm_delete_msg
                    DatabaseAction.IMPORT -> R.string.settings_db_confirm_import_msg
                    DatabaseAction.EXPORT -> null
                }
                msgRes?.let { Text(stringResource(it)) }
                
                if (action != DatabaseAction.IMPORT) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_db_scope_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(Modifier.selectableGroup()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = scope == DatabaseScope.USER,
                                    onClick = { onScopeChange(action, DatabaseScope.USER) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = scope == DatabaseScope.USER, onClick = null)
                            Text(stringResource(R.string.settings_db_scope_user, userName), modifier = Modifier.padding(start = 12.dp))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = scope == DatabaseScope.ALL,
                                    onClick = { onScopeChange(action, DatabaseScope.ALL) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = scope == DatabaseScope.ALL, onClick = null)
                            Text(stringResource(R.string.settings_db_scope_all), modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(action, scope) },
                colors = if (action == DatabaseAction.DELETE) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
            ) {
                Text(stringResource(R.string.common_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
