package com.glassplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassplayer.app.PlaybackUi
import com.glassplayer.app.data.Song

// ---- grouping --------------------------------------------------------------------------------

data class SongGroup(val title: String, val subtitle: String, val songs: List<Song>)

fun albumsOf(songs: List<Song>): List<SongGroup> =
    songs.groupBy { if (it.albumId > 0) "a${it.albumId}" else "n${it.album}" }
        .values
        .map { list ->
            val artists = list.map { it.artist }.distinct()
            SongGroup(
                title = list.first().album,
                subtitle = if (artists.size == 1) artists[0] else "Various artists",
                songs = list.sortedWith(compareBy({ it.track }, { it.title.lowercase() })),
            )
        }
        .sortedBy { it.title.lowercase() }

fun artistsOf(songs: List<Song>): List<SongGroup> =
    songs.groupBy { it.artist }
        .map { (artist, list) ->
            SongGroup(artist, songCount(list.size), list.sortedBy { it.title.lowercase() })
        }
        .sortedBy { it.title.lowercase() }

// ---- shared bits -----------------------------------------------------------------------------

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        GText(title, size = 24.sp, weight = FontWeight.SemiBold, tracking = (-0.35).sp)
        GText(subtitle, size = 13.sp, color = Glass.TextLo, modifier = Modifier.padding(top = 3.dp))
        if (actions != null) {
            Row(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { actions() }
        }
    }
}

@Composable
fun EmptyNote(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 36.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GText(title, size = 16.sp, weight = FontWeight.SemiBold, align = TextAlign.Center)
        GText(body, size = 13.sp, color = Glass.TextLo, maxLines = 3, align = TextAlign.Center)
    }
}

@Composable
fun SongRow(
    song: Song,
    active: Boolean,
    playing: Boolean,
    accent: Color,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (active) Modifier.glassCard(shape, accent, 0.9f) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Artwork(song, Modifier.size(40.dp), RoundedCornerShape(7.dp))
        Column(Modifier.weight(1f)) {
            GText(
                song.title,
                size = 14.sp,
                weight = FontWeight.Medium,
                color = if (active) accent else Glass.TextHi,
            )
            GText(song.artist, size = 12.sp, color = Glass.TextLo, modifier = Modifier.padding(top = 2.dp))
        }
        if (active && playing) {
            EqBars(accent, Modifier.padding(end = 2.dp))
        } else {
            GText(formatTime(song.durationMs), size = 12.sp, color = Glass.TextLo, modifier = Modifier.padding(end = 2.dp))
        }
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (isFavorite) "Remove from liked songs" else "Add to liked songs",
                tint = if (isFavorite) Glass.Coral else Glass.TextLo,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
fun PermissionScreen(onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .glassCard(RoundedCornerShape(20.dp), strength = 0.95f)
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.LibraryMusic, null, tint = Glass.Aqua, modifier = Modifier.size(36.dp))
            GText("Your music, on this phone", size = 20.sp, weight = FontWeight.Bold, align = TextAlign.Center, maxLines = 2, tracking = (-0.4).sp)
            GText(
                "Zmusic reads the audio files stored on your device to build your library. Nothing is uploaded.",
                size = 13.sp, color = Glass.TextLo, maxLines = 4, align = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            GlassPillButton("Allow music access", Icons.Rounded.PlayArrow, onGrant, tint = Glass.Aqua)
        }
    }
}

// ---- tabs ------------------------------------------------------------------------------------


