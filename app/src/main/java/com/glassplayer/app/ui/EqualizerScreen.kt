package com.glassplayer.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.glassplayer.app.PlaybackService
import com.glassplayer.app.audio.AudioEffectsController
import com.glassplayer.app.audio.EqualizerBandState
import java.util.Locale

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(AudioEffectsController.snapshot()) }

    LaunchedEffect(Unit) {
        state = AudioEffectsController.attach(context, PlaybackService.audioSessionId)
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            GlassIconButton(Icons.Rounded.ArrowBack, "Back to player", onBack, size = 36.dp)
            Column(Modifier.weight(1f)) {
                GText("SOUND STUDIO", size = 11.sp, weight = FontWeight.Bold, color = Glass.Aqua)
                GText("Equalizer", size = 23.sp, weight = FontWeight.SemiBold)
                GText(
                    if (state.equalizerAvailable) "Device audio · ${state.bands.size} bands" else "Device audio effects",
                    size = 12.sp,
                    color = Glass.TextLo,
                )
            }
        }

        Column(
            Modifier.fillMaxWidth()
                .background(Color(0xFF151917), RoundedCornerShape(14.dp))
                .border(0.75.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    GText("Equalizer", size = 16.sp, weight = FontWeight.SemiBold)
                    GText(if (state.equalizerEnabled) "Custom sound profile active" else "Bypassed", size = 12.sp, color = Glass.TextLo)
                }
                Switch(
                    checked = state.equalizerEnabled,
                    onCheckedChange = { state = AudioEffectsController.setEqualizerEnabled(it) },
                    enabled = state.equalizerAvailable,
                )
            }

            if (state.equalizerAvailable) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        GText("PRESETS", size = 10.sp, weight = FontWeight.Bold, color = Glass.TextLo)
                        Spacer(Modifier.weight(1f))
                        GText(state.preset.uppercase(Locale.US), size = 10.sp, weight = FontWeight.Bold, color = Glass.Aqua)
                    }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AudioEffectsController.presets.forEach { preset ->
                            PresetChip(preset, state.preset == preset) { state = AudioEffectsController.applyPreset(preset) }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        GText("FREQUENCY RESPONSE", size = 10.sp, weight = FontWeight.Bold, color = Glass.TextLo)
                        Spacer(Modifier.weight(1f))
                        GText("dB", size = 10.sp, color = Glass.TextLo)
                    }
                    EqualizerGraph(state.bands, state.equalizerEnabled)
                }

                Column(
                    Modifier.fillMaxWidth().background(Color(0xFF0D100E), RoundedCornerShape(10.dp)).padding(vertical = 8.dp, horizontal = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        state.bands.forEach { band ->
                            VerticalBandControl(band, state.equalizerEnabled) { value ->
                                state = AudioEffectsController.setBandLevel(band.index, value)
                            }
                        }
                    }
                    GText(
                        if (state.equalizerEnabled) "Drag a fader to shape each frequency band." else "Turn the equalizer on to adjust the bands.",
                        size = 11.sp,
                        color = Glass.TextLo,
                        modifier = Modifier.padding(horizontal = 7.dp).padding(top = 4.dp),
                    )
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GText("Changes save automatically", size = 11.sp, color = Glass.TextLo, modifier = Modifier.weight(1f))
                    Box(
                        Modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                            .clickable { state = AudioEffectsController.applyPreset("Flat") }
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                    ) {
                        GText("Reset flat", size = 12.sp, weight = FontWeight.SemiBold)
                    }
                }
            } else {
                GText(
                    state.message ?: "Start a song and reopen Sound Studio. This device may not expose a system equalizer for the active audio session.",
                    size = 13.sp,
                    color = Glass.TextLo,
                    maxLines = 4,
                )
            }
        }

        if (state.bassAvailable) {
            EffectCard(
                title = "Bass boost",
                subtitle = "Add weight to low frequencies",
                enabled = state.bassEnabled,
                strength = state.bassStrength,
                iconLabel = "BASS",
                onToggle = { state = AudioEffectsController.setBassEnabled(it) },
                onStrength = { state = AudioEffectsController.setBassStrength(it) },
            )
        }
        if (state.virtualizerAvailable) {
            EffectCard(
                title = "Stereo widening",
                subtitle = "Widen the stereo image on supported outputs",
                enabled = state.virtualizerEnabled,
                strength = state.virtualizerStrength,
                iconLabel = "STEREO",
                onToggle = { state = AudioEffectsController.setVirtualizerEnabled(it) },
                onStrength = { state = AudioEffectsController.setVirtualizerStrength(it) },
            )
        }

        GText(
            "Sound processing is provided by Android’s audio engine. Available effects vary by device and output route.",
            size = 11.sp,
            color = Glass.TextLo,
            maxLines = 3,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun PresetChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.background(
            if (selected) Glass.Aqua.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.035f),
            shape,
        )
            .border(1.dp, if (selected) Glass.Aqua.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.09f), shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        GText(name, size = 12.sp, weight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) Glass.Aqua else Glass.TextLo)
    }
}

