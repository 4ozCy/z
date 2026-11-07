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
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val Ink = Color(0xFF0B1018)
    val Aqua = Color(0xFFFF7048)
    val Blue = Color(0xFF78BCD1)
    val Violet = Color(0xFFB7472D)
    val Coral = Color(0xFFF3B36A)
    val TextHi = Color(0xFFF5F1E8)
    val TextLo = Color(0xFFA0AABA)
}

/** Bright on the top-left and bottom-right edges, nearly clear between: the "light catching glass" rim. */
val RimBrush: Brush = Brush.linearGradient(
    0f to Color.White.copy(alpha = 0.20f),
    0.30f to Color.White.copy(alpha = 0.07f),
    0.70f to Color.White.copy(alpha = 0.035f),
    1f to Color.White.copy(alpha = 0.14f),
)

/** Frosted card for surfaces that sit on the already-blurred backdrop. No live blur, so it is cheap in lists. */
fun Modifier.glassCard(shape: Shape, tint: Color = Color.White, strength: Float = 1f): Modifier =
    this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    tint.copy(alpha = (0.075f * strength).coerceAtMost(1f)),
                    tint.copy(alpha = (0.035f * strength).coerceAtMost(1f)),
                )
            )
        )
        .border(1.dp, Color.White.copy(alpha = 0.105f), shape)

/** Frosted fill for the floating dock. A dark base keeps text readable, a light sheen and rim make it glass. */
fun Modifier.dockGlass(shape: Shape, tint: Color = Color.White): Modifier =
    this
        .clip(shape)
        .background(Glass.Ink.copy(alpha = 0.86f))
        .background(
            Brush.verticalGradient(
                listOf(tint.copy(alpha = 0.12f), tint.copy(alpha = 0.035f))
            )
        )
        .border(1.dp, Color.White.copy(alpha = 0.13f), shape)

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
            .then(if (filled) Modifier.glassCard(RoundedCornerShape(10.dp), glassTint) else Modifier.clip(RoundedCornerShape(10.dp)))
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
            .height(40.dp)
            .glassCard(RoundedCornerShape(9.dp), tint, 0.9f)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Glass.TextHi, modifier = Modifier.size(18.dp))
        GText(text, size = 14.sp, weight = FontWeight.SemiBold)
    }
}