@Composable
fun HomeScreen(
    songs: List<Song>,
    recentSongs: List<Song>,
    loaded: Boolean,
    ui: PlaybackUi,
    accent: Color,
    pad: PaddingValues,
    favoriteIds: Set<Long>,
    onOpenSongs: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenLiked: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
) {
    val featuredSong = recentSongs.firstOrNull() ?: songs.firstOrNull()
    val albumCount = remember(songs) { albumsOf(songs).size }
    val artistCount = remember(songs) { artistsOf(songs).size }
    val recentlyShown = if (recentSongs.isNotEmpty()) recentSongs.take(8) else songs.take(8)
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = pad,
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFFF7048), Color(0xFFB7472D)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = Color(0xFF0B1018), modifier = Modifier.size(23.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    GText("Z.", size = 27.sp, weight = FontWeight.Bold, tracking = (-1.3).sp)
                    GText("THE RECORD SHELF", size = 9.sp, color = Glass.TextLo, tracking = 1.0.sp)
                }
                GText(if (loaded) "${songs.size} tracks" else "Loading…", size = 12.sp, color = Glass.TextLo)
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(166.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF161F2A)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (featuredSong != null) {
                    Artwork(featuredSong, Modifier.width(136.dp).height(166.dp), RoundedCornerShape(0.dp))
                } else {
                    Box(Modifier.width(136.dp).height(166.dp).background(Color(0xFF202B38)))
                }
                Column(
                    Modifier
                        .weight(1f)
                        .height(166.dp)
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    GText("SIDE A / TONIGHT", size = 9.sp, weight = FontWeight.Bold, color = Color(0xFFFF7048), tracking = 1.0.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        GText(
                            featuredSong?.title ?: "Start with your library",
                            size = 16.sp,
                            weight = FontWeight.Bold,
                            maxLines = 2,
                            tracking = (-0.25).sp,
                        )
                        GText(
                            featuredSong?.artist ?: greeting,
                            size = 11.sp,
                            color = Glass.TextLo,
                            maxLines = 1,
                        )
                    }
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFFF7048), Color(0xFFF3B36A))))
                            .clickable {
                                if (featuredSong != null) {
                                    val index = songs.indexOfFirst { it.id == featuredSong.id }
                                    if (index >= 0) onPlay(songs, index)
                                } else onOpenSongs()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play featured song", tint = Color(0xFF0B1018), modifier = Modifier.size(23.dp))
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GText("Quick access", size = 17.sp, weight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HomeQuickAction("Liked", Icons.Rounded.Favorite, Color(0xFFF3B36A), Modifier.weight(1f), onOpenLiked)
                    HomeQuickAction("Albums", Icons.Rounded.Album, Color(0xFFFF7048), Modifier.weight(1f), onOpenAlbums)
                    HomeQuickAction("Artists", Icons.Rounded.Person, Color(0xFF78BCD1), Modifier.weight(1f), onOpenArtists)
                    HomeQuickAction("Equalizer", Icons.Rounded.GraphicEq, Color(0xFFFF7048), Modifier.weight(1f), onOpenEqualizer)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        GText(if (recentSongs.isNotEmpty()) "Recently played" else "Pick something to play", size = 17.sp, weight = FontWeight.SemiBold)
                        GText(
                            if (recentSongs.isNotEmpty()) "Get back to a familiar track" else "From your local collection",
                            size = 11.sp,
                            color = Glass.TextLo,
                        )
                    }
                    GText("See all", size = 12.sp, color = Color(0xFFFF7048), modifier = Modifier.clickable(onClick = onOpenSongs))
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(recentlyShown, key = { it.id }) { song ->
                        Row(
                            Modifier
                                .width(248.dp)
                                .height(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF161F2A))
                                .clickable {
                                    val index = songs.indexOfFirst { it.id == song.id }
                                    if (index >= 0) onPlay(songs, index)
                                }
                                .padding(7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Artwork(song, Modifier.size(50.dp), RoundedCornerShape(5.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                GText(song.title, size = 12.sp, weight = FontWeight.SemiBold, maxLines = 2)
                                GText(song.artist, size = 10.sp, color = Glass.TextLo, maxLines = 1)
                            }
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${song.title}", tint = Color(0xFFFF7048), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GText("Your library", size = 17.sp, weight = FontWeight.SemiBold)
                HomeLibraryRow("Songs", "${songs.size} tracks", Icons.Rounded.MusicNote, Color(0xFFFF7048), onOpenSongs)
                HomeLibraryRow("Albums", "$albumCount albums", Icons.Rounded.Album, Color(0xFF78BCD1), onOpenAlbums)
                HomeLibraryRow("Artists", "$artistCount artists", Icons.Rounded.Person, Color(0xFF78BCD1), onOpenArtists)
            }
        }
    }
}

@Composable
private fun HomeQuickAction(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .height(68.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161F2A))
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
        GText(title, size = 10.sp, weight = FontWeight.Medium, color = Glass.TextHi, maxLines = 1)
    }
}

@Composable
private fun HomeLibraryRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF161F2A))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            GText(title, size = 13.sp, weight = FontWeight.Medium)
            GText(subtitle, size = 10.sp, color = Glass.TextLo)
        }
        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = Glass.TextLo, modifier = Modifier.size(20.dp))
    }
}


