package com.vortex.downloader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.downloader.BuildConfig
import com.vortex.downloader.data.model.ThemeMode
import com.vortex.downloader.data.preferences.UserPreferences
import com.vortex.downloader.ui.components.GlassCard
import com.vortex.downloader.ui.theme.LocalVortexGlass
import com.vortex.downloader.util.FormatUtils
import com.vortex.downloader.util.StorageStats

@Composable
fun SettingsScreen(
    prefs: UserPreferences,
    storageStats: StorageStats,
    onThemeChange: (ThemeMode) -> Unit,
    onDefaultVideoQualityChange: (Int) -> Unit,
    onDefaultAudioBitrateChange: (Int) -> Unit,
    onMaxSimultaneousChange: (Int) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onClearCache: () -> String
) {
    val glass = LocalVortexGlass.current
    var cacheMessage by remember { mutableStateOf<String?>(null) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = glass.textPrimary
            )
        }

        // Theme selection
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Appearance", fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = prefs.themeMode == mode,
                                onClick = { onThemeChange(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }
            }
        }

        // Default Video & Audio Quality
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Default Video Quality", fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2160 to "4K", 1440 to "2K", 1080 to "1080p", 720 to "720p").forEach { (h, label) ->
                            FilterChip(
                                selected = prefs.defaultVideoQualityHeight == h,
                                onClick = { onDefaultVideoQualityChange(h) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("Default Audio Bitrate", fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(320, 256, 192).forEach { kbps ->
                            FilterChip(
                                selected = prefs.defaultAudioBitrateKbps == kbps,
                                onClick = { onDefaultAudioBitrateChange(kbps) },
                                label = { Text("$kbps kbps") }
                            )
                        }
                    }
                }
            }
        }

        // Download Behavior & Network
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Simultaneous Downloads: ${prefs.maxSimultaneousDownloads}",
                        fontWeight = FontWeight.SemiBold,
                        color = glass.textPrimary
                    )
                    Slider(
                        value = prefs.maxSimultaneousDownloads.toFloat(),
                        onValueChange = { onMaxSimultaneousChange(it.toInt()) },
                        valueRange = 1f..4f,
                        steps = 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wi-Fi Only Downloads", color = glass.textPrimary, fontWeight = FontWeight.Medium)
                            Text("Pause mobile data downloads", color = glass.textSecondary, fontSize = 12.sp)
                        }
                        Switch(checked = prefs.wifiOnly, onCheckedChange = onWifiOnlyChange)
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Download Notifications", color = glass.textPrimary, fontWeight = FontWeight.Medium)
                            Text("Show active & completed alerts", color = glass.textSecondary, fontSize = 12.sp)
                        }
                        Switch(checked = prefs.notificationsEnabled, onCheckedChange = onNotificationsChange)
                    }
                }
            }
        }

        // Storage & Cache
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Storage Management", fontWeight = FontWeight.SemiBold, color = glass.textPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text("Video folder: ${prefs.videoSubfolder}", color = glass.textSecondary, fontSize = 13.sp)
                    Text("Audio folder: ${prefs.audioSubfolder}", color = glass.textSecondary, fontSize = 13.sp)
                    Text(
                        "Available: ${FormatUtils.formatBytes(storageStats.availableBytes)} • Cache: ${FormatUtils.formatBytes(storageStats.appCacheBytes)}",
                        color = glass.accentCyan,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { cacheMessage = onClearCache() }
                    ) {
                        Text("Clear Temporary Cache")
                    }
                    cacheMessage?.let {
                        Text(it, color = glass.successColor, fontSize = 12.sp)
                    }
                }
            }
        }

        // Privacy Policy & About
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { showPrivacyDialog = true }) { Text("Privacy Policy") }
                    TextButton(onClick = { showAboutDialog = true }) { Text("About VORTEX") }
                }
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Text(
                    "VORTEX Downloader contains zero advertisements, zero ad SDKs, and zero analytics trackers. " +
                        "All download history and preferences stay locally on your device. Network requests are made " +
                        "only to the authorized media URLs you explicitly submit."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("OK") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About VORTEX Downloader") },
            text = {
                Text(
                    "Version ${BuildConfig.VERSION_NAME}\n" +
                        "Built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, WorkManager, and Android Media3.\n" +
                        "Supports authorized open-media sources and direct media streams."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }
}
