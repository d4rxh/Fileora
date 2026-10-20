package com.fileora.app.ui.screens.zipcreator

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fileora.app.FileoraApp
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.domain.utils.ZipEntryInfo
import com.fileora.app.domain.utils.ZipProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class SelectedFileItem(
    val uri: Uri,
    val name: String,
    val size: Long
)

enum class ZipMode {
    CREATE,
    INSPECT
}

data class ZipCreatorUiState(
    val mode: ZipMode = ZipMode.CREATE,
    val selectedFiles: List<SelectedFileItem> = emptyList(),
    val totalOriginalSize: Long = 0,
    val zipArchiveName: String = "fileora_archive",
    val isProcessing: Boolean = false,
    val currentProgress: Int = 0,
    val totalProgress: Int = 0,
    val outputZipUri: Uri? = null,
    val outputZipSize: Long = 0,
    // Inspect mode
    val inspectedZipUri: Uri? = null,
    val inspectedZipName: String = "",
    val inspectedEntries: List<ZipEntryInfo> = emptyList(),
    val errorMessage: String? = null
)

class ZipCreatorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val historyRepo = FileoraApp.instance.historyRepository

    private val _uiState = MutableStateFlow(ZipCreatorUiState())
    val uiState: StateFlow<ZipCreatorUiState> = _uiState.asStateFlow()

    fun setMode(mode: ZipMode) {
        _uiState.value = _uiState.value.copy(mode = mode, outputZipUri = null, errorMessage = null)
    }

    fun onFilesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val list = uris.map { uri ->
            SelectedFileItem(
                uri = uri,
                name = FileUtils.getFileName(context, uri),
                size = FileUtils.getFileSize(context, uri)
            )
        }
        val current = _uiState.value.selectedFiles + list
        _uiState.value = _uiState.value.copy(
            selectedFiles = current,
            totalOriginalSize = current.sumOf { it.size },
            outputZipUri = null
        )
    }

    fun removeFile(index: Int) {
        val list = _uiState.value.selectedFiles.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _uiState.value = _uiState.value.copy(
                selectedFiles = list,
                totalOriginalSize = list.sumOf { it.size }
            )
        }
    }

    fun setZipArchiveName(name: String) {
        _uiState.value = _uiState.value.copy(zipArchiveName = name)
    }

    fun createZip() {
        val files = _uiState.value.selectedFiles
        if (files.isEmpty()) return

        val cleanName = _uiState.value.zipArchiveName.trim().let {
            if (it.endsWith(".zip", ignoreCase = true)) it else "$it.zip"
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                currentProgress = 0,
                totalProgress = files.size,
                errorMessage = null
            )
            try {
                val zipFile = ZipProcessor.createZip(
                    context = context,
                    fileUris = files.map { it.uri },
                    zipFileName = cleanName,
                    onProgress = { cur, tot ->
                        _uiState.value = _uiState.value.copy(currentProgress = cur, totalProgress = tot)
                    }
                )

                val outUri = FileUtils.getFileProviderUri(context, zipFile)
                val outSize = zipFile.length()

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    outputZipUri = outUri,
                    outputZipSize = outSize
                )

                historyRepo.addRecord(
                    toolTitle = "ZIP Creator (${files.size} files)",
                    inputName = "${files.size} items",
                    outputName = cleanName,
                    originalSize = _uiState.value.totalOriginalSize,
                    finalSize = outSize,
                    outputUri = outUri
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.localizedMessage ?: "Failed to create ZIP archive"
                )
            }
        }
    }

    fun onInspectZipSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val name = FileUtils.getFileName(context, uri)
            val entries = ZipProcessor.inspectZip(context, uri)
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                inspectedZipUri = uri,
                inspectedZipName = name,
                inspectedEntries = entries
            )
        }
    }

    fun saveToDownloads() {
        val uri = _uiState.value.outputZipUri ?: return
        val temp = FileUtils.copyUriToTempFile(context, uri, "zip_", ".zip")
        val cleanName = _uiState.value.zipArchiveName.trim().let {
            if (it.endsWith(".zip", ignoreCase = true)) it else "$it.zip"
        }
        val saved = FileUtils.saveFileToDownloads(context, temp, "application/zip", cleanName)
        if (saved != null) {
            Toast.makeText(context, "Saved to Downloads/Fileora", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareZip() {
        val uri = _uiState.value.outputZipUri ?: return
        FileUtils.shareFile(context, uri, "application/zip")
    }
}
