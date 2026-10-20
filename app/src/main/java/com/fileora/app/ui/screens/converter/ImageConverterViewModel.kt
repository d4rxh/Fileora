package com.fileora.app.ui.screens.converter

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fileora.app.FileoraApp
import com.fileora.app.domain.model.ImageFormat
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.domain.utils.ImageProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ConverterUiState(
    val selectedImageUri: Uri? = null,
    val fileName: String = "",
    val originalSize: Long = 0,
    val targetFormat: ImageFormat = ImageFormat.PNG,
    val quality: Int = 90,
    val isProcessing: Boolean = false,
    val outputUri: Uri? = null,
    val outputSize: Long = 0,
    val errorMessage: String? = null
)

class ImageConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val historyRepo = FileoraApp.instance.historyRepository

    private val _uiState = MutableStateFlow(ConverterUiState())
    val uiState: StateFlow<ConverterUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri?) {
        if (uri == null) return
        val name = FileUtils.getFileName(context, uri)
        val size = FileUtils.getFileSize(context, uri)
        _uiState.value = _uiState.value.copy(
            selectedImageUri = uri,
            fileName = name,
            originalSize = size,
            outputUri = null,
            errorMessage = null
        )
    }

    fun setTargetFormat(format: ImageFormat) {
        _uiState.value = _uiState.value.copy(targetFormat = format)
    }

    fun setQuality(quality: Int) {
        _uiState.value = _uiState.value.copy(quality = quality)
    }

    fun convert() {
        val uri = _uiState.value.selectedImageUri ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)
            try {
                val outputFile = ImageProcessor.convertImage(
                    context = context,
                    inputUri = uri,
                    targetFormat = _uiState.value.targetFormat,
                    quality = _uiState.value.quality
                )
                val outUri = FileUtils.getFileProviderUri(context, outputFile)
                val outSize = outputFile.length()

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    outputUri = outUri,
                    outputSize = outSize
                )

                val outName = "${FileUtils.getFileExtension(_uiState.value.fileName)}_${System.currentTimeMillis()}.${_uiState.value.targetFormat.extension}"
                historyRepo.addRecord(
                    toolTitle = "Image Converter (${_uiState.value.targetFormat.name})",
                    inputName = _uiState.value.fileName,
                    outputName = outName,
                    originalSize = _uiState.value.originalSize,
                    finalSize = outSize,
                    outputUri = outUri
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.localizedMessage ?: "Failed to convert image"
                )
            }
        }
    }

    fun saveToDownloads() {
        val uri = _uiState.value.outputUri ?: return
        val tempFile = FileUtils.copyUriToTempFile(context, uri, "conv_out_", ".${_uiState.value.targetFormat.extension}")
        val targetName = "fileora_${System.currentTimeMillis()}.${_uiState.value.targetFormat.extension}"
        val saved = FileUtils.saveFileToDownloads(context, tempFile, _uiState.value.targetFormat.mimeType, targetName)
        if (saved != null) {
            Toast.makeText(context, "Saved to Downloads/Fileora", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Failed to save file", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareResult() {
        val uri = _uiState.value.outputUri ?: return
        FileUtils.shareFile(context, uri, _uiState.value.targetFormat.mimeType)
    }
}
