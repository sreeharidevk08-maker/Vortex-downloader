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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vortex.downloader.data.local.DownloadEntity
import com.vortex.downloader.data.model.DownloadStatus
import com.vortex.downloader.ui.components.GlassCard
import com.vortex.downloader.ui.theme.LocalVortexGlass
import com.vortex.downloader.util.FormatUtils

private enum class DownloadFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    QUEUED("Queued"),
    COMPLETED("Completed"),
    FAILED("Failed")
}

@Composable
fun DownloadsScreen(
    downloads: List<DownloadEntity>,
    onPause: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onCancel: (Long) -> Unit,
    onRetry: (Long) -> Unit
) {
    val glass = LocalVortexGlass.current
    var filter by remember { mutableStateOf(DownloadFilter.ALL) }

    val filtered = remember(downloads, filter) {
        when (filter) {
            DownloadFilter.ALL -> downloads.filter { it.status != DownloadStatus.CANCELLED }
            DownloadFilter.ACTIVE -> downloads.filter {
                it.status == DownloadStatus.DOWNLOADING ||
                    it.status == DownloadStatus.PAUSED ||
                    it.status == DownloadStatus.CONVERTING
            }
            DownloadFilter.QUEUED -> downloads.filter { it.status == DownloadStatus.QUEUED }
            DownloadFilter.COMPLETED -> downloads.filter { it.status == DownloadStatus.COMPLETED }
            DownloadFilter.FAILED -> downloads.filter { it.status == DownloadStatus.FAILED }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "Download Manager",
            style = MaterialTheme.typography.headlineMedium,
            color = glass.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DownloadFilter.entries) { item ->
                FilterChip(
                    selected = filter == item,
                    onClick = { filter = item },
                    label = { Text(item.label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "No downloads in ${filter.label.lowercase()}.",
                    color = glass.textSecondary,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    DownloadItemCard(
                        item = item,
                        onPause = { onPause(item.id) },
                        onResume = { onResume(item.id) },
                        onCancel = { onCancel(item.id) },
                        onRetry = { onRetry(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    item: DownloadEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    val glass = LocalVortexGlass.current
    val pct = if (item.status == DownloadStatus.CONVERTING) {
        item.conversionProgress
    } else if (item.totalBytes > 0L) {
        ((item.downloadedBytes * 100) / item.totalBytes).toInt().coerceIn(0, 100)
    } else 0
    val remaining = (item.totalBytes - item.downloadedBytes).coerceAtLeast(0L)

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.qualityLabel} • ${item.format} • ${item.status.name}",
                        color = glass.accentCyan,
                        fontSize = 12.sp
                    )
                }

                Row {
                    when (item.status) {
                        DownloadStatus.DOWNLOADING -> {
                            IconButton(onClick = onPause) {
                                Icon(Icons.Rounded.Pause, contentDescription = "Pause", tint = glass.textPrimary)
                            }
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Rounded.Cancel, contentDescription = "Cancel", tint = glass.errorColor)
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = onResume) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume", tint = glass.accentCyan)
                            }
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Rounded.Cancel, contentDescription = "Cancel", tint = glass.errorColor)
                            }
                        }
                        DownloadStatus.QUEUED -> {
                            IconButton(onClick = onCancel) {
                                Icon(Icons.Rounded.Cancel, contentDescription = "Cancel", tint = glass.errorColor)
                            }
                        }
                        DownloadStatus.FAILED -> {
                            IconButton(onClick = onRetry) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Retry", tint = glass.accentBlue)
                            }
                        }
                        else -> Unit
                    }
                }
            }

            if (item.status == DownloadStatus.DOWNLOADING ||
                item.status == DownloadStatus.PAUSED ||
                item.status == DownloadStatus.CONVERTING
            ) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { pct / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (item.status == DownloadStatus.CONVERTING) glass.accentViolet else glass.accentCyan,
                    trackColor = glass.cardBorder
                )
                Spacer(Modifier.height(6.dp))
                val metrics = if (item.status == DownloadStatus.CONVERTING) {
                    "Converting/Muxing: $pct%"
                } else {
                    "$pct% • ${FormatUtils.formatBytes(item.downloadedBytes)} / ${FormatUtils.formatBytes(item.totalBytes)} • " +
                        "Left ${FormatUtils.formatBytes(remaining)} • ${FormatUtils.formatSpeed(item.speedBytesPerSec)} • " +
                        "ETA ${FormatUtils.formatEta(remaining, item.speedBytesPerSec)}"
                }
                Text(metrics, color = glass.textSecondary, fontSize = 11.sp)
            } else if (item.status == DownloadStatus.FAILED && !item.errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(item.errorMessage, color = glass.errorColor, fontSize = 12.sp)
            } else if (item.status == DownloadStatus.COMPLETED) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Saved to ${item.relativePath}/${item.fileName} • ${FormatUtils.formatBytes(item.totalBytes)}",
                    color = glass.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
