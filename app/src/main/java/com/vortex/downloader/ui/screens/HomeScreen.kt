package com.vortex.downloader.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vortex.downloader.data.local.DownloadEntity
import com.vortex.downloader.data.model.DownloadMode
import com.vortex.downloader.data.model.DownloadStatus
import com.vortex.downloader.data.model.SourceAnalysisResult
import com.vortex.downloader.ui.components.GlassCard
import com.vortex.downloader.ui.components.VortexLogo
import com.vortex.downloader.ui.theme.LocalVortexGlass
import com.vortex.downloader.ui.viewmodel.HomeUiState
import com.vortex.downloader.util.FormatUtils
import com.vortex.downloader.util.StorageStats

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    recentDownloads: List<DownloadEntity>,
    storageStats: StorageStats,
    onUrlChange: (String) -> Unit,
    onClearUrl: () -> Unit,
    onModeChange: (DownloadMode) -> Unit,
    onAnalyse: () -> Unit,
    onSelectVideoQuality: (com.vortex.downloader.data.model.VideoQualityOption) -> Unit,
    onSelectAudioBitrate: (com.vortex.downloader.data.model.AudioBitrateOption) -> Unit,
    onDownload: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLibraryItem: (DownloadEntity) -> Unit
) {
    val glass = LocalVortexGlass.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VortexLogo(size = 40.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "VORTEX",
                            style = MaterialTheme.typography.headlineMedium,
                            color = glass.textPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Authorized Media Downloader",
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.textSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(glass.cardFillTop)
                        .border(1.dp, glass.cardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = glass.textPrimary
                    )
                }
            }
        }

        // URL Input & Mode Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    OutlinedTextField(
                        value = state.urlInput,
                        onValueChange = onUrlChange,
                        placeholder = {
                            Text(
                                "Paste authorized media or Archive.org URL",
                                color = glass.textSecondary,
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = glass.accentCyan,
                            unfocusedBorderColor = glass.cardBorder,
                            focusedTextColor = glass.textPrimary,
                            unfocusedTextColor = glass.textPrimary,
                            cursorColor = glass.accentCyan
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onAnalyse() }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = cm.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                                if (clip.isNotBlank()) onUrlChange(clip)
                            }
                        ) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Paste")
                        }

                        if (state.urlInput.isNotEmpty()) {
                            TextButton(onClick = onClearUrl) {
                                Icon(Icons.Rounded.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = onAnalyse,
                            enabled = !state.isAnalysing && state.urlInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = glass.accentBlue)
                        ) {
                            if (state.isAnalysing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Analyse")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Video / Audio mode switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DownloadMode.entries.forEach { mode ->
                            val selected = state.mode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) {
                                            Brush.horizontalGradient(listOf(glass.accentBlue, glass.accentViolet))
                                        } else {
                                            Brush.horizontalGradient(listOf(glass.cardFillBottom, glass.cardFillBottom))
                                        }
                                    )
                                    .border(1.dp, if (selected) Color.Transparent else glass.cardBorder, RoundedCornerShape(14.dp))
                                    .clickable { onModeChange(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (mode == DownloadMode.VIDEO) Icons.Rounded.VideoFile else Icons.Rounded.AudioFile,
                                        contentDescription = null,
                                        tint = if (selected) Color.White else glass.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (mode == DownloadMode.VIDEO) "Video (MP4)" else "Audio (MP3)",
                                        color = if (selected) Color.White else glass.textSecondary,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Analysed Source Card or Restriction Notice
        item {
            AnimatedVisibility(visible = state.analysisResult != null, enter = fadeIn(), exit = fadeOut()) {
                when (val res = state.analysisResult) {
                    is SourceAnalysisResult.Success -> {
                        val src = res.source
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(glass.backgroundSecondary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (src.thumbnailUrl != null) {
                                            AsyncImage(
                                                model = src.thumbnailUrl,
                                                contentDescription = src.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (state.mode == DownloadMode.VIDEO) Icons.Rounded.VideoFile else Icons.Rounded.AudioFile,
                                                contentDescription = null,
                                                tint = glass.accentCyan,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = src.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = glass.textPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!src.artist.isNullOrBlank()) {
                                            Text(
                                                text = src.artist,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = glass.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "${src.sourceName} • ${FormatUtils.formatDuration(src.durationMs)} • ${src.availableFormats.joinToString("/")}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = glass.accentCyan
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                if (state.mode == DownloadMode.VIDEO) {
                                    Text(
                                        text = "Available Video Qualities",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = glass.textSecondary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    if (src.videoQualities.isEmpty()) {
                                        Text(
                                            "No video streams available from this source. Switch to Audio mode.",
                                            color = glass.errorColor,
                                            fontSize = 13.sp
                                        )
                                    } else {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            src.videoQualities.forEach { q ->
                                                val chosen = state.selectedVideoQuality == q
                                                FilterChip(
                                                    selected = chosen,
                                                    onClick = { onSelectVideoQuality(q) },
                                                    label = {
                                                        Text("${q.label} • ${FormatUtils.formatBytes(q.estimatedBytes)}")
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = glass.accentBlue.copy(alpha = 0.28f),
                                                        selectedLabelColor = glass.textPrimary
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "Available Audio Bitrates",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = glass.textSecondary
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    if (src.audioBitrates.isEmpty()) {
                                        Text(
                                            "No audio streams available from this source.",
                                            color = glass.errorColor,
                                            fontSize = 13.sp
                                        )
                                    } else {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            src.audioBitrates.forEach { b ->
                                                val chosen = state.selectedAudioBitrate == b
                                                FilterChip(
                                                    selected = chosen,
                                                    onClick = { onSelectAudioBitrate(b) },
                                                    label = {
                                                        Text("${b.bitrateKbps} kbps ${b.format} • ${FormatUtils.formatBytes(b.estimatedBytes)}")
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = glass.accentViolet.copy(alpha = 0.28f),
                                                        selectedLabelColor = glass.textPrimary
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = onDownload,
                                    enabled = (state.mode == DownloadMode.VIDEO && state.selectedVideoQuality != null) ||
                                        (state.mode == DownloadMode.AUDIO && state.selectedAudioBitrate != null),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = glass.accentViolet),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Rounded.Download, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Download to ${if (state.mode == DownloadMode.VIDEO) "Movies/VORTEX" else "Music/VORTEX"}")
                                }
                                state.feedbackMessage?.let { msg ->
                                    Spacer(Modifier.height(8.dp))
                                    Text(msg, color = glass.accentCyan, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    is SourceAnalysisResult.Restricted -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = glass.errorColor)
                                Column {
                                    Text("Source Restricted", color = glass.errorColor, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(res.reason, color = glass.textPrimary, fontSize = 13.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(res.suggestion, color = glass.textSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    is SourceAnalysisResult.Error -> {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.Info, contentDescription = null, tint = glass.errorColor)
                                Text(res.message, color = glass.textPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                    null -> Unit
                }
            }
        }

        // Stats & Storage Summary
        item {
            val completedCount = recentDownloads.count { it.status == DownloadStatus.COMPLETED }
            val activeCount = recentDownloads.count {
                it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassCard(modifier = Modifier.weight(1f), cornerRadius = 20.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Downloads", color = glass.textSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "$completedCount saved",
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "$activeCount active",
                            color = glass.accentCyan,
                            fontSize = 12.sp
                        )
                    }
                }
                GlassCard(modifier = Modifier.weight(1f), cornerRadius = 20.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Storage, contentDescription = null, tint = glass.accentViolet, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Free Storage", color = glass.textSecondary, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatBytes(storageStats.availableBytes),
                            color = glass.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "of ${FormatUtils.formatBytes(storageStats.totalBytes)}",
                            color = glass.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Recent Completed Downloads
        val completed = recentDownloads.filter { it.status == DownloadStatus.COMPLETED }.take(5)
        if (completed.isNotEmpty()) {
            item {
                Text(
                    text = "Recently Downloaded",
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textPrimary
                )
            }
            items(completed, key = { it.id }) { item ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    onClick = { onOpenLibraryItem(item) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (item.mediaType == DownloadMode.VIDEO) Icons.Rounded.VideoFile else Icons.Rounded.AudioFile,
                            contentDescription = null,
                            tint = glass.accentBlue
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = glass.textPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.qualityLabel} • ${item.format} • ${FormatUtils.formatBytes(item.totalBytes)}",
                                color = glass.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
