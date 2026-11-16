package com.vortex.downloader.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.vortex.downloader.data.model.DownloadMode
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

data class StorageStats(
    val availableBytes: Long,
    val totalBytes: Long,
    val appCacheBytes: Long
)

object StorageHelper {

    fun getStorageStats(context: Context): StorageStats {
        val path = Environment.getExternalStorageDirectory()
        val statFs = StatFs(path.path)
        val available = statFs.availableBlocksLong * statFs.blockSizeLong
        val total = statFs.blockCountLong * statFs.blockSizeLong
        val cacheBytes = calculateDirSize(context.cacheDir)
        return StorageStats(
            availableBytes = available,
            totalBytes = total,
            appCacheBytes = cacheBytes
        )
    }

    fun clearAppCache(context: Context): Long {
        val before = calculateDirSize(context.cacheDir)
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name != "vortex_active_downloads") {
                file.deleteRecursively()
            }
        }
        return before
    }

    fun getTempDownloadDir(context: Context): File {
        return File(context.cacheDir, "vortex_active_downloads").apply {
            if (!exists()) mkdirs()
        }
    }

    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun saveToMediaStore(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String,
        mode: DownloadMode
    ): Uri? {
        val resolver = context.contentResolver
        val relativeLocation = when (mode) {
            DownloadMode.VIDEO -> "${Environment.DIRECTORY_MOVIES}/VORTEX"
            DownloadMode.AUDIO -> "${Environment.DIRECTORY_MUSIC}/VORTEX"
        }
        val collection = when (mode) {
            DownloadMode.VIDEO -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            DownloadMode.AUDIO -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val itemUri = resolver.insert(collection, values) ?: return null
        return try {
            resolver.openOutputStream(itemUri)?.use { output ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(output, bufferSize = 32 * 1024)
                }
            } ?: return null

            val publishValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(itemUri, publishValues, null, null)
            itemUri
        } catch (e: Exception) {
            resolver.delete(itemUri, null, null)
            null
        }
    }

    private fun calculateDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { child ->
            size += if (child.isDirectory) calculateDirSize(child) else child.length()
        }
        return size
    }
}
