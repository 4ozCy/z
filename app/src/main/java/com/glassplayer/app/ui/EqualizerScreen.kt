package com.glassplayer.app.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassplayer.app.PlaybackService
import com.glassplayer.app.audio.AudioEffectsController
import com.glassplayer.app.audio.SoundEffectsSnapshot
import java.util.Locale

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(AudioEffectsController.snapshot()) }
    LaunchedEffect(Unit) {
        state = AudioEffectsController.attach(context, PlaybackService.audioSessionId)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassIconButton(Icons.Rounded.ArrowBack, "Back to player", onBack, size = 42.dp)
            Column(Modifier.weight(1f)) {
                GText("Sound Studio", size = 24.sp, weight = FontWeight.ExtraBold)
                GText("Shape your sound", size = 12.sp, color = Glass.TextLo)
            }
            GlassIconButton(Icons.Rounded.GraphicEq, "Equalizer", {}, size = 42.dp, tint = Glass.Aqua)
        }

        Column(
            Modifier.fillMaxWidth().glassCard(RoundedCornerShape(16.dp), Glass.Aqua, 1.25f).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    GText("Equalizer", size = 16.sp, weight = FontWeight.Bold)
                    GText(
                        if (state.equalizerAvailable) state.bands.size.toString() + "-band audio engine"
                        else "Device audio effect",
                        size = 12.sp,
                        color = Glass.TextLo,
                    )
                }
                GlassPillButton(
                    if (state.equalizerEnabled) "On" else "Off",
                    Icons.Rounded.Tune,
                    { state = AudioEffectsController.setEqualizerEnabled(!state.equalizerEnabled) },
                    tint = if (state.equalizerEnabled) Glass.Aqua else Glass.TextLo,
                )
            }
            if (state.equalizerAvailable) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    AudioEffectsController.presets.forEach { preset ->
                        GlassPillButton(
                            preset,
                            Icons.Rounded.MusicNote,
                            { state = AudioEffectsController.applyPreset(preset) },
                            tint = if (state.preset == preset) Glass.Aqua else Color.White,
                        )
                    }
                }
                GText("Frequency bands · dB", size = 12.sp, color = Glass.TextLo)
                state.bands.forEach { band ->
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            GText(band.label, size = 13.sp, weight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            GText(
                                String.format(Locale.US, "%+.1f dB", band.levelDb),
                                size = 12.sp,
                                color = if (band.levelDb > 0.1f) Glass.Aqua else Glass.TextLo,
                            )
                        }
                        Slider(
                            value = band.levelDb.coerceIn(band.minDb, band.maxDb),
                            onValueChange = { state = AudioEffectsController.setBandLevel(band.index, it) },
                            valueRange = band.minDb..band.maxDb,
                            enabled = state.equalizerEnabled,
                        )
                    }
                }
                GlassPillButton(
                    "Reset to flat",
                    Icons.Rounded.Tune,
                    { state = AudioEffectsController.applyPreset("Flat") },
                    tint = Color.White,
                )
            } else {
                GText(
                    state.message ?: "The phone did not expose an equalizer for this audio session. Other sound controls may still be available.",
                    size = 13.sp,
                    color = Glass.TextLo,
                    maxLines = 4,
                )
            }
        }

        if (state.bassAvailable) {
            Column(
                Modifier.fillMaxWidth().glassCard(RoundedCornerShape(24.dp), Glass.Violet, 1.1f).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        GText("Bass boost", size = 16.sp, weight = FontWeight.Bold)
                        GText("Add weight to the low end", size = 12.sp, color = Glass.TextLo)
                    }
                    GlassPillButton(
                        if (state.bassEnabled) "On" else "Off",
                        Icons.Rounded.GraphicEq,
                        { state = AudioEffectsController.setBassEnabled(!state.bassEnabled) },
                        tint = if (state.bassEnabled) Glass.Aqua else Glass.TextLo,
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GText("Strength", size = 12.sp, color = Glass.TextLo)
                    Spacer(Modifier.weight(1f))
                    GText(state.bassStrength.toInt().toString() + "%", size = 12.sp)
                }
                Slider(
                    value = state.bassStrength,
                    onValueChange = { state = AudioEffectsController.setBassStrength(it) },
                    valueRange = 0f..100f,
                )
            }
        }

        if (state.virtualizerAvailable) {
            Column(
                Modifier.fillMaxWidth().glassCard(RoundedCornerShape(24.dp), Glass.Aqua, 1.1f).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        GText("Stereo widening", size = 16.sp, weight = FontWeight.Bold)
                        GText("Create a wider headphone image", size = 12.sp, color = Glass.TextLo)
                    }
                    GlassPillButton(
                        if (state.virtualizerEnabled) "On" else "Off",
                        Icons.Rounded.SurroundSound,
                        { state = AudioEffectsController.setVirtualizerEnabled(!state.virtualizerEnabled) },
                        tint = if (state.virtualizerEnabled) Glass.Aqua else Glass.TextLo,
                    )
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GText("Width", size = 12.sp, color = Glass.TextLo)
                    Spacer(Modifier.weight(1f))
                    GText(state.virtualizerStrength.toInt().toString() + "%", size = 12.sp)
                }
                Slider(
                    value = state.virtualizerStrength,
                    onValueChange = { state = AudioEffectsController.setVirtualizerStrength(it) },
                    valueRange = 0f..100f,
                )
            }
        }

        Column(
            Modifier.fillMaxWidth().glassCard(RoundedCornerShape(16.dp), strength = 0.9f).padding(14.dp),
        ) {
            GText("Sound settings are saved on this device. Availability depends on the phone's audio-effect engine.", size = 12.sp, color = Glass.TextLo, maxLines = 3)
        }
    }
}