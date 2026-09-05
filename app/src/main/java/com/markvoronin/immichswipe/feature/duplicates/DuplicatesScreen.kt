package com.markvoronin.immichswipe.feature.duplicates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markvoronin.immichswipe.core.SessionManager
import com.markvoronin.immichswipe.domain.model.Asset
import com.markvoronin.immichswipe.ui.theme.VirtualGold

import kotlinx.coroutines.flow.SharedFlow

@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.clusters.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = VirtualGold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No duplicates found!",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Duplicates",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val deleteCount = uiState.decisions.count { it.value == DuplicateDecision.DELETE }
                    Button(
                        onClick = { viewModel.syncDeletions() },
                        enabled = !uiState.isSyncing && deleteCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onError,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Delete $deleteCount")
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.autoSelect(keepLargest = true) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Keep Largest", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.autoSelect(keepLargest = false) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Keep Smallest", fontSize = 12.sp)
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.clusters, key = { it.clusterId }) { cluster ->
                        var isCardZoomed by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth().zIndex(if (isCardZoomed) 1f else 0f)) {
                            DuplicateClusterCard(
                                cluster = cluster,
                                decisions = uiState.decisions,
                                onDecisionToggle = { assetId -> viewModel.toggleDecision(assetId) },
                                onZoomChange = { isZoomed -> isCardZoomed = isZoomed }
                            )
                        }
                    }
                }
            }

            if (uiState.error != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicateClusterCard(
    cluster: DuplicateClusterUiModel,
    decisions: Map<String, DuplicateDecision>,
    onDecisionToggle: (String) -> Unit,
    onZoomChange: (Boolean) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${cluster.assets.size} similar photos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cluster.assets.forEach { asset ->
                    var isItemZoomed by remember { mutableStateOf(false) }
                    val decision = decisions[asset.id] ?: DuplicateDecision.KEEP
                    DuplicateAssetItem(
                        asset = asset,
                        decision = decision,
                        modifier = Modifier.weight(1f).zIndex(if (isItemZoomed) 1f else 0f),
                        onToggle = { onDecisionToggle(asset.id) },
                        onZoomStateChange = { isZoomed -> 
                            isItemZoomed = isZoomed
                            onZoomChange(isZoomed) 
                        } 
                    )
                }
            }
        }
    }
}

@Composable
fun DuplicateAssetItem(
    asset: Asset,
    decision: DuplicateDecision,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onZoomStateChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val baseUrl = remember { SessionManager.getBaseUrl()?.removeSuffix("/") }
    val apiKey = remember { SessionManager.getApiKey() ?: "" }

    val isDelete = decision == DuplicateDecision.DELETE
    val borderColor = if (isDelete) MaterialTheme.colorScheme.error else Color(0xFF4CAF50)
    
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val zIndexModifier = if (scale > 1f) Modifier.zIndex(1f) else Modifier
    Column(modifier = modifier.then(zIndexModifier), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(8.dp))
                .border(3.dp, borderColor, RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onToggle() }
                    )
                }
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            awaitFirstDown()
                            do {
                                val event = awaitPointerEvent()
                                val pointers = event.changes
                                
                                if (pointers.size >= 2 || scale > 1f) {
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    
                                    val oldScale = scale
                                    scale = (scale * zoom).coerceIn(1f, 3f)
                                    
                                    if (oldScale == 1f && scale > 1f) {
                                        onZoomStateChange(true)
                                    } else if (scale == 1f && oldScale > 1f) {
                                        onZoomStateChange(false)
                                    }
                                    
                                    val maxOffset = (scale - 1f) * size.width / 2
                                    offsetX = (offsetX + pan.x * scale).coerceIn(-maxOffset, maxOffset)
                                    offsetY = (offsetY + pan.y * scale).coerceIn(-maxOffset, maxOffset)
                                    
                                    if (scale == 1f) {
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                    
                                    pointers.forEach { 
                                        if (it.positionChange() != androidx.compose.ui.geometry.Offset.Zero) {
                                            it.consume()
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                            
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            onZoomStateChange(false)
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            val imageRequest = remember(asset.id, baseUrl, apiKey) {
                ImageRequest.Builder(context)
                    .data("$baseUrl/api/assets/${asset.id}/thumbnail?format=WEBP&size=preview")
                    .addHeader("x-api-key", apiKey)
                    .crossfade(true)
                    .build()
            }
            
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isDelete) Color.Black.copy(alpha = 0.4f) else Color.Transparent)
            ) {
                Icon(
                    imageVector = if (isDelete) Icons.Default.Delete else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isDelete) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(Color.White, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val sizeStr = asset.exifInfo?.fileSizeInBytes?.let { formatSizeStr(it) } ?: "Unknown size"
        Text(text = sizeStr, style = MaterialTheme.typography.bodySmall)
        Text(
            text = if (isDelete) "DELETE" else "KEEP",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = borderColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

fun formatSizeStr(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.2f MB", mb)
        else -> String.format("%.0f KB", kb)
    }
}
