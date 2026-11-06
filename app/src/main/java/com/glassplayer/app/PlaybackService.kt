package com.glassplayer.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.ByteArrayOutputStream
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.glassplayer.app.audio.AudioEffectsController
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var artworkJob: Job? = null
    private var artworkListener: Player.Listener? = null

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

        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                loadNotificationArtwork(player, mediaItem)
            }
        }
        artworkListener = listener
        player.addListener(listener)

        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player).setSessionActivity(openApp).build()
    }

    @OptIn(UnstableApi::class)
    private fun loadNotificationArtwork(player: ExoPlayer, item: MediaItem?) {
        if (item == null || item.mediaMetadata.artworkData != null) return
        val uri = item.localConfiguration?.uri ?: return
        val mediaId = item.mediaId
        artworkJob?.cancel()
        artworkJob = serviceScope.launch {
            val artwork = withContext(Dispatchers.IO) { readEmbeddedArtwork(uri) } ?: return@launch
            val index = player.currentMediaItemIndex
            val current = player.currentMediaItem
            if (current == null || current.mediaId != mediaId || index < 0 ||
                current.mediaMetadata.artworkData != null
            ) return@launch

            val old = current.mediaMetadata
            val metadata = MediaMetadata.Builder()
                .setTitle(old.title)
                .setDisplayTitle(old.displayTitle)
                .setArtist(old.artist)
                .setAlbumTitle(old.albumTitle)
                .setAlbumArtist(old.albumArtist)
                .setArtworkData(artwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                .build()
            player.replaceMediaItem(index, current.buildUpon().setMediaMetadata(metadata).build())
        }
    }

    private fun readEmbeddedArtwork(uri: Uri): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(this, uri)
            val raw = retriever.embeddedPicture ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2
            val bitmap = BitmapFactory.decodeByteArray(
                raw,
                0,
                raw.size,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                },
            ) ?: return null
            try {
                ByteArrayOutputStream().use { output ->
                    if (bitmap.compress(Bitmap.CompressFormat.JPEG, 86, output)) output.toByteArray() else null
                }
            } finally {
                bitmap.recycle()
            }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    // Swiping Zmusic out of recents stops the music, like most players.
    override fun onTaskRemoved(rootIntent: Intent?) {
        session?.player?.pause()
        stopSelf()
    }

    override fun onDestroy() {
        artworkJob?.cancel()
        artworkListener?.let { listener -> session?.player?.removeListener(listener) }
        serviceScope.cancel()
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
