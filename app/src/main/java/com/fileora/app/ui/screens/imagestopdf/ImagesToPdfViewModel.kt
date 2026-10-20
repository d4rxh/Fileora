package com.fileora.app.ui.screens.imagestopdf

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fileora.app.FileoraApp
import com.fileora.app.domain.model.ImageInfo
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.domain.utils.PdfProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class ImagesToPdfUiState(
    val selectedImages: List<ImageInfo> = emptyList(),
    val orientation: String = "Auto",
    val margin: Int = 18,
    val isProcessing: Boolean = false,
    val outputPdfUri: Uri? = null,
    val outputPdfSize: Long = 0,
    val totalOriginalSize: Long = 0,
    val errorMessage: String? = null
)

class ImagesToPdfViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val historyRepo = FileoraApp.instance.historyRepository

    private val _uiState = MutableStateFlow(ImagesToPdfUiState())
    val uiState: StateFlow<ImagesToPdfUiState> = _uiState.asStateFlow()

    fun onImagesSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val list = uris.map { uri ->
            ImageInfo(
                uri = uri,
                name = FileUtils.getFileName(context, uri),
                size = FileUtils.getFileSize(context, uri)
            )
        }
        val current = _uiState.value.selectedImages + list
        _uiState.value = _uiState.value.copy(
            selectedImages = current,
            totalOriginalSize = current.sumOf { it.size },
            outputPdfUri = null,
            errorMessage = null
        )
    }

    fun moveImageUp(index: Int) {
        if (index <= 0) return
        val list = _uiState.value.selectedImages.toMutableList()
        val temp = list[index]
        list[index] = list[index - 1]
        list[index - 1] = temp
        _uiState.value = _uiState.value.copy(selectedImages = list)
    }

    fun moveImageDown(index: Int) {
        val list = _uiState.value.selectedImages.toMutableList()
        if (index >= list.size - 1) return
        val temp = list[index]
        list[index] = list[index + 1]
        list[index + 1] = temp
        _uiState.value = _uiState.value.copy(selectedImages = list)
    }

    fun removeImage(index: Int) {
        val list = _uiState.value.selectedImages.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _uiState.value = _uiState.value.copy(
                selectedImages = list,
                totalOriginalSize = list.sumOf { it.size }
            )
        }
    }

    fun setOrientation(orientation: String) {
        _uiState.value = _uiState.value.copy(orientation = orientation)
    }

    fun setMargin(margin: Int) {
        _uiState.value = _uiState.value.copy(margin = margin)
    }

    fun generatePdf() {
        val images = _uiState.value.selectedImages
        if (images.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)
            try {
                val outputFile = PdfProcessor.imagesToPdf(
                    context = context,
                    imageUris = images.map { it.uri },
                    orientation = _uiState.value.orientation,
                    margin = _uiState.value.margin
                )
                val outUri = FileUtils.getFileProviderUri(context, outputFile)
                val outSize = outputFile.length()

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    outputPdfUri = outUri,
                    outputPdfSize = outSize
                )

                val docName = "doc_${images.size}_pages.pdf"
                historyRepo.addRecord(
                    toolTitle = "Images to PDF (${images.size} pages)",
                    inputName = "${images.size} images",
                    outputName = docName,
                    originalSize = _uiState.value.totalOriginalSize,
                    finalSize = outSize,
                    outputUri = outUri
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.localizedMessage ?: "Failed to generate PDF"
                )
            }
        }
    }

    fun saveToDownloads() {
        val uri = _uiState.value.outputPdfUri ?: return
        val temp = FileUtils.copyUriToTempFile(context, uri, "pdf_", ".pdf")
        val saved = FileUtils.saveFileToDownloads(context, temp, "application/pdf", "fileora_document_${System.currentTimeMillis()}.pdf")
        if (saved != null) {
            Toast.makeText(context, "PDF saved to Downloads/Fileora", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdf() {
        val uri = _uiState.value.outputPdfUri ?: return
        FileUtils.shareFile(context, uri, "application/pdf")
    }
}
