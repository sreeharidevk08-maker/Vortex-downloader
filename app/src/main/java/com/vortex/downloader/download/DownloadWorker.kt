package com.vortex.downloader.download

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vortex.downloader.VortexApp
import com.vortex.downloader.data.model.DownloadMode
import com.vortex.downloader.data.model.DownloadStatus
import com.vortex.downloader.media.MediaProcessor
import com.vortex.downloader.util.FormatUtils
import com.vortex.downloader.util.StorageHelper
import kotlinx.coroutines.flow.first
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class DownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
    }

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        if (downloadId <= 0L) return Result.failure()

        val app = applicationContext as VortexApp
        val dao = app.database.downloadDao()
        val prefs = app.preferencesRepository.preferencesFlow.first()
        val entity = dao.getById(downloadId) ?: return Result.failure()

        val tempDir = StorageHelper.getTempDownloadDir(applicationContext)
        val rawFile = entity.tempFilePath?.let { File(it) }
            ?: File(tempDir, "dl_${entity.id}_primary.tmp")

        dao.update(entity.copy(status = DownloadStatus.DOWNLOADING, tempFilePath = rawFile.absolutePath, errorMessage = null))

        if (prefs.notificationsEnabled) {
            setForeground(
                DownloadNotificationHelper.buildForegroundInfo(
                    applicationContext,
                    entity.id,
                    entity.title,
                    0,
                    "Starting download..."
                )
            )
        }

        return try {
            // 1. Stream primary file with Range resume support
            val downloadedPrimary = streamToFile(
                url = entity.downloadUrl,
                targetFile = rawFile,
                supportsRange = entity.supportsRange
            ) { downloaded, total, speed ->
                dao.updateProgress(entity.id, downloaded, total, speed, DownloadStatus.DOWNLOADING)
                if (prefs.notificationsEnabled && total > 0) {
                    val pct = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                    setForegroundAsync(
                        DownloadNotificationHelper.buildForegroundInfo(
                            applicationContext,
                            entity.id,
                            entity.title,
                            pct,
                            "$pct% • ${FormatUtils.formatSpeed(speed)}"
                        )
                    )
                }
                val latest = dao.getById(entity.id)
                latest?.status == DownloadStatus.PAUSED || latest?.status == DownloadStatus.CANCELLED || isStopped
            }

            if (!downloadedPrimary) {
                val latest = dao.getById(entity.id)
                if (latest?.status == DownloadStatus.CANCELLED) {
                    rawFile.delete()
                    return Result.success()
                }
                // Paused cleanly
                dao.updateStatus(entity.id, DownloadStatus.PAUSED)
                return Result.success()
            }

            // 2. Optional conversion / muxing step
            var finalFile = rawFile
            var finalExt = entity.format.lowercase()
            var finalMime = if (entity.mediaType == DownloadMode.VIDEO) "video/mp4" else "audio/mpeg"

            if (entity.separateAudioUrl != null && entity.mediaType == DownloadMode.VIDEO) {
                val audioTmp = File(tempDir, "dl_${entity.id}_audio.tmp")
                streamToFile(entity.separateAudioUrl, audioTmp, entity.supportsRange) { _, _, _ -> isStopped }
                val muxed = File(tempDir, "dl_${entity.id}_muxed.mp4")
                dao.updateConversionProgress(entity.id, 0, DownloadStatus.CONVERTING)
                val muxResult = MediaProcessor.muxVideoAndAudio(rawFile, audioTmp, muxed) { pct ->
                    dao.updateConversionProgress(entity.id, pct, DownloadStatus.CONVERTING)
                }
                audioTmp.delete()
                if (muxResult.isSuccess) {
                    finalFile = muxed
                    finalExt = "mp4"
                    finalMime = "video/mp4"
                }
            } else if (entity.mediaType == DownloadMode.AUDIO && entity.requiresConversion) {
                val extracted = File(tempDir, "dl_${entity.id}_audio.m4a")
                dao.updateConversionProgress(entity.id, 0, DownloadStatus.CONVERTING)
                val extractResult = MediaProcessor.extractAudioTrack(rawFile, extracted) { pct ->
                    dao.updateConversionProgress(entity.id, pct, DownloadStatus.CONVERTING)
                }
                if (extractResult.isSuccess) {
                    finalFile = extracted
                    finalExt = "m4a"
                    finalMime = "audio/mp4"
                }
            }

            // 3. Verify integrity & extract duration
            val sha256 = StorageHelper.computeSha256(finalFile)
            val durationMs = extractDurationMs(finalFile).takeIf { it > 0 } ?: entity.durationMs
            val safeTitle = FormatUtils.sanitizeFileName(entity.title)
            val displayName = "$safeTitle.$finalExt"
            val relativePath = if (entity.mediaType == DownloadMode.VIDEO) "Movies/VORTEX" else "Music/VORTEX"

            // 4. Save to Scoped Storage MediaStore
            val savedUri = StorageHelper.saveToMediaStore(
                context = applicationContext,
                sourceFile = finalFile,
                displayName = displayName,
                mimeType = finalMime,
                mode = entity.mediaType
            ) ?: throw IllegalStateException("Failed to write file to $relativePath")

            val finalSize = finalFile.length()
            rawFile.delete()
            if (finalFile != rawFile) finalFile.delete()

            val updated = dao.getById(entity.id) ?: entity
            dao.update(
                updated.copy(
                    status = DownloadStatus.COMPLETED,
                    downloadedBytes = finalSize,
                    totalBytes = finalSize,
                    speedBytesPerSec = 0L,
                    conversionProgress = 100,
                    outputUri = savedUri.toString(),
                    relativePath = relativePath,
                    fileName = displayName,
                    format = finalExt.uppercase(),
                    durationMs = durationMs,
                    sha256 = sha256,
                    tempFilePath = null,
                    completedAt = System.currentTimeMillis()
                )
            )

            if (prefs.notificationsEnabled) {
                DownloadNotificationHelper.showCompletedNotification(
                    applicationContext,
                    entity.id,
                    entity.title,
                    relativePath
                )
            }
            Result.success()
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Download interrupted"
            dao.updateStatus(entity.id, DownloadStatus.FAILED, msg)
            if (prefs.notificationsEnabled) {
                DownloadNotificationHelper.showFailedNotification(applicationContext, entity.id, entity.title, msg)
            }
            Result.failure()
        }
    }

    private suspend fun streamToFile(
        url: String,
        targetFile: File,
        supportsRange: Boolean,
        onProgress: suspend (downloaded: Long, total: Long, speed: Long) -> Boolean
    ): Boolean {
        val existingBytes = if (supportsRange && targetFile.exists()) targetFile.length() else 0L
        val reqBuilder = Request.Builder().url(url).get()
        if (existingBytes > 0L) {
            reqBuilder.addHeader("Range", "bytes=$existingBytes-")
        }

        val app = applicationContext as VortexApp
        app.okHttpClient.newCall(reqBuilder.build()).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw IllegalStateException("HTTP ${response.code} while downloading")
            }
            val body = response.body ?: throw IllegalStateException("Empty response stream")
            val isResumed = response.code == 206 && existingBytes > 0L
            val contentLength = body.contentLength().coerceAtLeast(0L)
            val totalBytes = if (isResumed) existingBytes + contentLength else contentLength

            var downloaded = if (isResumed) existingBytes else 0L
            val buffer = ByteArray(32 * 1024)
            var lastTickTime = System.currentTimeMillis()
            var bytesSinceTick = 0L

            FileOutputStream(targetFile, isResumed).use { output ->
                body.byteStream().use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        bytesSinceTick += read

                        val now = System.currentTimeMillis()
                        val elapsed = now - lastTickTime
                        if (elapsed >= 350L) {
                            val speed = (bytesSinceTick * 1000L) / elapsed.coerceAtLeast(1L)
                            lastTickTime = now
                            bytesSinceTick = 0L
                            val shouldPauseOrStop = onProgress(downloaded, totalBytes, speed)
                            if (shouldPauseOrStop) return false
                        }
                    }
                    output.flush()
                }
            }
            onProgress(downloaded, totalBytes.coerceAtLeast(downloaded), 0L)
            return true
        }
    }

    private fun extractDurationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }
}
