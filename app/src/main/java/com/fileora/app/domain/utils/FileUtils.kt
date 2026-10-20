package com.fileora.app.domain.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object FileUtils {

    fun getFileName(context: Context, uri: Uri): String {
        var name = "unknown"
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex) ?: name
                    }
                }
            }
        } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            uri.path?.let {
                name = File(it).name
            }
        }
        return name
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        var size: Long = 0
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        size = it.getLong(sizeIndex)
                    }
                }
            }
        } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
            uri.path?.let {
                size = File(it).length()
            }
        }
        if (size <= 0) {
            try {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                    size = it.length
                }
            } catch (_: Exception) {}
        }
        return size
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val df = DecimalFormat("#,##0.#")
        return "${df.format(bytes / 1024.0.pow(digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    fun getFileExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot + 1).lowercase()
        } else {
            ""
        }
    }

    fun copyUriToTempFile(context: Context, uri: Uri, prefix: String = "input_", extension: String? = null): File {
        val ext = extension ?: getFileExtension(getFileName(context, uri)).let { if (it.isNotEmpty()) ".$it" else ".tmp" }
        val tempFile = File.createTempFile(prefix, ext, context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun shareFile(context: Context, uri: Uri, mimeType: String, title: String = "Share File") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun shareMultipleFiles(context: Context, uris: ArrayList<Uri>, mimeType: String = "*/*", title: String = "Share Files") {
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun saveFileToDownloads(context: Context, sourceFile: File, targetMimeType: String, targetFileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, targetMimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Fileora")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { output ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(output)
                    }
                }
            }
            uri
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileoraDir = File(downloadsDir, "Fileora").apply { mkdirs() }
            val destFile = File(fileoraDir, targetFileName)
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile)
        }
    }
}
