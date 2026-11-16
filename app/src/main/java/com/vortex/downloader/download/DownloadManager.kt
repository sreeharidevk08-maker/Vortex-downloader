package com.vortex.downloader.download

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.vortex.downloader.data.local.DownloadDao
import com.vortex.downloader.data.local.DownloadEntity
import com.vortex.downloader.data.model.AnalysedSource
import com.vortex.downloader.data.model.AudioBitrateOption
import com.vortex.downloader.data.model.DownloadMode
import com.vortex.downloader.data.model.DownloadStatus
import com.vortex.downloader.data.model.VideoQualityOption
import com.vortex.downloader.data.preferences.UserPreferencesRepository
import com.vortex.downloader.util.FormatUtils
import kotlinx.coroutines.flow.first
import java.io.File

sealed interface EnqueueResult {
    data class Enqueued(val id: Long) : EnqueueResult
    data class Duplicate(val existing: DownloadEntity) : EnqueueResult
    data class LimitReached(val message: String) : EnqueueResult
}

class DownloadManager(
    private val context: Context,
    private val dao: DownloadDao,
    private val prefsRepo: UserPreferencesRepository
) {
    private val workManager = WorkManager.getInstance(context)

    suspend fun enqueueVideo(source: AnalysedSource, quality: VideoQualityOption): EnqueueResult {
        val key = "${source.canonicalUrl}|VIDEO|${quality.resolutionHeight}|${quality.format}"
        dao.findDuplicate(key)?.let { return EnqueueResult.Duplicate(it) }

        val entity = DownloadEntity(
            canonicalKey = key,
            originalUrl = source.originalUrl,
            downloadUrl = quality.downloadUrl,
            separateAudioUrl = quality.separateAudioUrl,
            title = source.title,
            artist = source.artist,
            sourceName = source.sourceName,
            thumbnailUrl = source.thumbnailUrl,
            mediaType = DownloadMode.VIDEO,
            qualityLabel = quality.label,
            format = quality.format,
            supportsRange = quality.supportsRange,
            totalBytes = quality.estimatedBytes,
            durationMs = source.durationMs,
            status = DownloadStatus.QUEUED
        )
        val id = dao.insert(entity)
        scheduleWorker(id)
        return EnqueueResult.Enqueued(id)
    }

    suspend fun enqueueAudio(source: AnalysedSource, bitrate: AudioBitrateOption): EnqueueResult {
        val key = "${source.canonicalUrl}|AUDIO|${bitrate.bitrateKbps}|${bitrate.format}"
        dao.findDuplicate(key)?.let { return EnqueueResult.Duplicate(it) }

        val entity = DownloadEntity(
            canonicalKey = key,
            originalUrl = source.originalUrl,
            downloadUrl = bitrate.downloadUrl,
            title = source.title,
            artist = source.artist,
            sourceName = source.sourceName,
            thumbnailUrl = source.thumbnailUrl,
            mediaType = DownloadMode.AUDIO,
            qualityLabel = "${bitrate.bitrateKbps} kbps",
            format = bitrate.format,
            bitrateKbps = bitrate.bitrateKbps,
            requiresConversion = bitrate.requiresConversion,
            supportsRange = bitrate.supportsRange,
            totalBytes = bitrate.estimatedBytes,
            durationMs = source.durationMs,
            status = DownloadStatus.QUEUED
        )
        val id = dao.insert(entity)
        scheduleWorker(id)
        return EnqueueResult.Enqueued(id)
    }

    suspend fun pause(id: Long) {
        dao.updateStatus(id, DownloadStatus.PAUSED)
        workManager.cancelUniqueWork(uniqueName(id))
    }

    suspend fun resume(id: Long) {
        val entity = dao.getById(id) ?: return
        dao.updateStatus(id, DownloadStatus.QUEUED)
        scheduleWorker(entity.id)
    }

    suspend fun cancel(id: Long) {
        val entity = dao.getById(id)
        dao.updateStatus(id, DownloadStatus.CANCELLED)
        workManager.cancelUniqueWork(uniqueName(id))
        entity?.tempFilePath?.let { File(it).delete() }
    }

    suspend fun retry(id: Long) {
        dao.updateStatus(id, DownloadStatus.QUEUED, null)
        scheduleWorker(id)
    }

    suspend fun deleteDownload(entity: DownloadEntity) {
        workManager.cancelUniqueWork(uniqueName(entity.id))
        entity.tempFilePath?.let { File(it).delete() }
        entity.outputUri?.let { uriStr ->
            runCatching { context.contentResolver.delete(Uri.parse(uriStr), null, null) }
        }
        dao.deleteById(entity.id)
    }

    suspend fun renameDownload(entity: DownloadEntity, newTitle: String) {
        val safe = FormatUtils.sanitizeFileName(newTitle)
        val ext = entity.format.lowercase()
        dao.rename(entity.id, newTitle.trim(), "$safe.$ext")
    }

    private suspend fun scheduleWorker(downloadId: Long) {
        val prefs = prefsRepo.preferencesFlow.first()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (prefs.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putLong(DownloadWorker.KEY_DOWNLOAD_ID, downloadId).build())
            .addTag("vortex_download")
            .build()

        workManager.enqueueUniqueWork(uniqueName(downloadId), ExistingWorkPolicy.REPLACE, request)
    }

    private fun uniqueName(id: Long) = "vortex_dl_$id"
}
