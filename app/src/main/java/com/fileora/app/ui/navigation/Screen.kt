package com.fileora.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "Fileora")
    data object ImageCompressor : Screen("image_compressor", "Image Compressor")
    data object ImageConverter : Screen("image_converter", "Image Converter")
    data object ImageResizer : Screen("image_resizer", "Image Resizer")
    data object BatchProcessing : Screen("batch_processing", "Batch Image Processing")
    data object ImagesToPdf : Screen("images_to_pdf", "Images to PDF")
    data object PdfMerger : Screen("pdf_merger", "PDF Merger & Organizer")
    data object PdfInspector : Screen("pdf_inspector", "PDF Inspector")
    data object ZipCreator : Screen("zip_creator", "ZIP Archive Creator")
    data object FileInspector : Screen("file_inspector", "File & EXIF Inspector")
    data object History : Screen("history", "Processing History")
    data object Settings : Screen("settings", "Settings & About")
}
