package com.example.chatdz.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    private const val TAG = "FileUtils"

    data class FileMeta(
        val name: String,
        val sizeFormatted: String,
        val localPath: String
    )

    fun copyUriToInternalStorage(context: Context, uri: Uri): FileMeta? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "file_${System.currentTimeMillis()}"
            var fileSize: Long = 0

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val docDir = File(context.filesDir, "documents")
            if (!docDir.exists()) docDir.mkdirs()

            val destinationFile = File(docDir, "${System.currentTimeMillis()}_$fileName")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            val sizeStr = formatFileSize(if (fileSize > 0) fileSize else destinationFile.length())
            FileMeta(name = fileName, sizeFormatted = sizeStr, localPath = destinationFile.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error copying document", e)
            null
        }
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format("%.1f KB", bytes.toDouble() / 1024)
            else -> "$bytes B"
        }
    }
}
