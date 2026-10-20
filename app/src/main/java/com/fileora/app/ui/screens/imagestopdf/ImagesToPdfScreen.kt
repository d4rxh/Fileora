package com.fileora.app.ui.screens.imagestopdf

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesToPdfScreen(
    viewModel: ImagesToPdfViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val photosPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        viewModel.onImagesSelected(uris)
    }

    Scaffold(
        topBar = {
            FileoraTopBar(
                title = "Images to PDF",
                subtitle = "Turn pictures into a clean PDF document",
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
            item {
                FilePickerBox(
                    title = if (uiState.selectedImages.isEmpty()) "Select Photos for PDF" else "Add More Images (${uiState.selectedImages.size} pages)",
                    subtitle = "Pick photos in desired order or rearrange them below",
                    icon = Icons.Default.PictureAsPdf,
                    onClick = {
                        photosPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            if (uiState.selectedImages.isNotEmpty()) {
                // PDF Options Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Page Layout & Margins", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Column {
                                Text("Orientation", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Auto", "Portrait", "Landscape").forEach { ori ->
                                        FilterChip(
                                            selected = uiState.orientation == ori,
                                            onClick = { viewModel.setOrientation(ori) },
                                            label = { Text(ori) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Column {
                                Text("Margins", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(0 to "None", 18 to "Normal", 36 to "Wide").forEach { (margin, label) ->
                                        FilterChip(
                                            selected = uiState.margin == margin,
                                            onClick = { viewModel.setMargin(margin) },
                                            label = { Text(label) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = { viewModel.generatePdf() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate PDF (${uiState.selectedImages.size} Pages)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (uiState.outputPdfUri != null) {
                    item {
                        ResultActionCard(
                            title = "PDF Created Successfully! 📄",
                            originalSize = uiState.totalOriginalSize,
                            outputSize = uiState.outputPdfSize,
                            onSaveToDownloads = { viewModel.saveToDownloads() },
                            onShare = { viewModel.sharePdf() }
                        )
                    }
                }

                item {
                    Text(
                        text = "Page Order (${uiState.selectedImages.size} pages) • Tap arrows to reorder",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

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

                            IconButton(
                                onClick = { viewModel.moveImageUp(index) },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                            }

                            IconButton(
                                onClick = { viewModel.moveImageDown(index) },
                                enabled = index < uiState.selectedImages.size - 1
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
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
                title = "Creating PDF",
                progressMessage = "Assembling ${uiState.selectedImages.size} pages and rendering document..."
            )
        }
    }
}
