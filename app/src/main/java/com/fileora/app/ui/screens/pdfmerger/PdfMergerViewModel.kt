package com.fileora.app.ui.screens.pdfmerger

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fileora.app.FileoraApp
import com.fileora.app.domain.utils.FileUtils
import com.fileora.app.domain.utils.PdfProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PdfFileItem(
    val uri: Uri,
    val name: String,
    val size: Long,
    val pageCount: Int = 0
)

data class PageThumbnailItem(
    val pageIndex: Int,
    val thumbnail: Bitmap?,
    val isIncluded: Boolean = true
)

enum class PdfToolMode {
    MERGE,
    ORGANIZE
}

data class PdfMergerUiState(
    val mode: PdfToolMode = PdfToolMode.MERGE,
    val pdfList: List<PdfFileItem> = emptyList(),
    // Organize mode state
    val singlePdfUri: Uri? = null,
    val singlePdfName: String = "",
    val pageThumbnails: List<PageThumbnailItem> = emptyList(),
    val isProcessing: Boolean = false,
    val outputUri: Uri? = null,
    val outputSize: Long = 0,
    val totalOriginalSize: Long = 0,
    val errorMessage: String? = null
)

class PdfMergerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val historyRepo = FileoraApp.instance.historyRepository

    private val _uiState = MutableStateFlow(PdfMergerUiState())
    val uiState: StateFlow<PdfMergerUiState> = _uiState.asStateFlow()

    fun setMode(mode: PdfToolMode) {
        _uiState.value = _uiState.value.copy(mode = mode, outputUri = null, errorMessage = null)
    }

    fun onPdfsSelected(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val items = uris.map { uri ->
                val meta = PdfProcessor.getPdfMetadata(context, uri)
                PdfFileItem(
                    uri = uri,
                    name = meta.name,
                    size = meta.fileSize,
                    pageCount = meta.pageCount
                )
            }
            val current = _uiState.value.pdfList + items
            _uiState.value = _uiState.value.copy(
                pdfList = current,
                totalOriginalSize = current.sumOf { it.size },
                outputUri = null
            )
        }
    }

    fun removePdf(index: Int) {
        val list = _uiState.value.pdfList.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _uiState.value = _uiState.value.copy(
                pdfList = list,
                totalOriginalSize = list.sumOf { it.size }
            )
        }
    }

    fun movePdfUp(index: Int) {
        if (index <= 0) return
        val list = _uiState.value.pdfList.toMutableList()
        val temp = list[index]
        list[index] = list[index - 1]
        list[index - 1] = temp
        _uiState.value = _uiState.value.copy(pdfList = list)
    }

    fun movePdfDown(index: Int) {
        val list = _uiState.value.pdfList.toMutableList()
        if (index >= list.size - 1) return
        val temp = list[index]
        list[index] = list[index + 1]
        list[index + 1] = temp
        _uiState.value = _uiState.value.copy(pdfList = list)
    }

    // Organize mode: Load single PDF and generate page thumbnails
    fun onSinglePdfSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val name = FileUtils.getFileName(context, uri)
            val meta = PdfProcessor.getPdfMetadata(context, uri)
            val thumbs = mutableListOf<PageThumbnailItem>()

            for (i in 0 until meta.pageCount) {
                val bmp = PdfProcessor.renderPagePreview(context, uri, i, maxDim = 300)
                thumbs.add(PageThumbnailItem(pageIndex = i, thumbnail = bmp, isIncluded = true))
            }

            _uiState.value = _uiState.value.copy(
                singlePdfUri = uri,
                singlePdfName = name,
                totalOriginalSize = meta.fileSize,
                pageThumbnails = thumbs,
                isProcessing = false,
                outputUri = null
            )
        }
    }

    fun togglePageInclusion(pageIndex: Int) {
        val list = _uiState.value.pageThumbnails.map {
            if (it.pageIndex == pageIndex) it.copy(isIncluded = !it.isIncluded) else it
        }
        _uiState.value = _uiState.value.copy(pageThumbnails = list)
    }

    fun mergePdfs() {
        val list = _uiState.value.pdfList
        if (list.size < 2) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select at least 2 PDF files to merge")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)
            try {
                val outFile = PdfProcessor.mergePdfs(context, list.map { it.uri })
                val outUri = FileUtils.getFileProviderUri(context, outFile)
                val outSize = outFile.length()

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    outputUri = outUri,
                    outputSize = outSize
                )

                historyRepo.addRecord(
                    toolTitle = "PDF Merger (${list.size} files)",
                    inputName = "${list.size} PDF files",
                    outputName = "merged_${System.currentTimeMillis()}.pdf",
                    originalSize = _uiState.value.totalOriginalSize,
                    finalSize = outSize,
                    outputUri = outUri
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.localizedMessage ?: "Failed to merge PDFs"
                )
            }
        }
    }

    fun exportOrganizedPdf() {
        val uri = _uiState.value.singlePdfUri ?: return
        val selectedPages = _uiState.value.pageThumbnails.filter { it.isIncluded }.map { it.pageIndex }

        if (selectedPages.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please keep at least one page")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, errorMessage = null)
            try {
                val outFile = PdfProcessor.organizePdfPages(context, uri, selectedPages)
                val outUri = FileUtils.getFileProviderUri(context, outFile)
                val outSize = outFile.length()

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    outputUri = outUri,
                    outputSize = outSize
                )

                historyRepo.addRecord(
                    toolTitle = "PDF Page Organizer (${selectedPages.size} pages)",
                    inputName = _uiState.value.singlePdfName,
                    outputName = "organized_${System.currentTimeMillis()}.pdf",
                    originalSize = _uiState.value.totalOriginalSize,
                    finalSize = outSize,
                    outputUri = outUri
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.localizedMessage ?: "Failed to organize PDF pages"
                )
            }
        }
    }

    fun saveToDownloads() {
        val uri = _uiState.value.outputUri ?: return
        val temp = FileUtils.copyUriToTempFile(context, uri, "pdf_out_", ".pdf")
        val saved = FileUtils.saveFileToDownloads(context, temp, "application/pdf", "fileora_pdf_${System.currentTimeMillis()}.pdf")
        if (saved != null) {
            Toast.makeText(context, "Saved to Downloads/Fileora", Toast.LENGTH_SHORT).show()
        }
    }

    fun sharePdf() {
        val uri = _uiState.value.outputUri ?: return
        FileUtils.shareFile(context, uri, "application/pdf")
    }
}
