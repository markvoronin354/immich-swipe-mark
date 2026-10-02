package com.markvoronin.immichswipe.feature.settings.components

import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.markvoronin.immichswipe.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun LogsDialog(
    rawLogs: String,
    context: Context,
    clipboard: Clipboard,
    scope: CoroutineScope,
    onClearLogs: () -> Unit,
    onDismiss: () -> Unit
) {
    val logsCopiedMessage = stringResource(R.string.settings_logs_copied_toast)
    val errorColor = MaterialTheme.colorScheme.error
    val warningColor = Color(0xFFFFA500)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_logs_dialog_title))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_close))
                }
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxSize().padding(16.dp),
        text = {
            val annotatedLogs = remember(rawLogs, errorColor) {
                buildAnnotatedString {
                    if (rawLogs.isNotEmpty()) {
                        rawLogs.lineSequence().forEach { line ->
                            val color = when {
                                line.contains(" E/") -> errorColor
                                line.contains(" W/") -> warningColor
                                else -> Color.Unspecified
                            }
                            withStyle(style = SpanStyle(color = color)) {
                                append(line + "\n")
                            }
                        }
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                val scroll = rememberScrollState()
                Text(
                    text = if (rawLogs.isEmpty()) AnnotatedString(stringResource(R.string.settings_logs_empty)) else annotatedLogs,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(scroll)
                )

                val isScrollable = remember { derivedStateOf { scroll.maxValue > 0 } }
                if (rawLogs.isNotEmpty() && isScrollable.value) {
                    val indicatorHeightFraction = 0.1f
                    val availableHeight = maxHeight
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(4.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(indicatorHeightFraction)
                                .fillMaxWidth()
                                .offset {
                                    IntOffset(x = 0, y = (availableHeight.toPx() * (scroll.value.toFloat() / scroll.maxValue) * (1f - indicatorHeightFraction)).toInt())
                                }
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    scope.launch {
                        val clipData = ClipData.newPlainText("Immich Swipe Logs", rawLogs)
                        clipboard.setClipEntry(ClipEntry(clipData))
                    }
                    Toast.makeText(context, logsCopiedMessage, Toast.LENGTH_SHORT).show()
                }
            ) {
                Text(stringResource(R.string.settings_logs_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = { 
                onClearLogs()
                onDismiss()
            }) {
                Text(stringResource(R.string.settings_logs_clear), color = MaterialTheme.colorScheme.error)
            }
        }
    )
}
