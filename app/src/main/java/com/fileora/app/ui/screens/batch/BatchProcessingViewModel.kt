package com.fileora.app.ui.screens.batch

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fileora.app.FileoraApp
import com.fileora.app.domain.model.ImageFormat
import com.fileora.app.domain.model.ImageInfo
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.domain.utils.ImageProcessor
import com.fileora.app.domain.utils.ZipProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class BatchAction {
    COMPRESS,
    CONVERT,
    RESIZE
}

data class BatchUiState(
    val selectedImages: List<ImageInfo> = emptyList(),
    val action: BatchAction = BatchAction.COMPRESS,
    val quality: Int = 80,
    val targetFormat: ImageFormat = ImageFormat.JPEG,
    val scalePercent: Int = 75,
    val isProcessing: Boolean = false,
    val currentProgress: Int = 0,
    val totalProgress: Int = 0,
    val processedUris: List<Uri> = emptyList(),
    val zipArchiveUri: Uri? = null,
    val totalOriginalBytes: Long = 0,
    val totalProcessedBytes: Long = 0,
    val errorMessage: String? = null
)

class BatchProcessingViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val historyRepo = FileoraApp.instance.historyRepository

    private val _uiState = MutableStateFlow(BatchUiState())
    val uiState: StateFlow<BatchUiState> = _uiState.asStateFlow()

    fun onImagesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val list = uris.map { uri ->
            ImageInfo(
                uri = uri,
                name = FileUtils.getFileName(context, uri),
                size = FileUtils.getFileSize(context, uri)
            )
        }
        val current = _uiState.value.selectedImages
        val combined = current + list
        val totalSize = combined.sumOf { it.size }
        _uiState.value = _uiState.value.copy(
            selectedImages = combined,
            totalOriginalBytes = totalSize,
            processedUris = emptyList(),
            zipArchiveUri = null,
            errorMessage = null
        )
    }

    fun removeImage(index: Int) {
        val current = _uiState.value.selectedImages.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            val totalSize = current.sumOf { it.size }
            _uiState.value = _uiState.value.copy(
                selectedImages = current,
                totalOriginalBytes = totalSize
            )
        }
    }

    fun setAction(action: BatchAction) {
        _uiState.value = _uiState.value.copy(action = action)
    }

    fun setQuality(q: Int) {
        _uiState.value = _uiState.value.copy(quality = q)
    }

    fun setTargetFormat(format: ImageFormat) {
        _uiState.value = _uiState.value.copy(targetFormat = format)
    }

    fun setScalePercent(scale: Int) {
        _uiState.value = _uiState.value.copy(scalePercent = scale)
    }

    fun processBatch() {
        val images = _uiState.value.selectedImages
        if (images.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                currentProgress = 0,
                totalProgress = images.size,
                errorMessage = null
            )

            try {
                val processedFiles = mutableListOf<File>()
                val processedUris = mutableListOf<Uri>()

                for ((idx, img) in images.withIndex()) {
                    _uiState.value = _uiState.value.copy(currentProgress = idx + 1)
                    val outFile: File = when (_uiState.value.action) {
                        BatchAction.COMPRESS -> {
                            val res = ImageProcessor.compressImage(
                                context = context,
                                inputUri = img.uri,
                                quality = _uiState.value.quality,
                                scalePercent = _uiState.value.scalePercent,
                                targetFormat = _uiState.value.targetFormat
                            )
                            FileUtils.copyUriToTempFile(context, res.outputUri, "batch_comp_", ".${_uiState.value.targetFormat.extension}")
                        }
                        BatchAction.CONVERT -> {
                            ImageProcessor.convertImage(
                                context = context,
                                inputUri = img.uri,
                                targetFormat = _uiState.value.targetFormat,
                                quality = _uiState.value.quality
                            )
                        }
                        BatchAction.RESIZE -> {
                            val (w, h) = ImageProcessor.getImageDimensions(context, img.uri)
                            val factor = _uiState.value.scalePercent / 100f
                            val targetW = (w * factor).toInt().coerceAtLeast(1)
                            val targetH = (h * factor).toInt().coerceAtLeast(1)
                            ImageProcessor.resizeImage(
                                context = context,
                                inputUri = img.uri,
                                targetWidth = targetW,
                                targetHeight = targetH,
                                targetFormat = _uiState.value.targetFormat
                            )
                        }
                    }
                    processedFiles.add(outFile)
                    processedUris.add(FileUtils.getFileProviderUri(context, outFile))
                }

                val totalProcessedSize = processedFiles.sumOf { it.length() }

                // Create a ZIP package containing all processed files
                val zipFile = ZipProcessor.createZip(
                    context = context,
                    fileUris = processedUris,
                    zipFileName = "batch_processed.zip"
                )
                val zipUri = FileUtils.getFileProviderUri(context, zipFile)

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    processedUris = processedUris,
                    zipArchiveUri = zipUri,
                    totalProcessedBytes = totalProcessedSize
                )

                historyRepo.addRecord(
                    toolTitle = "Batch Image Processor (${images.size} items)",
                    inputName = "${images.size} photos",
                    outputName = "batch_processed.zip",
                    originalSize = _uiState.value.totalOriginalBytes,
                    finalSize = totalProcessedSize,
                    outputUri = zipUri
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.localizedMessage ?: "Failed during batch processing"
                )
            }
        }
    }

    fun saveZipToDownloads() {
        val uri = _uiState.value.zipArchiveUri ?: return
        val temp = FileUtils.copyUriToTempFile(context, uri, "batch_", ".zip")
        val saved = FileUtils.saveFileToDownloads(context, temp, "application/zip", "fileora_batch_${System.currentTimeMillis()}.zip")
        if (saved != null) {
            Toast.makeText(context, "ZIP saved to Downloads/Fileora", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareZip() {
        val uri = _uiState.value.zipArchiveUri ?: return
        FileUtils.shareFile(context, uri, "application/zip")
    }
}
