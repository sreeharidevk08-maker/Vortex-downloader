package com.vortex.downloader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vortex.downloader.VortexApp
import com.vortex.downloader.data.local.DownloadEntity
import com.vortex.downloader.data.model.AudioBitrateOption
import com.vortex.downloader.data.model.DownloadMode
import com.vortex.downloader.data.model.DownloadStatus
import com.vortex.downloader.data.model.LibrarySort
import com.vortex.downloader.data.model.LibraryTab
import com.vortex.downloader.data.model.SourceAnalysisResult
import com.vortex.downloader.data.model.ThemeMode
import com.vortex.downloader.data.model.VideoQualityOption
import com.vortex.downloader.data.preferences.UserPreferences
import com.vortex.downloader.download.EnqueueResult
import com.vortex.downloader.util.FormatUtils
import com.vortex.downloader.util.StorageHelper
import com.vortex.downloader.util.StorageStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val urlInput: String = "",
    val mode: DownloadMode = DownloadMode.VIDEO,
    val isAnalysing: Boolean = false,
    val analysisResult: SourceAnalysisResult? = null,
    val selectedVideoQuality: VideoQualityOption? = null,
    val selectedAudioBitrate: AudioBitrateOption? = null,
    val feedbackMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VortexApp
    private val dao = app.database.downloadDao()
    private val prefsRepo = app.preferencesRepository
    private val resolver = app.sourceResolver
    private val downloadManager = app.downloadManager

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _storageStats = MutableStateFlow(StorageHelper.getStorageStats(app))
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _libraryTab = MutableStateFlow(LibraryTab.ALL)
    val libraryTab: StateFlow<LibraryTab> = _libraryTab.asStateFlow()

    private val _librarySort = MutableStateFlow(LibrarySort.DATE_DESC)
    val librarySort: StateFlow<LibrarySort> = _librarySort.asStateFlow()

    private val _libraryQuery = MutableStateFlow("")
    val libraryQuery: StateFlow<String> = _libraryQuery.asStateFlow()

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val allDownloads: StateFlow<List<DownloadEntity>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloadCount: StateFlow<Int> = dao.observeActiveAndQueued()
        .combine(_homeState) { active, _ -> active.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val libraryItems: StateFlow<List<DownloadEntity>> = combine(
        dao.observeCompleted(),
        _libraryTab,
        _librarySort,
        _libraryQuery
    ) { items, tab, sort, query ->
        val filteredByTab = when (tab) {
            LibraryTab.ALL -> items
            LibraryTab.VIDEOS -> items.filter { it.mediaType == DownloadMode.VIDEO }
            LibraryTab.MUSIC -> items.filter { it.mediaType == DownloadMode.AUDIO }
        }
        val filteredByQuery = if (query.isBlank()) filteredByTab else {
            val q = query.trim().lowercase()
            filteredByTab.filter {
                it.title.lowercase().contains(q) ||
                    (it.artist?.lowercase()?.contains(q) == true) ||
                    it.sourceName.lowercase().contains(q)
            }
        }
        when (sort) {
            LibrarySort.DATE_DESC -> filteredByQuery.sortedByDescending { it.completedAt ?: it.createdAt }
            LibrarySort.NAME_ASC -> filteredByQuery.sortedBy { it.title.lowercase() }
            LibrarySort.SIZE_DESC -> filteredByQuery.sortedByDescending { it.totalBytes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateUrl(url: String) {
        _homeState.value = _homeState.value.copy(urlInput = url, feedbackMessage = null)
    }

    fun clearUrl() {
        _homeState.value = HomeUiState(mode = _homeState.value.mode)
    }

    fun setMode(mode: DownloadMode) {
        _homeState.value = _homeState.value.copy(mode = mode)
    }

    fun analyseUrl() {
        val url = _homeState.value.urlInput
        viewModelScope.launch {
            _homeState.value = _homeState.value.copy(
                isAnalysing = true,
                analysisResult = null,
                feedbackMessage = null
            )
            val result = resolver.analyse(url)
            val prefs = preferences.value
            var defaultVideo: VideoQualityOption? = null
            var defaultAudio: AudioBitrateOption? = null
            if (result is SourceAnalysisResult.Success) {
                defaultVideo = result.source.videoQualities.firstOrNull {
                    it.resolutionHeight == prefs.defaultVideoQualityHeight
                } ?: result.source.videoQualities.firstOrNull()

                defaultAudio = result.source.audioBitrates.firstOrNull {
                    it.bitrateKbps == prefs.defaultAudioBitrateKbps
                } ?: result.source.audioBitrates.firstOrNull()
            }
            _homeState.value = _homeState.value.copy(
                isAnalysing = false,
                analysisResult = result,
                selectedVideoQuality = defaultVideo,
                selectedAudioBitrate = defaultAudio
            )
        }
    }

    fun selectVideoQuality(option: VideoQualityOption) {
        _homeState.value = _homeState.value.copy(selectedVideoQuality = option)
    }

    fun selectAudioBitrate(option: AudioBitrateOption) {
        _homeState.value = _homeState.value.copy(selectedAudioBitrate = option)
    }

    fun startDownload() {
        val state = _homeState.value
        val success = state.analysisResult as? SourceAnalysisResult.Success ?: return
        viewModelScope.launch {
            val result = when (state.mode) {
                DownloadMode.VIDEO -> {
                    val quality = state.selectedVideoQuality ?: return@launch
                    downloadManager.enqueueVideo(success.source, quality)
                }
                DownloadMode.AUDIO -> {
                    val bitrate = state.selectedAudioBitrate ?: return@launch
                    downloadManager.enqueueAudio(success.source, bitrate)
                }
            }
            val msg = when (result) {
                is EnqueueResult.Enqueued -> "Added to Downloads queue"
                is EnqueueResult.Duplicate -> "Already in Downloads (${result.existing.status.name.lowercase()})"
                is EnqueueResult.LimitReached -> result.message
            }
            _homeState.value = _homeState.value.copy(feedbackMessage = msg)
            refreshStorageStats()
        }
    }

    fun pauseDownload(id: Long) = viewModelScope.launch { downloadManager.pause(id) }
    fun resumeDownload(id: Long) = viewModelScope.launch { downloadManager.resume(id) }
    fun cancelDownload(id: Long) = viewModelScope.launch { downloadManager.cancel(id) }
    fun retryDownload(id: Long) = viewModelScope.launch { downloadManager.retry(id) }
    fun deleteItem(entity: DownloadEntity) = viewModelScope.launch {
        downloadManager.deleteDownload(entity)
        refreshStorageStats()
    }
    fun renameItem(entity: DownloadEntity, newTitle: String) = viewModelScope.launch {
        if (newTitle.isNotBlank()) downloadManager.renameDownload(entity, newTitle)
    }

    fun setLibraryTab(tab: LibraryTab) { _libraryTab.value = tab }
    fun setLibrarySort(sort: LibrarySort) { _librarySort.value = sort }
    fun setLibraryQuery(query: String) { _libraryQuery.value = query }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    fun setDefaultVideoQuality(height: Int) = viewModelScope.launch { prefsRepo.setDefaultVideoQuality(height) }
    fun setDefaultAudioBitrate(kbps: Int) = viewModelScope.launch { prefsRepo.setDefaultAudioBitrate(kbps) }
    fun setMaxSimultaneous(limit: Int) = viewModelScope.launch { prefsRepo.setMaxSimultaneousDownloads(limit) }
    fun setWifiOnly(enabled: Boolean) = viewModelScope.launch { prefsRepo.setWifiOnly(enabled) }
    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setNotificationsEnabled(enabled) }

    fun clearCache(): String {
        val freed = StorageHelper.clearAppCache(app)
        refreshStorageStats()
        return "Cleared ${FormatUtils.formatBytes(freed)} of temporary cache"
    }

    fun refreshStorageStats() {
        _storageStats.value = StorageHelper.getStorageStats(app)
    }
}