@Composable
private fun EqualizerGraph(bands: List<EqualizerBandState>, enabled: Boolean) {
    Canvas(Modifier.fillMaxWidth().height(84.dp)) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        val rangeMin = bands.minOfOrNull { it.minDb } ?: -15f
        val rangeMax = bands.maxOfOrNull { it.maxDb } ?: 15f
        val range = (rangeMax - rangeMin).coerceAtLeast(1f)
        for (i in 0..4) {
            val y = top + (bottom - top) * i / 4f
            val lineColor = if (i == 2) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.075f)
            drawLine(lineColor, Offset(left, y), Offset(right, y), strokeWidth = 1.dp.toPx())
        }
        if (bands.isEmpty()) return@Canvas
        val points = bands.mapIndexed { index, band ->
            val x = if (bands.size == 1) (left + right) / 2f else left + (right - left) * index / (bands.size - 1).toFloat()
            val normalized = ((band.levelDb.coerceIn(rangeMin, rangeMax) - rangeMin) / range).coerceIn(0f, 1f)
            Offset(x, bottom - normalized * (bottom - top))
        }
        val area = Path().apply {
            moveTo(points.first().x, bottom)
            lineTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, bottom)
            close()
        }
        drawPath(
            area,
            Brush.verticalGradient(
                listOf(Glass.Aqua.copy(alpha = if (enabled) 0.18f else 0.03f), Color.Transparent),
                startY = top,
                endY = bottom,
            ),
        )
        val curveColor = if (enabled) Glass.Aqua else Glass.TextLo.copy(alpha = 0.5f)
        points.zipWithNext().forEach { (a, b) -> drawLine(curveColor, a, b, strokeWidth = 2.2.dp.toPx()) }
        points.forEach { point ->
            drawCircle(curveColor.copy(alpha = 0.22f), 6.dp.toPx(), point)
            drawCircle(curveColor, 3.dp.toPx(), point)
        }
    }
}

@Composable
private fun VerticalBandControl(
    band: EqualizerBandState,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    val change by rememberUpdatedState(onValueChange)
    val valueText = String.format(Locale.US, "%+.1f", band.levelDb)
    Column(
        Modifier.width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        GText(valueText, size = 10.sp, weight = FontWeight.SemiBold, color = if (enabled) Glass.Aqua else Glass.TextLo)
        Canvas(
            Modifier.width(32.dp).height(104.dp)
                .semantics {
                    contentDescription = "${band.label} equalizer band"
                    stateDescription = "$valueText dB"
                    progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(
                        band.levelDb.coerceIn(band.minDb, band.maxDb),
                        band.minDb..band.maxDb,
                    )
                }
                .pointerInput(enabled, band.index, band.minDb, band.maxDb) {
                    if (!enabled) return@pointerInput
                    val pad = 10.dp.toPx()
                    fun update(y: Float) {
                        val usable = (size.height - pad * 2f).coerceAtLeast(1f)
                        val fraction = (1f - ((y - pad) / usable)).coerceIn(0f, 1f)
                        change(band.minDb + (band.maxDb - band.minDb) * fraction)
                    }
                    detectTapGestures { update(it.y) }
                }
                .pointerInput(enabled, band.index, band.minDb, band.maxDb) {
                    if (!enabled) return@pointerInput
                    val pad = 10.dp.toPx()
                    fun update(y: Float) {
                        val usable = (size.height - pad * 2f).coerceAtLeast(1f)
                        val fraction = (1f - ((y - pad) / usable)).coerceIn(0f, 1f)
                        change(band.minDb + (band.maxDb - band.minDb) * fraction)
                    }
                    detectDragGestures(
                        onDragStart = { update(it.y) },
                        onDrag = { pointer, _ ->
                            pointer.consume()
                            update(pointer.position.y)
                        },
                    )
                },
        ) {
            val centerX = size.width / 2f
            val top = 8.dp.toPx()
            val bottom = size.height - 8.dp.toPx()
            val usable = (bottom - top).coerceAtLeast(1f)
            val zero = ((0f - band.minDb) / (band.maxDb - band.minDb).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val zeroY = bottom - usable * zero
            val valueFraction = ((band.levelDb.coerceIn(band.minDb, band.maxDb) - band.minDb) /
                (band.maxDb - band.minDb).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val valueY = bottom - usable * valueFraction
            drawLine(Color.White.copy(alpha = 0.16f), Offset(centerX, top), Offset(centerX, bottom), strokeWidth = 3.dp.toPx())
            drawLine(Color.White.copy(alpha = 0.25f), Offset(centerX - 7.dp.toPx(), zeroY), Offset(centerX + 7.dp.toPx(), zeroY), strokeWidth = 1.dp.toPx())
            drawLine(if (enabled) Glass.Aqua else Glass.TextLo.copy(alpha = 0.45f), Offset(centerX, zeroY), Offset(centerX, valueY), strokeWidth = 4.dp.toPx())
            drawCircle(Color(0xFF171D19), 7.dp.toPx(), Offset(centerX, valueY))
            drawCircle(if (enabled) Glass.Aqua else Glass.TextLo, 5.dp.toPx(), Offset(centerX, valueY))
        }
        GText(band.label, size = 9.sp, color = Glass.TextLo, maxLines = 1)
    }
}

@Composable
private fun EffectCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    strength: Float,
    iconLabel: String,
    onToggle: (Boolean) -> Unit,
    onStrength: (Float) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFF111513), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).background(Glass.Aqua.copy(alpha = 0.07f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                GText(iconLabel, size = 9.sp, weight = FontWeight.Bold, color = Glass.Aqua)
            }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                GText(title, size = 14.sp, weight = FontWeight.Medium)
                GText(subtitle, size = 10.sp, color = Glass.TextLo)
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GText("INTENSITY", size = 10.sp, weight = FontWeight.Bold, color = Glass.TextLo)
            Spacer(Modifier.weight(1f))
            GText("${strength.toInt()}%", size = 12.sp, weight = FontWeight.SemiBold)
        }
        Slider(value = strength.coerceIn(0f, 100f), onValueChange = onStrength, valueRange = 0f..100f, enabled = enabled)
    }
}
