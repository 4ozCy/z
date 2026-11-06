package com.glassplayer.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.glassplayer.app.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

private val artworkCache = object : LruCache<Long, Bitmap>(12 * 1024 * 1024) {
    override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
}

fun songHue(song: Song?): Float {
    val key = song?.album?.takeIf { it.isNotBlank() } ?: song?.artist.orEmpty()
    return 142f + (key.hashCode().absoluteValue % 26).toFloat()
}

fun accentFor(song: Song?): Color = Glass.Aqua

@Composable
fun Artwork(
    song: Song?,
    modifier: Modifier = Modifier,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
) {
    val context = LocalContext.current
    var bitmap by remember(song?.id) {
        mutableStateOf(song?.id?.let { artworkCache.get(it) })
    }

    LaunchedEffect(song?.id, song?.uri) {
        val selectedSong = song ?: return@LaunchedEffect
        if (bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                loadEmbeddedArtwork(context.applicationContext, selectedSong)
            }
            if (loaded != null) {
                artworkCache.put(selectedSong.id, loaded)
                bitmap = loaded
            }
        }
    }

    val hue = songHue(song)
    Box(
        modifier.clip(shape).background(
            Brush.linearGradient(
                listOf(
                    Color.hsv(hue, 0.20f, 0.24f),
                    Color.hsv((hue + 10f) % 360f, 0.24f, 0.12f),
                )
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        val art = bitmap
        if (art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = "Album artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.62f),
                modifier = Modifier.fillMaxSize(0.38f),
            )
        }
    }
}

private fun loadEmbeddedArtwork(context: Context, song: Song): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, song.uri)
        val picture = retriever.embeddedPicture ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(picture, 0, picture.size, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) {
            sample *= 2
        }
        BitmapFactory.decodeByteArray(
            picture,
            0,
            picture.size,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            },
        )
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

@Composable
fun EqBars(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width / 5f
        floatArrayOf(0.55f, 1f, 0.7f).forEachIndexed { i, v ->
            val h = size.height * v
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(i * 2 * w, size.height - h),
                size = androidx.compose.ui.geometry.Size(w, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2f),
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

fun songCount(n: Int): String = "$n song" + if (n == 1) "" else "s"
