package com.vortex.downloader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vortex.downloader.data.local.DownloadEntity
import com.vortex.downloader.ui.components.FloatingGlassNavBar
import com.vortex.downloader.ui.components.VortexMeshBackground
import com.vortex.downloader.ui.components.VortexNavItem
import com.vortex.downloader.ui.screens.DownloadsScreen
import com.vortex.downloader.ui.screens.HomeScreen
import com.vortex.downloader.ui.screens.LibraryScreen
import com.vortex.downloader.ui.screens.MediaPlayerModal
import com.vortex.downloader.ui.screens.SettingsScreen
import com.vortex.downloader.ui.theme.VortexTheme
import com.vortex.downloader.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions handled gracefully via MediaStore & notification checks */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestRuntimePermissionsIfNeeded()

        setContent {
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()
            val homeState by viewModel.homeState.collectAsStateWithLifecycle()
            val allDownloads by viewModel.allDownloads.collectAsStateWithLifecycle()
            val activeCount by viewModel.activeDownloadCount.collectAsStateWithLifecycle()
            val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
            val libraryItems by viewModel.libraryItems.collectAsStateWithLifecycle()
            val libraryTab by viewModel.libraryTab.collectAsStateWithLifecycle()
            val librarySort by viewModel.librarySort.collectAsStateWithLifecycle()
            val libraryQuery by viewModel.libraryQuery.collectAsStateWithLifecycle()

            var playingItem by remember { mutableStateOf<DownloadEntity?>(null) }

            VortexTheme(themeMode = prefs.themeMode) {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route ?: "home"

                val navItems = listOf(
                    VortexNavItem("home", "Home", Icons.Rounded.Home),
                    VortexNavItem("downloads", "Downloads", Icons.Rounded.Download, badgeCount = activeCount),
                    VortexNavItem("library", "Library", Icons.Rounded.VideoLibrary),
                    VortexNavItem("settings", "Settings", Icons.Rounded.Settings)
                )

                VortexMeshBackground {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable("home") {
                                HomeScreen(
                                    state = homeState,
                                    recentDownloads = allDownloads,
                                    storageStats = storageStats,
                                    onUrlChange = viewModel::updateUrl,
                                    onClearUrl = viewModel::clearUrl,
                                    onModeChange = viewModel::setMode,
                                    onAnalyse = viewModel::analyseUrl,
                                    onSelectVideoQuality = viewModel::selectVideoQuality,
                                    onSelectAudioBitrate = viewModel::selectAudioBitrate,
                                    onDownload = viewModel::startDownload,
                                    onOpenSettings = { navController.navigate("settings") },
                                    onOpenLibraryItem = { playingItem = it }
                                )
                            }
                            composable("downloads") {
                                DownloadsScreen(
                                    downloads = allDownloads,
                                    onPause = viewModel::pauseDownload,
                                    onResume = viewModel::resumeDownload,
                                    onCancel = viewModel::cancelDownload,
                                    onRetry = viewModel::retryDownload
                                )
                            }
                            composable("library") {
                                LibraryScreen(
                                    items = libraryItems,
                                    selectedTab = libraryTab,
                                    selectedSort = librarySort,
                                    searchQuery = libraryQuery,
                                    onTabChange = viewModel::setLibraryTab,
                                    onSortChange = viewModel::setLibrarySort,
                                    onSearchChange = viewModel::setLibraryQuery,
                                    onPlay = { playingItem = it },
                                    onRename = viewModel::renameItem,
                                    onDelete = viewModel::deleteItem
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    prefs = prefs,
                                    storageStats = storageStats,
                                    onThemeChange = viewModel::setThemeMode,
                                    onDefaultVideoQualityChange = viewModel::setDefaultVideoQuality,
                                    onDefaultAudioBitrateChange = viewModel::setDefaultAudioBitrate,
                                    onMaxSimultaneousChange = viewModel::setMaxSimultaneous,
                                    onWifiOnlyChange = viewModel::setWifiOnly,
                                    onNotificationsChange = viewModel::setNotificationsEnabled,
                                    onClearCache = viewModel::clearCache
                                )
                            }
                        }

                        FloatingGlassNavBar(
                            items = navItems,
                            currentRoute = currentRoute,
                            onSelect = { route ->
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo("home") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }

                    playingItem?.let { item ->
                        MediaPlayerModal(
                            item = item,
                            onDismiss = { playingItem = null }
                        )
                    }
                }
            }
        }
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}
