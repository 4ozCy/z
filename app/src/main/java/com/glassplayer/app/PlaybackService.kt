package com.glassplayer.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.UnstableApi
import com.glassplayer.app.audio.AudioEffectsController
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns the player so music keeps going with the screen off.
 * Media3 builds the notification and lock-screen controls from the session.
 */
class PlaybackService : MediaSessionService() {

    companion object {
        @Volatile
        var audioSessionId: Int = AudioManager.ERROR
            private set
    }

    private var session: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply { repeatMode = Player.REPEAT_MODE_ALL } // never falls silent at the end of the queue

        val generatedSession = runCatching {
            (getSystemService(Context.AUDIO_SERVICE) as AudioManager).generateAudioSessionId()
        }.getOrDefault(AudioManager.ERROR)
        audioSessionId = generatedSession
        if (generatedSession > 0) {
            runCatching { player.setAudioSessionId(generatedSession) }
                .onFailure { audioSessionId = AudioManager.ERROR }
        }

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player).setSessionActivity(openApp).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    // Swiping Zmusic out of recents stops the music, like most players.
    override fun onTaskRemoved(rootIntent: Intent?) {
        session?.player?.pause()
        stopSelf()
    }

    override fun onDestroy() {
        AudioEffectsController.release()
        audioSessionId = AudioManager.ERROR
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}
