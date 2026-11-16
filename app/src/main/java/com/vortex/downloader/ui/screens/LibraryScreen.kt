package com.vortex.downloader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vortex.downloader.data.local.DownloadEntity
import com.vortex.downloader.data.model.DownloadMode
import com.vortex.downloader.data.model.LibrarySort
import com.vortex.downloader.data.model.LibraryTab
import com.vortex.downloader.ui.components.GlassCard
import com.vortex.downloader.ui.theme.LocalVortexGlass
import com.vortex.downloader.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    items: List<DownloadEntity>,
    selectedTab: LibraryTab,
    selectedSort: LibrarySort,
    searchQuery: String,
    onTabChange: (LibraryTab) -> Unit,
    onSortChange: (LibrarySort) -> Unit,
    onSearchChange: (String) -> Unit,
    onPlay: (DownloadEntity) -> Unit,
    onRename: (DownloadEntity, String) -> Unit,
    onDelete: (DownloadEntity) -> Unit
) {
    val glass = LocalVortexGlass.current
    val context = LocalContext.current
    var renameTarget by remember { mutableStateOf<DownloadEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "Media Library",
            style = MaterialTheme.typography.headlineMedium,
            color = glass.textPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        TabRow(selectedTabIndex = selectedTab.ordinal) {
            LibraryTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabChange(tab) },
                    text = { Text(tab.label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search library...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )

        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibrarySort.entries.forEach { sort ->
                FilterChip(
                    selected = selectedSort == sort,
                    onClick = { onSortChange(sort) },
                    label = { Text(sort.label) }
                )
            }
        }

        if (items.isEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "No downloaded media yet.",
                    color = glass.textSecondary,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    var menuOpen by remember { mutableStateOf(false) }
                    val dateStr = remember(item.completedAt) {
                        SimpleDateFormat("MMM dd, yyyy", Locale.US)
                            .format(Date(item.completedAt ?: item.createdAt))
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        onClick = { onPlay(item) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(glass.backgroundSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = item.thumbnailUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (item.mediaType == DownloadMode.VIDEO) Icons.Rounded.VideoFile else Icons.Rounded.AudioFile,
                                        contentDescription = null,
                                        tint = glass.accentCyan
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    color = glass.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${FormatUtils.formatBytes(item.totalBytes)} • ${FormatUtils.formatDuration(item.durationMs)} • $dateStr",
                                    color = glass.textSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            IconButton(onClick = { onPlay(item) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = glass.accentCyan)
                            }

                            Box {
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = glass.textSecondary)
                                }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        onClick = {
                                            menuOpen = false
                                            renameTarget = item
                                            renameText = item.title
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Share") },
                                        onClick = {
                                            menuOpen = false
                                            item.outputUri?.let { uriStr ->
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = if (item.mediaType == DownloadMode.VIDEO) "video/*" else "audio/*"
                                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(uriStr))
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share media"))
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Open with...") },
                                        onClick = {
                                            menuOpen = false
                                            item.outputUri?.let { uriStr ->
                                                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(
                                                        Uri.parse(uriStr),
                                                        if (item.mediaType == DownloadMode.VIDEO) "video/*" else "audio/*"
                                                    )
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                runCatching { context.startActivity(viewIntent) }
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = glass.errorColor) },
                                        onClick = {
                                            menuOpen = false
                                            onDelete(item)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Media") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(target, renameText)
                        renameTarget = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }
}
