package com.glassplayer.app.ui

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.glassplayer.app.PlayerViewModel
import com.glassplayer.app.data.Song
import com.glassplayer.app.hasAudioPermission
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

@Composable
fun GlassPlayerApp(vm: PlayerViewModel) {
    val context = LocalContext.current
    val songs by vm.songs.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val ui by vm.ui.collectAsState()
    val positionState = vm.position.collectAsState()
    val queueState by vm.queue.collectAsState()
    val sleepTimerRemainingMs by vm.sleepTimerRemainingMs.collectAsState()
    val prefs = remember(context) { context.getSharedPreferences("zmusic_prefs", Context.MODE_PRIVATE) }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sortMode by rememberSaveable { mutableIntStateOf(prefs.getInt("sortMode", 0)) }
    var favoriteIds by remember(prefs) {
        mutableStateOf(
            prefs.getStringSet("favoriteSongIds", emptySet())
                .orEmpty()
                .mapNotNull { it.toLongOrNull() }
                .toSet()
        )
    }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var recentSongIds by remember(prefs) {
        mutableStateOf(
            prefs.getString("recentSongIds", "")
                .orEmpty()
                .split(',')
                .mapNotNull { it.toLongOrNull() }
                .take(20)
        )
    }
    var lastLibraryTab by rememberSaveable { mutableIntStateOf(1) }
    var secondaryReturnTab by rememberSaveable { mutableIntStateOf(4) }
    var renameDraft by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var pendingRename by remember { mutableStateOf<Pair<Song, String>?>(null) }

    val renamePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val request = pendingRename
        pendingRename = null
        if (result.resultCode == android.app.Activity.RESULT_OK && request != null) {
            if (renameSong(context, request.first, request.second)) {
                vm.refresh()
                Toast.makeText(context, "File renamed", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Could not rename this file", Toast.LENGTH_LONG).show()
            }
        }
    }

    val current = songs.firstOrNull { it.id == ui.currentId }
    val renameTarget = current ?: songs.firstOrNull()
    val recentSongIdsSet = remember(recentSongIds) { recentSongIds.toSet() }
    val recentSongs = remember(songs, recentSongIds) {
        recentSongIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }
    }
    LaunchedEffect(ui.currentId) {
        val id = ui.currentId
        if (id != null) {
            val updated = (listOf(id) + recentSongIds.filterNot { it == id }).take(20)
            if (updated != recentSongIds) {
                recentSongIds = updated
                prefs.edit().putString("recentSongIds", updated.joinToString(",")).apply()
            }
        }
    }
    val selectLibraryTab: (Int) -> Unit = { next ->
        lastLibraryTab = next
        tab = next
    }
    val openPlayer: () -> Unit = {
        if (tab in 0..3) lastLibraryTab = tab
        tab = 4
    }
    val albums = remember(songs) { albumsOf(songs) }
    val artists = remember(songs) { artistsOf(songs) }
    val songsList = rememberLazyListState()
    val albumsGrid = rememberLazyGridState()
    val accent = Glass.Aqua

    val density = LocalDensity.current
    var dockHeight by remember { mutableStateOf(0.dp) }
    val topInset: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val pad = remember(topInset, dockHeight) {
        PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topInset + 10.dp,
            bottom = dockHeight + 20.dp,
        )
    }

    val onRenameCurrent: () -> Unit = {
        renameTarget?.let { song ->
            renameDraft = song.title
            showRenameDialog = true
        }
    }

    val onDeleteCurrent: () -> Unit = {
        if (current != null) {
            val deleted = deleteCurrentSong(context, current)
            if (deleted) {
                favoriteIds = favoriteIds - current.id
                prefs.edit().putStringSet("favoriteSongIds", favoriteIds.map { it.toString() }.toSet()).apply()
                vm.refresh()
            }
        }
    }

    val onToggleFavorite: (Song) -> Unit = { song ->
        val updated = if (song.id in favoriteIds) favoriteIds - song.id else favoriteIds + song.id
        favoriteIds = updated
        prefs.edit().putStringSet("favoriteSongIds", updated.map { it.toString() }.toSet()).apply()
    }

    val onExtractAudio: (String) -> Unit = { url ->
        if (url.isNotBlank()) requestAudioExtraction(context, url) { vm.refresh() }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080D15), Color(0xFF111722), Color(0xFF161F2A))
                )
            )
    ) {

        when (tab) {
            0 -> HomeScreen(
                songs = songs,
                recentSongs = recentSongs,
                loaded = loaded,
                ui = ui,
                accent = accent,
                pad = pad,
                favoriteIds = favoriteIds,
                onOpenSongs = { selectLibraryTab(1) },
                onOpenAlbums = { selectLibraryTab(2) },
                onOpenArtists = { selectLibraryTab(3) },
                onOpenLiked = { favoritesOnly = true; selectLibraryTab(1) },
                onOpenEqualizer = { secondaryReturnTab = 0; tab = 5 },
                onPlay = vm::playQueue,
            )
            1 -> SongsScreen(
                songs = songs,
                loaded = loaded,
                ui = ui,
                accent = accent,
                state = songsList,
                pad = pad,
                onPlay = vm::playQueue,
                onShuffle = vm::playShuffled,
                favoriteIds = favoriteIds,
                favoritesOnly = favoritesOnly,
                onFavoritesOnlyChange = { favoritesOnly = it },
                onToggleFavorite = onToggleFavorite,
                recentSongIds = recentSongIdsSet,
            )
            2 -> AlbumsScreen(
                albums = albums,
                state = albumsGrid,
                pad = pad,
                onOpen = { group -> vm.playQueue(group.songs, 0) },
            )
            3 -> ArtistsScreen(
                artists = artists,
                state = songsList,
                pad = pad,
                onOpen = { group -> vm.playQueue(group.songs, 0) },
            )
            4 -> {
                if (current != null) {
                    NowPlaying(
                        song = current,
                        ui = ui,
                        position = positionState,
                        accent = accent,
                        onClose = { tab = lastLibraryTab },
                        onTogglePlay = vm::togglePlay,
                        onPrevious = vm::previous,
                        onNext = vm::next,
                        onSeek = vm::seekTo,
                        onShuffle = vm::toggleShuffle,
                        onRepeat = vm::cycleRepeat,
                        isFavorite = current.id in favoriteIds,
                        onToggleFavorite = { onToggleFavorite(current) },
                        playbackSpeed = ui.playbackSpeed,
                        onSetPlaybackSpeed = vm::setPlaybackSpeed,
                        sleepTimerRemainingMs = sleepTimerRemainingMs,
                        onStartSleepTimer = vm::startSleepTimer,
                        onCancelSleepTimer = vm::cancelSleepTimer,
                        onOpenEqualizer = { secondaryReturnTab = 4; tab = 5 },
                        onOpenQueue = { secondaryReturnTab = 4; tab = 6 },
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyNote("Nothing playing", "Choose a song from your library to open the player.")
                    }
                }
            }
            5 -> EqualizerScreen(onBack = { tab = secondaryReturnTab })
            6 -> QueueScreen(
                queue = queueState.songs,
                currentIndex = queueState.currentIndex,
                onBack = { tab = 4 },
                onPlay = vm::playQueueItem,
                onMove = vm::moveQueueItem,
                onRemove = vm::removeQueueItem,
            )
            else -> HomeScreen(
                songs = songs,
                recentSongs = recentSongs,
                loaded = loaded,
                ui = ui,
                accent = accent,
                pad = pad,
                favoriteIds = favoriteIds,
                onOpenSongs = { selectLibraryTab(1) },
                onOpenAlbums = { selectLibraryTab(2) },
                onOpenArtists = { selectLibraryTab(3) },
                onOpenLiked = { favoritesOnly = true; selectLibraryTab(1) },
                onOpenEqualizer = { secondaryReturnTab = 0; tab = 5 },
                onPlay = vm::playQueue,
            )
        }

        if (tab !in 4..6 || current == null) Dock(
            tab = if (tab in 0..3) tab else 0,
            onTab = selectLibraryTab,
            current = current,
            ui = ui,
            position = positionState,
            accent = accent,
            onTogglePlay = vm::togglePlay,
            onNext = vm::next,
            onOpenPlayer = { if (current != null) openPlayer() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { dockHeight = with(density) { it.height.toDp() } },
        )

        if (!loaded && songs.isEmpty() && !hasAudioPermission(context)) {
            PermissionScreen(onGrant = vm::refresh)
        }
    }

    val dialogSong = renameTarget
    if (showRenameDialog && dialogSong != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { GText("Rename file", size = 20.sp) },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    label = { Text("New file name") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = renameDraft.trim()
                        if (newName.isNotEmpty()) {
                            showRenameDialog = false
                            pendingRename = dialogSong to newName
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val request = MediaStore.createWriteRequest(context.contentResolver, listOf(dialogSong.uri))
                                    renamePermissionLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                } else {
                                    try {
                                        if (renameSong(context, dialogSong, newName)) {
                                            vm.refresh()
                                            Toast.makeText(context, "File renamed", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Could not rename this file", Toast.LENGTH_LONG).show()
                                        }
                                        pendingRename = null
                                    } catch (e: RecoverableSecurityException) {
                                        renamePermissionLauncher.launch(
                                            IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                pendingRename = null
                                Toast.makeText(context, "Could not rename this file: ${e.localizedMessage ?: "permission denied"}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            },
        )
    }
}

private fun renameSong(context: Context, song: Song, requestedName: String): Boolean {
    val displayName = context.contentResolver.query(
        song.uri,
        arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        ?: song.uri.lastPathSegment ?: return false

    val trimmed = requestedName.trim()
    val extension = displayName.substringAfterLast('.', "").takeIf { it.isNotEmpty() }
    val requestedBase = trimmed.substringBeforeLast('.', trimmed).ifBlank { trimmed }
    val newDisplayName = if (extension == null) requestedBase else "$requestedBase.$extension"

    if (newDisplayName.isBlank() || newDisplayName == displayName) return false

    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, newDisplayName)
        put(MediaStore.Audio.AudioColumns.TITLE, requestedBase)
    }

    val direct = runCatching {
        context.contentResolver.update(song.uri, values, null, null)
    }.getOrDefault(0)
    if (direct > 0) return true

    val fallback = runCatching {
        context.contentResolver.update(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            values,
            "${MediaStore.Audio.Media._ID}=?",
            arrayOf(song.id.toString()),
        )
    }.getOrDefault(0)
    return fallback > 0
}

private fun deleteCurrentSong(context: Context, song: Song): Boolean = try {
    context.contentResolver.delete(song.uri, null, null) > 0
} catch (_: Exception) {
    val path = song.uri.path ?: return false
    try { File(path).delete() } catch (_: Exception) { false }
}

private fun requestAudioExtraction(context: Context, url: String, onSaved: () -> Unit) {
    val mainHandler = Handler(Looper.getMainLooper())
    Toast.makeText(context, "Converting to MP3…", Toast.LENGTH_SHORT).show()

    Thread {
        var savedUri: android.net.Uri? = null
        var tempFile: File? = null
        try {
            val cleanUrl = url.trim()
            if (cleanUrl.isBlank()) throw IllegalArgumentException("Enter a YouTube URL")

            val infoUrl = "https://api.vevioz.com/api/v1/info?url=" + URLEncoder.encode(cleanUrl, "UTF-8")
            val infoConnection = (URL(infoUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 45_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Zmusic/1.0 (Android)")
            }
            val infoText: String
            try {
                val status = infoConnection.responseCode
                infoText = readHttpResponse(infoConnection)
                if (status !in 200..299) {
                    throw IllegalStateException("Vevioz metadata request returned HTTP " + status + ": " + responseMessage(infoText))
                }
            } finally {
                infoConnection.disconnect()
            }

            val info = JSONObject(infoText)
            val infoError = findJsonString(info, setOf("error", "message", "detail"))
            if (!info.optBoolean("success", true)) {
                throw IllegalStateException(infoError ?: "Vevioz could not read this video")
            }

            val formats = findJsonArray(info, setOf("formats", "available_formats", "availableFormats", "audio_formats", "audioFormats"))
            var formatId = ""
            if (formats != null) {
                var foundMp3 = false
                for (i in 0 until formats.length()) {
                    val item = formats.optJSONObject(i) ?: continue
                    val formatName = findJsonString(item, setOf("format", "type", "ext", "container", "mime_type", "mimeType")).orEmpty()
                    val itemId = findJsonString(item, setOf("format_id", "formatId", "id")).orEmpty()
                    if (formatName.contains("mp3", ignoreCase = true) || itemId.contains("mp3", ignoreCase = true)) {
                        foundMp3 = true
                        formatId = itemId
                        break
                    }
                }
                if (!foundMp3) throw IllegalStateException("Vevioz does not list an MP3 format for this video")
            }

            val jobRequest = JSONObject()
                .put("url", cleanUrl)
                .put("format", "mp3")
                .put("quality", "192")
            if (formatId.isNotBlank()) jobRequest.put("format_id", formatId)

            val jobConnection = (URL("https://api.vevioz.com/api/v1/jobs").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 45_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "Zmusic/1.0 (Android)")
            }
            val jobText: String
            try {
                jobConnection.outputStream.use { it.write(jobRequest.toString().toByteArray(Charsets.UTF_8)) }
                val status = jobConnection.responseCode
                jobText = readHttpResponse(jobConnection)
                if (status !in 200..299) {
                    throw IllegalStateException("Vevioz job request returned HTTP " + status + ": " + responseMessage(jobText))
                }
            } finally {
                jobConnection.disconnect()
            }

            var job = JSONObject(jobText)
            val jobId = findJsonString(job, setOf("job_id", "jobId", "job", "id")).orEmpty()
            var accessToken = findJsonString(job, setOf("access_token", "accessToken", "token")).orEmpty()
            var downloadUrl = findJsonString(job, setOf("download_url", "downloadUrl", "file_url", "fileUrl", "download_link", "downloadLink")).orEmpty()

            val deadline = System.currentTimeMillis() + 5 * 60 * 1000L
            while (downloadUrl.isBlank() && jobId.isNotBlank() && System.currentTimeMillis() < deadline) {
                val state = findJsonString(job, setOf("status", "state")).orEmpty().lowercase()
                if (state in setOf("failed", "error", "cancelled", "canceled")) {
                    throw IllegalStateException(findJsonString(job, setOf("error", "message", "detail")) ?: "Vevioz conversion failed")
                }
                Thread.sleep(1_500)
                val pollUrl = buildString {
                    append("https://api.vevioz.com/api/v1/jobs/")
                    append(URLEncoder.encode(jobId, "UTF-8"))
                    if (accessToken.isNotBlank()) append("?token=" + URLEncoder.encode(accessToken, "UTF-8"))
                }
                val pollConnection = (URL(pollUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 20_000
                    readTimeout = 45_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "Zmusic/1.0 (Android)")
                }
                val pollText: String
                try {
                    val statusCode = pollConnection.responseCode
                    pollText = readHttpResponse(pollConnection)
                    if (statusCode !in 200..299) {
                        throw IllegalStateException("Vevioz job check returned HTTP " + statusCode + ": " + responseMessage(pollText))
                    }
                } finally {
                    pollConnection.disconnect()
                }
                job = JSONObject(pollText)
                val latestToken = findJsonString(job, setOf("access_token", "accessToken", "token"))
                if (!latestToken.isNullOrBlank()) accessToken = latestToken
                downloadUrl = findJsonString(job, setOf("download_url", "downloadUrl", "file_url", "fileUrl", "download_link", "downloadLink")).orEmpty()
            }
            if (downloadUrl.isBlank()) {
                throw IllegalStateException(
                    if (jobId.isBlank()) findJsonString(job, setOf("error", "message", "detail")) ?: "Vevioz did not return a download job ID"
                    else "MP3 conversion timed out before a download link was ready"
                )
            }

            val rawTitle = findJsonString(job, setOf("title", "filename", "file_name", "fileName"))
                ?: findJsonString(info, setOf("title", "name")) ?: "Zmusic download"
            val safeTitle = rawTitle.replace("\\", "_").replace(Regex("[/:*?\"<>|]"), "_")
                .trim().trim('.').take(100).ifBlank { "Zmusic download" }

            val resolvedDownloadUrl = URL(URL("https://api.vevioz.com/"), downloadUrl)
            if (resolvedDownloadUrl.protocol != "https" ||
                !resolvedDownloadUrl.host.equals("api.vevioz.com", ignoreCase = true) ||
                !resolvedDownloadUrl.path.startsWith("/api/v1/files/")) {
                throw IllegalStateException("Vevioz returned an unexpected download URL")
            }

            tempFile = File.createTempFile("zmusic-audio-", ".mp3", context.cacheDir)
            val downloadConnection = (resolvedDownloadUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 20_000
                readTimeout = 300_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "audio/mpeg, application/octet-stream, */*")
                setRequestProperty("User-Agent", "Zmusic/1.0 (Android)")
            }

            var downloadedBytes = 0L
            try {
                val status = downloadConnection.responseCode
                if (status !in 200..299) {
                    val body = readHttpResponse(downloadConnection)
                    throw IllegalStateException("MP3 download returned HTTP " + status + ": " + responseMessage(body))
                }
                val contentType = downloadConnection.contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
                if (contentType == "text/html" || contentType == "application/json" || contentType.startsWith("text/")) {
                    val body = readHttpResponse(downloadConnection)
                    throw IllegalStateException("Vevioz returned " + contentType + " instead of audio: " + responseMessage(body))
                }
                downloadConnection.inputStream.use { input ->
                    tempFile!!.outputStream().buffered().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (count > 0) {
                                output.write(buffer, 0, count)
                                downloadedBytes += count
                            }
                        }
                    }
                }
            } finally {
                downloadConnection.disconnect()
            }
            if (downloadedBytes <= 0L || tempFile!!.length() <= 0L) {
                throw IllegalStateException("Vevioz returned an empty MP3. Please try again.")
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, safeTitle + ".mp3")
                put(MediaStore.MediaColumns.MIME_TYPE, "audio/mpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/Zmusic")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            savedUri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Could not create the MP3 file in Music/Zmusic")

            var savedBytes = 0L
            context.contentResolver.openOutputStream(savedUri!!, "w")?.use { output ->
                tempFile!!.inputStream().buffered().use { input ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count > 0) {
                            output.write(buffer, 0, count)
                            savedBytes += count
                        }
                    }
                }
            } ?: throw IllegalStateException("Could not write the MP3 file")

            if (savedBytes <= 0L || savedBytes != downloadedBytes) {
                throw IllegalStateException("The MP3 was not saved completely. Please try again.")
            }

            val completed = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            context.contentResolver.update(savedUri!!, completed, null, null)
            mainHandler.post {
                Toast.makeText(context, "Saved to Music/Zmusic: " + safeTitle, Toast.LENGTH_LONG).show()
                onSaved()
            }
        } catch (error: Exception) {
            savedUri?.let { uri -> runCatching { context.contentResolver.delete(uri, null, null) } }
            val message = error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Unknown error"
            mainHandler.post {
                Toast.makeText(context, "Audio extraction failed: " + message, Toast.LENGTH_LONG).show()
            }
        } finally {
            tempFile?.let { runCatching { it.delete() } }
        }
    }.start()
}

