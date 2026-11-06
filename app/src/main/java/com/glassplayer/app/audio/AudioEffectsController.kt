package com.glassplayer.app.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import java.util.Locale
import kotlin.math.roundToInt

data class EqualizerBandState(
    val index: Int,
    val label: String,
    val levelDb: Float,
    val minDb: Float,
    val maxDb: Float,
)

data class SoundEffectsSnapshot(
    val equalizerAvailable: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val bands: List<EqualizerBandState> = emptyList(),
    val preset: String = "Flat",
    val bassAvailable: Boolean = false,
    val bassEnabled: Boolean = false,
    val bassStrength: Float = 45f,
    val virtualizerAvailable: Boolean = false,
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Float = 25f,
    val message: String? = null,
)

object AudioEffectsController {
    val presets = listOf("Flat", "Bass Boost", "Treble", "Vocal", "Rock", "Pop", "Jazz", "Classical")

    private var sessionId: Int = -1
    private var preferences: SharedPreferences? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var lastError: String? = null

    @Synchronized
    fun attach(context: Context, requestedSessionId: Int): SoundEffectsSnapshot {
        if (requestedSessionId <= 0) {
            lastError = "The audio session is not ready yet. Start a song and reopen Sound Studio."
            return snapshot()
        }
        if (sessionId == requestedSessionId &&
            (equalizer != null || bassBoost != null || virtualizer != null)
        ) return snapshot()

        releaseEffects()
        sessionId = requestedSessionId
        preferences = context.applicationContext.getSharedPreferences("zmusic_sound", Context.MODE_PRIVATE)
        lastError = null

        try {
            equalizer = Equalizer(0, requestedSessionId).also { effect ->
                val range = effect.bandLevelRange
                for (i in 0 until effect.numberOfBands.toInt()) {
                    val saved = preferences?.getInt("eq.band." + i, 0) ?: 0
                    val level = saved.coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                    runCatching { effect.setBandLevel(i.toShort(), level) }
                }
                effect.enabled = preferences?.getBoolean("eq.enabled", false) ?: false
            }
        } catch (error: Exception) {
            lastError = "This phone does not expose a usable equalizer: " +
                (error.localizedMessage ?: error.javaClass.simpleName)
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, requestedSessionId).also { effect ->
                val savedStrength = preferences?.getInt("bass.strength", 450) ?: 450
                if (effect.strengthSupported) effect.setStrength(savedStrength.coerceIn(0, 1000).toShort())
                effect.enabled = preferences?.getBoolean("bass.enabled", false) ?: false
            }
        } catch (_: Exception) {
            bassBoost = null
        }

