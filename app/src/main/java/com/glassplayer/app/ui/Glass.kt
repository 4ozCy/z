package com.glassplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Glass {
    val Ink = Color(0xFF060912)
    val Aqua = Color(0xFF6FE3FF)
    val Violet = Color(0xFF7B6CFF)
    val Coral = Color(0xFFFF7AA2)
    val TextHi = Color(0xFFF5F8FF)
    val TextLo = Color(0xFFA3AEC6)
}

/** Bright on the top-left and bottom-right edges, nearly clear between: the "light catching glass" rim. */
val RimBrush: Brush = Brush.linearGradient(
    0f to Color.White.copy(alpha = 0.75f),
    0.30f to Color.White.copy(alpha = 0.12f),
    0.70f to Color.White.copy(alpha = 0.06f),
    1f to Color.White.copy(alpha = 0.45f),
)

/** Frosted card for surfaces that sit on the already-blurred backdrop. No live blur, so it is cheap in lists. */
fun Modifier.glassCard(shape: Shape, tint: Color = Color.White, strength: Float = 1f): Modifier =
    this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    tint.copy(alpha = (0.16f * strength).coerceAtMost(1f)),
                    tint.copy(alpha = (0.05f * strength).coerceAtMost(1f)),
                )
            )
        )
        .border(1.dp, RimBrush, shape)

/** Frosted fill for the floating dock. A dark base keeps text readable, a light sheen and rim make it glass. */
fun Modifier.dockGlass(shape: Shape, tint: Color = Color.White): Modifier =
    this
        .clip(shape)
        .background(Glass.Ink.copy(alpha = 0.64f))
        .background(
            Brush.verticalGradient(
                listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.07f))
            )
        )
        .border(1.dp, RimBrush, shape)

@Composable
fun GText(
    text: String,
    modifier: Modifier = Modifier,
    size: TextUnit = 16.sp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Glass.TextHi,
    maxLines: Int = 1,
    tracking: TextUnit = TextUnit.Unspecified,
    align: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size,
        fontWeight = weight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        letterSpacing = tracking,
        textAlign = align,
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = size * 0.5f,
    tint: Color = Glass.TextHi,
    glassTint: Color = Color.White,
    filled: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .then(if (filled) Modifier.glassCard(CircleShape, glassTint) else Modifier.clip(CircleShape))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun GlassPillButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
) {
    Row(
        modifier
            .height(38.dp)
            .glassCard(CircleShape, tint, 1.3f)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Glass.TextHi, modifier = Modifier.size(17.dp))
        GText(text, size = 13.sp, weight = FontWeight.SemiBold)
    }
}
