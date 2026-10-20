package com.fileora.app.domain.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.fileora.app.domain.model.CompressionResult
import com.fileora.app.domain.model.ImageFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

object ImageProcessor {

    suspend fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        Pair(options.outWidth, options.outHeight)
    }

    suspend fun compressImage(
        context: Context,
        inputUri: Uri,
        quality: Int = 80,
        scalePercent: Int = 100,
        targetFormat: ImageFormat = ImageFormat.JPEG
    ): CompressionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val originalSize = FileUtils.getFileSize(context, inputUri)

        // Decode bitmap
        val originalBitmap = decodeSampledBitmap(context, inputUri, 4096, 4096)
            ?: throw IllegalStateException("Failed to decode image")

        // Scale if needed
        val scaledBitmap = if (scalePercent < 100) {
            val scale = scalePercent / 100f
            val newW = (originalBitmap.width * scale).roundToInt().coerceAtLeast(1)
            val newH = (originalBitmap.height * scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
        } else {
            originalBitmap
        }

        val tempFile = File.createTempFile("comp_", ".${targetFormat.extension}", context.cacheDir)
        FileOutputStream(tempFile).use { out ->
            val compressFormat = getCompressFormat(targetFormat)
            scaledBitmap.compress(compressFormat, quality.coerceIn(1, 100), out)
        }

        val compressedSize = tempFile.length()
        val savedBytes = (originalSize - compressedSize).coerceAtLeast(0)
        val savedPercentage = if (originalSize > 0) {
            ((savedBytes.toFloat() / originalSize) * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }

        val outputUri = FileUtils.getFileProviderUri(context, tempFile)
        val timeTaken = System.currentTimeMillis() - startTime

        CompressionResult(
            originalSize = originalSize,
            compressedSize = compressedSize,
            savedBytes = savedBytes,
            savedPercentage = savedPercentage,
            outputUri = outputUri,
            outputWidth = scaledBitmap.width,
            outputHeight = scaledBitmap.height,
            outputFormat = targetFormat.displayName,
            timeTakenMs = timeTaken
        )
    }

    suspend fun convertImage(
        context: Context,
        inputUri: Uri,
        targetFormat: ImageFormat,
        quality: Int = 90
    ): File = withContext(Dispatchers.IO) {
        val bitmap = decodeSampledBitmap(context, inputUri, 4096, 4096)
            ?: throw IllegalStateException("Failed to decode image")

        val tempFile = File.createTempFile("conv_", ".${targetFormat.extension}", context.cacheDir)
        FileOutputStream(tempFile).use { out ->
            val compressFormat = getCompressFormat(targetFormat)
            bitmap.compress(compressFormat, quality.coerceIn(1, 100), out)
        }
        tempFile
    }

    suspend fun resizeImage(
        context: Context,
        inputUri: Uri,
        targetWidth: Int,
        targetHeight: Int,
        targetFormat: ImageFormat = ImageFormat.JPEG,
        quality: Int = 90
    ): File = withContext(Dispatchers.IO) {
        val originalBitmap = decodeSampledBitmap(context, inputUri, 4096, 4096)
            ?: throw IllegalStateException("Failed to decode image")

        val resizedBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            targetWidth.coerceAtLeast(1),
            targetHeight.coerceAtLeast(1),
            true
        )

        val tempFile = File.createTempFile("resize_", ".${targetFormat.extension}", context.cacheDir)
        FileOutputStream(tempFile).use { out ->
            val compressFormat = getCompressFormat(targetFormat)
            resizedBitmap.compress(compressFormat, quality.coerceIn(1, 100), out)
        }
        tempFile
    }

    fun getExifMetadata(context: Context, uri: Uri): Map<String, String> {
        val exifMap = mutableMapOf<String, String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { exifMap["Date Taken"] = it }
                exif.getAttribute(ExifInterface.TAG_MAKE)?.let { exifMap["Device Make"] = it }
                exif.getAttribute(ExifInterface.TAG_MODEL)?.let { exifMap["Device Model"] = it }
                exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { exifMap["Focal Length"] = "$it mm" }
                exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.let { exifMap["ISO"] = it }
                exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { exifMap["Exposure Time"] = "$it s" }
                exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { exifMap["Aperture"] = "f/$it" }
                exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)?.let { exifMap["Exif Width"] = "${it}px" }
                exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)?.let { exifMap["Exif Height"] = "${it}px" }
                exif.getAttribute(ExifInterface.TAG_ORIENTATION)?.let {
                    val desc = when (it.toIntOrNull()) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> "Rotate 90°"
                        ExifInterface.ORIENTATION_ROTATE_180 -> "Rotate 180°"
                        ExifInterface.ORIENTATION_ROTATE_270 -> "Rotate 270°"
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Flip Horizontal"
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Flip Vertical"
                        else -> "Normal"
                    }
                    exifMap["Orientation"] = desc
                }
            }
        } catch (_: Exception) {}
        return exifMap
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false

        var bitmap: Bitmap? = null
        context.contentResolver.openInputStream(uri)?.use {
            bitmap = BitmapFactory.decodeStream(it, null, options)
        }

        // Check EXIF rotation and rotate bitmap if necessary
        bitmap?.let { bmp ->
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val exif = ExifInterface(input)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
                    }
                    if (!matrix.isIdentity) {
                        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                    }
                }
            } catch (_: Exception) {}
        }

        return bitmap
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getCompressFormat(format: ImageFormat): Bitmap.CompressFormat {
        return when (format) {
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }
    }
}
