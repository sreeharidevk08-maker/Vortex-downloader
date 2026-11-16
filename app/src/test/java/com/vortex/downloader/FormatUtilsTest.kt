package com.vortex.downloader

import com.vortex.downloader.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun formatBytes_scalesCorrectly() {
        assertEquals("0 B", FormatUtils.formatBytes(0))
        assertEquals("512.0 B", FormatUtils.formatBytes(512))
        assertEquals("1.0 KB", FormatUtils.formatBytes(1024))
        assertEquals("1.0 MB", FormatUtils.formatBytes(1024L * 1024L))
    }

    @Test
    fun formatDuration_formatsHoursAndMinutes() {
        assertEquals("--:--", FormatUtils.formatDuration(0))
        assertEquals("01:05", FormatUtils.formatDuration(65_000))
        assertEquals("1:01:05", FormatUtils.formatDuration(3_665_000))
    }

    @Test
    fun sanitizeFileName_removesIllegalCharacters() {
        val cleaned = FormatUtils.sanitizeFileName("My: Video / Test*?")
        assertEquals("My_ Video _ Test__", cleaned)
    }
}
