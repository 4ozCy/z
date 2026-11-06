package com.glassplayer.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassplayer.app.PlaybackUi
import com.glassplayer.app.data.Song
import kotlin.math.abs

private data class NavTab(val label: String, val icon: ImageVector)

private val NavTabs = listOf(
    NavTab("Songs", Icons.Rounded.MusicNote),
    NavTab("Albums", Icons.Rounded.Album),
    NavTab("Artists", Icons.Rounded.Person),
    NavTab("Search", Icons.Rounded.Search),
    NavTab("Player", Icons.Rounded.PlayArrow),
)

/** Floating bottom control: a mini player that docks above a pill of tabs, both made of live glass. */
@Composable
fun Dock(
    tab: Int,
    onTab: (Int) -> Unit,
    current: Song?,
    ui: PlaybackUi,
    position: State<Long>,
    accent: Color,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (current != null) {
            MiniPlayer(current, ui, position, accent, onTogglePlay, onNext, onOpenPlayer)
        }
        NavPill(tab, accent, onTab)
    }
}

@Composable
private fun MiniPlayer(
    song: Song,
    ui: PlaybackUi,
    position: State<Long>,
    accent: Color,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit,
) {
    val shape = RoundedCornerShape(33.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(66.dp)
            .dockGlass(shape, accent)
            .clickable(onClick = onOpen)
            .drawBehind {
                val dur = ui.durationMs
                if (dur > 0) {
                    val p = (position.value.toFloat() / dur).coerceIn(0f, 1f)
                    val x0 = 26.dp.toPx()
                    val w = size.width - 2 * x0
                    val y = size.height - 7.dp.toPx()
                    val h = 2.dp.toPx()
                    drawRoundRect(Color.White.copy(alpha = 0.14f), Offset(x0, y), Size(w, h), CornerRadius(h / 2))
                    drawRoundRect(accent, Offset(x0, y), Size(w * p, h), CornerRadius(h / 2))
                }
            }
            .padding(start = 10.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Artwork(song, Modifier.size(46.dp), RoundedCornerShape(15.dp))
        Column(Modifier.weight(1f)) {
            GText(song.title, size = 15.sp, weight = FontWeight.SemiBold)
            GText(song.artist, size = 11.sp, color = Glass.TextLo)
        }
        PlayPauseButton(ui.isPlaying, onTogglePlay, Modifier.size(44.dp))
        GlassIconButton(
            Icons.Rounded.SkipNext, "Next song", onNext,
            size = 42.dp, iconSize = 26.dp, filled = false,
        )
    }
}

@Composable
fun PlayPauseButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier
            .glassCard(androidx.compose.foundation.shape.CircleShape, Color.White, 1.2f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = playing, label = "playPause") { isPlaying ->
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Glass.TextHi,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun NavPill(
    selected: Int,
    accent: Color,
    onSelect: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    // The lens slides on an under-damped spring, so it overshoots and settles like a drop of liquid.
    val pos = remember { Animatable(selected.toFloat()) }
    LaunchedEffect(selected) {
        pos.animateTo(selected.toFloat(), spring(dampingRatio = 0.52f, stiffness = 240f))
    }
    val shape = RoundedCornerShape(34.dp)

    Row(
        Modifier
            .fillMaxWidth()
            .height(68.dp)
            .dockGlass(shape)
            .drawBehind {
                val itemW = size.width / NavTabs.size
                val inset = 6.dp.toPx()
                val stretch = abs(selected - pos.value).coerceIn(0f, 1f)
                val w = (itemW - inset * 2) * (1f + 0.30f * stretch)
                val h = (size.height - inset * 2) * (1f - 0.06f * stretch)
                val left = (pos.value + 0.5f) * itemW - w / 2f
                val top = (size.height - h) / 2f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.40f), Color.White.copy(alpha = 0.12f)),
                        startY = top,
                        endY = top + h,
                    ),
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h / 2f),
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.5f)),
                        start = Offset(left, top),
                        end = Offset(left + w, top + h),
                    ),
                    topLeft = Offset(left, top),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h / 2f),
                    style = Stroke(width = 1.2.dp.toPx()),
                )
            },
    ) {
        NavTabs.forEachIndexed { i, t ->
            val active = i == selected
            val color by animateColorAsState(
                if (active) Glass.TextHi else Glass.TextLo,
                label = "tabColor",
            )
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (!active) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(i)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(t.icon, contentDescription = t.label, tint = color, modifier = Modifier.size(24.dp))
                GText(
                    t.label,
                    size = 11.sp,
                    weight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = color,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