        try {
            virtualizer = Virtualizer(0, requestedSessionId).also { effect ->
                val savedStrength = preferences?.getInt("virtualizer.strength", 250) ?: 250
                if (effect.strengthSupported) effect.setStrength(savedStrength.coerceIn(0, 1000).toShort())
                effect.enabled = preferences?.getBoolean("virtualizer.enabled", false) ?: false
            }
        } catch (_: Exception) {
            virtualizer = null
        }
        return snapshot()
    }

    @Synchronized
    fun snapshot(): SoundEffectsSnapshot {
        val eq = equalizer
        val bands = runCatching {
            if (eq == null) emptyList() else {
                val range = eq.bandLevelRange
                (0 until eq.numberOfBands.toInt()).map { i ->
                    val hz = eq.getCenterFreq(i.toShort()) / 1000
                    val label = if (hz >= 1000) String.format(Locale.US, "%.1f kHz", hz / 1000f)
                        else hz.toString() + " Hz"
                    EqualizerBandState(
                        index = i,
                        label = label,
                        levelDb = eq.getBandLevel(i.toShort()) / 100f,
                        minDb = range[0] / 100f,
                        maxDb = range[1] / 100f,
                    )
                }
            }
        }.getOrDefault(emptyList())

        val bass = bassBoost
        val virtual = virtualizer
        return SoundEffectsSnapshot(
            equalizerAvailable = eq != null && bands.isNotEmpty(),
            equalizerEnabled = runCatching { eq?.enabled == true }.getOrDefault(false),
            bands = bands,
            preset = preferences?.getString("eq.preset", "Flat") ?: "Flat",
            bassAvailable = bass != null,
            bassEnabled = runCatching { bass?.enabled == true }.getOrDefault(false),
            bassStrength = (preferences?.getInt("bass.strength", 450) ?: 450) / 10f,
            virtualizerAvailable = virtual != null,
            virtualizerEnabled = runCatching { virtual?.enabled == true }.getOrDefault(false),
            virtualizerStrength = (preferences?.getInt("virtualizer.strength", 250) ?: 250) / 10f,
            message = lastError,
        )
    }

    @Synchronized
    fun setEqualizerEnabled(enabled: Boolean): SoundEffectsSnapshot {
        runCatching { equalizer?.enabled = enabled }
        preferences?.edit()?.putBoolean("eq.enabled", enabled)?.apply()
        return snapshot()
    }

    @Synchronized
    fun setBandLevel(index: Int, levelDb: Float): SoundEffectsSnapshot {
        val eq = equalizer ?: return snapshot()
        runCatching {
            val range = eq.bandLevelRange
            val level = (levelDb * 100f).roundToInt().coerceIn(range[0].toInt(), range[1].toInt())
            eq.setBandLevel(index.toShort(), level.toShort())
            preferences?.edit()?.putInt("eq.band." + index, level)?.putString("eq.preset", "Custom")?.apply()
        }
        return snapshot()
    }

    @Synchronized
    fun applyPreset(name: String): SoundEffectsSnapshot {
        val eq = equalizer ?: return snapshot()
        if (name !in presets) return snapshot()
        runCatching {
            val range = eq.bandLevelRange
            val count = eq.numberOfBands.toInt().coerceAtLeast(1)
            for (i in 0 until count) {
                val x = if (count == 1) 0.5f else i.toFloat() / (count - 1).toFloat()
                val db = presetGain(name, x)
                val level = (db * 100f).roundToInt().coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(i.toShort(), level.toShort())
                preferences?.edit()?.putInt("eq.band." + i, level)?.apply()
            }
            eq.enabled = true
            preferences?.edit()?.putBoolean("eq.enabled", true)?.putString("eq.preset", name)?.apply()
        }
        return snapshot()
    }

    private fun presetGain(name: String, x: Float): Float = when (name) {
        "Bass Boost" -> when { x < 0.20f -> 7f; x < 0.42f -> 3.5f; x > 0.82f -> -1f; else -> 0f }
        "Treble" -> when { x < 0.22f -> -1.5f; x > 0.62f -> 5.5f; else -> 0.5f }
        "Vocal" -> when { x < 0.20f -> -1.5f; x in 0.34f..0.72f -> 4.5f; x > 0.82f -> -1f; else -> 0f }
        "Rock" -> when { x < 0.20f -> 5f; x < 0.40f -> 2f; x < 0.62f -> -1.5f; x > 0.78f -> 4.5f; else -> 1.5f }
        "Pop" -> when { x < 0.22f -> 3f; x in 0.35f..0.60f -> -0.5f; x > 0.70f -> 2.8f; else -> 0.5f }
        "Jazz" -> when { x < 0.22f -> 3f; x in 0.30f..0.65f -> 1.5f; x > 0.70f -> 3f; else -> 0.5f }
        "Classical" -> when { x < 0.20f -> 2f; x > 0.72f -> 2.2f; else -> 0f }
        else -> 0f
    }

    @Synchronized
    fun setBassEnabled(enabled: Boolean): SoundEffectsSnapshot {
        runCatching {
            bassBoost?.let { effect ->
                if (enabled && (preferences?.getInt("bass.strength", 450) ?: 450) == 0) {
                    effect.setStrength(450.toShort())
                    preferences?.edit()?.putInt("bass.strength", 450)?.apply()
                }
                effect.enabled = enabled
            }
        }
        preferences?.edit()?.putBoolean("bass.enabled", enabled)?.apply()
        return snapshot()
    }

    @Synchronized
    fun setBassStrength(value: Float): SoundEffectsSnapshot {
        val strength = (value.coerceIn(0f, 100f) * 10f).roundToInt()
        runCatching {
            bassBoost?.let { effect ->
                if (effect.strengthSupported) effect.setStrength(strength.toShort())
                effect.enabled = strength > 0
            }
        }
        preferences?.edit()?.putInt("bass.strength", strength)?.putBoolean("bass.enabled", strength > 0)?.apply()
        return snapshot()
    }

    @Synchronized
    fun setVirtualizerEnabled(enabled: Boolean): SoundEffectsSnapshot {
        runCatching {
            virtualizer?.let { effect ->
                if (enabled && (preferences?.getInt("virtualizer.strength", 250) ?: 250) == 0) {
                    effect.setStrength(250.toShort())
                    preferences?.edit()?.putInt("virtualizer.strength", 250)?.apply()
                }
                effect.enabled = enabled
            }
        }
        preferences?.edit()?.putBoolean("virtualizer.enabled", enabled)?.apply()
        return snapshot()
    }

    @Synchronized
    fun setVirtualizerStrength(value: Float): SoundEffectsSnapshot {
        val strength = (value.coerceIn(0f, 100f) * 10f).roundToInt()
        runCatching {
            virtualizer?.let { effect ->
                if (effect.strengthSupported) effect.setStrength(strength.toShort())
                effect.enabled = strength > 0
            }
        }
        preferences?.edit()?.putInt("virtualizer.strength", strength)?.putBoolean("virtualizer.enabled", strength > 0)?.apply()
        return snapshot()
    }

    @Synchronized
    fun release() {
        releaseEffects()
        sessionId = -1
        preferences = null
    }

    private fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }
}