package com.fileora.app.domain.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ZipEntryInfo(
    val name: String,
    val uncompressedSize: Long,
    val compressedSize: Long,
    val isDirectory: Boolean
)

object ZipProcessor {

    suspend fun createZip(
        context: Context,
        fileUris: List<Uri>,
        zipFileName: String = "archive.zip",
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val outputFile = File.createTempFile("zip_", ".zip", context.cacheDir)
        val usedNames = mutableSetOf<String>()

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            val total = fileUris.size
            fileUris.forEachIndexed { index, uri ->
                onProgress(index + 1, total)
                val originalName = FileUtils.getFileName(context, uri)
                var entryName = originalName

                // Handle duplicate names inside zip
                var counter = 1
                while (usedNames.contains(entryName)) {
                    val dot = originalName.lastIndexOf('.')
                    entryName = if (dot > 0) {
                        "${originalName.substring(0, dot)}_$counter.${originalName.substring(dot + 1)}"
                    } else {
                        "${originalName}_$counter"
                    }
                    counter++
                }
                usedNames.add(entryName)

                val zipEntry = ZipEntry(entryName)
                zos.putNextEntry(zipEntry)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.copyTo(zos)
                }
                zos.closeEntry()
            }
        }

        outputFile
    }

    suspend fun inspectZip(context: Context, zipUri: Uri): List<ZipEntryInfo> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<ZipEntryInfo>()
        try {
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        entries.add(
                            ZipEntryInfo(
                                name = entry.name,
                                uncompressedSize = entry.size,
                                compressedSize = entry.compressedSize,
                                isDirectory = entry.isDirectory
                            )
                        )
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }
        } catch (_: Exception) {}
        entries
    }
}
