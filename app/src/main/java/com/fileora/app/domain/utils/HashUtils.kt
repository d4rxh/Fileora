package com.fileora.app.domain.utils

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.security.MessageDigest

object HashUtils {

    fun calculateHashes(context: Context, uri: Uri): Pair<String, String> {
        return try {
            val md5Digest = MessageDigest.getInstance("MD5")
            val sha256Digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    md5Digest.update(buffer, 0, read)
                    sha256Digest.update(buffer, 0, read)
                }
            }

            val md5String = md5Digest.digest().joinToString("") { "%02x".format(it) }
            val sha256String = sha256Digest.digest().joinToString("") { "%02x".format(it) }
            Pair(md5String, sha256String)
        } catch (_: Exception) {
            Pair("Unavailable", "Unavailable")
        }
    }
}
