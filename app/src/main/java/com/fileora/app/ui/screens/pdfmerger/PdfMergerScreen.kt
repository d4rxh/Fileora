package com.fileora.app.ui.screens.pdfmerger

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfMergerScreen(
    viewModel: PdfMergerViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Multiple PDFs picker for Merge
    val multiplePdfsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        viewModel.onPdfsSelected(uris)
    }

    // Single PDF picker for Organize
    val singlePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onSinglePdfSelected(uri)
    }

    Scaffold(
        topBar = {
            FileoraTopBar(
                title = "PDF Merger & Organizer",
                subtitle = "Merge docs or extract & reorder pages",
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
            // Mode Selector Tabs
            TabRow(
                selectedTabIndex = if (uiState.mode == PdfToolMode.MERGE) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = uiState.mode == PdfToolMode.MERGE,
                    onClick = { viewModel.setMode(PdfToolMode.MERGE) },
                    text = { Text("Merge Multiple PDFs", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Layers, contentDescription = null) }
                )
                Tab(
                    selected = uiState.mode == PdfToolMode.ORGANIZE,
                    onClick = { viewModel.setMode(PdfToolMode.ORGANIZE) },
                    text = { Text("Organize & Delete Pages", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ViewModule, contentDescription = null) }
                )
            }

            if (uiState.mode == PdfToolMode.MERGE) {
                // Merge Mode View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        FilePickerBox(
                            title = if (uiState.pdfList.isEmpty()) "Select PDF Files to Merge" else "Add More PDFs (${uiState.pdfList.size} selected)",
                            subtitle = "Pick 2 or more PDFs to combine in sequential order",
                            icon = Icons.Default.Layers,
                            onClick = {
                                multiplePdfsLauncher.launch(arrayOf("application/pdf"))
                            }
                        )
                    }

                    if (uiState.pdfList.isNotEmpty()) {
                        item {
                            Button(
                                onClick = { viewModel.mergePdfs() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.CallMerge, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Merge ${uiState.pdfList.size} PDFs Into One Document", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (uiState.outputUri != null) {
                            item {
                                ResultActionCard(
                                    title = "PDFs Merged Successfully! 📑",
                                    originalSize = uiState.totalOriginalSize,
                                    outputSize = uiState.outputSize,
                                    onSaveToDownloads = { viewModel.saveToDownloads() },
                                    onShare = { viewModel.sharePdf() }
                                )
                            }
                        }

                        item {
                            Text(
                                text = "Merge Sequence (${uiState.pdfList.size} files)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        itemsIndexed(uiState.pdfList) { index, pdf ->
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

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pdf.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text(
                                            "${pdf.pageCount} pages • ${FileUtils.formatBytes(pdf.size)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(onClick = { viewModel.movePdfUp(index) }, enabled = index > 0) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                                    }

                                    IconButton(onClick = { viewModel.movePdfDown(index) }, enabled = index < uiState.pdfList.size - 1) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                                    }

                                    IconButton(onClick = { viewModel.removePdf(index) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Page Organizer Mode View
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    if (uiState.singlePdfUri == null) {
                        FilePickerBox(
                            title = "Open PDF to Organize Pages",
                            subtitle = "View all page thumbnails, toggle to delete or extract pages",
                            icon = Icons.Default.ViewModule,
                            onClick = {
                                singlePdfLauncher.launch(arrayOf("application/pdf"))
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(uiState.singlePdfName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                val kept = uiState.pageThumbnails.count { it.isIncluded }
                                Text("Keeping $kept of ${uiState.pageThumbnails.size} pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { singlePdfLauncher.launch(arrayOf("application/pdf")) }) {
                                Text("Change")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.exportOrganizedPdf() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export New PDF (${uiState.pageThumbnails.count { it.isIncluded }} Pages)")
                        }

                        if (uiState.outputUri != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            ResultActionCard(
                                title = "Organized PDF Ready! 📑",
                                originalSize = uiState.totalOriginalSize,
                                outputSize = uiState.outputSize,
                                onSaveToDownloads = { viewModel.saveToDownloads() },
                                onShare = { viewModel.sharePdf() }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Grid of Page Thumbnails
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.pageThumbnails) { page ->
                                Card(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { viewModel.togglePageInclusion(page.pageIndex) }
                                        .border(
                                            width = 2.dp,
                                            color = if (page.isIncluded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (page.isIncluded) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (page.thumbnail != null) {
                                                Image(
                                                    bitmap = page.thumbnail.asImageBitmap(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            } else {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("P. ${page.pageIndex + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            Checkbox(
                                                checked = page.isIncluded,
                                                onCheckedChange = { viewModel.togglePageInclusion(page.pageIndex) },
                                                modifier = Modifier.size(24.dp)
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
                title = "Processing PDF Document",
                progressMessage = "Analyzing document vector structures and rendering pages..."
            )
        }
    }
}
