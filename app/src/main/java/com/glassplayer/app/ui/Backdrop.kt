package com.glassplayer.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/** Ink with three soft colour glows. All drawn in a single pass. */
@Composable
fun GlassBackdrop(accent: Color, modifier: Modifier = Modifier.fillMaxSize()) {
    val glow by animateColorAsState(accent, tween(900), label = "glow")
    Canvas(modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        drawRect(Glass.Ink)
        drawRect(
            Brush.radialGradient(
                listOf(glow.copy(alpha = 0.42f), Color.Transparent),
                center = Offset(size.width * 0.10f, size.height * 0.05f),
                radius = size.maxDimension * 0.62f,
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(Glass.Coral.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * 0.95f, size.height * 0.78f),
                radius = size.maxDimension * 0.55f,
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(Glass.Violet.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(size.width * 0.8f, size.height * 0.25f),
                radius = size.maxDimension * 0.45f,
            )
        )
    }
}
