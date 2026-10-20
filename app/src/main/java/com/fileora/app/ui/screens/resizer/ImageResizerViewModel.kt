package com.fileora.app.ui.screens.resizer

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
import kotlin.math.roundToInt

data class ResizerUiState(
    val selectedImageUri: Uri? = null,
    val fileName: String = "",
    val originalSize: Long = 0,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val targetWidth: String = "",
    val targetHeight: String = "",
    val maintainAspectRatio: Boolean = true,
    val targetFormat: ImageFormat = ImageFormat.JPEG,
    val isProcessing: Boolean = false,
    val outputUri: Uri? = null,
    val outputSize: Long = 0,
    val errorMessage: String? = null
)

class ImageResizerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val historyRepo = FileoraApp.instance.historyRepository

    private val _uiState = MutableStateFlow(ResizerUiState())
    val uiState: StateFlow<ResizerUiState> = _uiState.asStateFlow()

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
                targetWidth = w.toString(),
                targetHeight = h.toString(),
                outputUri = null,
                errorMessage = null
            )
        }
    }

    fun onWidthChanged(newWidthStr: String) {
        val w = newWidthStr.filter { it.isDigit() }
        val current = _uiState.value
        if (current.maintainAspectRatio && current.originalWidth > 0 && w.isNotEmpty()) {
            val wInt = w.toIntOrNull() ?: 0
            val ratio = current.originalHeight.toFloat() / current.originalWidth
            val h = (wInt * ratio).roundToInt()
            _uiState.value = current.copy(targetWidth = w, targetHeight = h.toString())
        } else {
            _uiState.value = current.copy(targetWidth = w)
        }
    }

    fun onHeightChanged(newHeightStr: String) {
        val h = newHeightStr.filter { it.isDigit() }
        val current = _uiState.value
        if (current.maintainAspectRatio && current.originalHeight > 0 && h.isNotEmpty()) {
            val hInt = h.toIntOrNull() ?: 0
            val ratio = current.originalWidth.toFloat() / current.originalHeight
            val w = (hInt * ratio).roundToInt()
            _uiState.value = current.copy(targetWidth = w.toString(), targetHeight = h)
        } else {
            _uiState.value = current.copy(targetHeight = h)
        }
    }

    fun toggleMaintainAspectRatio() {
        _uiState.value = _uiState.value.copy(maintainAspectRatio = !_uiState.value.maintainAspectRatio)
    }

    fun applyPercentageScale(percentage: Int) {
        val current = _uiState.value
        if (current.originalWidth <= 0 || current.originalHeight <= 0) return
        val factor = percentage / 100f
        val newW = (current.originalWidth * factor).roundToInt()
        val newH = (current.originalHeight * factor).roundToInt()
        _uiState.value = current.copy(targetWidth = newW.toString(), targetHeight = newH.toString())
    }

    fun applyPresetDimensions(w: Int, h: Int) {
        _uiState.value = _uiState.value.copy(
            targetWidth = w.toString(),
            targetHeight = h.toString(),
            maintainAspectRatio = false
        )
    }

    fun resize() {
        val uri = _uiState.value.selectedImageUri ?: return
        val w = _uiState.value.targetWidth.toIntOrNull() ?: return
        val h = _uiState.value.targetHeight.toIntOrNull() ?: return

        if (w <= 0 || h <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter valid dimensions greater than 0")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)
            try {
                val outputFile = ImageProcessor.resizeImage(
                    context = context,
                    inputUri = uri,
                    targetWidth = w,
                    targetHeight = h,
                    targetFormat = _uiState.value.targetFormat
                )
                val outUri = FileUtils.getFileProviderUri(context, outputFile)
                val outSize = outputFile.length()

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    outputUri = outUri,
                    outputSize = outSize
                )

                historyRepo.addRecord(
                    toolTitle = "Image Resizer (${w}x${h})",
                    inputName = _uiState.value.fileName,
                    outputName = "resized_${w}x${h}_${_uiState.value.fileName}",
                    originalSize = _uiState.value.originalSize,
                    finalSize = outSize,
                    outputUri = outUri
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.localizedMessage ?: "Failed to resize image"
                )
            }
        }
    }

    fun saveToDownloads() {
        val uri = _uiState.value.outputUri ?: return
        val ext = _uiState.value.targetFormat.extension
        val temp = FileUtils.copyUriToTempFile(context, uri, "res_", ".$ext")
        val saved = FileUtils.saveFileToDownloads(context, temp, _uiState.value.targetFormat.mimeType, "resized_${System.currentTimeMillis()}.$ext")
        if (saved != null) {
            Toast.makeText(context, "Saved to Downloads/Fileora", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareResult() {
        val uri = _uiState.value.outputUri ?: return
        FileUtils.shareFile(context, uri, _uiState.value.targetFormat.mimeType)
    }
}
