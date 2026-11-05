package com.glassplayer.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassplayer.app.PlayerViewModel
import com.glassplayer.app.audioPermission
import com.glassplayer.app.data.Song
import com.glassplayer.app.hasAudioPermission

private fun permissionsToAsk(): Array<String> =
    if (Build.VERSION.SDK_INT >= 33) arrayOf(audioPermission(), Manifest.permission.POST_NOTIFICATIONS)
    else arrayOf(audioPermission())

/** Tecno and other phones kill background apps hard. This asks, once, to be left alone while playing. */
private fun askBatteryExemptionOnce(context: Context) {
    val prefs = context.getSharedPreferences("zmusic", Context.MODE_PRIVATE)
    if (prefs.getBoolean("askedBattery", false)) return
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (pm.isIgnoringBatteryOptimizations(context.packageName)) return
    prefs.edit().putBoolean("askedBattery", true).apply()
    try {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun renameCurrentSong(context: Context, song: Song, newTitle: String): Boolean {
    val clean = newTitle.trim()
    if (clean.isEmpty()) return false
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentsContract.renameDocument(context.contentResolver, song.uri, clean) != null
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}

private fun deleteCurrentSong(context: Context, song: Song): Boolean =
    try {
        context.contentResolver.delete(song.uri, null, null) > 0
    } catch (e: Exception) {
        false
    }

@Composable
fun GlassPlayerApp(vm: PlayerViewModel) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Glass.Aqua,
            background = Glass.Ink,
            surface = Glass.Ink,
            onSurface = Glass.TextHi,
            onBackground = Glass.TextHi,
        )
    ) {
        val context = LocalContext.current
        val prefs = remember(context) { context.getSharedPreferences("zmusic", Context.MODE_PRIVATE) }
        var granted by remember { mutableStateOf(hasAudioPermission(context)) }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            granted = hasAudioPermission(context)
            if (granted) vm.refresh()
        }
        var wallpaperUri by remember {
            mutableStateOf(prefs.getString("wallpaperUri", null)?.let(Uri::parse))
        }
        val wallpaperPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {
                    // Some document providers do not offer persistable grants; keep the current session usable.
                }
                wallpaperUri = uri
                prefs.edit().putString("wallpaperUri", uri.toString()).apply()
            }
        }

        val songs by vm.songs.collectAsState()
        val loaded by vm.loaded.collectAsState()
        val ui by vm.ui.collectAsState()
        val position = vm.position.collectAsState()

        val current = remember(songs, ui.currentId) { songs.firstOrNull { it.id == ui.currentId } }
        val accent = remember(current?.album, current?.artist) { accentFor(current) }

        var sortMode by rememberSaveable { mutableIntStateOf(prefs.getInt("sortMode", 0).coerceIn(0, 2)) }
        val displaySongs = remember(songs, sortMode) {
            when (sortMode) {
                1 -> songs.sortedWith { a: Song, b: Song ->
                    a.artist.compareTo(b.artist, ignoreCase = true).takeIf { it != 0 }
                        ?: a.title.compareTo(b.title, ignoreCase = true)
                }
                2 -> songs.sortedWith { a: Song, b: Song ->
                    a.album.compareTo(b.album, ignoreCase = true).takeIf { it != 0 }
                        ?: a.title.compareTo(b.title, ignoreCase = true)
                }
                else -> songs.sortedWith { a: Song, b: Song ->
                    a.title.compareTo(b.title, ignoreCase = true)
                }
            }
        }
        val albums = remember(songs) { albumsOf(songs) }
        val artists = remember(songs) { artistsOf(songs) }

        var tab by rememberSaveable { mutableIntStateOf(0) }
        var playerOpen by rememberSaveable { mutableStateOf(false) }
        var detail by remember { mutableStateOf<SongGroup?>(null) }
        var detailIsArtist by remember { mutableStateOf(false) }
        var showRenameDialog by remember { mutableStateOf(false) }
        var renameDraft by remember(current) { mutableStateOf(current?.title ?: "") }

        val songsList = rememberLazyListState()
        val albumsGrid = rememberLazyGridState()
        val artistsList = rememberLazyListState()

        BackHandler(enabled = playerOpen) { playerOpen = false }
        BackHandler(enabled = !playerOpen && detail != null) { detail = null }
        LaunchedEffect(current == null) { if (current == null) playerOpen = false }
        LaunchedEffect(current != null) { if (current != null) askBatteryExemptionOnce(context) }

        val density = LocalDensity.current
        var dockHeight by remember { mutableStateOf(0.dp) }
        val topInset: Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val pad = remember(topInset, dockHeight, granted) {
            PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = topInset + 14.dp,
                bottom = if (granted) dockHeight + 28.dp else 0.dp,
            )
        }

        Box(Modifier.fillMaxSize().background(Glass.Ink)) {
            GlassBackdrop(accent, wallpaperUri)

            if (!granted) {
                PermissionScreen { launcher.launch(permissionsToAsk()) }
            } else {
                when (tab) {
                    0 -> SongsScreen(
                        displaySongs, loaded, ui, accent, songsList, pad,
                        onPlay = vm::playQueue, onShuffle = vm::playShuffled,
                    )
                    1 -> AlbumsScreen(albums, albumsGrid, pad) {
                        detailIsArtist = false
                        detail = it
                    }
                    2 -> ArtistsScreen(artists, artistsList, pad) {
                        detailIsArtist = true
                        detail = it
                    }
                    3 -> SearchScreen(displaySongs, ui, accent, pad, onPlay = vm::playQueue)
                    else -> SettingsScreen(
                        songCount = songs.size,
                        sortMode = sortMode,
                        currentSong = current,
                        onSortModeChange = {
                            sortMode = it
                            prefs.edit().putInt("sortMode", it).apply()
                        },
                        onRefresh = vm::refresh,
                        onRenameCurrent = {
                            if (current != null) {
                                renameDraft = current.title
                                showRenameDialog = true
                            }
                        },
                        onDeleteCurrent = {
                            if (current != null) {
                                val deleted = deleteCurrentSong(context, current)
                                if (deleted) vm.refresh()
                            }
                        },
                        onChooseWallpaper = { wallpaperPicker.launch(arrayOf("image/*")) },
                        onResetWallpaper = {
                            wallpaperUri = null
                            prefs.edit().remove("wallpaperUri").apply()
                        },
                        pad = pad,
                    )
                }

                AnimatedVisibility(
                    visible = detail != null,
                    enter = slideInHorizontally { it / 3 } + fadeIn(),
                    exit = slideOutHorizontally { it / 3 } + fadeOut(),
                ) {
                    detail?.let { group ->
                        DetailScreen(
                            group, ui, accent, pad, detailIsArtist,
                            onBack = { detail = null },
                            onPlay = vm::playQueue,
                            onShuffle = vm::playShuffled,
                        )
                    }
                }

                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(dockHeight + 36.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Glass.Ink.copy(alpha = 0.94f))))
                )

                Dock(
                    tab = tab,
                    onTab = { tab = it; detail = null },
                    current = current,
                    ui = ui,
                    position = position,
                    accent = accent,
                    onTogglePlay = vm::togglePlay,
                    onNext = vm::next,
                    onOpenPlayer = { playerOpen = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { dockHeight = with(density) { it.height.toDp() } },
                )
            }

            AnimatedVisibility(
                visible = playerOpen && current != null,
                enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = 380f)) { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                if (current != null) {
                    NowPlaying(
                        song = current,
                        ui = ui,
                        position = position,
                        accent = accent,
                        onClose = { playerOpen = false },
                        onTogglePlay = vm::togglePlay,
                        onPrevious = vm::previous,
                        onNext = vm::next,
                        onSeek = vm::seekTo,
                        onShuffle = vm::toggleShuffle,
                        onRepeat = vm::cycleRepeat,
                    )
                }
            }

            if (showRenameDialog && current != null) {
                AlertDialog(
                    onDismissRequest = { showRenameDialog = false },
                    title = { GText("Rename track") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GText("Update the current title for \"${current.title}\".", size = 14.sp, color = Glass.TextLo)
                            TextField(
                                value = renameDraft,
                                onValueChange = { renameDraft = it },
                                singleLine = true,
                                placeholder = { Text("New title") },
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val renamed = renameCurrentSong(context, current, renameDraft)
                                showRenameDialog = false
                                if (renamed) vm.refresh()
                            },
                        ) { Text("Rename") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
                    },
                )
            }
        }
    }
}
