package com.glassplayer.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
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
) {
    val glow by animateColorAsState(accent, tween(700), label = "nowPlayingAccent")

    Box(Modifier.fillMaxSize()) {
        GlassBackdrop(accent)

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 26.dp, end = 26.dp, top = 8.dp, bottom = 20.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlassIconButton(
                    Icons.Rounded.KeyboardArrowDown, "Close player", onClose,
                    modifier = Modifier.align(Alignment.CenterStart), size = 44.dp,
                )
                GText("Now playing", size = 15.sp, weight = FontWeight.Medium, color = Glass.TextLo)
            }

            // The one big moment: the tile breathes between paused and playing.
            BoxWithConstraints(
                Modifier.weight(1f).fillMaxWidth().padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                val side = minOf(maxWidth, maxHeight)
                val scale by animateFloatAsState(
                    if (ui.isPlaying) 1f else 0.88f,
                    spring(dampingRatio = 0.5f, stiffness = 260f),
                    label = "artScale",
                )
                val shape = RoundedCornerShape(38.dp)
                Box(
                    Modifier
                        .size(side)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .border(1.5.dp, RimBrush, shape),
                ) {
                    Artwork(song, Modifier.fillMaxSize(), shape)
                }
            }

            GText(song.title, size = 28.sp, weight = FontWeight.Bold, maxLines = 2, tracking = (-0.6).sp)
            GText(song.artist, size = 17.sp, color = Glass.TextLo, modifier = Modifier.padding(top = 4.dp))

            SeekSection(position, ui.durationMs, glow, onSeek)

            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(
                    Icons.Rounded.Shuffle, "Shuffle", onShuffle,
                    size = 48.dp, iconSize = 24.dp,
                    tint = if (ui.shuffle) glow else Glass.TextLo, filled = ui.shuffle,
                )
                GlassIconButton(
                    Icons.Rounded.SkipPrevious, "Previous song", onPrevious,
                    size = 60.dp, iconSize = 36.dp, filled = false,
                )
                BigPlayButton(ui.isPlaying, glow, onTogglePlay)
                GlassIconButton(
                    Icons.Rounded.SkipNext, "Next song", onNext,
                    size = 60.dp, iconSize = 36.dp, filled = false,
                )
                GlassIconButton(
                    if (ui.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    "Repeat", onRepeat,
                    size = 48.dp, iconSize = 24.dp,
                    tint = if (ui.repeatMode == Player.REPEAT_MODE_OFF) Glass.TextLo else glow,
                    filled = ui.repeatMode != Player.REPEAT_MODE_OFF,
                )
            }
        }
    }
}

@Composable
private fun SeekSection(position: State<Long>, durationMs: Long, accent: Color, onSeek: (Long) -> Unit) {
    val pos by position
    Column(Modifier.padding(top = 20.dp)) {
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
            .size(84.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .glassCard(CircleShape, accent, 2.2f)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = playing, label = "bigPlayPause") { isPlaying ->
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Glass.TextHi,
                modifier = Modifier.size(42.dp),
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
            .height(32.dp)
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
        val trackH = 8.dp.toPx()
        val cy = size.height / 2f
        val top = cy - trackH / 2f
        drawRoundRect(Color.White.copy(alpha = 0.14f), Offset(0f, top), Size(size.width, trackH), CornerRadius(trackH / 2f))
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(accent, Color.White)),
            topLeft = Offset(0f, top),
            size = Size((size.width * shown).coerceAtLeast(trackH), trackH),
            cornerRadius = CornerRadius(trackH / 2f),
        )
        val r = (if (dragging) 13.dp else 10.dp).toPx()
        val cx = (size.width * shown).coerceIn(r, size.width - r)
        drawCircle(Color.White.copy(alpha = 0.96f), r, Offset(cx, cy))
        drawCircle(accent.copy(alpha = 0.6f), r * 0.55f, Offset(cx, cy))
    }
}
