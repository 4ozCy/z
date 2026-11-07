package com.glassplayer.app

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.glassplayer.app.data.MusicRepository
import com.glassplayer.app.data.Song
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

fun hasAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED

data class PlaybackUi(
    val currentId: Long? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1f,
)

data class QueueState(
    val songs: List<Song> = emptyList(),
    val currentIndex: Int = -1,
)

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _ui = MutableStateFlow(PlaybackUi())
    val ui: StateFlow<PlaybackUi> = _ui.asStateFlow()

    /** Kept separate from [ui] so a ticking position only redraws the bits that show it. */
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _queue = MutableStateFlow(QueueState())
    val queue: StateFlow<QueueState> = _queue.asStateFlow()

    private val _sleepTimerRemainingMs = MutableStateFlow(0L)
    val sleepTimerRemainingMs: StateFlow<Long> = _sleepTimerRemainingMs.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var ticker: Job? = null
    private var mediaItems: Map<Long, MediaItem> = emptyMap()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync()

        /**
         * Recover from a broken/unavailable media item instead of leaving the
         * player stuck after ExoPlayer reports an error.
         */
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val c = controller ?: return
            if (c.mediaItemCount == 0) {
                sync()
                return
            }

            val currentIndex = c.currentMediaItemIndex
            val nextIndex = when {
                currentIndex in 0 until c.mediaItemCount - 1 -> currentIndex + 1
                c.repeatMode == Player.REPEAT_MODE_ALL && c.mediaItemCount > 1 -> 0
                else -> C.INDEX_UNSET
            }

            if (nextIndex != C.INDEX_UNSET) {
                runCatching {
                    c.seekToDefaultPosition(nextIndex)
                    c.prepare()
                    c.play()
                }
            }
            sync()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            val c = controller ?: return
            // Repeat ALL should keep the queue moving even if the transition
            // occasionally fails to advance on its own.
            if (c.mediaItemCount > 1 && c.repeatMode == Player.REPEAT_MODE_ALL) {
                val nextIndex = if (c.currentMediaItemIndex in 0 until c.mediaItemCount - 1) {
                    c.currentMediaItemIndex + 1
                } else {
                    0
                }
                runCatching {
                    c.seekToDefaultPosition(nextIndex)
                    c.prepare()
                    c.play()
                }
            }
            sync()
        }
    }
    init {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(listener)
                val savedSpeed = ctx.getSharedPreferences("zmusic_prefs", Context.MODE_PRIVATE)
                    .getFloat("playbackSpeed", 1f)
                if (c.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) {
                    runCatching { c.setPlaybackSpeed(savedSpeed.coerceIn(0.5f, 2.0f)) }
                }
                sync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    fun refresh() {
        val ctx = getApplication<Application>()
        if (!hasAudioPermission(ctx)) return
        viewModelScope.launch {
            val list = MusicRepository.loadSongs(ctx)
            mediaItems = withContext(Dispatchers.Default) { list.associate { it.id to it.toMediaItem() } }
            _songs.value = list
            _loaded.value = true
            sync()
        }
    }

    private fun sync() {
        val c = controller ?: return
        val id = c.currentMediaItem?.mediaId?.toLongOrNull()
        val duration = c.duration.takeIf { it != C.TIME_UNSET && it > 0 }
            ?: _songs.value.firstOrNull { it.id == id }?.durationMs
            ?: 0L
        val wantsToPlay = c.playWhenReady &&
            c.playbackState != Player.STATE_IDLE &&
            c.playbackState != Player.STATE_ENDED
        _ui.value = PlaybackUi(
            currentId = id,
            isPlaying = wantsToPlay,
            durationMs = duration,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            playbackSpeed = c.playbackParameters.speed,
        )
        _position.value = c.currentPosition.coerceAtLeast(0L)
        val songsById = _songs.value.associateBy { it.id }
        val queueSongs = (0 until c.mediaItemCount).mapNotNull { index ->
            c.getMediaItemAt(index).mediaId.toLongOrNull()?.let(songsById::get)
        }
        _queue.value = QueueState(
            songs = queueSongs,
            currentIndex = if (c.mediaItemCount > 0) c.currentMediaItemIndex else -1,
        )
        if (c.isPlaying) startTicker() else ticker?.cancel()
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (isActive) {
                val c = controller ?: break
                _position.value = c.currentPosition.coerceAtLeast(0L)
                delay(250)
            }
        }
    }

    // ---- actions -----------------------------------------------------------------------------

    fun playQueue(list: List<Song>, index: Int) {
        val c = controller ?: return
        if (list.isEmpty()) return
        c.setMediaItems(list.map { mediaItems[it.id] ?: it.toMediaItem() }, index.coerceIn(0, list.lastIndex), 0L)
        c.prepare()
        c.play()
    }

    fun playShuffled(list: List<Song>) {
        val c = controller ?: return
        if (list.isEmpty()) return
        c.shuffleModeEnabled = true
        playQueue(list, list.indices.random())
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.playWhenReady && c.playbackState != Player.STATE_ENDED) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0L)
            c.play()
        }
    }

    fun next() { controller?.seekToNext() }

    fun previous() { controller?.seekToPrevious() }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _position.value = ms
    }

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val c = controller ?: return
        val safeSpeed = speed.coerceIn(0.5f, 2.0f)
        if (c.isCommandAvailable(Player.COMMAND_SET_SPEED_AND_PITCH)) {
            runCatching { c.setPlaybackSpeed(safeSpeed) }
                .onSuccess {
                    getApplication<Application>()
                        .getSharedPreferences("zmusic_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .putFloat("playbackSpeed", safeSpeed)
                        .apply()
                    sync()
                }
        }
    }

    fun startSleepTimer(minutes: Int) {
        if (minutes <= 0) return
        sleepTimerJob?.cancel()
        val endAt = System.currentTimeMillis() + minutes * 60_000L
        sleepTimerJob = viewModelScope.launch {
            while (isActive) {
                val remaining = (endAt - System.currentTimeMillis()).coerceAtLeast(0L)
                _sleepTimerRemainingMs.value = remaining
                if (remaining <= 0L) {
                    controller?.pause()
                    _sleepTimerRemainingMs.value = 0L
                    break
                }
                delay(minOf(1_000L, remaining))
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = 0L
    }

    fun playQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        runCatching {
            c.seekToDefaultPosition(index)
            c.play()
        }
        sync()
    }

    fun moveQueueItem(from: Int, to: Int) {
        val c = controller ?: return
        if (from !in 0 until c.mediaItemCount || to !in 0 until c.mediaItemCount || from == to) return
        if (!c.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) return
        runCatching { c.moveMediaItem(from, to) }
        sync()
    }

    fun removeQueueItem(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        if (!c.isCommandAvailable(Player.COMMAND_CHANGE_MEDIA_ITEMS)) return
        runCatching { c.removeMediaItem(index) }
        sync()
    }

    override fun onCleared() {
        ticker?.cancel()
        sleepTimerJob?.cancel()
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}

private fun Song.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build()
        )
        .build()
