package com.fileora.app.ui.screens.resizer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageResizerScreen(
    viewModel: ImageResizerViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onImageSelected(uri)
    }

    Scaffold(
        topBar = {
            FileoraTopBar(
                title = "Image Resizer",
                subtitle = "Scale dimensions with aspect ratio lock",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.selectedImageUri == null) {
                FilePickerBox(
                    title = "Choose an Image to Resize",
                    subtitle = "Select photo to change pixel dimensions or aspect ratio",
                    icon = Icons.Default.AspectRatio,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.fileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Current: ${uiState.originalWidth} × ${uiState.originalHeight}px (${FileUtils.formatBytes(uiState.originalSize)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text("Change")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = uiState.selectedImageUri,
                                contentDescription = "Original",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // Dimension Control Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Target Dimensions (px)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = uiState.targetWidth,
                                onValueChange = { viewModel.onWidthChanged(it) },
                                label = { Text("Width") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            )

                            IconButton(
                                onClick = { viewModel.toggleMaintainAspectRatio() },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.maintainAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock Aspect Ratio",
                                    tint = if (uiState.maintainAspectRatio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = uiState.targetHeight,
                                onValueChange = { viewModel.onHeightChanged(it) },
                                label = { Text("Height") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        // Percentage Scale Presets
                        Column {
                            Text("Percentage Scale", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf(25, 50, 75, 125, 150, 200)) { pct ->
                                    OutlinedButton(
                                        onClick = { viewModel.applyPercentageScale(pct) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("$pct%")
                                    }
                                }
                            }
                        }

                        // Social Media Presets
                        Column {
                            Text("Standard Presets", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    AssistChip(
                                        onClick = { viewModel.applyPresetDimensions(1080, 1080) },
                                        label = { Text("Square (1080×1080)") }
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = { viewModel.applyPresetDimensions(1080, 1920) },
                                        label = { Text("Story (1080×1920)") }
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = { viewModel.applyPresetDimensions(1280, 720) },
                                        label = { Text("YouTube (1280×720)") }
                                    )
                                }
                                item {
                                    AssistChip(
                                        onClick = { viewModel.applyPresetDimensions(1920, 1080) },
                                        label = { Text("FHD (1920×1080)") }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.resize() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resize Image Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (uiState.errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                if (uiState.outputUri != null) {
                    ResultActionCard(
                        title = "Resized Successfully! 🎉",
                        originalSize = uiState.originalSize,
                        outputSize = uiState.outputSize,
                        onSaveToDownloads = { viewModel.saveToDownloads() },
                        onShare = { viewModel.shareResult() }
                    )
                }
            }
        }

        if (uiState.isProcessing) {
            ProcessingProgressDialog(
                title = "Resizing Image",
                progressMessage = "Scaling pixels to ${uiState.targetWidth} × ${uiState.targetHeight}..."
            )
        }
    }
}
