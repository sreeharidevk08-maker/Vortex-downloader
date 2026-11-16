package com.vortex.downloader

import android.app.Application
import com.vortex.downloader.data.local.VortexDatabase
import com.vortex.downloader.data.preferences.UserPreferencesRepository
import com.vortex.downloader.download.DownloadManager
import com.vortex.downloader.download.DownloadNotificationHelper
import com.vortex.downloader.network.AuthorizedSourceResolver
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class VortexApp : Application() {

    lateinit var database: VortexDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var okHttpClient: OkHttpClient
        private set

    lateinit var sourceResolver: AuthorizedSourceResolver
        private set

    lateinit var downloadManager: DownloadManager
        private set

    override fun onCreate() {
        super.onCreate()
        DownloadNotificationHelper.createChannel(this)

        database = VortexDatabase.getInstance(this)
        preferencesRepository = UserPreferencesRepository(this)
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        sourceResolver = AuthorizedSourceResolver(okHttpClient)
        downloadManager = DownloadManager(this, database.downloadDao(), preferencesRepository)
    }
}
