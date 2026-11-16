package com.vortex.downloader.data.model

enum class DownloadMode {
    VIDEO,
    AUDIO
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    CONVERTING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System")
}

enum class LibraryTab(val label: String) {
    ALL("All"),
    VIDEOS("Videos"),
    MUSIC("Music")
}

enum class LibrarySort(val label: String) {
    DATE_DESC("Newest"),
    NAME_ASC("Name"),
    SIZE_DESC("Size")
}

data class VideoQualityOption(
    val label: String,         // e.g. "2160p (4K)", "1080p (Full HD)"
    val resolutionHeight: Int, // 2160, 1440, 1080, 720, 360, 144
    val format: String,        // "MP4"
    val estimatedBytes: Long,
    val downloadUrl: String,
    val separateAudioUrl: String? = null,
    val supportsRange: Boolean = false
)

data class AudioBitrateOption(
    val bitrateKbps: Int,      // 320, 256, 192
    val format: String,        // "MP3" or preserved container
    val estimatedBytes: Long,
    val downloadUrl: String,
    val sourceBitrateKbps: Int,
    val requiresConversion: Boolean,
    val supportsRange: Boolean = false
)

data class AnalysedSource(
    val originalUrl: String,
    val canonicalUrl: String,
    val title: String,
    val artist: String?,
    val sourceName: String,
    val thumbnailUrl: String?,
    val durationMs: Long,
    val availableFormats: List<String>,
    val videoQualities: List<VideoQualityOption>,
    val audioBitrates: List<AudioBitrateOption>,
    val authorizationNote: String
)

sealed interface SourceAnalysisResult {
    data class Success(val source: AnalysedSource) : SourceAnalysisResult
    data class Restricted(val reason: String, val suggestion: String) : SourceAnalysisResult
    data class Error(val message: String) : SourceAnalysisResult
}
