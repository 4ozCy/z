package com.glassplayer.app.ui

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassplayer.app.PlayerViewModel
import com.glassplayer.app.data.Song
import com.glassplayer.app.hasAudioPermission
import java.io.File

@Composable
fun GlassPlayerApp(vm: PlayerViewModel) {
    val context = LocalContext.current
    val songs by vm.songs.collectAsState()
    val loaded by vm.loaded.collectAsState()
    val ui by vm.ui.collectAsState()
    val positionState = vm.position.collectAsState()
    val prefs = remember(context) { context.getSharedPreferences("zmusic_prefs", Context.MODE_PRIVATE) }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<SongGroup?>(null) }
    var detailIsArtist by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableIntStateOf(prefs.getInt("sortMode", 0)) }
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
    val albums = remember(songs) { albumsOf(songs) }
    val artists = remember(songs) { artistsOf(songs) }
    val songsList = rememberLazyListState()
    val albumsGrid = rememberLazyGridState()
    val accent = Glass.Aqua

    val onRenameCurrent: () -> Unit = {
        renameTarget?.let { song ->
            renameDraft = song.title
            showRenameDialog = true
        }
    }

    val onDeleteCurrent: () -> Unit = {
        if (current != null) {
            val deleted = deleteCurrentSong(context, current)
            if (deleted) vm.refresh()
        }
    }

    val onExtractAudio: (String) -> Unit = { url ->
        if (url.isNotBlank()) requestAudioExtraction(context, url)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF080808), Color(0xFF1C1C1C), Color(0xFF303030))
                )
            )
    ) {
        if (detail != null) {
            DetailScreen(
                group = detail!!,
                ui = ui,
                accent = accent,
                pad = PaddingValues(top = 28.dp, bottom = 100.dp),
                round = detailIsArtist,
                onBack = { detail = null },
                onPlay = vm::playQueue,
                onShuffle = vm::playShuffled,
            )
        } else {
            when (tab) {
                0 -> SongsScreen(
                    songs = songs,
                    loaded = loaded,
                    ui = ui,
                    accent = accent,
                    state = songsList,
                    pad = PaddingValues(top = 28.dp, bottom = 100.dp),
                    onPlay = vm::playQueue,
                    onShuffle = vm::playShuffled,
                )
                1 -> AlbumsScreen(
                    albums = albums,
                    state = albumsGrid,
                    pad = PaddingValues(top = 28.dp, bottom = 100.dp),
                    onOpen = { group ->
                        detailIsArtist = false
                        detail = group
                    },
                )
                2 -> ArtistsScreen(
                    artists = artists,
                    state = songsList,
                    pad = PaddingValues(top = 28.dp, bottom = 100.dp),
                    onOpen = { group ->
                        detailIsArtist = true
                        detail = group
                    },
                )
                3 -> SearchScreen(
                    songs = songs,
                    ui = ui,
                    accent = accent,
                    pad = PaddingValues(top = 28.dp, bottom = 100.dp),
                    onPlay = vm::playQueue,
                )
                else -> SettingsScreen(
                    songCount = songs.size,
                    sortMode = sortMode,
                    currentSong = renameTarget,
                    onSortModeChange = {
                        sortMode = it
                        prefs.edit().putInt("sortMode", it).apply()
                    },
                    onRefresh = vm::refresh,
                    onRenameCurrent = onRenameCurrent,
                    onDeleteCurrent = onDeleteCurrent,
                    onExtractAudio = onExtractAudio,
                    pad = PaddingValues(top = 28.dp, bottom = 100.dp),
                )
            }
        }

        Dock(
            tab = tab,
            onTab = { tab = it },
            current = current,
            ui = ui,
            position = positionState,
            accent = accent,
            onTogglePlay = vm::togglePlay,
            onNext = vm::next,
            onOpenPlayer = { if (current != null) detail = SongGroup(current.title, current.artist, listOf(current)) },
            modifier = Modifier.align(Alignment.BottomCenter),
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

private fun requestAudioExtraction(context: Context, url: String) {
    // No-op placeholder. The app can later wire this to Termux/ytdlp.
}
