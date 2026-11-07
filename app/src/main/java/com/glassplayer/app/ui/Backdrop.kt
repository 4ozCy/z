package com.glassplayer.app.ui

import android.net.Uri
import android.widget.ImageView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView

/** Ink with soft colour glows, optionally layered over a user-selected wallpaper. */
@Composable
fun GlassBackdrop(
    accent: Color,
    modifier: Modifier = Modifier.fillMaxSize(),
    wallpaperUri: Uri? = null,
) {
    val glow by animateColorAsState(accent, tween(900), label = "glow")
    Box(modifier) {
        if (wallpaperUri != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageURI(wallpaperUri)
                    }
                },
                update = { it.setImageURI(wallpaperUri) },
            )
        }
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(if (wallpaperUri == null) Glass.Ink else Color.Black.copy(alpha = 0.48f))
            drawRect(
                Brush.radialGradient(
                    listOf(glow.copy(alpha = 0.075f), Color.Transparent),
                    center = Offset(size.width * 0.10f, size.height * 0.05f),
                    radius = size.maxDimension * 0.48f,
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Glass.Coral.copy(alpha = 0.008f), Color.Transparent),
                    center = Offset(size.width * 0.95f, size.height * 0.78f),
                    radius = size.maxDimension * 0.55f,
                )
            )
            drawRect(
                Brush.radialGradient(
                    listOf(Glass.Violet.copy(alpha = 0.006f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.25f),
                    radius = size.maxDimension * 0.45f,
                )
            )
        }
    }
}
