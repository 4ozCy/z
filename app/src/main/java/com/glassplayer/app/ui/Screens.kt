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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
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
    Column(modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        GText(title, size = 36.sp, weight = FontWeight.ExtraBold, tracking = (-1).sp)
        GText(subtitle, size = 14.sp, color = Glass.TextLo, modifier = Modifier.padding(top = 2.dp))
        if (actions != null) {
            Row(
                Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { actions() }
        }
    }
}

@Composable
fun EmptyNote(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GText(title, size = 18.sp, weight = FontWeight.SemiBold, align = TextAlign.Center)
        GText(body, size = 14.sp, color = Glass.TextLo, maxLines = 3, align = TextAlign.Center)
    }
}

@Composable
fun SongRow(
    song: Song,
    active: Boolean,
    playing: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (active) Modifier.glassCard(shape, accent, 1.3f) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Artwork(song, Modifier.size(52.dp), RoundedCornerShape(14.dp))
        Column(Modifier.weight(1f)) {
            GText(
                song.title,
                size = 16.sp,
                weight = FontWeight.SemiBold,
                color = if (active) accent else Glass.TextHi,
            )
            GText(song.artist, size = 13.sp, color = Glass.TextLo, modifier = Modifier.padding(top = 1.dp))
        }
        if (active && playing) {
            EqBars(accent, Modifier.padding(end = 6.dp))
        } else {
            GText(formatTime(song.durationMs), size = 13.sp, color = Glass.TextLo, modifier = Modifier.padding(end = 6.dp))
        }
    }
}

@Composable
fun PermissionScreen(onGrant: () -> Unit) {
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxWidth()
                .glassCard(RoundedCornerShape(36.dp), strength = 1.2f)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.LibraryMusic, null, tint = Glass.Aqua, modifier = Modifier.size(44.dp))
            GText("Your music, on this phone", size = 24.sp, weight = FontWeight.Bold, align = TextAlign.Center, maxLines = 2, tracking = (-0.4).sp)
            GText(
                "Zmusic reads the audio files stored on your device to build your library. Nothing is uploaded.",
                size = 15.sp, color = Glass.TextLo, maxLines = 4, align = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            GlassPillButton("Allow music access", Icons.Rounded.PlayArrow, onGrant, tint = Glass.Aqua)
        }
    }
}

// ---- tabs ------------------------------------------------------------------------------------

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
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        state = state,
        contentPadding = pad,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            ScreenHeader(
                "Songs",
                if (loaded) songCount(songs.size) else "Scanning your library…",
            ) {
                if (songs.isNotEmpty()) {
                    GlassPillButton("Play all", Icons.Rounded.PlayArrow, { onPlay(songs, 0) }, tint = accent)
                    GlassPillButton("Shuffle", Icons.Rounded.Shuffle, { onShuffle(songs) })
                }
            }
        }
        if (loaded && songs.isEmpty()) {
            item { EmptyNote("No music found", "Copy some songs to this phone and reopen Zmusic.") }
        }
        itemsIndexed(songs, key = { _, s -> s.id }) { i, s ->
            SongRow(s, s.id == ui.currentId, ui.isPlaying, accent) { onPlay(songs, i) }
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
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader("Albums", "${albums.size} in your library")
        }
        items(albums, key = { it.songs.first().id }) { album ->
            val shape = RoundedCornerShape(26.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .glassCard(shape)
                    .clickable { onOpen(album) }
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Artwork(album.songs.first(), Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(18.dp))
                GText(album.title, size = 15.sp, weight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
                GText(album.subtitle, size = 13.sp, color = Glass.TextLo, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
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
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Artwork(artist.songs.first(), Modifier.size(56.dp), CircleShape)
                Column(Modifier.weight(1f)) {
                    GText(artist.title, size = 16.sp, weight = FontWeight.SemiBold)
                    GText(artist.subtitle, size = 13.sp, color = Glass.TextLo, modifier = Modifier.padding(top = 2.dp))
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
                textStyle = TextStyle(color = Glass.TextHi, fontSize = 17.sp),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { inner ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .glassCard(CircleShape, strength = 1.2f)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = Glass.TextLo)
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) GText("Search your music", size = 17.sp, color = Glass.TextLo)
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
        item { Spacer(Modifier.height(12.dp)) }
        if (query.isNotBlank() && results.isEmpty()) {
            item { EmptyNote("Nothing matches “${query.trim()}”", "Check the spelling or try an artist name.") }
        }
        itemsIndexed(results, key = { _, s -> s.id }) { i, s ->
            SongRow(s, s.id == ui.currentId, ui.isPlaying, accent) { onPlay(results, i) }
        }
    }
}