private fun readHttpResponse(connection: HttpURLConnection): String {
    val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
    return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
}

private fun responseMessage(body: String): String {
    val cleaned = runCatching {
        val json = JSONObject(body)
        findJsonString(json, setOf("error", "message", "detail", "title"))
    }.getOrNull() ?: body
    return cleaned.replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(180)
        .ifBlank { "No details returned" }
}

private fun findJsonString(value: Any?, keys: Set<String>): String? {
    when (value) {
        is JSONObject -> {
            val iterator = value.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                val child = value.opt(key)
                if (key in keys && child != null && child != JSONObject.NULL &&
                    (child is String || child is Number)) {
                    val text = child.toString().trim()
                    if (text.isNotBlank()) return text
                }
            }
            val iterator2 = value.keys()
            while (iterator2.hasNext()) {
                val found = findJsonString(value.opt(iterator2.next()), keys)
                if (found != null) return found
            }
        }
        is org.json.JSONArray -> {
            for (i in 0 until value.length()) {
                val found = findJsonString(value.opt(i), keys)
                if (found != null) return found
            }
        }
    }
    return null
}

private fun findJsonArray(value: Any?, keys: Set<String>): org.json.JSONArray? {
    when (value) {
        is JSONObject -> {
            val iterator = value.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                val child = value.opt(key)
                if (key in keys && child is org.json.JSONArray) return child
            }
            val iterator2 = value.keys()
            while (iterator2.hasNext()) {
                val found = findJsonArray(value.opt(iterator2.next()), keys)
                if (found != null) return found
            }
        }
        is org.json.JSONArray -> {
            for (i in 0 until value.length()) {
                val found = findJsonArray(value.opt(i), keys)
                if (found != null) return found
            }
        }
    }
    return null
}
