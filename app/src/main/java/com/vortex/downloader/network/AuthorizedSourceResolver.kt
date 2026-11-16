package com.vortex.downloader.network

import android.net.Uri
import com.vortex.downloader.data.model.AnalysedSource
import com.vortex.downloader.data.model.AudioBitrateOption
import com.vortex.downloader.data.model.SourceAnalysisResult
import com.vortex.downloader.data.model.VideoQualityOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetAddress
import java.util.Locale

class AuthorizedSourceResolver(
    private val client: OkHttpClient
) {
    companion object {
        private val RESTRICTED_HOSTS = setOf(
            "youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be",
            "music.youtube.com", "netflix.com", "www.netflix.com",
            "spotify.com", "open.spotify.com", "disneyplus.com", "www.disneyplus.com",
            "primevideo.com", "www.primevideo.com", "hulu.com", "www.hulu.com"
        )

        private val VIDEO_EXTENSIONS = setOf("mp4", "m4v", "webm", "mov", "mkv")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "ogg", "flac")
    }

    suspend fun analyse(rawUrl: String): SourceAnalysisResult = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return@withContext SourceAnalysisResult.Error("Please enter a media URL to analyse.")
        }

        val parsed = runCatching { Uri.parse(trimmed) }.getOrNull()
        val scheme = parsed?.scheme?.lowercase(Locale.US)
        val host = parsed?.host?.lowercase(Locale.US)

        if (parsed == null || host.isNullOrBlank() || (scheme != "https" && scheme != "http")) {
            return@withContext SourceAnalysisResult.Error(
                "Invalid URL. Enter a valid HTTPS link (or local LAN HTTP link)."
            )
        }

        if (scheme == "http" && !isLocalOrLanHost(host)) {
            return@withContext SourceAnalysisResult.Restricted(
                reason = "Cleartext HTTP is blocked for public hosts to protect your privacy.",
                suggestion = "Use an HTTPS URL from an authorized source."
            )
        }

        if (RESTRICTED_HOSTS.any { host == it || host.endsWith(".$it") }) {
            return@withContext SourceAnalysisResult.Restricted(
                reason = "Downloads from $host are not permitted. VORTEX does not bypass DRM, access controls, or extract unauthorized streams.",
                suggestion = "Use an authorized open-media link (such as Internet Archive, Wikimedia Commons, or a direct MP4/MP3 media URL)."
            )
        }

        // 1. Internet Archive authorized metadata API
        if (host == "archive.org" || host.endsWith(".archive.org")) {
            val itemId = extractArchiveOrgId(parsed)
            if (itemId != null) {
                return@withContext resolveArchiveOrg(trimmed, itemId)
            }
        }

        // 2. Direct media or open JSON manifest probe
        probeDirectMediaOrManifest(trimmed, host)
    }

    private fun isLocalOrLanHost(host: String): Boolean {
        if (host == "localhost" || host == "127.0.0.1") return true
        return runCatching {
            val addr = InetAddress.getByName(host)
            addr.isSiteLocalAddress || addr.isLoopbackAddress
        }.getOrDefault(false)
    }

    private fun extractArchiveOrgId(uri: Uri): String? {
        val segments = uri.pathSegments
        val detailsIdx = segments.indexOf("details")
        if (detailsIdx != -1 && detailsIdx + 1 < segments.size) {
            return segments[detailsIdx + 1]
        }
        val downloadIdx = segments.indexOf("download")
        if (downloadIdx != -1 && downloadIdx + 1 < segments.size && segments.size == downloadIdx + 2) {
            return segments[downloadIdx + 1]
        }
        return null
    }

    private fun resolveArchiveOrg(originalUrl: String, itemId: String): SourceAnalysisResult {
        val metadataUrl = "https://archive.org/metadata/$itemId"
        val request = Request.Builder().url(metadataUrl).get().build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return SourceAnalysisResult.Error("Could not fetch Internet Archive metadata (HTTP ${response.code}).")
                }
                val body = response.body?.string()
                    ?: return SourceAnalysisResult.Error("Empty metadata response from Internet Archive.")
                val json = JSONObject(body)
                if (json.optBoolean("is_dark", false)) {
                    return SourceAnalysisResult.Restricted(
                        reason = "This Internet Archive item is currently restricted or unavailable.",
                        suggestion = "Choose a publicly accessible open-license item."
                    )
                }

                val meta = json.optJSONObject("metadata")
                val title = meta?.optString("title")?.takeIf { it.isNotBlank() } ?: itemId
                val creator = meta?.optString("creator")?.takeIf { it.isNotBlank() }
                val files = json.optJSONArray("files")
                    ?: return SourceAnalysisResult.Error("No downloadable files listed in this item.")

                val videoOptions = mutableListOf<VideoQualityOption>()
                val audioOptions = mutableListOf<AudioBitrateOption>()
                var thumbnailUrl: String? = null
                var durationMs = 0L

                for (i in 0 until files.length()) {
                    val file = files.getJSONObject(i)
                    val name = file.optString("name")
                    if (name.isBlank()) continue
                    val lowerName = name.lowercase(Locale.US)
                    val size = file.optString("size").toLongOrNull() ?: 0L
                    val lengthSec = file.optString("length").toDoubleOrNull()
                    if (lengthSec != null && lengthSec > 0 && durationMs == 0L) {
                        durationMs = (lengthSec * 1000).toLong()
                    }
                    val height = file.optString("height").toIntOrNull()
                    val encodedName = Uri.encode(name, "/")
                    val downloadUrl = "https://archive.org/download/$itemId/$encodedName"

                    if (thumbnailUrl == null && (lowerName.endsWith(".jpg") || lowerName.endsWith(".png"))) {
                        thumbnailUrl = downloadUrl
                    }

                    val ext = lowerName.substringAfterLast('.', "")
                    if (ext in VIDEO_EXTENSIONS && size > 0) {
                        val normalizedHeight = normalizeVideoHeight(height ?: 720)
                        videoOptions.add(
                            VideoQualityOption(
                                label = qualityLabelForHeight(normalizedHeight),
                                resolutionHeight = normalizedHeight,
                                format = ext.uppercase(Locale.US),
                                estimatedBytes = size,
                                downloadUrl = downloadUrl,
                                supportsRange = true
                            )
                        )
                    } else if (ext in AUDIO_EXTENSIONS && size > 0) {
                        val bitrate = file.optString("bitrate").toIntOrNull()
                            ?: estimateBitrateKbps(size, durationMs)
                        val clampedBitrate = closestAllowedBitrate(bitrate)
                        audioOptions.add(
                            AudioBitrateOption(
                                bitrateKbps = clampedBitrate,
                                format = if (ext == "mp3") "MP3" else ext.uppercase(Locale.US),
                                estimatedBytes = size,
                                downloadUrl = downloadUrl,
                                sourceBitrateKbps = bitrate,
                                requiresConversion = ext != "mp3",
                                supportsRange = true
                            )
                        )
                    }
                }

                val dedupVideos = videoOptions
                    .sortedByDescending { it.resolutionHeight }
                    .distinctBy { it.resolutionHeight }

                val dedupAudios = audioOptions
                    .sortedByDescending { it.bitrateKbps }
                    .distinctBy { it.bitrateKbps }

                if (dedupVideos.isEmpty() && dedupAudios.isEmpty()) {
                    return SourceAnalysisResult.Error("No supported video or audio files found in this Internet Archive item.")
                }

                val formats = (dedupVideos.map { it.format } + dedupAudios.map { it.format }).distinct()
                SourceAnalysisResult.Success(
                    AnalysedSource(
                        originalUrl = originalUrl,
                        canonicalUrl = "https://archive.org/details/$itemId",
                        title = title,
                        artist = creator,
                        sourceName = "Internet Archive",
                        thumbnailUrl = thumbnailUrl,
                        durationMs = durationMs,
                        availableFormats = formats,
                        videoQualities = dedupVideos,
                        audioBitrates = dedupAudios,
                        authorizationNote = "Authorized open-access media via Internet Archive Metadata API"
                    )
                )
            }
        }.getOrElse { err ->
            SourceAnalysisResult.Error("Network error while analysing source: ${err.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun probeDirectMediaOrManifest(url: String, host: String): SourceAnalysisResult {
        val headRequest = Request.Builder().url(url).head().build()
        return runCatching {
            client.newCall(headRequest).execute().use { response ->
                if (!response.isSuccessful && response.code != 405) {
                    return SourceAnalysisResult.Error("Server returned HTTP ${response.code} for this URL.")
                }
                val contentType = response.header("Content-Type")?.lowercase(Locale.US).orEmpty()
                val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
                val acceptRanges = response.header("Accept-Ranges")?.equals("bytes", ignoreCase = true) == true
                val fileName = Uri.parse(url).lastPathSegment?.substringBefore('?') ?: "VORTEX_Media"
                val ext = fileName.substringAfterLast('.', "").lowercase(Locale.US)

                val isVideo = contentType.startsWith("video/") || ext in VIDEO_EXTENSIONS
                val isAudio = contentType.startsWith("audio/") || ext in AUDIO_EXTENSIONS

                if (!isVideo && !isAudio) {
                    return SourceAnalysisResult.Restricted(
                        reason = "This URL does not point to a direct downloadable video/audio file or a supported authorized media API (Content-Type: ${contentType.ifBlank { "unknown" }}).",
                        suggestion = "Paste a direct MP4/MP3/M4A media link or an Internet Archive link."
                    )
                }

                val title = fileName.substringBeforeLast('.').ifBlank { "Authorized Media" }
                val videoOptions = if (isVideo) {
                    listOf(
                        VideoQualityOption(
                            label = "Source Stream (720p+)",
                            resolutionHeight = 720,
                            format = if (ext.isNotBlank()) ext.uppercase(Locale.US) else "MP4",
                            estimatedBytes = contentLength,
                            downloadUrl = url,
                            supportsRange = acceptRanges
                        )
                    )
                } else emptyList()

                val audioOptions = if (isAudio) {
                    listOf(
                        AudioBitrateOption(
                            bitrateKbps = 192,
                            format = if (ext == "mp3") "MP3" else ext.uppercase(Locale.US).ifBlank { "M4A" },
                            estimatedBytes = contentLength,
                            downloadUrl = url,
                            sourceBitrateKbps = 192,
                            requiresConversion = ext != "mp3",
                            supportsRange = acceptRanges
                        )
                    )
                } else {
                    // Video file can also have its audio track extracted at up to 192 kbps without artificial upscaling
                    listOf(
                        AudioBitrateOption(
                            bitrateKbps = 192,
                            format = "M4A",
                            estimatedBytes = (contentLength * 0.15).toLong().coerceAtLeast(0L),
                            downloadUrl = url,
                            sourceBitrateKbps = 192,
                            requiresConversion = true,
                            supportsRange = acceptRanges
                        )
                    )
                }

                SourceAnalysisResult.Success(
                    AnalysedSource(
                        originalUrl = url,
                        canonicalUrl = url,
                        title = title,
                        artist = null,
                        sourceName = host,
                        thumbnailUrl = null,
                        durationMs = 0L,
                        availableFormats = (videoOptions.map { it.format } + audioOptions.map { it.format }).distinct(),
                        videoQualities = videoOptions,
                        audioBitrates = audioOptions,
                        authorizationNote = "Direct authorized media stream"
                    )
                )
            }
        }.getOrElse { err ->
            SourceAnalysisResult.Error("Failed to connect: ${err.localizedMessage ?: "Network error"}")
        }
    }

    internal fun normalizeVideoHeight(height: Int): Int = when {
        height >= 2000 -> 2160
        height >= 1400 -> 1440
        height >= 1000 -> 1080
        height >= 700 -> 720
        height >= 340 -> 360
        else -> 144
    }

    internal fun qualityLabelForHeight(height: Int): String = when (height) {
        2160 -> "2160p (4K)"
        1440 -> "1440p (2K)"
        1080 -> "1080p (Full HD)"
        720 -> "720p (HD)"
        360 -> "360p"
        else -> "144p"
    }

    internal fun closestAllowedBitrate(sourceKbps: Int): Int = when {
        sourceKbps >= 290 -> 320
        sourceKbps >= 220 -> 256
        else -> 192
    }

    private fun estimateBitrateKbps(sizeBytes: Long, durationMs: Long): Int {
        if (sizeBytes <= 0L || durationMs <= 0L) return 192
        return ((sizeBytes * 8) / durationMs).toInt().coerceIn(64, 320)
    }
}
