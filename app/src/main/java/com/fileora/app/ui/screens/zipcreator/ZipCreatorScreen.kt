package com.fileora.app.ui.screens.zipcreator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipCreatorScreen(
    viewModel: ZipCreatorViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Multiple file picker for ZIP creation
    val multiFilesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.onFilesSelected(uris)
    }

    // Single ZIP file picker for inspection
    val zipInspectPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onInspectZipSelected(uri)
    }

    Scaffold(
        topBar = {
            FileoraTopBar(
                title = "ZIP Archive Suite",
                subtitle = "Package files into compressed archives",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = if (uiState.mode == ZipMode.CREATE) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.mode == ZipMode.CREATE,
                    onClick = { viewModel.setMode(ZipMode.CREATE) },
                    text = { Text("Create ZIP Archive", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.FolderZip, contentDescription = null) }
                )
                Tab(
                    selected = uiState.mode == ZipMode.INSPECT,
                    onClick = { viewModel.setMode(ZipMode.INSPECT) },
                    text = { Text("Inspect ZIP Contents", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
            }

            if (uiState.mode == ZipMode.CREATE) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        FilePickerBox(
                            title = if (uiState.selectedFiles.isEmpty()) "Select Files to Archive" else "Add More Files (${uiState.selectedFiles.size} selected)",
                            subtitle = "Pick images, docs, videos, or any file format",
                            icon = Icons.Default.FolderZip,
                            onClick = {
                                multiFilesPicker.launch(arrayOf("*/*"))
                            }
                        )
                    }

                    if (uiState.selectedFiles.isNotEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text("Archive Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                                    OutlinedTextField(
                                        value = uiState.zipArchiveName,
                                        onValueChange = { viewModel.setZipArchiveName(it) },
                                        label = { Text("ZIP Archive Name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Button(
                                        onClick = { viewModel.createZip() },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Compress, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Compress ${uiState.selectedFiles.size} Files Into ZIP", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (uiState.outputZipUri != null) {
                            item {
                                ResultActionCard(
                                    title = "ZIP Archive Created Successfully! 📦",
                                    originalSize = uiState.totalOriginalSize,
                                    outputSize = uiState.outputZipSize,
                                    onSaveToDownloads = { viewModel.saveToDownloads() },
                                    onShare = { viewModel.shareZip() }
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Selected Files (${uiState.selectedFiles.size}) • Total: ${FileUtils.formatBytes(uiState.totalOriginalSize)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        itemsIndexed(uiState.selectedFiles) { index, file ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(FileUtils.formatBytes(file.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { viewModel.removeFile(index) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Inspect Mode
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        FilePickerBox(
                            title = if (uiState.inspectedZipUri == null) "Select a ZIP File to Inspect" else "Change ZIP Archive",
                            subtitle = "Inspect file entries and compressed vs uncompressed ratios without unzipping",
                            icon = Icons.Default.Search,
                            onClick = {
                                zipInspectPicker.launch(arrayOf("application/zip", "application/x-zip-compressed"))
                            }
                        )
                    }

                    if (uiState.inspectedZipUri != null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(uiState.inspectedZipName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("${uiState.inspectedEntries.size} items inside archive", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        items(uiState.inspectedEntries) { entry ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = if (entry.isDirectory) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                        if (!entry.isDirectory) {
                                            Text(
                                                "Uncompressed: ${FileUtils.formatBytes(entry.uncompressedSize)} • Compressed: ${FileUtils.formatBytes(entry.compressedSize)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isProcessing) {
            ProcessingProgressDialog(
                title = "Packaging ZIP Archive",
                progressMessage = "Packing ${uiState.currentProgress} of ${uiState.totalProgress} files..."
            )
        }
    }
}
