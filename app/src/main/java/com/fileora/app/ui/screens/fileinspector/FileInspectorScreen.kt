package com.fileora.app.ui.screens.fileinspector

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileInspectorScreen(
    viewModel: FileInspectorViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onFileSelected(uri)
    }

    Scaffold(
        topBar = {
            FileoraTopBar(
                title = "File & EXIF Inspector",
                subtitle = "Deep inspection, checksums, and metadata",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    if (uiState.selectedFileUri != null) {
                        IconButton(onClick = { viewModel.shareFile() }) {
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
            if (uiState.selectedFileUri == null) {
                FilePickerBox(
                    title = "Choose Any File to Inspect",
                    subtitle = "Inspect documents, pictures, videos, audio, or unknown formats",
                    icon = Icons.Default.Info,
                    onClick = {
                        filePicker.launch(arrayOf("*/*"))
                    }
                )
            } else {
                uiState.fileDetails?.let { details ->
                    // File General Card
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
                                    Text(details.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(details.mimeType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                                    Text("Change")
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatCard(label = "Format", value = details.extension.uppercase().ifEmpty { "BIN" }, modifier = Modifier.weight(1f))
                                StatCard(label = "File Size", value = FileUtils.formatBytes(details.sizeBytes), modifier = Modifier.weight(1f))
                                StatCard(label = "Exact Bytes", value = "${details.sizeBytes} B", modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Checksums Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Cryptographic Integrity Checksums", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("MD5 Hash", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(details.md5Hash))
                                            Toast.makeText(context, "MD5 copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(details.md5Hash, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SHA-256 Hash", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(details.sha256Hash))
                                            Toast.makeText(context, "SHA-256 copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(details.sha256Hash, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Image / EXIF Info Card
                    if (details.isImage) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Image & Camera EXIF Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    StatCard(label = "Width", value = "${details.imageWidth} px", modifier = Modifier.weight(1f))
                                    StatCard(label = "Height", value = "${details.imageHeight} px", modifier = Modifier.weight(1f))
                                    val ratio = if (details.imageHeight > 0) String.format("%.2f", details.imageWidth.toFloat() / details.imageHeight) else "-"
                                    StatCard(label = "Aspect Ratio", value = ratio, modifier = Modifier.weight(1f))
                                }

                                if (details.exifData.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    details.exifData.forEach { (key, value) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(key, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // PDF Info Card
                    if (details.isPdf) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("PDF Document Properties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Page Count", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${details.pdfPageCount} pages", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isLoading) {
            ProcessingProgressDialog(
                title = "Inspecting File",
                progressMessage = "Analyzing file headers, calculating cryptographic checksums, and reading EXIF..."
            )
        }
    }
}
