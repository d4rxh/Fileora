package com.fileora.app.ui.screens.pdfinspector

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fileora.app.domain.model.PdfMetadata
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.domain.utils.HashUtils
import com.fileora.app.domain.utils.PdfProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PdfInspectorUiState(
    val selectedPdfUri: Uri? = null,
    val metadata: PdfMetadata? = null,
    val md5Hash: String = "",
    val sha256Hash: String = "",
    val currentPageIndex: Int = 0,
    val currentPageBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class PdfInspectorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(PdfInspectorUiState())
    val uiState: StateFlow<PdfInspectorUiState> = _uiState.asStateFlow()

    fun onPdfSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedPdfUri = uri, errorMessage = null)
            try {
                val meta = PdfProcessor.getPdfMetadata(context, uri)
                val (md5, sha) = HashUtils.calculateHashes(context, uri)
                val firstPage = if (meta.pageCount > 0) {
                    PdfProcessor.renderPagePreview(context, uri, 0, maxDim = 800)
                } else null

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    metadata = meta,
                    md5Hash = md5,
                    sha256Hash = sha,
                    currentPageIndex = 0,
                    currentPageBitmap = firstPage
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Failed to inspect PDF"
                )
            }
        }
    }

    fun loadPage(index: Int) {
        val uri = _uiState.value.selectedPdfUri ?: return
        val count = _uiState.value.metadata?.pageCount ?: 0
        if (index !in 0 until count) return

        viewModelScope.launch {
            val bmp = PdfProcessor.renderPagePreview(context, uri, index, maxDim = 800)
            _uiState.value = _uiState.value.copy(
                currentPageIndex = index,
                currentPageBitmap = bmp
            )
        }
    }

    fun sharePdf() {
        val uri = _uiState.value.selectedPdfUri ?: return
        FileUtils.shareFile(context, uri, "application/pdf")
    }
}
