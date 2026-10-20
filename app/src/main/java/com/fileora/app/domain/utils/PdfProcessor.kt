package com.fileora.app.domain.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.fileora.app.domain.model.PdfMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

object PdfProcessor {

    // Standard A4 dimensions in PostScript points (72 points per inch)
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    suspend fun imagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        orientation: String = "Auto", // "Auto", "Portrait", "Landscape"
        margin: Int = 18 // points margin
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        imageUris.forEachIndexed { index, uri ->
            val bitmap = decodeBitmapFromUri(context, uri) ?: return@forEachIndexed

            val isLandscape = when (orientation) {
                "Landscape" -> true
                "Portrait" -> false
                else -> bitmap.width > bitmap.height
            }

            val pageWidth = if (isLandscape) A4_HEIGHT else A4_WIDTH
            val pageHeight = if (isLandscape) A4_WIDTH else A4_HEIGHT

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Fill white background
            canvas.drawColor(Color.WHITE)

            val availableWidth = (pageWidth - (margin * 2)).toFloat().coerceAtLeast(1f)
            val availableHeight = (pageHeight - (margin * 2)).toFloat().coerceAtLeast(1f)

            // Scale to fit inside available bounds while preserving aspect ratio
            val scale = min(availableWidth / bitmap.width, availableHeight / bitmap.height)
            val destWidth = bitmap.width * scale
            val destHeight = bitmap.height * scale

            val left = margin + (availableWidth - destWidth) / 2f
            val top = margin + (availableHeight - destHeight) / 2f

            val destRect = Rect(left.toInt(), top.toInt(), (left + destWidth).toInt(), (top + destHeight).toInt())
            val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(bitmap, null, destRect, paint)

            document.finishPage(page)
            bitmap.recycle()
        }

        val outputFile = File.createTempFile("pdf_", ".pdf", context.cacheDir)
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        outputFile
    }

    suspend fun mergePdfs(
        context: Context,
        pdfUris: List<Uri>
    ): File = withContext(Dispatchers.IO) {
        val outputDoc = PdfDocument()
        var globalPageNumber = 1

        for (uri in pdfUris) {
            val tempFile = FileUtils.copyUriToTempFile(context, uri, "merge_src_", ".pdf")
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                // Use 2x density for crisp quality
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, globalPageNumber++).create()
                val docPage = outputDoc.startPage(pageInfo)
                val canvas = docPage.canvas
                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, null, Rect(0, 0, page.width, page.height), paint)

                outputDoc.finishPage(docPage)
                page.close()
                bitmap.recycle()
            }

            renderer.close()
            pfd.close()
            tempFile.delete()
        }

        val outputFile = File.createTempFile("merged_", ".pdf", context.cacheDir)
        FileOutputStream(outputFile).use { out ->
            outputDoc.writeTo(out)
        }
        outputDoc.close()
        outputFile
    }

    suspend fun organizePdfPages(
        context: Context,
        pdfUri: Uri,
        pageIndicesToKeepInOrder: List<Int>
    ): File = withContext(Dispatchers.IO) {
        val tempFile = FileUtils.copyUriToTempFile(context, pdfUri, "org_src_", ".pdf")
        val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val outputDoc = PdfDocument()

        var pageNum = 1
        for (pageIdx in pageIndicesToKeepInOrder) {
            if (pageIdx in 0 until renderer.pageCount) {
                val page = renderer.openPage(pageIdx)
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, pageNum++).create()
                val docPage = outputDoc.startPage(pageInfo)
                val canvas = docPage.canvas
                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, null, Rect(0, 0, page.width, page.height), paint)

                outputDoc.finishPage(docPage)
                page.close()
                bitmap.recycle()
            }
        }

        renderer.close()
        pfd.close()
        tempFile.delete()

        val outputFile = File.createTempFile("organized_", ".pdf", context.cacheDir)
        FileOutputStream(outputFile).use { out ->
            outputDoc.writeTo(out)
        }
        outputDoc.close()
        outputFile
    }

    suspend fun getPdfMetadata(context: Context, uri: Uri): PdfMetadata = withContext(Dispatchers.IO) {
        val fileName = FileUtils.getFileName(context, uri)
        val fileSize = FileUtils.getFileSize(context, uri)
        val tempFile = FileUtils.copyUriToTempFile(context, uri, "meta_", ".pdf")
        var pageCount = 0
        var dimensions = "Standard"

        try {
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            pageCount = renderer.pageCount
            if (pageCount > 0) {
                val firstPage = renderer.openPage(0)
                dimensions = "${firstPage.width} x ${firstPage.height} pt"
                firstPage.close()
            }
            renderer.close()
            pfd.close()
        } catch (_: Exception) {} finally {
            tempFile.delete()
        }

        PdfMetadata(
            name = fileName,
            uri = uri,
            pageCount = pageCount,
            fileSize = fileSize,
            pageDimensions = dimensions
        )
    }

    suspend fun renderPagePreview(context: Context, uri: Uri, pageIndex: Int, maxDim: Int = 500): Bitmap? = withContext(Dispatchers.IO) {
        val tempFile = FileUtils.copyUriToTempFile(context, uri, "thumb_", ".pdf")
        var bitmap: Bitmap? = null
        try {
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (pageIndex in 0 until renderer.pageCount) {
                val page = renderer.openPage(pageIndex)
                val scale = min(maxDim.toFloat() / page.width, maxDim.toFloat() / page.height).coerceAtMost(2f)
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
            }
            renderer.close()
            pfd.close()
        } catch (_: Exception) {} finally {
            tempFile.delete()
        }
        bitmap
    }

    private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (_: Exception) {
            null
        }
    }
}