@Composable
fun SongsScreen(
    songs: List<Song>,
    loaded: Boolean,
    ui: PlaybackUi,
    accent: Color,
    state: LazyListState,
    pad: PaddingValues,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    favoriteIds: Set<Long>,
    favoritesOnly: Boolean,
    onFavoritesOnlyChange: (Boolean) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    recentSongIds: Set<Long>,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var recentOnly by rememberSaveable { mutableStateOf(false) }

    val visibleSongs = remember(songs, favoriteIds, favoritesOnly, recentOnly, query, recentSongIds) {
        val source = when {
            favoritesOnly -> songs.filter { it.id in favoriteIds }
            recentOnly -> songs.filter { it.id in recentSongIds }
            else -> songs
        }
        val q = query.trim().lowercase()
        if (q.isBlank()) source else source.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        state = state,
        contentPadding = pad,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    GText("Your music", size = 27.sp, weight = FontWeight.Bold, tracking = (-0.65).sp)
                    GText(if (loaded) songCount(songs.size) else "Scanning your library…", size = 12.sp, color = Glass.TextLo)
                }
                IconButton(onClick = { showSearch = !showSearch; if (!showSearch) query = "" }) {
                    Icon(
                        if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                        contentDescription = if (showSearch) "Close search" else "Search your library",
                        tint = Glass.TextHi,
                    )
                }
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF161F2A))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                LibraryFilter("All", !favoritesOnly && !recentOnly, Modifier.weight(1f)) {
                    onFavoritesOnlyChange(false)
                    recentOnly = false
                }
                LibraryFilter("Favorites", favoritesOnly, Modifier.weight(1f)) {
                    onFavoritesOnlyChange(true)
                    recentOnly = false
                }
                LibraryFilter("Recent", recentOnly, Modifier.weight(1f)) {
                    onFavoritesOnlyChange(false)
                    recentOnly = true
                }
            }
        }
        if (showSearch) {
            item {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Glass.TextHi, fontSize = 14.sp),
                    cursorBrush = SolidColor(accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox = { inner ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color(0xFF161F2A))
                                .padding(horizontal = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Icon(Icons.Rounded.Search, null, tint = Glass.TextLo, modifier = Modifier.size(18.dp))
                            Box(Modifier.weight(1f)) {
                                if (query.isEmpty()) GText("Search songs, artists, albums", size = 13.sp, color = Glass.TextLo)
                                inner()
                            }
                            if (query.isNotEmpty()) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = Glass.TextLo,
                                    modifier = Modifier.clickable { query = "" }.padding(2.dp))
                            }
                        }
                    },
                )
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFF7048), Color(0xFFF3B36A))))
                    .clickable { if (visibleSongs.isNotEmpty()) onShuffle(visibleSongs) }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color(0xFF0B1018), modifier = Modifier.size(20.dp))
                GText("Shuffle play", size = 14.sp, weight = FontWeight.Bold, color = Color(0xFF0B1018))
                Spacer(Modifier.weight(1f))
                GText("${visibleSongs.size} songs", size = 11.sp, color = Color(0xFF0B1018).copy(alpha = 0.78f))
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFF0B1018), modifier = Modifier.size(21.dp))
            }
        }
        if (loaded && visibleSongs.isEmpty()) {
            item {
                EmptyNote(
                    when {
                        favoritesOnly -> "No liked songs yet"
                        recentOnly -> "Nothing played yet"
                        query.isNotBlank() -> "No matches found"
                        else -> "No music found"
                    },
                    when {
                        favoritesOnly -> "Tap the heart in the player or a song menu to save tracks here."
                        recentOnly -> "Play a song and it will appear in your recent list."
                        query.isNotBlank() -> "Try a different title, artist, or album."
                        else -> "Copy music to this phone and refresh your library."
                    },
                )
            }
        }
        itemsIndexed(visibleSongs, key = { _, song -> song.id }) { index, song ->
            LibraryTrackRow(
                song = song,
                active = song.id == ui.currentId,
                playing = ui.isPlaying,
                isFavorite = song.id in favoriteIds,
                accent = accent,
                onPlay = { onPlay(visibleSongs, index) },
                onToggleFavorite = { onToggleFavorite(song) },
            )
        }
    }
}

