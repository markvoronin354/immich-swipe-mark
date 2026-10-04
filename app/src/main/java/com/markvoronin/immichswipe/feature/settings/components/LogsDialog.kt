package com.markvoronin.immichswipe.feature.settings.components

import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.markvoronin.immichswipe.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.common_close)
                    )
                }
                Text(
                    text = stringResource(R.string.settings_logs_dialog_title),
                    modifier = Modifier.padding(horizontal = 36.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
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

                LaunchedEffect(scroll.maxValue) {
                    if (scroll.maxValue > 0) {
                        scroll.scrollTo(scroll.maxValue)
                    }
                }
                Text(
                    text = if (rawLogs.isEmpty()) AnnotatedString(stringResource(R.string.settings_logs_empty)) else annotatedLogs,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 20.dp)
                        .verticalScroll(scroll)
                )

                val isScrollable = remember { derivedStateOf { scroll.maxValue > 0 } }
                if (rawLogs.isNotEmpty() && isScrollable.value) {
                    val density = LocalDensity.current
                    val coroutineScope = rememberCoroutineScope()
                    var isDragging by remember { mutableStateOf(false) }

                    val minThumbHeightDp = 44.dp
                    val minThumbHeightPx = with(density) { minThumbHeightDp.toPx() }
                    val trackWidthDp = 12.dp

                    val availableHeightPx = with(density) { maxHeight.toPx() }
                    val maxScrollPx = scroll.maxValue.toFloat()

                    val (thumbHeightPx, trackDraggableRangePx) = remember(maxScrollPx, availableHeightPx) {
                        val contentHeightPx = maxScrollPx + availableHeightPx
                        val rawThumbHeightPx = availableHeightPx * (availableHeightPx / contentHeightPx)
                        val thumbHeight = rawThumbHeightPx.coerceIn(minThumbHeightPx, availableHeightPx * 0.8f)
                        val draggableRange = (availableHeightPx - thumbHeight).coerceAtLeast(1f)
                        thumbHeight to draggableRange
                    }

                    var scrollJob by remember { mutableStateOf<Job?>(null) }

                    fun updateScrollPosition(touchYPx: Float) {
                        val targetThumbTopPx = touchYPx - (thumbHeightPx / 2f)
                        val targetRatio = (targetThumbTopPx / trackDraggableRangePx).coerceIn(0f, 1f)
                        val targetScrollValue = (targetRatio * maxScrollPx).roundToInt()
                        scrollJob?.cancel()
                        scrollJob = coroutineScope.launch {
                            scroll.scrollTo(targetScrollValue)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(trackWidthDp + 6.dp)
                            .background(
                                color = if (isDragging) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .pointerInput(maxScrollPx, availableHeightPx) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    isDragging = true
                                    updateScrollPosition(down.position.y)

                                    val dragPointerId = down.id
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val dragChange = event.changes.firstOrNull { it.id == dragPointerId }
                                        if (dragChange == null || !dragChange.pressed) {
                                            break
                                        }
                                        dragChange.consume()
                                        updateScrollPosition(dragChange.position.y)
                                    }
                                    isDragging = false
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .width(trackWidthDp)
                                .height(with(density) { thumbHeightPx.toDp() })
                                .offset {
                                    val currentScrollRatio = (scroll.value.toFloat() / maxScrollPx).coerceIn(0f, 1f)
                                    val thumbOffsetPx = trackDraggableRangePx * currentScrollRatio
                                    IntOffset(x = 0, y = thumbOffsetPx.roundToInt())
                                }
                                .background(
                                    color = if (isDragging) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(6.dp)
                                )
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
