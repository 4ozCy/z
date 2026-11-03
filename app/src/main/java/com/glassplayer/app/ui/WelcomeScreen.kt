package com.glassplayer.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    val motion = rememberInfiniteTransition(label = "welcomeMotion")
    val rotation by motion.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "recordTilt",
    )
    val pulse by motion.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "recordPulse",
    )

    Box(Modifier.fillMaxSize().background(Glass.Ink), contentAlignment = Alignment.Center) {
        GlassBackdrop(Glass.Violet)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier
                    .size(196.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .background(
                        Brush.radialGradient(listOf(Glass.Aqua.copy(alpha = 0.35f), Glass.Violet.copy(alpha = 0.28f), Glass.Ink)),
                        CircleShape,
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(116.dp)
                        .background(Brush.linearGradient(listOf(Glass.Coral, Glass.Violet, Glass.Aqua)), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = "Music equalizer", tint = Glass.TextHi, modifier = Modifier.size(58.dp))
                }
            }
            Spacer(Modifier.height(40.dp))
            GText(
                "Welcome to Zmusic",
                size = 34.sp,
                weight = FontWeight.ExtraBold,
                tracking = (-1).sp,
                align = TextAlign.Center,
                maxLines = 2,
            )
            GText(
                "Your music. Your moment.",
                size = 16.sp,
                color = Glass.TextLo,
                modifier = Modifier.padding(top = 10.dp),
                align = TextAlign.Center,
            )
            GText(
                "Made by: @4zcy",
                size = 15.sp,
                color = Glass.Aqua,
                weight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 22.dp),
                align = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))
            GlassPillButton("Let's listen", Icons.Rounded.GraphicEq, onContinue, tint = Glass.Aqua)
        }
    }
}
