package com.vortex.downloader.ui.screens

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.vortex.downloader.data.local.DownloadEntity
import com.vortex.downloader.data.model.DownloadMode
import com.vortex.downloader.ui.components.GlassCard
import com.vortex.downloader.ui.theme.LocalVortexGlass
import com.vortex.downloader.util.FormatUtils
import kotlinx.coroutines.delay

@Composable
fun MediaPlayerModal(
    item: DownloadEntity,
    onDismiss: () -> Unit
) {
    val glass = LocalVortexGlass.current
    val context = LocalContext.current

    val player = remember(item.id) {
        ExoPlayer.Builder(context).build().apply {
            item.outputUri?.let { uri ->
                setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
                prepare()
                playWhenReady = true
            }
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(item.durationMs.coerceAtLeast(1L)) }
    var volume by remember { mutableFloatStateOf(1f) }
    var speed by remember { mutableFloatStateOf(1f) }
    var fullscreen by remember { mutableStateOf(false) }

    LaunchedEffect(player) {
        while (true) {
            isPlaying = player.isPlaying
            positionMs = player.currentPosition.coerceAtLeast(0L)
            if (player.duration > 0) durationMs = player.duration
            delay(300L)
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !fullscreen)
    ) {
        GlassCard(
            modifier = if (fullscreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
            cornerRadius = if (fullscreen) 0.dp else 24.dp
        ) {
            Column(
                modifier = Modifier
                    .background(glass.backgroundBase.copy(alpha = 0.92f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        color = glass.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.mediaType == DownloadMode.VIDEO) {
                        IconButton(onClick = { fullscreen = !fullscreen }) {
                            Icon(
                                imageVector = if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = "Toggle fullscreen",
                                tint = glass.textPrimary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = glass.textPrimary)
                    }
                }

                if (item.mediaType == DownloadMode.VIDEO) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (fullscreen) Modifier.weight(1f) else Modifier.aspectRatio(16f / 9f))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    useController = false
                                    this.player = player
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(glass.backgroundSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${item.qualityLabel} • ${item.format}",
                            color = glass.accentCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Seek bar
                Slider(
                    value = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
                    onValueChange = { ratio ->
                        val target = (ratio * durationMs).toLong()
                        player.seekTo(target)
                        positionMs = target
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(FormatUtils.formatDuration(positionMs), color = glass.textSecondary, fontSize = 12.sp)
                    Text(FormatUtils.formatDuration(durationMs), color = glass.textSecondary, fontSize = 12.sp)
                }

                // Play/Pause + Volume
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (player.isPlaying) player.pause() else player.play()
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = glass.accentCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = glass.textSecondary)
                    Slider(
                        value = volume,
                        onValueChange = {
                            volume = it
                            player.volume = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Playback speed
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { rate ->
                        FilterChip(
                            selected = speed == rate,
                            onClick = {
                                speed = rate
                                player.playbackParameters = PlaybackParameters(rate)
                            },
                            label = { Text("${rate}x", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}
