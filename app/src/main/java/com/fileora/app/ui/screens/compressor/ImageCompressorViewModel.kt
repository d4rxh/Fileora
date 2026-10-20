package com.fileora.app.ui.screens.compressor

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fileora.app.FileoraApp
import com.fileora.app.domain.model.CompressionResult
import com.fileora.app.domain.model.ImageFormat
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.domain.utils.ImageProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class CompressorUiState(
    val selectedImageUri: Uri? = null,
    val fileName: String = "",
    val originalSize: Long = 0,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val quality: Int = 75,
    val scalePercent: Int = 100,
    val targetFormat: ImageFormat = ImageFormat.JPEG,
    val isProcessing: Boolean = false,
    val compressionResult: CompressionResult? = null,
    val lastSavedFile: File? = null,
    val errorMessage: String? = null
)

class ImageCompressorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val historyRepo = FileoraApp.instance.historyRepository

    private val _uiState = MutableStateFlow(CompressorUiState())
    val uiState: StateFlow<CompressorUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            val name = FileUtils.getFileName(context, uri)
            val size = FileUtils.getFileSize(context, uri)
            val (w, h) = ImageProcessor.getImageDimensions(context, uri)
            _uiState.value = _uiState.value.copy(
                selectedImageUri = uri,
                fileName = name,
                originalSize = size,
                originalWidth = w,
                originalHeight = h,
                compressionResult = null,
                errorMessage = null
            )
        }
    }

    fun setQuality(quality: Int) {
        _uiState.value = _uiState.value.copy(quality = quality)
    }

    fun setScalePercent(scale: Int) {
        _uiState.value = _uiState.value.copy(scalePercent = scale)
    }

    fun setTargetFormat(format: ImageFormat) {
        _uiState.value = _uiState.value.copy(targetFormat = format)
    }

    fun compress() {
        val uri = _uiState.value.selectedImageUri ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)
            try {
                val result = ImageProcessor.compressImage(
                    context = context,
                    inputUri = uri,
                    quality = _uiState.value.quality,
                    scalePercent = _uiState.value.scalePercent,
                    targetFormat = _uiState.value.targetFormat
                )
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    compressionResult = result
                )

                // Save to history
                historyRepo.addRecord(
                    toolTitle = "Image Compressor",
                    inputName = _uiState.value.fileName,
                    outputName = "compressed_${_uiState.value.fileName}",
                    originalSize = result.originalSize,
                    finalSize = result.compressedSize,
                    outputUri = result.outputUri
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.localizedMessage ?: "Failed to compress image"
                )
            }
        }
    }

    fun saveToDownloads() {
        val result = _uiState.value.compressionResult ?: return
        val tempFile = FileUtils.copyUriToTempFile(context, result.outputUri, "out_", ".${_uiState.value.targetFormat.extension}")
        val targetName = "fileora_compressed_${System.currentTimeMillis()}.${_uiState.value.targetFormat.extension}"
        val savedUri = FileUtils.saveFileToDownloads(context, tempFile, _uiState.value.targetFormat.mimeType, targetName)
        if (savedUri != null) {
            Toast.makeText(context, "Saved to Downloads/Fileora", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareResult() {
        val result = _uiState.value.compressionResult ?: return
        FileUtils.shareFile(context, result.outputUri, _uiState.value.targetFormat.mimeType)
    }
}
