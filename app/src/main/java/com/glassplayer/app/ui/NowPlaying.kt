package com.glassplayer.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import android.net.Uri
import com.glassplayer.app.PlaybackUi
import com.glassplayer.app.data.Song

@Composable
fun NowPlaying(
    song: Song,
    ui: PlaybackUi,
    position: State<Long>,
    accent: Color,
    onClose: () -> Unit,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    playbackSpeed: Float,
    onSetPlaybackSpeed: (Float) -> Unit,
    sleepTimerRemainingMs: Long,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenQueue: () -> Unit,
    wallpaperUri: Uri? = null,
) {
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    val glow by animateColorAsState(accent, tween(700), label = "nowPlayingAccent")

    Box(Modifier.fillMaxSize()) {
        if (wallpaperUri != null) GlassBackdrop(accent, wallpaperUri = wallpaperUri)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 12.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlassIconButton(
                    Icons.Rounded.KeyboardArrowDown, "Close player", onClose,
                    modifier = Modifier.align(Alignment.CenterStart), size = 38.dp,
                )
                GText("NOW SPINNING", size = 10.sp, weight = FontWeight.Bold, color = Glass.TextLo, tracking = 1.2.sp)
                GlassIconButton(
                    Icons.Rounded.QueueMusic, "Up next queue", onOpenQueue,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    size = 34.dp, iconSize = 18.dp, filled = false,
                )
            }

            BoxWithConstraints(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                val side = minOf(maxWidth, maxHeight, 310.dp)
                val shape = RoundedCornerShape(6.dp)
                Box(
                    Modifier
                        .size(side)
                        .border(1.dp, Color.White.copy(alpha = 0.10f), shape),
                ) {
                    Artwork(song, Modifier.fillMaxSize(), shape)
                }
            }

            GText("SIDE A  /  LOCAL LIBRARY", size = 9.sp, weight = FontWeight.Bold, color = glow, tracking = 1.0.sp, modifier = Modifier.padding(top = 4.dp))

            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    GText(song.title, size = 23.sp, weight = FontWeight.Bold, maxLines = 2, tracking = (-0.4).sp)
                    GText(song.artist, size = 13.sp, color = Glass.TextLo, modifier = Modifier.padding(top = 2.dp))
                }
                GlassIconButton(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    if (isFavorite) "Remove from liked songs" else "Add to liked songs",
                    onToggleFavorite,
                    size = 38.dp,
                    iconSize = 20.dp,
                    tint = if (isFavorite) Glass.Aqua else Glass.TextLo,
                    filled = false,
                )
            }
            SeekSection(position, ui.durationMs, glow, onSeek)

            Row(
                Modifier.fillMaxWidth().padding(top = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    Icons.Rounded.Shuffle, "Shuffle", onShuffle,
                    size = 42.dp, iconSize = 21.dp,
                    tint = if (ui.shuffle) glow else Glass.TextLo, filled = ui.shuffle,
                )
                GlassIconButton(
                    Icons.Rounded.SkipPrevious, "Previous song", onPrevious,
                    size = 46.dp, iconSize = 28.dp, filled = false,
                )
                BigPlayButton(ui.isPlaying, glow, onTogglePlay)
                GlassIconButton(
                    Icons.Rounded.SkipNext, "Next song", onNext,
                    size = 46.dp, iconSize = 28.dp, filled = false,
                )
                GlassIconButton(
                    if (ui.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    "Repeat", onRepeat,
                    size = 48.dp, iconSize = 24.dp,
                    tint = if (ui.repeatMode == Player.REPEAT_MODE_OFF) Glass.TextLo else glow,
                    filled = ui.repeatMode != Player.REPEAT_MODE_OFF,

                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color(0xFF161F2A)).border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerActionItem(
                    title = "Equalizer",
                    icon = Icons.Rounded.Equalizer,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenEqualizer,
                )
                PlayerActionItem(
                    title = "Sleep",
                    subtitle = if (sleepTimerRemainingMs > 0L) formatSleepCountdown(sleepTimerRemainingMs) else "",
                    icon = Icons.Rounded.Timer,
                    modifier = Modifier.weight(1f),
                    onClick = { showSleepTimer = true },
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    PlayerActionItem(
                        title = "Speed",
                        subtitle = playbackSpeed.toString().removeSuffix(".0") + "×",
                        icon = Icons.Rounded.Speed,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showSpeedMenu = true },
                    )
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false },
                    ) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (speed == 1f) "Normal · 1.0×"
                                        else speed.toString().removeSuffix(".0") + "×",
                                    )
                                },
                                onClick = {
                                    onSetPlaybackSpeed(speed)
                                    showSpeedMenu = false
                                },
                            )
                        }
                    }
                }
                PlayerActionItem(
                    title = "Queue",
                    icon = Icons.Rounded.QueueMusic,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenQueue,
                )
            }
        }

        if (showSleepTimer) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp)
                        .clip(RoundedCornerShape(12.dp)).background(Color(0xFF161F2A)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            GText("Sleep timer", size = 20.sp, weight = FontWeight.Bold)
                            GText("Pause playback automatically", size = 12.sp, color = Glass.TextLo)
                        }
                        GlassIconButton(Icons.Rounded.KeyboardArrowDown, "Close timer", { showSleepTimer = false }, size = 38.dp)
                    }
                    if (sleepTimerRemainingMs > 0L) {
                        GText("Active · " + formatSleepCountdown(sleepTimerRemainingMs) + " remaining", size = 12.sp, color = glow)
                    }
                    listOf(5, 10, 15, 30, 45, 60).chunked(3).forEach { minuteRow ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            minuteRow.forEach { minutes ->
                                GlassPillButton(
                                    minutes.toString() + " min",
                                    Icons.Rounded.Timer,
                                    {
                                        onStartSleepTimer(minutes)
                                        showSleepTimer = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    tint = glow,
                                )
                            }
                        }
                    }
                    if (sleepTimerRemainingMs > 0L) {
                        GlassPillButton(
                            "Cancel timer",
                            Icons.Rounded.Close,
                            {
                                onCancelSleepTimer()
                                showSleepTimer = false
                            },
                            tint = Glass.Coral,
                        )
                    }
                }
            }
        }
    }
}

