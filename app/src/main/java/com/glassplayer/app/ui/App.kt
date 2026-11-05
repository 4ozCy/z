package com.glassplayer.app.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    val position by vm.position.collectAsState()
    val prefs = remember(context) { context.getSharedPreferences("zmusic_prefs", Context.MODE_PRIVATE) }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<SongGroup?>(null) }
    var detailIsArtist by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableIntStateOf(prefs.getInt("sortMode", 0)) }
    var renameDraft by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }

    val current = songs.firstOrNull { it.id == ui.currentId }
    val albums = remember(songs) { albumsOf(songs) }
    val artists = remember(songs) { artistsOf(songs) }
    val songsList = rememberLazyListState()
    val albumsGrid = rememberLazyGridState()
    val accent = Glass.Aqua

    val onRenameCurrent: () -> Unit = {
        if (current != null) {
            renameDraft = current.title
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
                    listOf(
                        Glass.Ink.copy(alpha = 0.96f),
                        Color(0xFF0c1322),
                    )
                )
            )
    ) {
        if (detail != null) {
            DetailScreen(
                group = detail!!,
                ui = ui,
                accent = accent,
                pad = PaddingValues(bottom = 120.dp),
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
                    pad = PaddingValues(bottom = 120.dp),
                    onPlay = vm::playQueue,
                    onShuffle = vm::playShuffled,
                )
                1 -> AlbumsScreen(
                    albums = albums,
                    state = albumsGrid,
                    pad = PaddingValues(bottom = 120.dp),
                    onOpen = { group ->
                        detailIsArtist = false
                        detail = group
                    },
                )
                2 -> ArtistsScreen(
                    artists = artists,
                    state = songsList,
                    pad = PaddingValues(bottom = 120.dp),
                    onOpen = { group ->
                        detailIsArtist = true
                        detail = group
                    },
                )
                3 -> SearchScreen(
                    songs = songs,
                    ui = ui,
                    accent = accent,
                    pad = PaddingValues(bottom = 120.dp),
                    onPlay = vm::playQueue,
                )
                else -> SettingsScreen(
                    songCount = songs.size,
                    sortMode = sortMode,
                    currentSong = current,
                    wallpaperUri = null,
                    onSortModeChange = {
                        sortMode = it
                        prefs.edit().putInt("sortMode", it).apply()
                    },
                    onRefresh = vm::refresh,
                    onRenameCurrent = onRenameCurrent,
                    onDeleteCurrent = onDeleteCurrent,
                    onExtractAudio = onExtractAudio,
                    onPickWallpaper = {},
                    onClearWallpaper = {},
                    pad = PaddingValues(bottom = 120.dp),
                )
            }
        }

        Dock(
            tab = tab,
            onTab = { tab = it },
            current = current,
            ui = ui,
            position = remember { androidx.compose.runtime.State { position } },
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

    if (showRenameDialog && current != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { GText("Rename file") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        if (renameDraft.isNotBlank()) {
                            // No-op placeholder to keep the project compiling.
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private fun deleteCurrentSong(context: Context, song: Song): Boolean {
    val uri = song.uri
    val path = uri.path ?: return false
    return try {
        File(path).delete()
    } catch (_: Exception) {
        false
    }
}

private fun requestAudioExtraction(context: Context, url: String) {
    // No-op placeholder. The app can later wire this to Termux/ytdlp.
}

private val tint = Color(0xFF7B6CFF)

private fun createWallpaperUri(): Uri? = null
