package com.vortex.downloader.media

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

object MediaProcessor {

    /**
     * Muxes separate video and audio files into a single MP4 container using Android's native
     * MediaExtractor + MediaMuxer (API 29+). Reports progress 0..100.
     */
    suspend fun muxVideoAndAudio(
        videoFile: File,
        audioFile: File,
        outputFile: File,
        onProgress: suspend (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            val audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            try {
                val videoTrackIndex = selectTrack(videoExtractor, "video/")
                val audioTrackIndex = selectTrack(audioExtractor, "audio/")
                require(videoTrackIndex >= 0) { "No video track found in downloaded stream." }
                require(audioTrackIndex >= 0) { "No audio track found in downloaded stream." }

                val videoFormat = videoExtractor.getTrackFormat(videoTrackIndex)
                val audioFormat = audioExtractor.getTrackFormat(audioTrackIndex)
                val durationUs = maxOf(
                    videoFormat.getLongOrZero(MediaFormat.KEY_DURATION),
                    audioFormat.getLongOrZero(MediaFormat.KEY_DURATION)
                )

                val outVideoTrack = muxer.addTrack(videoFormat)
                val outAudioTrack = muxer.addTrack(audioFormat)
                muxer.start()

                onProgress(10)
                copyTrackSamples(videoExtractor, muxer, outVideoTrack, durationUs) { pct ->
                    onProgress(10 + (pct * 0.45f).toInt())
                }
                copyTrackSamples(audioExtractor, muxer, outAudioTrack, durationUs) { pct ->
                    onProgress(55 + (pct * 0.45f).toInt())
                }
                onProgress(100)
                outputFile
            } finally {
                runCatching { muxer.stop() }
                runCatching { muxer.release() }
                videoExtractor.release()
                audioExtractor.release()
            }
        }
    }

    /**
     * Extracts the native audio track from a downloaded media file into an M4A container
     * without artificially upscaling quality. Reports progress 0..100.
     */
    suspend fun extractAudioTrack(
        inputFile: File,
        outputFile: File,
        onProgress: suspend (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val extractor = MediaExtractor().apply { setDataSource(inputFile.absolutePath) }
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            try {
                val audioTrackIndex = selectTrack(extractor, "audio/")
                require(audioTrackIndex >= 0) { "No audio track found in source media." }

                val audioFormat = extractor.getTrackFormat(audioTrackIndex)
                val durationUs = audioFormat.getLongOrZero(MediaFormat.KEY_DURATION)
                val outTrack = muxer.addTrack(audioFormat)
                muxer.start()

                copyTrackSamples(extractor, muxer, outTrack, durationUs, onProgress)
                onProgress(100)
                outputFile
            } finally {
                runCatching { muxer.stop() }
                runCatching { muxer.release() }
                extractor.release()
            }
        }
    }

    private fun selectTrack(extractor: MediaExtractor, mimePrefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith(mimePrefix)) {
                extractor.selectTrack(i)
                return i
            }
        }
        return -1
    }

    private suspend fun copyTrackSamples(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        outputTrackIndex: Int,
        durationUs: Long,
        onProgress: suspend (Int) -> Unit
    ) {
        val buffer = ByteBuffer.allocate(512 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()
        var lastReported = -1

        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break

            bufferInfo.offset = 0
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = extractor.sampleTime
            bufferInfo.flags = extractor.sampleFlags

            muxer.writeSampleData(outputTrackIndex, buffer, bufferInfo)
            extractor.advance()

            if (durationUs > 0) {
                val pct = ((bufferInfo.presentationTimeUs * 100) / durationUs).toInt().coerceIn(0, 100)
                if (pct - lastReported >= 5) {
                    lastReported = pct
                    onProgress(pct)
                }
            }
        }
    }

    private fun MediaFormat.getLongOrZero(key: String): Long {
        return if (containsKey(key)) getLong(key) else 0L
    }
}
