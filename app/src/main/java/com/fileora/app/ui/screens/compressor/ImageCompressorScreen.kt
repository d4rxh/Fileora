package com.fileora.app.ui.screens.compressor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.fileora.app.domain.model.ImageFormat
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCompressorScreen(
    viewModel: ImageCompressorViewModel = viewModel(),
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
                title = "Image Compressor",
                subtitle = "Reduce image weight with fine control",
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
            // File Picker Card or Selected Preview
            if (uiState.selectedImageUri == null) {
                FilePickerBox(
                    title = "Choose an Image to Compress",
                    subtitle = "Supports JPG, PNG, WEBP, and Camera Photos",
                    icon = Icons.Default.AddPhotoAlternate,
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
                                    text = "${FileUtils.formatBytes(uiState.originalSize)} • ${uiState.originalWidth} × ${uiState.originalHeight}px",
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
                                .height(200.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = uiState.selectedImageUri,
                                contentDescription = "Original Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // Compression Options Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Compression Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Quality Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Target Quality", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${uiState.quality}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = uiState.quality.toFloat(),
                                onValueChange = { viewModel.setQuality(it.toInt()) },
                                valueRange = 5f..100f,
                                steps = 18
                            )
                        }

                        // Scale Preset Buttons
                        Column {
                            Text("Resolution Scale", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(100, 75, 50, 25).forEach { scale ->
                                    FilterChip(
                                        selected = uiState.scalePercent == scale,
                                        onClick = { viewModel.setScalePercent(scale) },
                                        label = { Text("$scale%") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Output Format Choice
                        Column {
                            Text("Output Format", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(ImageFormat.JPEG, ImageFormat.WEBP).forEach { fmt ->
                                    FilterChip(
                                        selected = uiState.targetFormat == fmt,
                                        onClick = { viewModel.setTargetFormat(fmt) },
                                        label = { Text(fmt.displayName) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Compress Action Button
                        Button(
                            onClick = { viewModel.compress() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Compress, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compress Image Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Error Banner
                if (uiState.errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }

                // Result Card
                uiState.compressionResult?.let { result ->
                    ResultActionCard(
                        title = "Image Compressed Successfully! 🎉",
                        originalSize = result.originalSize,
                        outputSize = result.compressedSize,
                        onSaveToDownloads = { viewModel.saveToDownloads() },
                        onShare = { viewModel.shareResult() }
                    )
                }
            }
        }

        if (uiState.isProcessing) {
            ProcessingProgressDialog(
                title = "Compressing Image",
                progressMessage = "Applying downsampling and quality optimization..."
            )
        }
    }
}
