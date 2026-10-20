package com.fileora.app.ui.screens.batch

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fileora.app.domain.model.ImageFormat
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchProcessingScreen(
    viewModel: BatchProcessingViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val multiplePhotosPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        viewModel.onImagesSelected(uris)
    }

    Scaffold(
        topBar = {
            FileoraTopBar(
                title = "Batch Image Processing",
                subtitle = "Process dozens of photos in one tap",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Picker box or add more
            item {
                FilePickerBox(
                    title = if (uiState.selectedImages.isEmpty()) "Select Photos for Batch" else "Add More Photos (${uiState.selectedImages.size} selected)",
                    subtitle = "Supports selecting multiple photos at once",
                    icon = Icons.Default.PhotoLibrary,
                    onClick = {
                        multiplePhotosPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            if (uiState.selectedImages.isNotEmpty()) {
                // Action Selector
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Choose Batch Operation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = uiState.action == BatchAction.COMPRESS,
                                    onClick = { viewModel.setAction(BatchAction.COMPRESS) },
                                    label = { Text("Compress") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = uiState.action == BatchAction.CONVERT,
                                    onClick = { viewModel.setAction(BatchAction.CONVERT) },
                                    label = { Text("Convert") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = uiState.action == BatchAction.RESIZE,
                                    onClick = { viewModel.setAction(BatchAction.RESIZE) },
                                    label = { Text("Resize") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            when (uiState.action) {
                                BatchAction.COMPRESS -> {
                                    Column {
                                        Text("Quality: ${uiState.quality}%", style = MaterialTheme.typography.bodyMedium)
                                        Slider(
                                            value = uiState.quality.toFloat(),
                                            onValueChange = { viewModel.setQuality(it.toInt()) },
                                            valueRange = 10f..100f
                                        )
                                    }
                                }
                                BatchAction.CONVERT -> {
                                    Column {
                                        Text("Convert to format:", style = MaterialTheme.typography.bodyMedium)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            ImageFormat.entries.forEach { fmt ->
                                                FilterChip(
                                                    selected = uiState.targetFormat == fmt,
                                                    onClick = { viewModel.setTargetFormat(fmt) },
                                                    label = { Text(fmt.displayName) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                                BatchAction.RESIZE -> {
                                    Column {
                                        Text("Uniform Scale: ${uiState.scalePercent}%", style = MaterialTheme.typography.bodyMedium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                            listOf(25, 50, 75).forEach { pct ->
                                                FilterChip(
                                                    selected = uiState.scalePercent == pct,
                                                    onClick = { viewModel.setScalePercent(pct) },
                                                    label = { Text("$pct%") },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.processBatch() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Process ${uiState.selectedImages.size} Photos", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Result Card
                if (uiState.zipArchiveUri != null) {
                    item {
                        ResultActionCard(
                            title = "Batch Completed! (${uiState.processedUris.size} files ready)",
                            originalSize = uiState.totalOriginalBytes,
                            outputSize = uiState.totalProcessedBytes,
                            onSaveToDownloads = { viewModel.saveZipToDownloads() },
                            onShare = { viewModel.shareZip() }
                        )
                    }
                }

                // Selected Images list header
                item {
                    Text(
                        text = "Selected Images (${uiState.selectedImages.size}) • Total ${FileUtils.formatBytes(uiState.totalOriginalBytes)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Images items
                itemsIndexed(uiState.selectedImages) { index, img ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = img.uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(img.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(FileUtils.formatBytes(img.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.removeImage(index) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isProcessing) {
            ProcessingProgressDialog(
                title = "Processing Batch",
                progressMessage = "Processed ${uiState.currentProgress} of ${uiState.totalProgress} photos..."
            )
        }
    }
}
