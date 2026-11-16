package com.vortex.downloader.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.vortex.downloader.R

object DownloadNotificationHelper {

    const val CHANNEL_ID = "vortex_downloads"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_downloads_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_downloads_desc)
        }
        manager.createNotificationChannel(channel)
    }

    fun buildForegroundInfo(
        context: Context,
        downloadId: Long,
        title: String,
        progressPercent: Int,
        statusLine: String
    ): ForegroundInfo {
        val notification = buildProgressNotification(context, title, progressPercent, statusLine)
        return ForegroundInfo(
            downloadId.toInt() + 1000,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    fun buildProgressNotification(
        context: Context,
        title: String,
        progressPercent: Int,
        statusLine: String
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(statusLine)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progressPercent.coerceIn(0, 100), progressPercent <= 0)
            .build()
    }

    fun showCompletedNotification(context: Context, downloadId: Long, title: String, destination: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Download complete")
            .setContentText("$title saved to $destination")
            .setAutoCancel(true)
            .build()
        manager.notify(downloadId.toInt() + 2000, notification)
    }

    fun showFailedNotification(context: Context, downloadId: Long, title: String, reason: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Download failed: $title")
            .setContentText(reason)
            .setAutoCancel(true)
            .build()
        manager.notify(downloadId.toInt() + 3000, notification)
    }
}
