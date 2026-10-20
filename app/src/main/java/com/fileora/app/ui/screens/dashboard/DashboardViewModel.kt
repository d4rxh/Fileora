package com.fileora.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fileora.app.FileoraApp
import com.fileora.app.domain.model.ProcessingHistoryItem
import com.fileora.app.domain.model.ToolCategory
import com.fileora.app.domain.model.ToolItem
import com.fileora.app.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel : ViewModel() {

    private val historyRepo = FileoraApp.instance.historyRepository

    val recentHistory: StateFlow<List<ProcessingHistoryItem>> = historyRepo.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val toolsList = listOf(
        ToolItem(
            id = "compressor",
            title = "Image Compressor",
            description = "Reduce image file size with quality slider, preview, and size comparison",
            category = ToolCategory.IMAGE,
            route = Screen.ImageCompressor.route,
            iconName = "Compress",
            badge = "Popular"
        ),
        ToolItem(
            id = "converter",
            title = "Image Converter",
            description = "Convert images between JPG, PNG, and modern WebP formats seamlessly",
            category = ToolCategory.IMAGE,
            route = Screen.ImageConverter.route,
            iconName = "Transform"
        ),
        ToolItem(
            id = "resizer",
            title = "Image Resizer",
            description = "Resize dimensions with aspect ratio lock and social media presets",
            category = ToolCategory.IMAGE,
            route = Screen.ImageResizer.route,
            iconName = "AspectRatio"
        ),
        ToolItem(
            id = "batch",
            title = "Batch Processor",
            description = "Compress, convert, or resize multiple photos simultaneously",
            category = ToolCategory.IMAGE,
            route = Screen.BatchProcessing.route,
            iconName = "PhotoLibrary",
            badge = "Multi"
        ),
        ToolItem(
            id = "images_to_pdf",
            title = "Images to PDF",
            description = "Combine multiple pictures into an organized, high-quality PDF document",
            category = ToolCategory.PDF,
            route = Screen.ImagesToPdf.route,
            iconName = "PictureAsPdf",
            badge = "Essential"
        ),
        ToolItem(
            id = "pdf_merger",
            title = "PDF Merger & Organizer",
            description = "Merge multiple PDFs, reorder pages, and delete unwanted pages",
            category = ToolCategory.PDF,
            route = Screen.PdfMerger.route,
            iconName = "Layers"
        ),
        ToolItem(
            id = "pdf_inspector",
            title = "PDF Inspector",
            description = "Inspect page count, dimensions, version, and page previews",
            category = ToolCategory.PDF,
            route = Screen.PdfInspector.route,
            iconName = "FindInPage"
        ),
        ToolItem(
            id = "zip_creator",
            title = "ZIP Archive Creator",
            description = "Package multiple files into a compressed ZIP file and inspect archives",
            category = ToolCategory.ARCHIVE,
            route = Screen.ZipCreator.route,
            iconName = "FolderZip"
        ),
        ToolItem(
            id = "file_inspector",
            title = "File & EXIF Inspector",
            description = "Inspect exact byte sizes, MD5/SHA-256 hashes, MIME types, and camera EXIF",
            category = ToolCategory.INSPECTOR,
            route = Screen.FileInspector.route,
            iconName = "Info"
        )
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow(ToolCategory.ALL)
    val selectedCategory: StateFlow<ToolCategory> = _selectedCategory

    val filteredTools: StateFlow<List<ToolItem>> = combine(
        _searchQuery,
        _selectedCategory
    ) { query, category ->
        toolsList.filter { tool ->
            val matchesCategory = category == ToolCategory.ALL || tool.category == category
            val matchesQuery = query.isBlank() ||
                    tool.title.contains(query, ignoreCase = true) ||
                    tool.description.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), toolsList)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: ToolCategory) {
        _selectedCategory.value = category
    }
}
