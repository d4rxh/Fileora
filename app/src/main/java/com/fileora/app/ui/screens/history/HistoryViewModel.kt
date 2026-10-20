package com.fileora.app.ui.screens.history

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fileora.app.FileoraApp
import com.fileora.app.domain.model.ProcessingHistoryItem
import com.fileora.app.domain.utils.FileUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val historyRepo = FileoraApp.instance.historyRepository

    val historyList: StateFlow<List<ProcessingHistoryItem>> = historyRepo.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearHistory() {
        viewModelScope.launch {
            historyRepo.clearHistory()
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            historyRepo.deleteItem(id)
        }
    }

    fun shareItem(item: ProcessingHistoryItem) {
        val uri = item.outputUri ?: return
        val ext = FileUtils.getFileExtension(item.outputName)
        val mime = when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            "zip" -> "application/zip"
            else -> "*/*"
        }
        FileUtils.shareFile(context, uri, mime)
    }
}