@Composable
private fun LibraryFilter(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(32.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (selected) Brush.linearGradient(listOf(Color(0xFFB7472D), Color(0xFF4B2A28))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GText(label, size = 12.sp, weight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = if (selected) Color(0xFF0B1018) else Glass.TextLo)
    }
}

@Composable
private fun LibraryTrackRow(
    song: Song,
    active: Boolean,
    playing: Boolean,
    isFavorite: Boolean,
    accent: Color,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) Color(0xFF1D2837) else Color.Transparent)
            .clickable(onClick = onPlay)
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Artwork(song, Modifier.size(42.dp), RoundedCornerShape(7.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            GText(song.title, size = 13.sp, weight = if (active) FontWeight.SemiBold else FontWeight.Medium, color = if (active) Color(0xFFFFD2B8) else Glass.TextHi)
            GText(song.artist, size = 11.sp, color = Glass.TextLo)
        }
        if (active && playing) {
            EqBars(accent, Modifier.size(width = 13.dp, height = 17.dp))
        } else {
            GText(formatTime(song.durationMs), size = 11.sp, color = Glass.TextLo)
        }
        Box {
            IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Song options", tint = Glass.TextLo, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (isFavorite) "Remove from Liked Songs" else "Add to Liked Songs") },
                    leadingIcon = {
                        Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = null)
                    },
                    onClick = {
                        onToggleFavorite()
                        menuOpen = false
                    },
                )
                DropdownMenuItem(
                    text = { Text("Play now") },
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                    onClick = {
                        onPlay()
                        menuOpen = false
                    },
                )
            }
        }
    }
}

@Composable
fun AlbumsScreen(
    albums: List<SongGroup>,
    state: LazyGridState,
    pad: PaddingValues,
    onOpen: (SongGroup) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = pad,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader("Albums", "${albums.size} in your library")
        }
        items(albums, key = { it.songs.first().id }) { album ->
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .glassCard(shape)
                    .clickable { onOpen(album) }
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Artwork(album.songs.first(), Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(8.dp))
                GText(album.title, size = 14.sp, weight = FontWeight.SemiBold, modifier = Modifier.padding(top = 7.dp, start = 2.dp))
                GText(album.subtitle, size = 12.sp, color = Glass.TextLo, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
            }
        }
    }
}

@Composable
fun ArtistsScreen(
    artists: List<SongGroup>,
    state: LazyListState,
    pad: PaddingValues,
    onOpen: (SongGroup) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        state = state,
        contentPadding = pad,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item { ScreenHeader("Artists", "${artists.size} in your library") }
        items(artists, key = { it.songs.first().id }) { artist ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(artist) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Artwork(artist.songs.first(), Modifier.size(40.dp), CircleShape)
                Column(Modifier.weight(1f)) {
                    GText(artist.title, size = 14.sp, weight = FontWeight.SemiBold)
                    GText(artist.subtitle, size = 12.sp, color = Glass.TextLo, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    songs: List<Song>,
    ui: PlaybackUi,
    accent: Color,
    pad: PaddingValues,
    onPlay: (List<Song>, Int) -> Unit,
    favoriteIds: Set<Long>,
    onToggleFavorite: (Song) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(songs, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else songs.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = pad,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item { ScreenHeader("Search", "Songs, artists and albums") }
        item {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Glass.TextHi, fontSize = 14.sp),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { inner ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .glassCard(RoundedCornerShape(12.dp), strength = 0.7f)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = Glass.TextLo)
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) GText("Search your music", size = 14.sp, color = Glass.TextLo)
                            inner()
                        }
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = Glass.TextLo,
                                modifier = Modifier.clip(CircleShape).clickable { query = "" }.padding(2.dp),
                            )
                        }
                    }
                },
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
        if (query.isNotBlank() && results.isEmpty()) {
            item { EmptyNote("Nothing matches “${query.trim()}”", "Check the spelling or try an artist name.") }
        }
        itemsIndexed(results, key = { _, s -> s.id }) { i, s ->
            SongRow(
                song = s,
                active = s.id == ui.currentId,
                playing = ui.isPlaying,
                accent = accent,
                isFavorite = s.id in favoriteIds,
                onToggleFavorite = { onToggleFavorite(s) },
                onClick = { onPlay(results, i) },
            )
        }
    }
}
