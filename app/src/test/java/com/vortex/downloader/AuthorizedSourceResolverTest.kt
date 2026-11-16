package com.vortex.downloader

import com.vortex.downloader.data.model.SourceAnalysisResult
import com.vortex.downloader.network.AuthorizedSourceResolver
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorizedSourceResolverTest {

    private val resolver = AuthorizedSourceResolver(OkHttpClient())

    @Test
    fun emptyUrl_returnsError() = runTest {
        val result = resolver.analyse("   ")
        assertTrue(result is SourceAnalysisResult.Error)
    }

    @Test
    fun restrictedYoutubeUrl_returnsRestrictedWithoutBypass() = runTest {
        val result = resolver.analyse("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(result is SourceAnalysisResult.Restricted)
    }

    @Test
    fun publicHttpCleartext_returnsRestricted() = runTest {
        val result = resolver.analyse("http://example.com/video.mp4")
        assertTrue(result is SourceAnalysisResult.Restricted)
    }
}
