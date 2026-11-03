package com.glassplayer.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.glassplayer.app.data.Song
import kotlin.math.absoluteValue

/** No images are loaded anywhere: every song gets a colour from its album/artist name instead. */
fun songHue(song: Song?): Float {
    if (song == null) return 200f
    val key = if (song.albumId > 0) song.album else song.artist
    return (key.hashCode().absoluteValue % 360).toFloat()
}

fun accentFor(song: Song?): Color =
    if (song == null) Glass.Aqua else Color.hsv(songHue(song), 0.55f, 1f)

/** A plain colour tile with a music note. Drawn directly as a rounded rect, with no clip layer. */
@Composable
fun Artwork(
    song: Song?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
) {
    val hue = songHue(song)
    Box(
        modifier.background(
            Brush.linearGradient(
                listOf(Color.hsv(hue, 0.55f, 0.78f), Color.hsv((hue + 40f) % 360f, 0.65f, 0.42f))
            ),
            shape,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.78f),
            modifier = Modifier.fillMaxSize(0.45f),
        )
    }
}

/** Three still bars marking the song that is playing. Static on purpose: a looping animation
 *  makes the whole screen redraw every frame, which is exactly what causes stutter. */
@Composable
fun EqBars(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp)) {
        val w = size.width / 5f
        floatArrayOf(0.55f, 1f, 0.7f).forEachIndexed { i, v ->
            val h = size.height * v
            drawRoundRect(
                color = color,
                topLeft = Offset(i * 2 * w, size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(w / 2f),
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

fun songCount(n: Int): String = "$n song" + if (n == 1) "" else "s"
