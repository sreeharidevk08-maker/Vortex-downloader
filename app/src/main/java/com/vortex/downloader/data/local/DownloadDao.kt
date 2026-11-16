package com.vortex.downloader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vortex.downloader.data.model.DownloadMode
import com.vortex.downloader.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY completedAt DESC, createdAt DESC")
    fun observeByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC")
    fun observeCompleted(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED', 'CONVERTING') ORDER BY createdAt DESC")
    fun observeActiveAndQueued(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE canonicalKey = :key AND status IN ('QUEUED', 'DOWNLOADING', 'PAUSED', 'CONVERTING', 'COMPLETED') LIMIT 1")
    suspend fun findDuplicate(key: String): DownloadEntity?

    @Query("SELECT COUNT(*) FROM downloads WHERE status IN ('DOWNLOADING', 'CONVERTING')")
    suspend fun countRunning(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DownloadEntity): Long

    @Update
    suspend fun update(entity: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, speedBytesPerSec = 0, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus, error: String? = null)

    @Query("UPDATE downloads SET downloadedBytes = :downloaded, totalBytes = :total, speedBytesPerSec = :speed, status = :status WHERE id = :id")
    suspend fun updateProgress(
        id: Long,
        downloaded: Long,
        total: Long,
        speed: Long,
        status: DownloadStatus
    )

    @Query("UPDATE downloads SET conversionProgress = :progress, status = :status WHERE id = :id")
    suspend fun updateConversionProgress(id: Long, progress: Int, status: DownloadStatus)

    @Query("UPDATE downloads SET title = :newTitle, fileName = :newFileName WHERE id = :id")
    suspend fun rename(id: Long, newTitle: String, newFileName: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)
}
