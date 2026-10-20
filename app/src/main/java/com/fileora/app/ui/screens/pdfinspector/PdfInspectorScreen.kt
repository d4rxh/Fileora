package com.fileora.app.ui.screens.pdfinspector

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfInspectorScreen(
    viewModel: PdfInspectorViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onPdfSelected(uri)
    }

    Scaffold(
        topBar = {
            FileoraTopBar(
                title = "PDF Inspector",
                subtitle = "Analyze metadata, dimensions, and pages",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    if (uiState.selectedPdfUri != null) {
                        IconButton(onClick = { viewModel.sharePdf() }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
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
            if (uiState.selectedPdfUri == null) {
                FilePickerBox(
                    title = "Choose a PDF Document to Inspect",
                    subtitle = "Inspect file structure, page dimensions, and cryptographic hashes",
                    icon = Icons.Default.FindInPage,
                    onClick = {
                        pdfPicker.launch(arrayOf("application/pdf"))
                    }
                )
            } else {
                uiState.metadata?.let { meta ->
                    // Main overview card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(meta.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("PDF Document", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = { pdfPicker.launch(arrayOf("application/pdf")) }) {
                                    Text("Change")
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard(label = "Total Pages", value = "${meta.pageCount}", modifier = Modifier.weight(1f))
                                StatCard(label = "File Size", value = FileUtils.formatBytes(meta.fileSize), modifier = Modifier.weight(1f))
                                StatCard(label = "Dimensions", value = meta.pageDimensions ?: "Standard", modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Hashes Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Cryptographic Checksums", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Column {
                                Text("MD5 Hash:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(uiState.md5Hash, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            Column {
                                Text("SHA-256 Hash:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(uiState.sha256Hash, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Page Preview Browser
                    if (meta.pageCount > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Page Preview Browser", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Page ${uiState.currentPageIndex + 1} of ${meta.pageCount}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.currentPageBitmap != null) {
                                        Image(
                                            bitmap = uiState.currentPageBitmap!!.asImageBitmap(),
                                            contentDescription = "PDF Page Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        CircularProgressIndicator()
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.loadPage(uiState.currentPageIndex - 1) },
                                        enabled = uiState.currentPageIndex > 0,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Previous Page")
                                    }

                                    Button(
                                        onClick = { viewModel.loadPage(uiState.currentPageIndex + 1) },
                                        enabled = uiState.currentPageIndex < meta.pageCount - 1,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Next Page")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isLoading) {
            ProcessingProgressDialog(
                title = "Analyzing PDF",
                progressMessage = "Reading structure, counting pages, and calculating hash values..."
            )
        }
    }
}
