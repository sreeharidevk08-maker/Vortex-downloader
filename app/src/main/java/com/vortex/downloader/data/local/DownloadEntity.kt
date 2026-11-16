package com.vortex.downloader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vortex.downloader.data.model.DownloadMode
import com.vortex.downloader.data.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val canonicalKey: String,
    val originalUrl: String,
    val downloadUrl: String,
    val separateAudioUrl: String? = null,
    val title: String,
    val artist: String? = null,
    val sourceName: String,
    val thumbnailUrl: String? = null,
    val mediaType: DownloadMode,
    val qualityLabel: String,
    val format: String,
    val bitrateKbps: Int? = null,
    val requiresConversion: Boolean = false,
    val supportsRange: Boolean = false,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val conversionProgress: Int = 0,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val tempFilePath: String? = null,
    val tempAudioPath: String? = null,
    val outputUri: String? = null,
    val relativePath: String = "",
    val fileName: String = "",
    val durationMs: Long = 0L,
    val sha256: String? = null,
    val errorMessage: String? = null,
    val workRequestId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