private fun formatSleepCountdown(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}

@Composable
private fun PlayerActionItem(
    title: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = title, tint = Color(0xFFFF7048), modifier = Modifier.size(20.dp))
        GText(title, size = 10.sp, weight = FontWeight.Medium, color = Glass.TextHi)
        if (subtitle.isNotEmpty()) GText(subtitle, size = 9.sp, color = Glass.TextLo)
    }
}

@Composable
private fun SeekSection(position: State<Long>, durationMs: Long, accent: Color, onSeek: (Long) -> Unit) {
    val pos by position
    Column(Modifier.padding(top = 10.dp)) {
        GlassSeekBar(
            progress = if (durationMs > 0) pos.toFloat() / durationMs else 0f,
            accent = accent,
            onSeek = { f -> onSeek((f * durationMs).toLong()) },
        )
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            GText(formatTime(pos), size = 13.sp, color = Glass.TextLo)
            GText(formatTime(durationMs), size = 13.sp, color = Glass.TextLo)
        }
    }
}

@Composable
private fun BigPlayButton(playing: Boolean, accent: Color, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        spring(dampingRatio = 0.45f, stiffness = 500f),
        label = "playPress",
    )
    Box(
        Modifier
            .size(60.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.76f))), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = playing, label = "bigPlayPause") { isPlaying ->
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
fun GlassSeekBar(progress: Float, accent: Color, onSeek: (Float) -> Unit, modifier: Modifier = Modifier) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val seek by rememberUpdatedState(onSeek)
    val shown = (if (dragging) dragValue else progress).coerceIn(0f, 1f)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { o -> seek((o.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        dragging = true
                        dragValue = (o.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        dragging = false
                        seek(dragValue)
                    },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        dragValue = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                )
            },
    ) {
        val trackH = (if (dragging) 6.dp else 4.dp).toPx()
        val cy = size.height / 2f
        val top = cy - trackH / 2f
        drawRoundRect(Color.White.copy(alpha = 0.14f), Offset(0f, top), Size(size.width, trackH), CornerRadius(trackH / 2f))
        drawRoundRect(
            color = accent,
            topLeft = Offset(0f, top),
            size = Size((size.width * shown).coerceAtLeast(trackH), trackH),
            cornerRadius = CornerRadius(trackH / 2f),
        )
        val r = (if (dragging) 10.dp else 7.dp).toPx()
        val cx = (size.width * shown).coerceIn(r, size.width - r)
        drawCircle(Color.White.copy(alpha = 0.96f), r, Offset(cx, cy))
        drawCircle(accent.copy(alpha = 0.6f), r * 0.55f, Offset(cx, cy))
    }
}
