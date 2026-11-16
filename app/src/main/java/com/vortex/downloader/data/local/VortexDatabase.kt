package com.vortex.downloader.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.vortex.downloader.data.model.DownloadMode
import com.vortex.downloader.data.model.DownloadStatus

class VortexConverters {
    @TypeConverter
    fun fromDownloadMode(value: DownloadMode): String = value.name

    @TypeConverter
    fun toDownloadMode(value: String): DownloadMode = DownloadMode.valueOf(value)

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}

@Database(
    entities = [DownloadEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(VortexConverters::class)
abstract class VortexDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: VortexDatabase? = null

        fun getInstance(context: Context): VortexDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VortexDatabase::class.java,
                    "vortex_downloader.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
