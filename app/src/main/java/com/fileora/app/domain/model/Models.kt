package com.fileora.app.domain.model

import android.net.Uri

enum class ToolCategory(val title: String) {
    ALL("All Tools"),
    IMAGE("Image Tools"),
    PDF("PDF Suite"),
    ARCHIVE("Archives"),
    INSPECTOR("Inspectors")
}

enum class ImageFormat(val extension: String, val displayName: String, val mimeType: String) {
    JPEG("jpg", "JPEG (.jpg)", "image/jpeg"),
    PNG("png", "PNG (.png)", "image/png"),
    WEBP("webp", "WebP (.webp)", "image/webp")
}

data class ToolItem(
    val id: String,
    val title: String,
    val description: String,
    val category: ToolCategory,
    val route: String,
    val iconName: String,
    val badge: String? = null
)

data class ImageInfo(
    val uri: Uri,
    val name: String,
    val size: Long,
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = ""
)

data class CompressionResult(
    val originalSize: Long,
    val compressedSize: Long,
    val savedBytes: Long,
    val savedPercentage: Float,
    val outputUri: Uri,
    val outputWidth: Int,
    val outputHeight: Int,
    val outputFormat: String,
    val timeTakenMs: Long
)

data class FileDetails(
    val name: String,
    val uri: Uri,
    val sizeBytes: Long,
    val extension: String,
    val mimeType: String,
    val md5Hash: String = "",
    val sha256Hash: String = "",
    val isImage: Boolean = false,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val exifData: Map<String, String> = emptyMap(),
    val isPdf: Boolean = false,
    val pdfPageCount: Int = 0
)

data class PdfMetadata(
    val name: String,
    val uri: Uri,
    val pageCount: Int,
    val fileSize: Long,
    val title: String? = null,
    val author: String? = null,
    val creator: String? = null,
    val producer: String? = null,
    val creationDate: String? = null,
    val modificationDate: String? = null,
    val pageDimensions: String? = null
)

data class PdfPageItem(
    val pageNumber: Int,
    val isSelected: Boolean = true
)

data class ProcessingHistoryItem(
    val id: String,
    val toolTitle: String,
    val inputName: String,
    val outputName: String,
    val originalSize: Long,
    val finalSize: Long,
    val outputUri: Uri?,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = true
)
