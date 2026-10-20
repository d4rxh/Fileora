package com.fileora.app.ui.screens.fileinspector

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fileora.app.domain.model.FileDetails
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.domain.utils.HashUtils
import com.fileora.app.domain.utils.ImageProcessor
import com.fileora.app.domain.utils.PdfProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FileInspectorUiState(
    val selectedFileUri: Uri? = null,
    val fileDetails: FileDetails? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class FileInspectorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(FileInspectorUiState())
    val uiState: StateFlow<FileInspectorUiState> = _uiState.asStateFlow()

    fun onFileSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedFileUri = uri, errorMessage = null)
            try {
                val name = FileUtils.getFileName(context, uri)
                val size = FileUtils.getFileSize(context, uri)
                val ext = FileUtils.getFileExtension(name)
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"

                val (md5, sha) = HashUtils.calculateHashes(context, uri)

                val isImage = mime.startsWith("image/") || listOf("jpg", "jpeg", "png", "webp", "gif", "bmp").contains(ext)
                var imgW = 0
                var imgH = 0
                var exif = emptyMap<String, String>()

                if (isImage) {
                    val (w, h) = ImageProcessor.getImageDimensions(context, uri)
                    imgW = w
                    imgH = h
                    exif = ImageProcessor.getExifMetadata(context, uri)
                }

                val isPdf = mime == "application/pdf" || ext == "pdf"
                var pdfPages = 0
                if (isPdf) {
                    val meta = PdfProcessor.getPdfMetadata(context, uri)
                    pdfPages = meta.pageCount
                }

                val details = FileDetails(
                    name = name,
                    uri = uri,
                    sizeBytes = size,
                    extension = ext,
                    mimeType = mime,
                    md5Hash = md5,
                    sha256Hash = sha,
                    isImage = isImage,
                    imageWidth = imgW,
                    imageHeight = imgH,
                    exifData = exif,
                    isPdf = isPdf,
                    pdfPageCount = pdfPages
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    fileDetails = details
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to inspect file"
                )
            }
        }
    }

    fun shareFile() {
        val uri = _uiState.value.selectedFileUri ?: return
        val mime = _uiState.value.fileDetails?.mimeType ?: "*/*"
        FileUtils.shareFile(context, uri, mime)
    }
}
